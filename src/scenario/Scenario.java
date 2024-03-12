package scenario;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import turf.Connection;
import turf.Turf;
import turf.Zone;
import util.Logging;

// Represents a combination of a Turf object (Zone and Link data)
//   and a set of Conditions that specify the problem definition
public class Scenario extends Logging {

    /* Base Turf/Conditions data */

    public Set<Node> nodes;
    public Set<Link> links;

    public Node start;
    public Node end;
    public double timeLimit;
    public double speed;

    public Set<Node> priority;

    /* Derived date */

    public double distanceLimit;

    // node names -> nodes
    private Map<String, Node> nodeName;

    /* Route caches */
    
    // The result of Node.fastestRoutes() for each zone
    public Map<Node, Map<Node, Route>> nodeFastestRoutes;
    // Filtered version of nodeFastestRoutes, only containing fastest routes
    //  between two zones with no intermediate zones
    public Map<Node, Map<Node, Route>> nodeDirectRoutes;
    // The distance between each node and the end node
    public Map<Node, Double> nodeEndDistance;
    
    public Scenario(Turf turf, Conditions conditions) {
        log("Scenario: *** Initializing...");

        // Create a Node for each Zone in the Turf
        // Also create a temporary map from Zones to respective Nodes
        this.nodes = new HashSet<>();
        this.nodeName = new HashMap<>();
        Map<Zone, Node> childNode = new HashMap<>();
        int nodes = 0;
        for (Zone zone : turf.zones) {
            Node node = new Node(zone, conditions.username, conditions.infiniteRounds);
            this.nodes.add(node);
            this.nodeName.put(node.name, node);
            childNode.put(zone, node);
            nodes++;
        }
        log("Scenario: Created " + nodes + " nodes");

        // Fill in other things
        this.start = getNode(conditions.start);
        this.end = getNode(conditions.end);
        this.timeLimit = conditions.timeLimit;
        this.speed = conditions.speed;
        this.priority = getNodes(conditions.priority);
        this.distanceLimit = this.timeLimit * this.speed;

        // Create a Link for each Connection
        this.links = new HashSet<>();
        int links = 0;
        for (Connection connection : turf.connections) {
            Node leftNode = childNode.get(connection.left);
            Node rightNode = childNode.get(connection.right);
            Link leftLink = new Link(connection.distance, leftNode, rightNode);
            Link rightLink = new Link(connection.distance, rightNode, leftNode);
            leftLink.reverse = rightLink;
            rightLink.reverse = leftLink;
            this.links.add(leftLink);
            this.links.add(rightLink);
            links += 2;
        }
        log("Scenario: Created " + links + " links");

        // Apply white/blacklist
        if (conditions.whitelist != null) {
            log("Scenario: Applying whitelist...");
            Set<Node> safeNodes = getNodes(conditions.whitelist);
            for (Node node : this.nodes) {
                if (!safeNodes.contains(node)) {
                    removeNode(node);
                    log("Scenario: Removed unwhitelisted node " + node);
                }
            }
        } else if (conditions.blacklist != null) {
            log("Scenario: Applying blacklist...");
            for (Node node : getNodes(conditions.blacklist)) {
                removeNode(node);
                log("Scenario: Removed blacklisted node " + node);
            }
        }

        log("Scenario: Updating routes...");
        updateRoutes();

        log("Scenario: *** Initialized");
    }

    /* Utility functions */
    
    // Find a zone by name
    public Node getNode(String name) {
        return this.nodeName.get(name);
    }

    // Convert an array of names to a set of nodes
    public Set<Node> getNodes(String[] names) {
        Set<Node> nodes = new HashSet<>();
        if (names == null) {
            return nodes;
        }
        for (String name : names) {
            Node node = getNode(name);
            nodes.add(node);
        }
        return nodes;
    }

    // Completely remove all references to a Node, includes removing all Links to/from the Node
    private void removeNode(Node node) {
        if (node == null) {
            throw new RuntimeException("Tried to remove nonexistant node");
        }
        if (node == start || node == end) {
            throw new RuntimeException("Tried to remove start or end node");
        }
        this.nodes.remove(node);
        for (Link link : node.links) {
            removeLinkPair(link);
        }
    }

