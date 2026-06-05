package solver;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import scenario.Link;
import scenario.Node;
import scenario.Route;
import scenario.Scenario;

public abstract class Solver {

    public Scenario scenario;

    // the main solving method, to be implemented by subclasses
    // if timeLimit is null, there is none
    protected abstract Result solve(Scenario scenario, Long timeLimit);

    // convenience end time calculation
    protected long endTime(Long timeLimit) {
        if (timeLimit == null) {
            return Long.MAX_VALUE;
        } else {
            return System.currentTimeMillis() + timeLimit;
        }
    }

    public Result solve(Scenario scenario, int timeLimit) {
        return solve(scenario, (long) timeLimit * 1000);
    }

    public Result solve(Scenario scenario) {
        return solve(scenario, null);
    }

    // storage of finished routes

    public Map<Integer, Route> finishedRoutes;
    public Route bestRoute;

    protected void finishRoute(Route solution) {
        // If there is already a route with this many points, keep the shorter one
        if (finishedRoutes.containsKey(solution.points)) {
            Route existing = finishedRoutes.get(solution.points);
            if (solution.distance >= existing.distance) {
                return;
            }
        }
        finishedRoutes.put(solution.points, solution);
        // Print if best so far
        if (this.bestRoute == null ||
            solution.points > this.bestRoute.points ||
            (solution.points == this.bestRoute.points && solution.distance < this.bestRoute.distance)
        ) {
            this.bestRoute = solution;
            System.out.println(solution.routeString(scenario.speed));
        }
    }

    // handling of crosses (though utilizing this is optional)

    protected Map<Link, Set<Link>> crosses;

    protected void findCrosses() {
        // find crosses
        // these are pairs of links AB and CD that can't coexist in a route,
        //  because traveling AC and BD instead is shorter (usually because AB and CD cross)
        this.crosses = new HashMap<>();
        int c = 1;
        for (Node nodeB : this.scenario.nodes) {
            System.out.print("Finding crosses... (" + c++ + "/" + this.scenario.nodes.size() + ")\r");
            for (Node nodeC : this.scenario.nodes) {
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
                        // AB + CD < AC + BD
                        Link linkAB = nodeA.getLinkTo(nodeB);
                        Link linkCD = nodeC.getLinkTo(nodeD);
                        Link linkAC = nodeA.getLinkTo(nodeC);
                        Link linkBD = nodeB.getLinkTo(nodeD);
                        if (linkAB.distance + linkCD.distance < linkAC.distance + linkBD.distance) {
                            continue;
                        }
                        // AB and CD "cross"
                        this.crosses.computeIfAbsent(linkAB, k -> new HashSet<>())
                            .add(linkCD);
                        this.crosses.computeIfAbsent(linkCD, k -> new HashSet<>())
                            .add(linkAB);
                    }
                }
            }
        }
    }

    protected boolean crossesRoute(Link link, Route route) {
        for (Link routeLink : route.getLinks()) {
            Set<Link> crossesWith = this.crosses.get(link);
            if (crossesWith != null && crossesWith.contains(routeLink)) {
                return true;
            }
        }
        return false;
    }
}
