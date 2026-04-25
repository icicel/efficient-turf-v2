package turf;
import java.util.HashSet;
import java.util.Set;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

// An intersection between any number of Connections
public class Point {

    public double lat;
    public double lon;
    public String name;

    // All Connections that include this Point
    public Set<Connection> parents;

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

    // Calculates geodesic distance in meters to another Point
    public double distanceTo(Point other) {
        GeodesicData data = Geodesic.WGS84.Inverse(this.lat, this.lon, other.lat, other.lon);
        return data.s12;
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
