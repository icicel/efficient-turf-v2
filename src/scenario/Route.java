package scenario;

// A linked list (one-way graph?) of Nodes connected by Links
// Represents a path between two Nodes in a Scenario
// Can however be used to represent a tree (as two Routes can share the same 
//  previous Route), as long as all leaf Routes are kept track of
public class Route {

    public Node node;
    public Route previous;

    public double length;
    public int zones;

    // begin a Route with a start Node
    // (the "root Route" :P)
    public Route(Node root) {
        this.node = root;
        this.previous = null;
        this.length = 0.0;
        if (this.node.isZone) {
            this.zones = 1;
        } else {
            this.zones = 0;
        }
    }

    // extend a Route with a Link
    public Route(Link extension, Route previous) {
        if (extension.parent != previous.node) {
            throw new IllegalArgumentException("Link does not extend Route");
        }
        this.node = extension.neighbor;
        this.previous = previous;
        this.length = previous.length + extension.distance;
        if (this.node.isZone) {
            this.zones = previous.zones + 1;
        } else {
            this.zones = previous.zones;
        }
    }

    @Override
    public String toString() {
        if (this.previous == null) {
            return this.node.name;
        }
        return this.previous.toString() + " -> " + this.node.name;
    }
}
