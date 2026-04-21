package scenario;
import java.util.HashSet;
import java.util.Set;
import turf.Point;

// Represents a node in the graph
// Basically a Zone in a Scenario
public class Node {

    public String name;
    public Set<Link> in;
    public Set<Link> out;
    public Set<Node> inNodes;
    public Set<Node> outNodes;
    public int points;

    // For hashing, so Nodes can be renamed
    private String hashName;

    // Create a node from a point
    public Node(Point point, String username, boolean isNow) {
        this.name = point.toString();
        this.hashName = new String(this.name);
        this.in = new HashSet<>();
        this.out = new HashSet<>();
        this.inNodes = new HashSet<>();
        this.outNodes = new HashSet<>();
        if (point.isZone()) {
            this.points = point.zone.getPoints(username, isNow);
        } else {
            this.points = 0;
        }
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

    public boolean isZone() {
        return this.points > 0;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return this.hashName.hashCode();
    }
}
