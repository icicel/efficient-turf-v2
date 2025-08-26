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
        for (Route route : this.routes) {
            sb.append(routeString(route));
            sb.append("\n");
        }
        return sb.toString();
    }

    // Only show the zones captured, in the order they were captured (and start and end)
    public String routeString(Route result) {
        return routeZones(result) + " -> " + result.node.name + "\n(" 
            + result.points + " points, " + result.zones + " zones, " + (int) result.length + "m, " + (int) (result.length / this.speed) + "min)";
    }
    private String routeZones(Route result) {
        if (result.previous == null) {
            return result.node.name;
        }
        if (result.node.isZone() && !result.previous.hasVisited(result.node)) {
            return routeZones(result.previous) + " -> " + result.node.name;
        }
        return routeZones(result.previous);
    }
}
