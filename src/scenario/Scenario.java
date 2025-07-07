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
            Node node = new Node(zone, conditions.username, conditions.isNow);
            this.nodes.add(node);
            if (this.nodeName.containsKey(node.name)) {
                throw new RuntimeException("Duplicate node name: " + node.name);
            }
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
            addLinkPair(leftNode, rightNode, connection.distance);
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

    // Add and return a link between two nodes
    private Link addLink(Node from, Node to, double distance) {
        Link link = new Link(distance, from, to);
        this.links.add(link);
        return link;
    }

    // Add two links between two nodes, one in each direction
    private void addLinkPair(Node node1, Node node2, double distance) {
        Link link1 = addLink(node1, node2, distance);
        Link link2 = addLink(node2, node1, distance);
        link1.reverse = link2;
        link2.reverse = link1;
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
        Queue<Link> toRemove = new LinkedList<>(node.in);
        for (Link link : toRemove) {
            removeLinkPair(link);
        }
    }

    // Completely remove all references to a Link (but not its reverse)
    private void removeLink(Link link) {
        if (link == null) {
            throw new RuntimeException("Tried to remove nonexistant link");
        }
        this.links.remove(link);
        link.parent.out.remove(link);
        link.neighbor.in.remove(link);
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

    /* Graph setup */

    // Update graph information after changes
    private void updateRoutes() {

        // Generate initial caches
        updateCaches();

        // Check for unreachable nodes
        Set<Node> distantNodes = new HashSet<>();
        Set<Node> unreachableNodes = new HashSet<>();
        for (Node node : nodes) {
            Map<Node, Route> fastestRoutes = this.nodeFastestRoutes.get(node);

            // The route start->node->end isn't possible at all
            if (fastestRoutes.get(this.end) == null || fastestRoutes.get(this.start) == null) {
                unreachableNodes.add(node);
                continue;
            }

            // The route start->node->end isn't possible within time limit
            double startToNode = fastestRoutes.get(this.start).length;
            double nodeToEnd = fastestRoutes.get(this.end).length;
            if (startToNode + nodeToEnd > this.distanceLimit) {
                distantNodes.add(node);
                continue;
            }
        }
        for (Node node : distantNodes) {
            removeNode(node);
            log("Scenario: Removed distant node " + node);
        }
        for (Node node : unreachableNodes) {
            removeNode(node);
            log("Scenario: Removed unreachable node " + node);
        }

        // Generate final caches
        updateCaches();
    }

    // Update route caches
    private void updateCaches() {
        this.nodeFastestRoutes = new HashMap<>();
        this.nodeDirectRoutes = new HashMap<>();
        this.nodeEndDistance = new HashMap<>();
        for (Node node : this.nodes) {
            Map<Node, Route> fastestRoutes = node.findFastestRoutes();
            this.nodeFastestRoutes.put(node, fastestRoutes);
            if (node.isZone) {
                this.nodeDirectRoutes.put(node, getDirectRoutes(fastestRoutes));
            }
            if (fastestRoutes.get(this.end) != null) {
                this.nodeEndDistance.put(node, fastestRoutes.get(this.end).length);
            } else {
                this.nodeEndDistance.put(node, null);
            }
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

        log("Scenario: ** Removal complete");
    }

    // remove crossings and redistribute if doing so reduces the amount of links
    //   without reducing connectivity
    // simplify if 2*(inlinks + outlinks) > inlinks * outlinks
    // when we say inlink and outlink we ignore links that aren't part of an optimal route
    public void simplifyCrossings() {
        // TODO
    }

    // Rework cases where coming from a link A to a crossing, there is only one
    //   reasonable choice of next link, B, if following previously generated optimal routes
    // This is done by replacing A with a new link between A.parent and B.neighbor, bypassing
    //   the crossing while keeping A.reverse and B intact
    // Using this optimization will therefore remove the guarantee that all links have a reverse
    public void optimizeCrossings() {
        log("Scenario: ** Optimizing crossings...");

        optimizeCrossings(1);
    
        log("Scenario: ** Optimization complete");
    }

    // Used to optimize over and over until no more optimizations can be made
    // There are definitely prettier ways to do this
    private void optimizeCrossings(int attempt) {

        // Generate link pairs from fastest routes
        // routeSuccessors stores, per link A->B where B is a crossing, all links B->C where A->B->C
        //   is part of any fastest route ("route successors" to A->B)
        log("Scenario: Generating link successors...");
        Map<Link, Set<Link>> routeSuccessors = new HashMap<>();
        for (Link link : this.links) {
            if (!link.neighbor.isZone) {
                routeSuccessors.put(link, new HashSet<>());
            }
        }
        for (Node node : this.nodes) {
            if (!node.isZone) {
                continue;
            }
            for (Route fastestRoute : this.nodeDirectRoutes.get(node).values()) {
                if (fastestRoute.previous == null) { // route of length 0
                    continue;
                }
                Route current = fastestRoute;
                // Iterate through the route, finding all link pairs A->B, B->C where B is a crossing
                // current.link is B->C, current.previous.link is A->B
                while (current.previous.link != null) {
                    if (!current.link.parent.isZone) {
                        routeSuccessors.get(current.previous.link).add(current.link);
                    }
                    current = current.previous;
                }
            }
        }

        // for every link x->Q in L:
        //   get the size of the set of route successors of x, L[x->Q]
        //   0 - remove x->Q
        //   1 - remove x->Q, create x->y
        //      if y is a crossing, create L[x->y] = L[Q->y]
        //      if x is a crossing, replace x->Q with x->y in L[w->x] for all w!=Q
        //   >1 - do nothing
        // (since w->x exists, "all w" is necessarily the same as "all neighbors of x w")
        log("Scenario: Combining links, attempt " + attempt + "...");
        Queue<Link> successorQueue = new LinkedList<>(routeSuccessors.keySet());
        boolean changed = false;
        while (!successorQueue.isEmpty()) {
            Link x_Q = successorQueue.remove();
            if (!this.links.contains(x_Q)) {
                // the link was removed
                continue;
            }

            Node x = x_Q.parent;
            Node Q = x_Q.neighbor; // Q is a crossing
            Set<Link> successors = routeSuccessors.get(x_Q);

            if (successors.size() == 0) {
                changed = true;
                // remove x->Q
                removeLink(x_Q);
                log("Scenario: Removed link " + x_Q);

            } else if (successors.size() == 1) {
                changed = true;
                // replace x->Q with x->y
                Link Q_y = successors.iterator().next();
                Node y = Q_y.neighbor;
                Link x_y = addLink(x, y, x_Q.distance + Q_y.distance);
                removeLink(x_Q);
                log("Scenario: Added link " + x_y + " bypassing " + Q);
                // update routeSuccessors
                if (!x.isZone) {
                    for (Link w_x : x.in) {
                        Node w = w_x.parent;
                        if (w == Q) {
                            continue;
                        }
                        if (routeSuccessors.get(w_x).remove(x_Q)) {
                            routeSuccessors.get(w_x).add(x_y);
                        }
                    }
                }
                if (!y.isZone) {
                    routeSuccessors.put(x_y, routeSuccessors.get(Q_y));
                    successorQueue.add(x_y);
                }
            }
        }

        log("Scenario: Updating routes...");
        updateRoutes();

        if (changed) {
            optimizeCrossings(attempt + 1);
        }
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
