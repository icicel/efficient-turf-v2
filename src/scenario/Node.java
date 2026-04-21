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
    public boolean isZone;

    // Create a node from a point
    public Node(Point point, String username, boolean isNow) {
        this.name = point.toString();
        this.in = new HashSet<>();
        this.out = new HashSet<>();
        this.inNodes = new HashSet<>();
        this.outNodes = new HashSet<>();
        if (point.isZone()) {
            this.points = point.zone.getPoints(username, isNow);
            this.isZone = true;
        } else {
            this.points = 0;
            this.isZone = false;
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

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
