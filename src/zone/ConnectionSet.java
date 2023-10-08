package zone;
import java.util.Set;
import map.Line;

// Represents a set of Connections
public class ConnectionSet extends AbstractSet<Connection> {
    
    // Requires a ZoneSet to connect to
    public ConnectionSet(Set<Line> lines, ZoneSet zones) {
        super();
        
        // Convert Lines to Connections
        for (Line line : lines) {
            addLine(line, zones);
        }
    }
    public ConnectionSet() {
        super();
    }

    // Convert a Line to two Connections and add them
    private void addLine(Line line, ZoneSet zones) {
        Zone leftZone = zones.closestZoneTo(line.left);
        Zone rightZone = zones.closestZoneTo(line.right);
        Connection leftConnection = new Connection(line, leftZone, rightZone);
        Connection rightConnection = new Connection(line, rightZone, leftZone);
        leftConnection.reverse = rightConnection;
        rightConnection.reverse = leftConnection;
        boolean addedLeft = add(leftConnection);
        boolean addedRight = add(rightConnection);
        if (!addedLeft) {
            System.out.println("WARNING: Duplicate connection " + leftConnection);
        }
        if (!addedRight) {
            System.out.println("WARNING: Duplicate connection " + rightConnection);
        }
    }
}
