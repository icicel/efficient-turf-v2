package scenario;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import turf.Zone;

// Represents a node in the graph
// Basically a Zone in a Scenario
public class Node {

    public String name;
    public Set<Link> links;
    public int points;
    public boolean isZone; // as in a real zone

    // Should store the shortest Route to every other Node that is a zone, 
    //  but only if that Route contains no other zones
    public Map<Node, Route> fastestRoutes;

    // Create a node from a zone
    public Node(Zone zone, String username, boolean naïve) {
        this.name = zone.name;
        this.links = new HashSet<>();
        this.points = zone.getPoints(username, naïve);
        this.isZone = this.points != 0;
    }

    // Fill up fastestRoutes using Dijkstra's (I think?)
    // Keep a priority queue of all possible Route extensions (the frontier)
    //  sorted by their length
    // After finding the shortest Route, extend it further with all outgoing Links
    //  from its Node and add the new Routes to the queue
    public void createFastestRoutes() {
        this.fastestRoutes = new HashMap<>();
        PriorityQueue<Route> queue = new PriorityQueue<>(
            (a, b) -> Double.compare(a.length, b.length));
        Set<Node> visited = new HashSet<>();
        visited.add(this);
        Route start = new Route(this);
        for (Link link : this.links) {
            queue.add(new Route(link, start));
        }
        while (!queue.isEmpty()) {
            // Get the shortest Route from the queue
            Route route = queue.remove();
            Node neighbor = route.node;
            if (visited.contains(neighbor)) {
                continue;
            }
            visited.add(neighbor);

            // Extend the Route with all outgoing Links from the neighbor
            //  and add them to the queue
            for (Link link : neighbor.links) {
                queue.add(new Route(link, route));
            }

            // If the neighbor is a zone and the Route contains no other zones,
            //  add route to fastestRoutes
            if (neighbor.isZone && route.zones == 2 && !this.fastestRoutes.containsKey(neighbor)) {
                this.fastestRoutes.put(neighbor, route);
            }
        }
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
