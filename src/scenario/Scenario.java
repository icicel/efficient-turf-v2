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

    // Base Turf/Conditions data

    public Set<Node> nodes;
    public Set<Link> links;

    public Node start;
    public Node end;
    public double timeLimit;

    public double speed;
    public double waitTime;

    public Set<Node> priority;

    // node names -> nodes
    private Map<String, Node> nodeName;
    
    // For storing the result of Node.fastestRoutes() for each zone
    private Map<Node, Map<Node, Route>> nodeFastestRoutes;
    
    public Scenario(Turf turf, Conditions conditions) {
        log("Scenario: Initializing...");

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

        // Fill in other things
        this.start = getNode(conditions.start);
        this.end = getNode(conditions.end);
        this.timeLimit = conditions.timeLimit;
        this.speed = conditions.speed;
        this.waitTime = conditions.waitTime;
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
        this.priority = getNodes(conditions.priority);

        // Remove unreachable nodes
        log("Scenario: Checking reachability...");
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

        log("Scenario: Updating fastest routes...");
        updateFastestRoutes();

        log("Scenario: Initialized");
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
            return;
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
            return;
        }
        this.links.remove(link);
        link.parent.links.remove(link);
    }

    // Completely remove all references to a Link and its reverse
    private void removeLinkPair(Link link) {
        if (link == null) {
            return;
        }
        removeLink(link);
        removeLink(link.reverse);
    }

    // Update nodeFastestRoutes
    private void updateFastestRoutes() {
        this.nodeFastestRoutes = new HashMap<>();
        for (Node node : this.nodes) {
            if (!node.isZone) {
                continue;
            }
            this.nodeFastestRoutes.put(node, node.findFastestRoutes());
        }
    }

    /* Graph optimizations */

    // Remove redundant links/nodes (not used in any optimal routes)
    public void removeUnusedConnections() {
        log("Scenario: Removing unused connections...");
        Set<Node> unusedNodes = new HashSet<>(this.nodes);
        Set<Link> unusedLinks = new HashSet<>(this.links);
        for (Node node : this.nodes) {
            if (!node.isZone) {
                continue;
            }
            unusedNodes.remove(node);
            // Iterate through all fastest routes and remove the nodes and links from the unused sets
            for (Route route : nodeFastestRoutes.get(node).values()) {
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
        log("Scenario: Updating fastest routes...");
        updateFastestRoutes();
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
