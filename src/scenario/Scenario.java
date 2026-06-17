package scenario;
import java.io.Console;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import turf.Point;
import turf.Turf;
import turf.Trail;
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


        // Create a Node for each zone and endpoint
        this.nodes = new HashSet<>();
        this.nodeName = new HashMap<>();
        for (Point zone : turf.zones) {
            addNode(zone, conditions.username, conditions.isNow);
        }
        for (Point crossing : turf.crossings) {
            if (crossing.name != conditions.start && crossing.name != conditions.end) {
                continue;
            }
            addNode(crossing, conditions.username, conditions.isNow);
        }
        // Setup points
        Set<Point> reachablePoints = new HashSet<>();
        Map<String, Point> pointName = new HashMap<>();
        for (Point point : turf.allPoints()) {
            reachablePoints.add(point);
            Point previous = pointName.put(point.name, point);
            if (previous != null) {
                throw new RuntimeException("Duplicate point name: " + point.name);
            }
        }


        log("Scenario: Created " + this.nodes.size() + " nodes");


        // Fill in other things
        this.start = getNode(conditions.start);
        this.end = getNode(conditions.end);
        if (this.start == null) {
            throw new RuntimeException("Start node not found: " + conditions.start);
        }
        if (this.end == null) {
            throw new RuntimeException("End node not found: " + conditions.end);
        }
        this.timeLimit = conditions.timeLimit;
        this.speed = conditions.speed;
        this.distanceLimit = this.timeLimit * this.speed;
        // Apply blacklist (done early since it affects the Turf traversal)
        int c = 0;
        if (conditions.blacklist != null) {
            for (String name : conditions.blacklist) {
                Point point = pointName.get(name);
                if (point == null) {
                    continue;
                }
                // Also remove node if it exists
                Node node = getNode(name);
                if (node != null) {
                    if (node == start || node == end) {
                        throw new RuntimeException("Blacklisted start/end node");
                    }
                    removeNode(node);
                }
                reachablePoints.remove(point);
                c++;
            }
            log("Scenario: Removed " + c + " blacklisted point" + s(c));
        }


        log("Scenario: Traversing Turf...");


        // Find all Points that can be visited within the time limit
        Point startPoint = start.ancestor;
        Point endPoint = end.ancestor;
        Map<Point, Double> startDistances = turf.distancesFrom(startPoint);
        Map<Point, Double> endDistances = turf.distancesFrom(endPoint);
        reachablePoints.removeIf(
            point -> startDistances.get(point) + endDistances.get(point) > this.distanceLimit
        );
        if (reachablePoints.isEmpty()) {
            throw new RuntimeException("End node is unreachable within time limit");
            // or rather, no nodes are reachable within time limit
        }
        // Check if blacklist has made Points unreachable
        Map<Point, Trail> startTrails = turf.trailsOverSubset(startPoint, reachablePoints);
        reachablePoints = startTrails.keySet();
        if (!reachablePoints.contains(endPoint)) {
            throw new RuntimeException("End node is unreachable with current blacklist");
        }
        // Remove unreachable nodes
        c = 0;
        for (Node node : new LinkedList<>(this.nodes)) {
            if (!reachablePoints.contains(node.ancestor)) {
                removeNode(node);
                c++;
            }
        }
        log("Scenario: Removed " + c + " unreachable node" + s(c));
        // Remove nodes that are crossings
        c = 0;
        for (Node node : new LinkedList<>(this.nodes)) {
            if (node == start || node == end) {
                continue;
            }
            if (!node.isZone()) {
                removeNode(node);
                c++;
            }
        }
        if (c > 0) {
            log("Scenario: Removed " + c + " non-zone node" + s(c));
        }
        // Apply greylist
        c = 0;
        if (conditions.greylist != null) {
            for (Node node : getNodes(conditions.greylist)) {
                if (node == start || node == end) {
                    // start and end nodes should not be removed
                    node.points = 0;
                } else {
                    // since it's a crossing now, just remove it
                    removeNode(node);
                }
                c++;
            }
            log("Scenario: Blanked " + c + " greylisted node" + s(c));
        }
        // Apply takenlist
        c = 0;
        if (conditions.takenlist != null) {
            Set<Node> taken = getNodes(conditions.takenlist);
            for (Node node : this.nodes) {
                if (taken.contains(node)) {
                    continue;
                }
                node.points *= 2;
                c++;
            }
            log("Scenario: Doubled " + c + " untaken node" + s(c));
        }


        log("Scenario: Building graph...");


        Set<Point> points = new HashSet<>();
        Set<Point> zonePoints = new HashSet<>();
        Map<Point, Node> descendant = new HashMap<>();
        for (Node node : this.nodes) {
            points.add(node.ancestor);
            descendant.put(node.ancestor, node);
            if (node.isZone()) {
                zonePoints.add(node.ancestor);
            }
        }
        // Find all trails between nodes
        Map<Point, Map<Point, Trail>> allTrails = new HashMap<>();
        c = 1;
        for (Point point : points) {
            System.out.print("Finding edges... (" + c++ + "/" + points.size() + ")\r");
            allTrails.put(point, turf.trailsOverSubset(point, reachablePoints));
        }
        // Collect all trails between nodes that don't veer too close to a zone
        Map<Node, Map<Node, Trail>> edges = new HashMap<>();
        c = 1;
        for (Point point1 : points) {
            System.out.print("Filtering edges... (" + c++ + "/" + points.size() + ")\r");
            Map<Point, Trail> trailsFrom1 = allTrails.get(point1);
            Map<Node, Trail> nodeEdges = new HashMap<>();
            for (Point point2 : points) {
                if (point1 == point2) {
                    continue;
                }
                // "Veering too close" to a third point means that the trail 1->3->2 is
                //  less than 25% longer or less than 50m longer than the trail 1->2
                boolean bad = false;
                Map<Point, Trail> trailsFrom2 = allTrails.get(point2);
                Trail trail12 = trailsFrom1.get(point2);
                for (Point point3 : zonePoints) {
                    if (point3 == point1 || point3 == point2) {
                        continue;
                    }
                    Trail trail13 = trailsFrom1.get(point3);
                    Trail trail23 = trailsFrom2.get(point3);
                    if (veerCheck(trail12.distance, trail13.distance, trail23.distance)) {
                        bad = true;
                        break;
                    }
                }
                if (bad) {
                    continue;
                }
                nodeEdges.put(descendant.get(point2), trail12);
            }
            edges.put(descendant.get(point1), nodeEdges);
        }


        log("Scenario: Creating links...");


        this.links = new HashSet<>();
        // Recreate edges as links
        for (Node zone : this.nodes) {
            for (Node neighbor : this.nodes) {
                if (zone == neighbor) {
                    continue;
                }
                Trail trail = edges.get(zone).get(neighbor);
                if (trail == null) {
                    continue;
                }
                addLink(trail, zone, neighbor);
            }
        }


        log("Scenario: Cleaning up...");


        // Regenerate routes
        this.fastestRoutes = new HashMap<>();
        c = 1;
        for (Node node : this.nodes) {
            System.out.print("Caching routes... (" + c++ + "/" + this.nodes.size() + ")\r");
            this.fastestRoutes.put(node, findFastestRoutes(node));
        }
        // Sanity check for one way links
        for (Link link : this.links) {
            if (!this.links.contains(link.reverse)) {
                throw new RuntimeException("One way link: " + link);
            }
        }


        log("Scenario: *** Initialized with " + this.nodes.size() + " nodes and " + this.links.size() + " links");
    }

    /* Utility functions */

    // s
    public String s(int n) {
        return n == 1 ? "" : "s";
    }

    // Check if 1->2 veers too close to 3 using the three distances involved
    public boolean veerCheck(double distance12, double distance13, double distance23) {
        double minMultVeerDelay = 1.25; // 25% longer
        double minAbsVeerDelay = 50; // 50m longer
        // Return true if sidetracking across 3 is not inefficient enough to warrant the
        //  direct edge from 1 to 2
        // However, this is fine if the same is true for one of the direct edges 1 to 3 or 2 to 3
        // Use absolute delay if it applies, otherwise the more lenient multiplicative delay
        if (distance13 + distance23 < distance12 + minAbsVeerDelay) {
            return !(
                distance12 + distance23 < distance13 + minAbsVeerDelay
                || distance12 + distance13 < distance23 + minAbsVeerDelay
            );
        }
        if (distance13 + distance23 < distance12 * minMultVeerDelay) {
            return !(
                distance12 + distance23 < distance13 * minMultVeerDelay
                || distance12 + distance13 < distance23 * minMultVeerDelay
            );
        }
        return false;
    }

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

    // Create a Node from a Point and return it
    private Node addNode(Point point, String username, boolean isNow) {
        Node node = new Node(point, username, isNow);
        if (this.nodeName.containsKey(node.name)) {
            throw new RuntimeException("Duplicate node name: " + node.name);
        }
        this.nodes.add(node);
        this.nodeName.put(node.name, node);
        return node;
    }

    // Add a link between two nodes following a trail
    // Fails if there already is one in either direction or if the trail doesn't match the nodes
    private void addLink(Trail trail, Node start, Node end) {
        Link link = new Link(trail, start, end);
        this.links.add(link);
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
