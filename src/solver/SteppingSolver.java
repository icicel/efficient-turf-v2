package solver;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import scenario.Link;
import scenario.Scenario;

// Breaks down the search into steps, where each step is a brute force search from the current node to
//  *any* node (that can still reach the end node within the time limit)
// The end node of each step is used as the starting point for the next step
// Can by design only output a single route
// Outperforms GreedySolver on *very* large scenarios (3-4+ hours, entire cities) where its inefficiencies
//  are less significant
public class SteppingSolver extends BruteForceSolver {

    // for tweaking
    private final int MAX_STEP_TIME = 60; // min

    private AdvancedRoute stepBestRoute;
    
    @Override
    public Result solve(Scenario scenario, Long timeLimit) {
        if (timeLimit == null) {
            warn("ERROR: StepSolver is not designed for unlimited time, specify an end time or use BruteForceSolver instead (returning empty result)");
            return new Result(List.of(), scenario.speed);
        }
        this.scenario = scenario;
        this.finishedRoutes = new HashMap<>();
        this.bestRoute = null;
        findCrosses();
        // Divide into steps
        int steps = (int) Math.ceil(scenario.timeLimit / MAX_STEP_TIME);
        steps = Math.max(3, steps); // at least 3 steps
        long stepRealTimeLimit = timeLimit / steps;
        double stepDistanceLimit = scenario.distanceLimit / steps;
        log("StepSolver: Running " + steps + " steps at " + stepRealTimeLimit + " ms per step, " + stepDistanceLimit + " distance per step");
        // Start the first step
        AdvancedRoute currentRoute = new AdvancedRoute(scenario.start);
        long currentEndTime = super.endTime(stepRealTimeLimit + (timeLimit % steps));
        double currentStepDistanceLimit = stepDistanceLimit + (scenario.distanceLimit % steps);
        for (int step = 0; step < (steps - 1); step++) {
            log("StepSolver: *** Step " + (step + 1) + " of " + steps);
            this.stepBestRoute = null;
            stepSearch(currentRoute, currentEndTime, currentStepDistanceLimit);
            currentRoute = this.stepBestRoute;
            currentEndTime += stepRealTimeLimit;
            currentStepDistanceLimit += stepDistanceLimit;
        }
        // Final step, do a regular search to the end node
        log("StepSolver: *** Final step");
        search(currentRoute, currentEndTime);
        return new Result(Collections.singletonList(this.bestRoute), scenario.speed);
    }

    protected void stepSearch(AdvancedRoute base, long endTime, double distanceLimit) {
        if (System.currentTimeMillis() > endTime) {
            return;
        }
        for (Link link : base.node.out) {
            int error = stepInvalidRouteExtension(base, link, distanceLimit);
            if (error != 0) {
                continue;
            }
            AdvancedRoute next = new AdvancedRoute(base, link);
            // Always finish route
            stepFinishRoute(next);
            stepSearch(next, endTime, distanceLimit);
        }
    }

    protected void stepFinishRoute(AdvancedRoute solution) {
        // Print if best so far
        if (this.stepBestRoute == null ||
            solution.points > this.stepBestRoute.points ||
            (solution.points == this.stepBestRoute.points && solution.distance < this.stepBestRoute.distance)
        ) {
            this.stepBestRoute = solution;
            log(solution.routeString(scenario.speed));
        }
    }

    // Add a custom invalidity check
    protected int stepInvalidRouteExtension(AdvancedRoute route, Link newLink, double distanceLimit) {
        // Exceeds the distance limit for this step
        if (route.distance > distanceLimit) {
            return 1738; // im like hey whats up hello
        }

        // Otherwise, use the regular BruteForceSolver checks
        return super.invalidRouteExtension(route, newLink);
    }
}
