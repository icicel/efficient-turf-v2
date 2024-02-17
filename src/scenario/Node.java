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

    // Create a node from a zone
    public Node(Zone zone, String username, boolean naïve) {
        this.name = zone.name;
        this.links = new HashSet<>();
        this.points = zone.getPoints(username, naïve);
        this.isZone = this.points != 0;
    }

    // Returns the shortest Route to every other zone, but only if that Route
    //  contains no other zones than the start and end
    // This is done by keeping a priority queue of all Nodes neighboring
    //  already visited Nodes, (in the form of Routes) and extending them
    //  when visiting a new Node
    public Map<Node, Route> findFastestRoutes() {
        Map<Node, Route> fastestRoutes = new HashMap<>();
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
            if (neighbor.isZone && route.zones == 2 && !fastestRoutes.containsKey(neighbor)) {
                fastestRoutes.put(neighbor, route);
            }
        }
        return fastestRoutes;
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
