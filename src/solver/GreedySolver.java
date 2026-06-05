package solver;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import scenario.Link;
import scenario.Node;
import scenario.Route;
import scenario.Scenario;

// Also a brute force search, but prioritizes extending its search to the nearest unvisited zone
// Prints the current shortest finished route every time it finds a new one, so can be
//  stopped at any time and still give a result
// For an optimal solution, likely far slower than BruteForceSolver
// Creates custom Links that ignore intermediate crossings
public class GreedySolver extends Solver {

    // graph
    private Map<Node, Map<Node, Double>> connections;

    // for tweaking
    private final int SEARCH_WIDTH = 6;
    private final long MIN_LIFESPAN = 50; // ms

    public Result solve(Scenario scenario, Long timeLimit) {
        this.scenario = scenario;
        this.finishedRoutes = new HashMap<>();
        this.bestRoute = null;
        findCrosses();
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
        long end = super.endTime(timeLimit);
        Route start = new Route(scenario.start);
        search(start, end);
        return new Result(finishedRoutes.values(), scenario.speed);
    }

    // Recursively searches for valid, finished routes
    // Divides its lifespan equally among its branches, unless that goes below
    //   the minimum lifespan, in which case it will simply run until endTime
    // Will not try to run if endTime has passed
    private void search(Route base, long endTime) {
        if (System.currentTimeMillis() > endTime) {
            return;
        }
        Node current = base.node;
        Set<Node> visited = new HashSet<>(base.getNodes());
        // Get the closest nodes from current, only considering unvisited nodes
        List<Node> nearestNodes = scenario.nodes.stream()
            .filter(node -> !visited.contains(node) || node == scenario.end)
            .sorted(Comparator.comparingDouble(this.connections.get(current)::get))
            .toList();
        // Get lifespans/endtimes for each branch
        int branches = SEARCH_WIDTH;
        long now = System.currentTimeMillis();
        long lifespan = endTime - now;
        long branchLifespan = lifespan / branches;
        long nextBranchEnd;
        if (branchLifespan >= MIN_LIFESPAN) {
            long branchLifespanRemainder = lifespan % branches; // gotta account for all time
            nextBranchEnd = now + branchLifespan + branchLifespanRemainder;
        } else {
            // Ignore branching
            branchLifespan = 0;
            nextBranchEnd = endTime;
        }
        for (Node nextNode : nearestNodes) {
            if (System.currentTimeMillis() > endTime) {
                return;
            }
            if (nextNode == current) {
                continue;
            }
            double nextDistance = this.connections.get(current).get(nextNode);
            // Distance limit will be exceeded
            Route endRoute = this.scenario.fastestRoutes.get(nextNode).get(this.scenario.end);
            if (base.distance + nextDistance + endRoute.distance > this.scenario.distanceLimit) {
                continue;
            }
            // Extend route while checking for crosses
            Route currentToNext = this.scenario.fastestRoutes.get(current).get(nextNode);
            Route next = base;
            boolean crosses = false;
            for (Link link : currentToNext.getLinks()) {
                next = new Route(next, link);
                crosses = crossesRoute(link, next);
                if (crosses) {
                    break;
                }
            }
            if (crosses) {
                continue;
            }
            // Finish route if it has reached the end
            if (nextNode == this.scenario.end) {
                finishRoute(next);
                // Don't branch from end
                // May rethink this in the future, the optimal route could theoretically
                //  involve revisiting end like if it's at a chokepoint
                continue;
            }
            // Recurse
            search(next, nextBranchEnd);
            nextBranchEnd += branchLifespan;
        }
    }
}
