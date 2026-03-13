package scenario;
import java.io.Console;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import turf.Connection;
import turf.Point;
import turf.Turf;
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
        int nodes = 0;
        for (Point zone : turf.zones) {
            addNode(zone, conditions.username, conditions.isNow);
            nodes++;
        }
        for (Point crossing : turf.crossings) {
            addNode(crossing, conditions.username, conditions.isNow);
            nodes++;
        }
        log("Scenario: Created " + nodes + " nodes");

        // Fill in other things
        this.start = getNode(conditions.start);
        this.end = getNode(conditions.end);
        this.timeLimit = conditions.timeLimit;
        this.speed = conditions.speed;
        this.distanceLimit = this.timeLimit * this.speed;

        // Create a Link for each Connection
        this.links = new HashSet<>();
        int links = 0;
        for (Connection connection : turf.connections) {
            Node leftNode = this.getNode(connection.left.name);
            Node rightNode = this.getNode(connection.right.name);
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

    // Create a Node from a Point
    private void addNode(Point point, String username, boolean isNow) {
        Node node = new Node(point, username, isNow);
        if (this.nodeName.containsKey(node.name)) {
            throw new RuntimeException("Duplicate node name: " + node.name);
        }
        this.nodes.add(node);
        this.nodeName.put(node.name, node);
    }

    // Create and return a (one-way) link between two nodes
    // Fails if there already is one between the nodes
    private Link addLink(Node from, Node to, double distance) {
        Link link = new Link(distance, from, to);
        this.links.add(link);
        return link;
    }

    // Add two links between two nodes, one in each direction
    // Fails if there already is one in either direction
    private void addLinkPair(Node node1, Node node2, double distance) {
        addLink(node1, node2, distance);
        addLink(node2, node1, distance);
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
        this.nodeName.remove(node.name);
        for (Link link : new LinkedList<>(node.in)) {
            removeLinkPair(link);
        }
        for (Link link : new LinkedList<>(node.out)) {
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
        link.parent.outNodes.remove(link.neighbor);
        link.neighbor.in.remove(link);
        link.neighbor.inNodes.remove(link.parent);
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
            Route startToNode = this.nodeFastestRoutes.get(this.start).get(node);
            Route nodeToEnd = this.nodeFastestRoutes.get(node).get(this.end);

            // The route start->node->end isn't possible at all
            if (startToNode == null || nodeToEnd == null) {
                unreachableNodes.add(node);
                continue;
            }

            // The route start->node->end isn't possible within time limit
            if (startToNode.length + nodeToEnd.length > this.distanceLimit) {
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
            if (node.isZone()) {
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
            if (!node.isZone()) {
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

    // Remove crossings entirely
    // Links between zones are instead the direct routes between them
    public void removeCrossings() {
        log("Scenario: ** Removing crossings...");

        // Remove all crossings
        List<Node> crossingNodes = this.nodes.stream()
            .filter(node -> !node.isZone())
            .toList();
        for (Node node : crossingNodes) {
            removeNode(node);
        }

        // All remaining links are now direct links between zones
        // Add the remaining direct routes as links
        for (Node node : this.nodes) {
            Map<Node, Route> directRoutes = this.nodeDirectRoutes.get(node);
            for (Node target : directRoutes.keySet()) {
                if (node.hasLinkTo(target)) {
                    // This link already exists
                    continue;
                }
                Route route = directRoutes.get(target);
                addLink(node, target, route.length);
            }
        }

        // Update routes
        updateRoutes();

        log("Scenario: ** Removal complete");
    }

    /* Debug */

    // Easily view the graph structure via console, quickly and dirtily
    public void nodeViewer() {
        Console in = System.console(); // BufferedReader requires Charset shenanigans
        System.out.println(
            "out <node>\tView outgoing links\n" +
            "in <node>\tView incoming links\n" +
            "links <node>\tView outgoing/incoming links\n" +
            "route <node> <node>\tView fastest route\n" +
            "routes <node>\tView all fastest routes"
        );
        String[] input;
        Node node;
        Node node2;
        while (true) {
            System.out.print("> ");
            input = in.readLine().split(" ");
            node = getNode(input[1]);
            if (node == null) {
                System.out.println("Node not found: " + input[1]);
                continue;
            }
            switch (input[0]) {
                case "out":
                    for (Link link : node.out) {
                        System.out.println("\t" + link.neighbor.name + " (" + link.distance + ")");
                    }
                    break;
                
                case "in":
                    for (Link link : node.in) {
                        System.out.println("\t" + link.parent.name + " (" + link.distance + ")");
                    }
                    break;
                
                case "links":
                    List<Node> outNeighbors = node.out.stream()
                        .map(link -> link.neighbor)
                        .toList();
                    List<Node> inNeighbors = node.in.stream()
                        .map(link -> link.parent)
                        .toList();
                    for (Link outLink : node.out) {
                        if (inNeighbors.contains(outLink.neighbor)) {
                            System.out.println("\t<-> " + outLink.neighbor.name + " (" + outLink.distance + ")");
                        } else {
                            System.out.println("\t-> " + outLink.neighbor.name + " (" + outLink.distance + ")");
                        }
                    }
                    for (Link inLink : node.in) {
                        if (!outNeighbors.contains(inLink.parent)) {
                            System.out.println("\t<- " + inLink.parent.name + " (" + inLink.distance + ")");
                        }
                    }
                    break;
                
                case "route":
                    node2 = getNode(input[2]);
                    if (node2 == null) {
                        System.out.println("Node not found: " + input[2]);
                        continue;
                    }
                    Route route = this.nodeFastestRoutes.get(node).get(node2);
                    if (route == null) {
                        System.out.println("\tRoute not found");
                        continue;
                    }
                    System.out.println("\t" + route + " (" + route.length + ")");
                    break;
                
                case "routes":
                    Map<Node, Route> routes = this.nodeFastestRoutes.get(node);
                    for (Route r : routes.values()) {
                        System.out.println("\t" + r + " (" + r.length + ")");
                    }
                    break;
                
                case "exit":
                    return;
            }
        }
    }
}
