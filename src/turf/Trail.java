package turf;
import java.util.ArrayList;
import java.util.List;

// Turf equivalent of scenario.Route - a linked list with extra steps
public class Trail {
    public Point point;
    public Connection connectionFromPrevious;
    public Trail previous;
    public double distance;
    public double weightedDistance;
    public int zones;

    public Trail(Point point) {
        this.point = point;
        this.connectionFromPrevious = null;
        this.previous = null;
        this.distance = 0.0;
        this.weightedDistance = 0.0;
        this.zones = point.isZone() ? 1 : 0;
    }

    public Trail(Connection extension, Trail previous) {
        this.point = extension.other(previous.point);
        this.connectionFromPrevious = extension;
        this.previous = previous;
        this.distance = previous.distance + extension.distance;
        this.weightedDistance = previous.weightedDistance + extension.weightedDistance;
        this.zones = previous.zones + (this.point.isZone() ? 1 : 0);
    }

    public List<Point> getPoints() {
        List<Point> points;
        if (this.previous == null) {
            points = new ArrayList<>();
        } else {
            points = this.previous.getPoints();
        }
        points.add(this.point);
        return points;
    }

    public List<Connection> getConnections() {
        List<Connection> connections;
        if (this.previous == null) {
            connections = new ArrayList<>();
        } else {
            connections = this.previous.getConnections();
            connections.add(this.connectionFromPrevious);
        }
        return connections;
    }
}