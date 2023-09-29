package zone;
import map.Line;

// Represents a one-way connection from a parent zone to its neighbor
public class Connection {
    
    // meters
    public double distance;

    public Zone parent;
    public Zone neighbor;

    // Convert a Line to a Connection
    // parent.coords and neighbor.coords don't necessarily have to correspond to line.left and line.right
    public Connection(Line line, Zone parent, Zone neighbor) {
        this.distance = line.distance;
        this.parent = parent;
        this.neighbor = neighbor;

        this.parent.connections.add(this);
    }

    @Override
    public String toString() {
        return parent.name + " -> " + neighbor.name;
    }

    @Override
    public int hashCode() {
        return this.parent.hashCode() + this.neighbor.hashCode();
    }
}
