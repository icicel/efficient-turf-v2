package scenario;
import java.util.ArrayList;
import java.util.List;

// A linked list (one-way graph?) of Nodes connected by Links
// Represents a path between two Nodes in a Scenario
// Can however be used to represent a tree (as two Routes can share the same 
//  previous Route), as long as all leaf Routes are kept track of
public class Route {

    public Node node;
    public Link link; // the Link that connects this Route's node to the previous Route's node
    public Route previous;

    public double distance;
    public int nodes;
    public int zones;
    public int points;

    // begin a Route with a start Node
    // (the "root Route" :P)
    public Route(Node root) {
        this.node = root;
        this.link = null;
        this.previous = null;
        this.distance = 0.0;
        this.nodes = 1;
        this.zones = 1;
        this.points = root.points;
    }

    // extend a Route with a Link
    public Route(Route base, Link extension) {
        if (extension.parent != base.node) {
            throw new IllegalArgumentException("Link does not extend Route");
        }
        this.node = extension.neighbor;
        this.link = extension;
        this.previous = base;
        this.distance = base.distance + extension.distance;
        this.nodes = base.nodes + 1;
        if (!base.hasVisited(node)) {
            this.zones = base.zones + 1;
            this.points = base.points + this.node.points;
        } else {
            this.zones = base.zones;
            this.points = base.points;
        }
    }

    // extend a Route along another Route's path
    public static Route extend(Route base, Route extension) {
        if (extension.previous == null) {
            throw new IllegalArgumentException("Extension Route has no path");
        }
        if (extension.previous.previous == null) {
            // one-Link Route, end step
            return new Route(base, extension.link);
        }
        // recursive step
        Route previous = extend(base, extension.previous);
        return new Route(previous, extension.link);
    }
    
    public boolean hasVisited(Node node) {
        if (this.previous == null) {
            return this.node == node;
        }
        return this.node == node || this.previous.hasVisited(node);
    }
    public boolean hasVisited(Link link) {
        if (this.previous == null) {
            return false;
        }
        return this.link == link || this.previous.hasVisited(link);
    }

    public List<Node> getNodes() {
        List<Node> nodes;
        if (this.previous == null) {
            nodes = new ArrayList<>();
        } else {
            nodes = this.previous.getNodes();
        }
        nodes.add(this.node);
        return nodes;
    }

    @Override
    public String toString() {
        if (this.previous == null) {
            return this.node.name;
        }
        return this.previous.toString() + " -> " + this.node.name;
    }

    // Only print captured zones (a.k.a. first occurrences of nodes)
    public String zoneString() {
        if (this.previous == null) {
            return this.node.name;
        }
        if (this.previous.hasVisited(this.node)) {
            return this.previous.zoneString();
        }
        return this.previous.zoneString() + " -> " + this.node.name;
    }

    // Print route with stats and with ending node if skipped in the zoneString
    public String routeString(double speed) {
        StringBuilder sb = new StringBuilder(this.zoneString());
        if (this.previous != null && this.previous.hasVisited(this.node)) {
            sb.append(" -> " + this.node.name);
        }
        sb.append("\n(");
        sb.append(this.points).append(" points, ");
        sb.append(this.zones).append(" zones, ");
        sb.append((int) this.distance).append("m, ");
        sb.append((int) (this.distance / speed)).append("min)");
        return sb.toString();
    }
}
