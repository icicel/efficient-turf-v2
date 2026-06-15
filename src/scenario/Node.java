package scenario;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import turf.Point;

// Represents a node in the graph
// Basically a Zone in a Scenario
public class Node {

    public String name;
    public Set<Link> out;
    public Set<Node> outNodes;
    public Map<Node, Link> outMap;

    public int realPoints; // actual points
    public int points; // internal points

    public Point ancestor;

    // Create a node from a point
    public Node(Point point, String username, boolean isNow) {
        this.name = point.toString();
        this.out = new HashSet<>();
        this.outNodes = new HashSet<>();
        this.outMap = new HashMap<>();
        this.ancestor = point;
        if (point.isZone()) {
            this.realPoints = point.zone.getPoints(username, isNow);
            this.points = this.realPoints;
        } else {
            this.realPoints = 0;
            this.points = 0;
        }
    }

    public boolean isZone() {
        return this.realPoints > 0;
    }

    // Retrieve an outgoing link to a neighbor node
    public Link getLinkTo(Node neighbor) {
        return this.outMap.get(neighbor);
    }

    public boolean hasLinkTo(Node neighbor) {
        return this.outNodes.contains(neighbor);
    }

    public void clear() {
        this.out = new HashSet<>();
        this.outNodes = new HashSet<>();
        this.outMap = new HashMap<>();
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
