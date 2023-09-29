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
            Zone leftZone = zones.closestZoneTo(line.left);
            Zone rightZone = zones.closestZoneTo(line.right);
            Connection leftConnection = new Connection(line, leftZone, rightZone);
            Connection rightConnection = new Connection(line, rightZone, leftZone);
            add(leftConnection);
            add(rightConnection);
        }
    }
    // Empty constructor
    public ConnectionSet() {super();}
}
