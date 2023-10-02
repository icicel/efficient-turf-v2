package zone;
import java.util.HashSet;
import java.util.Set;
import map.Line;

// Represents a set of Connections
public class ConnectionSet extends AbstractSet<Connection> {
    
    // Requires a ZoneSet to connect to
    public ConnectionSet(Set<Line> lines, ZoneSet zones) {
        // Convert Lines to Connections
        this.set = new HashSet<>();
        for (Line line : lines) {
            addLine(line, zones);
        }
    }
    // Empty constructor
    public ConnectionSet() {super();}

    // Convert a Line to two Connections and add them
    public void addLine(Line line, ZoneSet zones) {
        Zone leftZone = zones.closestZoneTo(line.left);
        Zone rightZone = zones.closestZoneTo(line.right);
        Connection leftConnection = new Connection(line, leftZone, rightZone);
        Connection rightConnection = new Connection(line, rightZone, leftZone);
        leftConnection.reverse = rightConnection;
        rightConnection.reverse = leftConnection;
        add(leftConnection);
        add(rightConnection);
    }
}
