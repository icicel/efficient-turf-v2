package turf;
import java.util.HashSet;
import java.util.Set;
import zone.Zone;

// Represents a node in the graph
// Basically a Zone in a Scenario
public class Node {

    public String name;
    public Set<Link> links;
    public int points;

    // Create a node from a zone
    public Node(Zone zone, String username, boolean naïve) {
        this.name = zone.name;
        this.links = new HashSet<>();
        this.points = zone.getPoints(username, naïve);
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
