package scenario;
import java.io.Console;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
        int nodes = 0;
        for (Zone zone : turf.zones) {
            addNode(zone, conditions.username, conditions.isNow);
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

    // Create a Node from a Zone
    private void addNode(Zone zone, String username, boolean isNow) {
        Node node = new Node(zone, username, isNow);
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

    // Remove redundant links/nodes (not used in any optimal routes)
    public void removeUnusedConnections() {
        log("Scenario: ** Removing unused connections...");
        Set<Node> unusedNodes = new HashSet<>(this.nodes);
        Set<Link> unusedLinks = new HashSet<>(this.links);
        for (Node node : this.nodes) {
            if (!node.isZone()) {
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
            if (!link.neighbor.isZone()) {
                routeSuccessors.put(link, new HashSet<>());
            }
        }
        for (Node node : this.nodes) {
            if (!node.isZone()) {
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
                    if (!current.link.parent.isZone()) {
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
                if (!x.isZone()) {
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
                if (!y.isZone()) {
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

    // Optimize connections that are too short to matter
    // Done by merging nodes connected with a two-way link shorter than the given length, and
    //   redistributing the length of the link to the new node's other links
    // This adds total distance to the Scenario, meaning the optimal solution may change and distant nodes
    //   may become unreachable
    // Does not remove links between two zones as they shouldn't be merged
    // If keepNames is true, the merged node's name will be a combination of the two nodes' names
    public void removeShortConnections(double minLength, boolean keepNames) {
        log("Scenario: ** Removing short connections...");

        // Find all two-way links that are too short
        List<Link> shortLinks = this.links.stream()
            .filter(link -> link.distance < minLength && link.reverse != null)
            .sorted((a, b) -> Double.compare(a.distance, b.distance))
            .toList();
        
        // Merge each link's nodes and redistribute its length to their other links
        for (Link mergeLink : shortLinks) {
            Node node1 = mergeLink.parent;
            Node node2 = mergeLink.neighbor;

            if (!node1.hasLinkTo(node2)) {
                continue; // link already removed
            }
            if (node1.isZone() && node2.isZone()) {
                continue; // don't merge links between zones
            }
            if (mergeLink.distance > minLength) {
                continue; // link has been re-extended beyond the limit by a previous merge
            }
            removeLinkPair(mergeLink);

            // Define the node to be kept and node to remove
            Node mergedNode = node1;
            Node removedNode = node2;
            if (node2.isZone()) {
                mergedNode = node2;
                removedNode = node1;
            }

            // Calculate distance to be redistributed
            boolean mergeToMiddle = !node1.isZone() && !node2.isZone();
            double redistribution = mergeLink.distance / 2.0;
            if (!mergeToMiddle) {
                redistribution = mergeLink.distance;
            }

            // Redistribute to mergeNode's links if merging to the middle
            if (mergeToMiddle) {
                for (Link outLink : mergedNode.out) {
                    outLink.distance += redistribution;
                }
                for (Link inLink : mergedNode.in) {
                    inLink.distance += redistribution;
                }
            }

            // Redistribute to removedNode's links, moving them to mergedNode in the process
            // If this results in creating a link that already exists (because a node is
            //   connected to both removedNode and mergedNode), keep the shortest version
            for (Link outLink : removedNode.out) {
                double newDistance = outLink.distance + redistribution;
                if (mergedNode.hasLinkTo(outLink.neighbor)) {
                    Link existingLink = mergedNode.getLinkTo(outLink.neighbor);
                    existingLink.distance = Math.min(existingLink.distance, newDistance);
                } else {
                    addLink(mergedNode, outLink.neighbor, newDistance);
                }
            }
            for (Link inLink : removedNode.in) {
                double newDistance = inLink.distance + redistribution;
                if (inLink.parent.hasLinkTo(mergedNode)) {
                    Link existingLink = inLink.parent.getLinkTo(mergedNode);
                    existingLink.distance = Math.min(existingLink.distance, newDistance);
                } else {
                    addLink(inLink.parent, mergedNode, newDistance);
                }
            }

            // Finally remove the second node
            removeNode(removedNode);
            
            // Do name handling if requested
            if (keepNames) {
                String mergedName = mergedNode.name + "/" + removedNode.name;
                log("Scenario: Removed short connection " + mergeLink.pairString() +
                    ", created " + mergedName);
                this.nodeName.remove(mergedNode.name);
                this.nodeName.put(mergedName, mergedNode);
                mergedNode.name = mergedName;
            } else {
                log("Scenario: Removed short connection " + mergeLink.pairString() +
                    ", merged into " + removedNode.name);
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
