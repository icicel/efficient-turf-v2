package turf;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import map.Connection;
import map.Line;

// Represents a set of Connections
public class Connections implements Iterable<Connection> {

    private Set<Connection> connections;
    
    // Requires a set of zones to connect to
    public Connections(Set<Line> lines, Zones zones) {
        // Convert Lines to Connections
        this.connections = new HashSet<>();
        for (Line line : lines) {
            this.addConnection(line.leftConnection(zones));
            this.addConnection(line.rightConnection(zones));
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
