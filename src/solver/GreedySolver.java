package solver;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import scenario.Node;
import scenario.Route;
import scenario.Scenario;

// Also a brute force search, but prioritizes extending its search to the nearest unvisited zone
// Prints the current shortest finished route every time it finds a new one, so can be
//  stopped at any time and still give a result
// For an optimal solution, likely far slower than BruteForceSolver
// Creates custom Links that ignore intermediate crossings
public class GreedySolver implements Solver {

    public Scenario scenario;
    public Map<Integer, Route> finishedRoutes;

    // graph
    private Map<Node, Map<Node, Double>> connections;

    // record time
    private Route bestRoute;

    public long TIME_LIMIT = 60; // seconds

    public Result solve(Scenario scenario) {
        this.scenario = scenario;
        this.finishedRoutes = new HashMap<>();
        this.bestRoute = null;
        // create graph of direct connections
        this.connections = new HashMap<>();
        for (Node node : scenario.nodes) {
            Map<Node, Double> neighborConnections = new HashMap<>();
            for (Node other : scenario.nodes) {
                Route directRoute = scenario.fastestRoutes.get(node).get(other);
                neighborConnections.put(other, directRoute.distance);
            }
            this.connections.put(node, neighborConnections);
        }
        // search for a hardcoded amount of time (may rethink this in the future)
        long now = System.currentTimeMillis();
        long end = now + this.TIME_LIMIT;
        Route start = new Route(scenario.start);
        search(start, end);
        return new Result(finishedRoutes.values(), scenario.speed);
    }

    // Recursively searches for valid, finished routes
    private void search(Route base, long endTime) {
        if (System.currentTimeMillis() > endTime) {
            return;
        }
        Node current = base.node;
        // Get the 6 closest nodes from current, only considering unvisited nodes
        // (6 is arbitrary. may rethink)
        List<Node> nearestNodes = scenario.nodes.stream()
            .filter(node -> !base.hasVisited(node) || node == scenario.end)
            .sorted(Comparator.comparingDouble(this.connections.get(current)::get))
            .limit(6)
            .toList();
        for (Node nextNode : nearestNodes) {
            if (nextNode == current) {
                continue;
            }
            double nextDistance = this.connections.get(current).get(nextNode);
            // Distance limit will be exceeded
            Route endRoute = this.scenario.fastestRoutes.get(nextNode).get(this.scenario.end);
            if (base.distance + nextDistance + endRoute.distance > this.scenario.distanceLimit) {
                continue;
            }
            // Visiting the last four nodes in another order would've been more efficient
            if (lastFourCheck(base, nextNode)) {
                continue;
             }
            // Extend route
            Route currentToNext = this.scenario.fastestRoutes.get(current).get(nextNode);
            Route next = Route.extend(base, currentToNext);
            if (next.node == this.scenario.end) {
                finishRoute(next);
            }
            // Recurse
            search(next, endTime);
        }
    }

    private void finishRoute(Route next) {
        // If there is already a route with this many points, keep the shorter one
        if (finishedRoutes.containsKey(next.points)) {
            Route existing = finishedRoutes.get(next.points);
            if (next.distance > existing.distance) {
                return;
            }
        }
        finishedRoutes.put(next.points, next);
        // Print if best so far
        if (this.bestRoute == null ||
            next.points > this.bestRoute.points ||
            (next.points == this.bestRoute.points && next.distance < this.bestRoute.distance)
        ) {
            this.bestRoute = next;
            System.out.println(next.routeString(scenario.speed));
        }
    }

    private boolean lastFourCheck(Route route, Node newNode) {
        if (route.nodes >= 3) {
            Node lastNode = route.node;
            Node last2Node = route.previous.node;
            Node last3Node = route.previous.previous.node;
            return (
                this.connections.get(lastNode).get(newNode) +
                this.connections.get(last3Node).get(last2Node) >
                this.connections.get(last2Node).get(newNode) +
                this.connections.get(last3Node).get(lastNode)
            );
        }
        return false;
    }
}
