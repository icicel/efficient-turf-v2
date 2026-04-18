package solver;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import scenario.Route;

public class Result {
    
    public List<Route> routes;

    private double speed;

    public Result(Collection<? extends Route> finishedRoutes, double speed) {
        // Extract the 25 routes with the most points
        List<Route> sortedRoutes = new ArrayList<>(finishedRoutes);
        sortedRoutes.sort(
            (a, b) -> Integer.compare(b.points, a.points)
        );
        this.routes = sortedRoutes.subList(0, Math.min(25, sortedRoutes.size()));
        this.speed = speed;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Route route : this.routes.reversed()) {
            sb.append(route.routeString(this.speed));
            sb.append("\n");
        }
        return sb.toString();
    }
}
