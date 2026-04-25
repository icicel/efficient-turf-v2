package scenario;
import java.io.Console;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import turf.Connection;
import turf.Point;
import turf.Turf;
import util.Logging;

// Represents a combination of a Turf object (Zone and Link data)
//   and a set of Conditions that specify the problem definition
// Contains no crossings
public class Scenario extends Logging {

    /* Base Turf/Conditions data */

    public Set<Node> nodes;
    public Set<Link> links;

    public Node start;
    public Node end;
    public double timeLimit;
    public double speed;

    public Set<Node> priority;

    /* Derived data */

    public double distanceLimit;

    // node names -> nodes
    private Map<String, Node> nodeName;

    // route cache, the result of findFastestRoutes() for each node
    public Map<Node, Map<Node, Route>> fastestRoutes;
    
    public Scenario(Turf turf, Conditions conditions) {
        if (!turf.compressed) {
            // Not compressing hurts Solver performance, for some reason
            turf.compress();
        }
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
        if (this.start == null) {
            throw new RuntimeException("Start node not found: " + conditions.start);
        }
        if (this.end == null) {
            throw new RuntimeException("End node not found: " + conditions.end);
        }
        // Override
        this.start.isZone = true;
        this.end.isZone = true;

        // Create a Link for each Connection
        this.links = new HashSet<>();
        int links = 0;
        for (Connection connection : turf.connections) {
            Node leftNode = this.getNode(connection.left.toString());
            Node rightNode = this.getNode(connection.right.toString());
            if (leftNode == rightNode) {
                // Ignore loop
                continue;
            }
            if (leftNode.hasLinkTo(rightNode)) {
                // Update duplicate link if it's shorter than the existing one
                Link existingLink = leftNode.getLinkTo(rightNode);
                if (connection.distance < existingLink.distance) {
                    existingLink.distance = connection.distance;
                    existingLink.reverse.distance = connection.distance;
                }
                continue;
            }
            addLinkPair(leftNode, rightNode, connection.distance);
            links += 2;
        }
        log("Scenario: Created " + links + " links");



        log("Scenario: Applying conditions...");
        // Apply blacklist
        if (conditions.blacklist != null) {
            for (Node node : getNodes(conditions.blacklist)) {
                removeNode(node);
                log("Scenario: Removed blacklisted node " + node);
            }
        }
        // Apply greylist
        if (conditions.greylist != null) {
            for (Node node : getNodes(conditions.greylist)) {
                if (!node.isZone) {
                    continue;
                }
                if (node != start && node != end) {
                    // start and end nodes should not be removed
                    node.isZone = false;
                }
                node.points = 0;
                log("Scenario: Blanked greylisted zone " + node);
            }
        }
        // Apply redlist
        if (conditions.redlist != null) {
            for (Node node : getNodes(conditions.redlist)) {
                if (node.points == 0) {
                    continue;
                }
                node.points /= 2; // integer division
                log("Scenario: Halved redlisted zone " + node);
            }
        }
        // Remove unreachable nodes based on conditions (time limit and so on)
        removeUnreachableNodes();



        // Remove all crossings (zero-point nodes) and replace with direct links between zones
        log("Scenario: Optimizing...");
        // Generate routes
        this.fastestRoutes = new HashMap<>();
        for (Node node : this.nodes) {
            if (!node.isZone) {
                continue;
            }
            Map<Node, Route> fastestRoutes = findFastestRoutes(node);
            this.fastestRoutes.put(node, fastestRoutes);
        }
        // Remove crossings
        List<Node> crossingNodes = this.nodes.stream()
            .filter(node -> !node.isZone)
            .toList();
        for (Node node : crossingNodes) {
            removeNode(node);
        }
        if (crossingNodes.size() > 0) {
            log("Scenario: Removed " + crossingNodes.size() + " crossings");
        }
        // All remaining links are now direct links between zones
        // Add all routes that don't pass over intermediate zones as links
        // ("Direct" routes)
        for (Node node : this.nodes) {
            for (Node target : this.nodes) {
                if (node == target || node.hasLinkTo(target)) {
                    // This link already exists
                    continue;
                }
                Route route = this.fastestRoutes.get(node).get(target);
                // If route passes through an intermediate zone, it can't be a direct link
                // Start iterating just after target, go backwards, end before reaching node
                boolean hasIntermediateZone = false;
                Route current = route.previous;
                while (current.previous.previous != null) {
                    if (current.node.isZone) {
                        hasIntermediateZone = true;
                        break;
                    }
                    current = current.previous;
                }
                if (!hasIntermediateZone) {
                    addLinkPair(node, target, route.distance);
                }
            }
        }



        // Regenerate routes
        log("Scenario: Caching routes...");
        this.fastestRoutes = new HashMap<>();
        for (Node node : this.nodes) {
            Map<Node, Route> fastestRoutes = findFastestRoutes(node);
            this.fastestRoutes.put(node, fastestRoutes);
        }

        log("Scenario: *** Initialized with " + this.nodes.size() + " nodes and " + this.links.size() + " links");
    }

