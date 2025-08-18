package scenario;

// A linked list (one-way graph?) of Nodes connected by Links
// Represents a path between two Nodes in a Scenario
// Can however be used to represent a tree (as two Routes can share the same 
//  previous Route), as long as all leaf Routes are kept track of
public class Route {

    public Node node;
    public Link link; // the Link that connects this Route's node to the previous Route's node
    public Route previous;

    public double length;
    public int zones;
    public int points;

    // begin a Route with a start Node
    // (the "root Route" :P)
    public Route(Node root) {
        this.node = root;
        this.link = null;
        this.previous = null;
        this.length = 0.0;
        this.points = root.points;
        if (this.node.isZone()) {
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
        this.link = extension;
        this.previous = previous;
        this.length = previous.length + extension.distance;
        if (this.node.isZone() && !previous.hasVisited(node)) {
            this.zones = previous.zones + 1;
            this.points = previous.points + this.node.points;
        } else {
            this.zones = previous.zones;
            this.points = previous.points;
        }
    }
    
    public boolean hasVisited(Node node) {
        if (this.previous == null) {
            return this.node == node;
        }
        return this.node == node || this.previous.hasVisited(node);
    }

    @Override
    public String toString() {
        if (this.previous == null) {
            return this.node.name;
        }
        return this.previous.toString() + " -> " + this.node.name;
    }
}
