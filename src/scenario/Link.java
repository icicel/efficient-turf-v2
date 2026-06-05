package scenario;
import turf.Trail;

// Represents a one-way connection from a parent Node to its neighbor
public class Link {
    
    // meters
    public double distance;

    public Node parent;
    public Node neighbor;

    // The connection from neighbor to parent
    // Can usually be assumed to exist
    public Link reverse;

    public Trail ancestor;

    // Initialization
    // Will fail if parent already has a link to neighbor
    public Link(Trail trail, Node parent, Node neighbor) {
        this.distance = trail.distance;
        this.parent = parent;
        this.neighbor = neighbor;
        this.ancestor = trail;

        if (parent.ancestor != trail.start() || neighbor.ancestor != trail.end()) {
            throw new IllegalArgumentException("Trail does not match nodes");
        }
        if (parent.hasLinkTo(neighbor)) {
            // This link already exists
            throw new IllegalArgumentException("Link already exists from " + parent.name + " to " + neighbor.name);
        }

        parent.out.add(this);
        parent.outNodes.add(neighbor);
        parent.outMap.put(neighbor, this);

        // Reverse handling
        if (neighbor.hasLinkTo(parent)) {
            this.reverse = neighbor.getLinkTo(parent);
            this.reverse.reverse = this;
        }
    }

    @Override
    public String toString() {
        return parent.name + " -> " + neighbor.name;
    }

    @Override
    public int hashCode() {
        return this.parent.hashCode() - this.neighbor.hashCode();
    }
}
