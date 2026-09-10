package solver;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import scenario.Link;
import scenario.Node;
import scenario.Scenario;

// Also a brute force search, but prioritizes extending its search to the nearest unvisited zone
// For an optimal solution, somewhat slower than BruteForceSolver
public class GreedySolver extends BruteForceSolver {

    // for tweaking
    private final long MIN_LIFESPAN = 25; // ms

    @Override
    public Result solve(Scenario scenario, Long timeLimit) {
        if (timeLimit == null) {
            System.out.println("ERROR: GreedySolver is not designed for unlimited time, specify an end time or use BruteForceSolver instead (returning empty result)");
            return new Result(List.of(), scenario.speed);
        }
        return super.solve(scenario, timeLimit);
    }

    // Recursively searches for valid, finished routes
    // Divides its lifespan equally among its branches, unless that goes below
    //   the minimum lifespan, in which case it will simply run until endTime
    // Will not try to run if endTime has passed
    @Override
    protected void search(AdvancedRoute base, long endTime) {
        if (System.currentTimeMillis() > endTime) {
            return;
        }

        // Get lifespans/endtimes for each branch
        Node current = base.node;
        int branches = current.out.size();
        long now = System.currentTimeMillis();
        long lifespan = endTime - now;
        long branchLifespan = lifespan / branches;

        // Lifespan is long enough, continue branching
        if (branchLifespan >= MIN_LIFESPAN) {

            // Sort unvisited neighbors
            Set<Node> visited = new HashSet<>(base.getNodes());
            List<Link> sortedOut = current.out.stream()
                .filter(link -> !visited.contains(link.neighbor) || this.scenario.isEnd(link.neighbor))
                .sorted(Comparator.comparingDouble(link -> link.distance))
                .toList();

            // Add lifespan remainder to the first branch, so that all of the lifespan is used
            // This does come into play if MIN_LIFESPAN is very low
            long nextBranchEnd = now + branchLifespan + lifespan % branches;

            for (Link link : sortedOut) {
                int error = invalidRouteExtension(base, link);
                if (error != 0) {
                    continue;
                }
                AdvancedRoute next = new AdvancedRoute(base, link);
                if (this.scenario.isEnd(next.node)) {
                    finishRoute(next);
                }
                // Recurse
                search(next, nextBranchEnd);
                // Ready up for next branch
                nextBranchEnd += branchLifespan;
            }

        // Lifespan is too short, ignore branching
        } else {
            bruteForceSearch(base, endTime);
        }
    }

    // Recursively searches for valid, finished routes
    private void bruteForceSearch(AdvancedRoute base, long endTime) {
        if (System.currentTimeMillis() > endTime) {
            return;
        }
        // Sort unvisited neighbors
        Set<Node> visited = new HashSet<>(base.getNodes());
        List<Link> sortedOut = base.node.out.stream()
            .filter(link -> !visited.contains(link.neighbor) || this.scenario.isEnd(link.neighbor))
            .sorted(Comparator.comparingDouble(link -> link.distance))
            .toList();
        for (Link link : sortedOut) {
            int error = invalidRouteExtension(base, link);
            if (error != 0) {
                continue;
            }
            AdvancedRoute next = new AdvancedRoute(base, link);
            if (this.scenario.isEnd(next.node)) {
                finishRoute(next);
            }
            bruteForceSearch(next, endTime);
        }
    }
}
