package turf;
import java.util.HashSet;
import java.util.Set;
import map.Coords;

// An intersection between any number of Connections
public class Point {

    public Coords coords;
    public Set<Connection> parents;

    // null here represents a non-zone Point
    public Zone zone;

    public String name;

    public Point(Coords coords) {
        this(coords, null);
    }
    public Point(Coords coords, Zone zone) {
        this.coords = coords;
        this.parents = new HashSet<>();
        this.zone = zone;
        this.name = coords.toString();
    }

    public boolean isZone() {
        return this.zone != null;
    }

    public boolean isDeadEnd() {
        return parents.size() == 1 && !isZone();
    }

    public double distanceTo(Point other) {
        return this.coords.distanceTo(other.coords);
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int hashCode() {
        return this.coords.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Point other = (Point) obj;
        return this.coords.equals(other.coords);
    }
}
