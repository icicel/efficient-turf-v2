package solver;
import scenario.Scenario;

public abstract class Solver {

    // the main solving method, to be implemented by subclasses
    // if timeLimit is null, there is none
    // finally, a valid use case for protected!
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
}
