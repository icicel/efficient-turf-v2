package zone;
import map.Line;

// Represents a two-way connection between two Zones
public class Connection {
    
    // meters
    public double distance;

    public Zone left;
    public Zone right;

    // Convert a Line to a Connection
    // parent.coords and neighbor.coords don't necessarily have to correspond to line.left and line.right
    public Connection(Line line, Zone zone1, Zone zone2) {
        this.distance = line.distance;
        this.left = zone1;
        this.right = zone2;
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
