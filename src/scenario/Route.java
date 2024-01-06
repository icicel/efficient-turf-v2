package scenario;

// A linked list of Nodes representing a path from start to end
public class Route {

    public Node node;
    public Route previous;

    public double length;
    public int zones;

    // create a Route with a single Node
    public Route(Node node) {
        this.node = node;
        this.previous = null;
        this.length = 0.0;
        if (node.isZone) {
            this.zones = 1;
        } else {
            this.zones = 0;
        }
    }

    // extend a Route with a Link
    public Route(Link extension, Route previous) {
        this.previous = previous;
        if (previous == null) {
            this.length = extension.distance;
            this.zones = 0;
        } else {
            this.length = previous.length + extension.distance;
            this.zones = previous.zones;
        }
        this.node = extension.neighbor;
        if (this.node.isZone) {
            this.zones++;
        }
    }

    @Override
    public String toString() {
        if (this.previous == null) {
            return this.node.name;
        } else {
            return this.previous.toString() + " -> " + this.node.name;
        }
    }
}
