package scenario;

// Represents a one-way connection from a parent Node to its neighbor
public class Link {
    
    // meters
    public double distance;

    public Node parent;
    public Node neighbor;

    // The connection from neighbor to parent (if it exists)
    public Link reverse;

    // Initialization
    // Will fail if parent already has a link to neighbor
    public Link(double distance, Node parent, Node neighbor) {
        this.distance = distance;
        this.parent = parent;
        this.neighbor = neighbor;

        if (parent.outNodes.contains(neighbor)) {
            // This link already exists
            throw new IllegalArgumentException("Link already exists from " + parent.name + " to " + neighbor.name);
        }

        parent.out.add(this);
        parent.outNodes.add(neighbor);
        neighbor.in.add(this);
        neighbor.inNodes.add(parent);

        // Reverse handling
        if (neighbor.outNodes.contains(parent)) {
            this.reverse = neighbor.getLinkTo(parent);
            this.reverse.reverse = this;
        }
    }

    @Override
    public String toString() {
        return parent.name + " -> " + neighbor.name;
    }

    // Pretend the Link is a Connection if its reverse exists
    public String pairString() {
        if (this.reverse == null) {
            return this.toString();
        }
        return parent.name + " <-> " + neighbor.name;
    }

    @Override
    public int hashCode() {
        return this.parent.hashCode() - this.neighbor.hashCode();
    }
}