    // Completely remove all references to a Link (but not its reverse)
    private void removeLink(Link link) {
        if (link == null) {
            throw new RuntimeException("Tried to remove nonexistant link");
        }
        this.links.remove(link);
        link.parent.links.remove(link);
        if (link.reverse != null) {
            link.reverse.reverse = null;
        }
    }

    // Completely remove all references to a Link and its reverse (if it exists)
    // DOESN'T THROW ERROR if the reverse doesn't exist
    private void removeLinkPair(Link link) {
        removeLink(link);
        if (link.reverse != null) {
            removeLink(link.reverse);
        }
    }

    // Update graph information after changes
    private void updateRoutes() {

        // Check for unreachable nodes
        Set<Node> unreached = new HashSet<>(this.nodes);
        Queue<Link> frontier = new LinkedList<>(this.start.links);
        while (!frontier.isEmpty()) {
            Link link = frontier.remove();
            if (!unreached.contains(link.neighbor)) {
                continue;
            }
            unreached.remove(link.neighbor);
            frontier.addAll(link.neighbor.links);
        }
        for (Node node : unreached) {
            removeNode(node);
            log("Scenario: Removed unreachable node " + node);
        }

        // Update route caches
        this.nodeFastestRoutes = new HashMap<>();
        this.nodeDirectRoutes = new HashMap<>();
        this.nodeEndDistance = new HashMap<>();
        for (Node node : this.nodes) {
            Map<Node, Route> fastestRoutes = node.findFastestRoutes();
            this.nodeFastestRoutes.put(node, fastestRoutes);
            if (node.isZone) {
                this.nodeDirectRoutes.put(node, getDirectRoutes(fastestRoutes));
            }
            this.nodeEndDistance.put(node, fastestRoutes.get(this.end).length);
        }
    }

    // Assumes that the fastestRoutes is from a zone Node
    private Map<Node, Route> getDirectRoutes(Map<Node, Route> fastestRoutes) {
        Map<Node, Route> directRoutes = new HashMap<>();
        for (Node node : fastestRoutes.keySet()) {
            if (!node.isZone) {
                continue;
            }
            Route route = fastestRoutes.get(node);
            if (route.zones == 2) {
                directRoutes.put(node, route);
            }
        }
        return directRoutes;
    }

    /* Graph optimizations */

    // Remove redundant links/nodes (not used in any optimal routes)
    public void removeUnusedConnections() {
        log("Scenario: ** Removing unused connections...");
        Set<Node> unusedNodes = new HashSet<>(this.nodes);
        Set<Link> unusedLinks = new HashSet<>(this.links);
        for (Node node : this.nodes) {
            if (!node.isZone) {
                continue;
            }
            unusedNodes.remove(node);
            // Iterate through all fastest routes and remove the nodes and links from the unused sets
            for (Route route : nodeDirectRoutes.get(node).values()) {
                Route current = route;
                while (current != null) {
                    unusedNodes.remove(current.node);
                    unusedLinks.remove(current.link);
                    current = current.previous;
                }
            }
        }
        for (Node node : unusedNodes) {
            removeNode(node);
            log("Scenario: Removed unused node " + node);
        }
        for (Link link : unusedLinks) {
            if (!this.links.contains(link)) {
                continue; // this is just to not log an already removed link
            }
            removeLinkPair(link);
            log("Scenario: Removed unused link " + link.pairString());
        }

        log("Scenario: Updating routes...");
        updateRoutes();

        log("Scenario: ** Complete");
    }

    // remove crossings and redistribute if doing so reduces the amount of links
    //   without reducing connectivity
    // simplify if 2*(inlinks + outlinks) > inlinks * outlinks
    // when we say inlink and outlink we ignore links that aren't part of an optimal route
    public void simplifyCrossings() {
        // TODO
    }

    // rework cases where coming from a link A to a crossing, there is only one
    //   choice (link B) if following previously generated optimal routes
    //   by replacing A and B with a new link between A.parent and B.neighbor
    public void optimizeCrossings() {
        // TODO
    }

    // remove crossings entirely
    // links between zones are instead the optimal routes between them
    public void removeCrossings() {
        // TODO
    }

    // remove short connections
    public void removeShortConnections(double minLength) {
        // TODO
    }
}
