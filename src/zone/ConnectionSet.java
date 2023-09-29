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
            addConnection(leftConnection);
            addConnection(rightConnection);
        }
    }

    private void addConnection(Connection connection) {
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
