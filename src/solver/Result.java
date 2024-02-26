package solver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import scenario.Route;

public class Result {
    
    private List<Route> routes;

    public Result(List<? extends Route> finishedRoutes) {
        // Extract the 25 routes with the most points
        List<Route> sortedRoutes = new ArrayList<>(finishedRoutes);
        sortedRoutes.sort(
            (a, b) -> Integer.compare(a.points, b.points));
        Route[] sortedArray = sortedRoutes.toArray(new Route[0]);
        this.routes = List.of(Arrays.copyOfRange(sortedArray, 0, Math.min(25, sortedArray.length)));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Route route : this.routes) {
            sb.append(route.asResult());
            sb.append("\n");
        }
        return sb.toString();
    }
}