    /* Utility functions */
    
    // Find a zone by name
    public Node getNode(String name) {
        return this.nodeName.get(name);
    }

    // Convert an array of names to a set of nodes
    // Ignores nonexistant node names
    public Set<Node> getNodes(String[] names) {
        Set<Node> nodes = new HashSet<>();
        if (names == null) {
            return nodes;
        }
        for (String name : names) {
            Node node = getNode(name);
            if (node == null) {
                continue;
            }
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

    // Add two links between two nodes, one in each direction
    // Fails if there already is one in either direction
    private void addLinkPair(Node node1, Node node2, double distance) {
        Link link1 = new Link(distance, node1, node2);
        Link link2 = new Link(distance, node2, node1);
        this.links.add(link1);
        this.links.add(link2);
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

    // Completely remove all references to a Link and its reverse
    private void removeLinkPair(Link link) {
        if (link == null) {
            throw new RuntimeException("Tried to remove nonexistant link");
        }
        this.links.remove(link);
        link.parent.out.remove(link);
        link.parent.outNodes.remove(link.neighbor);
        link.neighbor.in.remove(link);
        link.neighbor.inNodes.remove(link.parent);
        this.links.remove(link.reverse);
        link.reverse.parent.out.remove(link.reverse);
        link.reverse.parent.outNodes.remove(link.reverse.neighbor);
        link.reverse.neighbor.in.remove(link.reverse);
        link.reverse.neighbor.inNodes.remove(link.reverse.parent);
    }

    /* Graph maintenance */

    // Clean graph of unreachable nodes
    private void removeUnreachableNodes() {

        // Create route tree from start and end
        Map<Node, Route> startRoutes = findFastestRoutes(this.start);
        Map<Node, Route> endRoutes = findFastestRoutes(this.end);

        // Sanity check
        if (startRoutes.get(this.end) == null) {
            throw new RuntimeException("Start and end nodes are not connected");
        }

        // Check for unreachable nodes
        Set<Node> distantNodes = new HashSet<>();
        Set<Node> unreachableNodes = new HashSet<>();
        int distantZones = 0;
        int unreachableZones = 0;
        for (Node node : nodes) {
            Route startToNode = startRoutes.get(node);
            Route nodeToEnd = endRoutes.get(node);

            // The route start->node->end isn't possible at all
            if (startToNode == null || nodeToEnd == null) {
                unreachableNodes.add(node);
                if (node.isZone) {
                    unreachableZones++;
                }
                continue;
            }

            // The route start->node->end isn't possible within time limit
            if (startToNode.distance + nodeToEnd.distance > this.distanceLimit) {
                distantNodes.add(node);
                if (node.isZone) {
                    distantZones++;
                }
                continue;
            }
        }

        for (Node node : distantNodes) {
            removeNode(node);
        }
        if (distantNodes.size() > 0) {
            log("Scenario: Removed " + distantNodes.size() + " distant nodes" +
                " (including " + distantZones + " zones)");
        }

        for (Node node : unreachableNodes) {
            removeNode(node);
        }
        if (unreachableNodes.size() > 0) {
            log("Scenario: Removed " + unreachableNodes.size() + " unreachable nodes" +
                " (including " + unreachableZones + " zones)");
        }
    }

    // Returns the shortest Route to every other Node
    // This is done by keeping a priority queue of all Nodes neighboring
    //  already visited Nodes, (in the form of Routes) and extending them
    //  when visiting a new Node (AKA Dijkstra's)
    public Map<Node, Route> findFastestRoutes(Node start) {
        Map<Node, Route> fastestRoutes = new HashMap<>();
        PriorityQueue<Route> queue = new PriorityQueue<>(
            (a, b) -> Double.compare(a.distance, b.distance)
        );
        Set<Node> visited = new HashSet<>();
        queue.add(new Route(start));
        while (!queue.isEmpty()) {
            // Get the shortest Route from the queue
            Route route = queue.remove();
            Node neighbor = route.node;
            if (visited.contains(neighbor)) {
                continue;
            }
            visited.add(neighbor);
            fastestRoutes.put(neighbor, route);

            // Extend the Route with all outgoing Links from the neighbor
            //  and add them to the queue
            for (Link link : neighbor.out) {
                queue.add(new Route(route, link));
            }
        }
        return fastestRoutes;
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
                    Route route = this.fastestRoutes.get(node).get(node2);
                    if (route == null) {
                        System.out.println("\tRoute not found");
                        continue;
                    }
                    System.out.println("\t" + route + " (" + route.distance + ")");
                    break;
                
                case "routes":
                    Map<Node, Route> routes = this.fastestRoutes.get(node);
                    for (Route r : routes.values()) {
                        System.out.println("\t" + r + " (" + r.distance + ")");
                    }
                    break;
                
                case "exit":
                    return;
            }
        }
    }
}
