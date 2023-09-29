package zone;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import map.Line;

// Represents a set of Connections
public class ConnectionSet implements Iterable<Connection> {

    private Set<Connection> connections;
    
    // Requires a ZoneSet to connect to
    public ConnectionSet(Set<Line> lines, ZoneSet zones) {
        // Convert Lines to Connections
        this.connections = new HashSet<>();
        for (Line line : lines) {
            Zone leftZone = zones.closestZoneTo(line.left);
            Zone rightZone = zones.closestZoneTo(line.right);
            Connection leftConnection = new Connection(line, leftZone, rightZone);
            Connection rightConnection = new Connection(line, rightZone, leftZone);
            add(leftConnection);
            add(rightConnection);
        }
    }
    // Empty set
    public ConnectionSet() {
        this.connections = new HashSet<>();
    }

    public void add(Connection connection) {
        // Set.add() returns false if the element already exists
        if (!this.connections.add(connection)) {
            System.out.println("WARNING: Duplicate connection " + connection);
        }
    }

    public int size() {
        return connections.size();
    }

    @Override
    public Iterator<Connection> iterator() {
        return connections.iterator();
    }
}
