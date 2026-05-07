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

    public Scenario scenario;
    public Map<Integer, Route> finishedRoutes;

    // graph
    private Map<Node, Map<Node, Double>> connections;
    private Map<Link, Set<Link>> crosses;

    // record points
    private Route bestRoute;

    // for tweaking
    private final int SEARCH_WIDTH = 6;
    private final long MIN_LIFESPAN = 50; // ms

    public Result solve(Scenario scenario, Long timeLimit) {
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
        // find crosses
        // these are pairs of links AB and CD that can't coexist in a route,
        //  because traveling AC and BD instead is shorter (usually because AB and CD cross)
        this.crosses = new HashMap<>();
        int c = 1;
        for (Node nodeB : scenario.nodes) {
            System.out.print("Finding crosses... (" + c++ + "/" + scenario.nodes.size() + ")\r");
            for (Node nodeC : scenario.nodes) {
                if (nodeB == nodeC) {
                    continue;
                }
                // find all nodes neighbored by both nodeB and nodeC
                Set<Node> bothNeighbors = new HashSet<>(nodeB.outNodes);
                bothNeighbors.retainAll(nodeC.outNodes);
                for (Node nodeA : bothNeighbors) {
                    for (Node nodeD : bothNeighbors) {
                        if (nodeA == nodeD) {
                            continue;
                        }
                        crossCheck(nodeA, nodeB, nodeC, nodeD);
                    }
                }
            }
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

    // AB + CD < AC + BD
    private void crossCheck(Node nodeA, Node nodeB, Node nodeC, Node nodeD) {
        Link linkAB = nodeA.getLinkTo(nodeB);
        Link linkCD = nodeC.getLinkTo(nodeD);
        Link linkAC = nodeA.getLinkTo(nodeC);
        Link linkBD = nodeB.getLinkTo(nodeD);
        if (linkAB.distance + linkCD.distance < linkAC.distance + linkBD.distance) {
            return;
        }
        // AB and CD "cross"
        this.crosses.computeIfAbsent(linkAB, k -> new HashSet<>())
            .add(linkCD);
        this.crosses.computeIfAbsent(linkCD, k -> new HashSet<>())
            .add(linkAB);
    }

    private boolean crossesRoute(Link link, Route route) {
        for (Link routeLink : route.getLinks()) {
            Set<Link> crossesWith = this.crosses.get(link);
            if (crossesWith != null && crossesWith.contains(routeLink)) {
                return true;
            }
        }
        return false;
    }
}
