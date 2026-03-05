package network;
import java.util.HashSet;
import java.util.Set;

import map.Coords;

// An intersection between any number of Ways
public class Point {

    public Coords coords;
    public Set<Way> parents;

    // null here represents a non-zone Point
    public String zone;

    public Point(Coords coords, String zone) {
        this.coords = coords;
        this.parents = new HashSet<>();
        this.zone = zone;
    }

    public boolean isZone() {
        return this.zone != null;
    }

    public double distanceTo(Point other) {
        return this.coords.distanceTo(other.coords);
    }
}
