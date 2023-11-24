package zone;

// Represents a two-way connection between two Zones
public class Connection {
    
    // meters
    public double distance;

    public Zone left;
    public Zone right;

    // Convert a Line to a Connection
    // parent.coords and neighbor.coords don't necessarily have to correspond to line.left and line.right
    public Connection(double distance, Zone left, Zone right) {
        this.distance = distance;
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return left.name + " <-> " + right.name;
    }

    @Override
    public int hashCode() {
        return this.left.hashCode() + this.right.hashCode();
    }
}
