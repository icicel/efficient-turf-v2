package turf;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

// A point on the map
public class Point implements Serializable {

    public double lat;
    public double lon;
    public String name;

    // All Connections that end at this Point
    public transient Set<Connection> parents;

    // Defaults to null, updated externally
    public Zone zone;

    public Point(String coordinateString, String name) {
        String[] coordinates = coordinateString.split(",");
        this.lat = Double.parseDouble(coordinates[1]);
        this.lon = Double.parseDouble(coordinates[0]);
        this.name = name != null ? name : "(" + lat + "," + lon + ")";
        this.parents = new HashSet<>();
        this.zone = null;
    }

    public Point(double lat, double lon, String name) {
        this.lat = lat;
        this.lon = lon;
        this.name = name != null ? name : "(" + lat + "," + lon + ")";
        this.parents = new HashSet<>();
        this.zone = null;
    }

    public Point(double lat, double lon) {
        this(lat, lon, null);
    }

    public boolean isZone() {
        return this.zone != null;
    }

    public boolean isDeadEnd() {
        return parents.size() == 1 && !isZone();
    }

    // Calculates *NON*-geodesic distance in meters to another Point
    public double distanceTo(double lat, double lon) {
        GeodesicData data = Geodesic.WGS84.Inverse(this.lat, this.lon, lat, lon);
        return data.s12;
    }
    public double distanceTo(Point other) {
        return distanceTo(other.lat, other.lon);
    }

    // Calculates *NON*-geodesic distance to the nearest point on a line
    public double distanceToLine(double lat1, double lon1, double lat2, double lon2) {
        Point nearest = nearestPointOnLine(new Point(lat1, lon1), new Point(lat2, lon2));
        return distanceTo(nearest);
    }
    public double distanceToLine(Point lineStart, Point lineEnd) {
        Point nearest = nearestPointOnLine(lineStart, lineEnd);
        return distanceTo(nearest);
    }

    // Can return line endpoints if they are nearest
    public Point nearestPointOnLine(Point lineStart, Point lineEnd) {
        // Vertical distortion due to latitude
        double distortion = Math.cos(Math.toRadians(this.lat));
        double x0 = this.lon * distortion;
        double y0 = this.lat;
        double x1 = lineStart.lon * distortion;
        double y1 = lineStart.lat;
        double x2 = lineEnd.lon * distortion;
        double y2 = lineEnd.lat;
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (dx == 0 && dy == 0) {
            // lineStart == lineEnd
            return lineStart;
        }
        // Project this point onto the extended line
        double t = ((x0 - x1) * dx + (y0 - y1) * dy) / (dx * dx + dy * dy);
        double resultLon = (x1 + t * dx) / distortion;
        double resultLat = (y1 + t * dy);
        if (t <= 0) {
            return lineStart;
        } else if (t >= 1) {
            return lineEnd;
        } else {
            return new Point(resultLat, resultLon);
        }
    }

    public boolean isNeighbor(Point other) {
        for (Connection connection : parents) {
            if (connection.other(this) == other) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(lat) * 31 + Double.hashCode(lon);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Point other = (Point) obj;
        // Allows no margin of error for now
        return Double.compare(lat, other.lat) == 0 && Double.compare(lon, other.lon) == 0;
    }
}
