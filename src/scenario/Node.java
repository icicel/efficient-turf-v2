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
    public Set<Link> in;
    public Set<Link> out;
    public Set<Node> inNodes;
    public Set<Node> outNodes;
    public int points;

    // Create a node from a zone
    public Node(Zone zone, String username, boolean isNow) {
        this.name = zone.name;
        this.in = new HashSet<>();
        this.out = new HashSet<>();
        this.inNodes = new HashSet<>();
        this.outNodes = new HashSet<>();
        this.points = zone.getPoints(username, isNow);
    }

    // Retrieve an outgoing link to a neighbor node
    public Link getLinkTo(Node neighbor) {
        for (Link link : this.out) {
            if (link.neighbor == neighbor) {
                return link;
            }
        }
        return null; // No link to that node
    }

    public boolean hasLinkTo(Node neighbor) {
        return this.outNodes.contains(neighbor);
    }

    // Returns the shortest Route to every other zone
    // This is done by keeping a priority queue of all Nodes neighboring
    //  already visited Nodes, (in the form of Routes) and extending them
    //  when visiting a new Node (AKA Dijkstra's)
    public Map<Node, Route> findFastestRoutes() {
        Map<Node, Route> fastestRoutes = new HashMap<>();
        PriorityQueue<Route> queue = new PriorityQueue<>(
            (a, b) -> Double.compare(a.length, b.length)
        );
        Set<Node> visited = new HashSet<>();
        queue.add(new Route(this));
        while (!queue.isEmpty()) {
            // Get the shortest Route from the queue
            Route route = queue.remove();
            Node neighbor = route.node;
            if (visited.contains(neighbor)) {
                continue;
            }
            visited.add(neighbor);
            fastestRoutes.put(neighbor, route);

            // Extend the Route with all outgoing Links from the neighbor
            //  and add them to the queue
            for (Link link : neighbor.out) {
                queue.add(new Route(link, route));
            }
        }
        return fastestRoutes;
    }

    public boolean isZone() {
        return this.points > 0;
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
