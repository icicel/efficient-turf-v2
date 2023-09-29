package map;
import turf.ZoneSet;

// Represents a one-way connection from a parent zone to its neighbor
public class Connection {
    
    // meters
    public double distance;

    public Zone parent;
    public Zone neighbor;

    // Convert a Line to a Connection
    // Requires a ZoneSet to find parent and neighbor
    // Also adds itself to the parent's connections
    public Connection(Line line, ZoneSet zones, boolean leftParent) {
        this.distance = line.distance;
        if (leftParent) {
            this.parent = zones.closestZoneTo(line.left);
            this.neighbor = zones.closestZoneTo(line.right);
        } else {
            this.parent = zones.closestZoneTo(line.right);
            this.neighbor = zones.closestZoneTo(line.left);
        }
        this.parent.connections.add(this);
    }

    @Override
    public String toString() {
        return parent.name + " -> " + neighbor.name;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Connection)) {
            return false;
        }
        Connection otherConnection = (Connection) other;
        return this.parent.equals(otherConnection.parent) 
            && this.neighbor.equals(otherConnection.neighbor);
    }
    @Override
    public int hashCode() {
        return this.parent.hashCode() + this.neighbor.hashCode();
    }
}
