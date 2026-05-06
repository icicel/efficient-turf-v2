package scenario;
import java.io.Console;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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

    /* Derived data */

    public double distanceLimit;

    // node names -> nodes
    private Map<String, Node> nodeName;

    // route cache, the result of findFastestRoutes() for each node
    public Map<Node, Map<Node, Route>> fastestRoutes;
    
    public Scenario(Turf turf, Conditions conditions) {
        log("Scenario: *** Initializing...");



        // Create a Node for each Point in the Turf
        this.nodes = new HashSet<>();
        this.nodeName = new HashMap<>();
        for (Point zone : turf.zones) {
            addNode(zone, conditions.username, conditions.isNow);
        }
        for (Point crossing : turf.crossings) {
            addNode(crossing, conditions.username, conditions.isNow);
        }
        log("Scenario: Created " + this.nodes.size() + " nodes");



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
        }
        log("Scenario: Created " + this.links.size() + " links");



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
                if (node == start || node == end) {
                    // start and end nodes should not be removed
                    continue;
                }
                node.isZone = false;
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



        // Recreate the entire scenario as a simplified graph with only zones
        log("Scenario: Optimizing...");
        Map<Node, Map<Node, Double>> edges = new HashMap<>();
        Set<Node> zones = new HashSet<>();
        for (Node node : this.nodes) {
            if (node.isZone) {
                zones.add(node);
            }
        }
        // Build graph
        for (Node zone : zones) {
            Map<Node, Route> fastestRoutes = findFastestRoutes(zone);
            Map<Node, Double> zoneEdges = new HashMap<>();
            edges.put(zone, zoneEdges);
            for (Node otherZone : zones) {
                if (zone == otherZone) {
                    continue;
                }
                Route route = fastestRoutes.get(otherZone);
                long zoneCount = route.getNodes().stream()
                    .filter(node -> node.isZone)
                    .count();
                // Only keep direct routes, that don't pass through any other zones
                if (zoneCount > 2) {
                    continue;
                }
                zoneEdges.put(otherZone, route.distance);
            }
        }



        // Remove *everything*!
        int before = this.nodes.size();
        for (Node node : this.nodes) {
            node.clear();
        }
        this.nodes = new HashSet<>();
        this.nodeName = new HashMap<>();
        this.links = new HashSet<>();
        this.fastestRoutes = new HashMap<>();
        // Recreate everything
        for (Node zone : zones) {
            this.nodes.add(zone);
            this.nodeName.put(zone.name, zone);
            for (Node neighbor : zones) {
                if (zone == neighbor) {
                    continue;
                }
                if (zone.hasLinkTo(neighbor)) {
                    continue;
                }
                double distance = edges.get(zone).get(neighbor);
                addLinkPair(zone, neighbor, distance);
            }
        }
        if (before > this.nodes.size()) {
            log("Scenario: Removed " + (before - this.nodes.size()) + " nodes");
        }



        // Regenerate routes
        log("Scenario: Caching routes...");
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
        this.links.remove(link.reverse);
        link.reverse.parent.out.remove(link.reverse);
        link.reverse.parent.outNodes.remove(link.reverse.neighbor);
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
            "links <node>\tView links\n" +
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
                case "links":
                    for (Link link : node.out) {
                        System.out.println("\t-> " + link.neighbor.name + " (" + link.distance + ")");
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
