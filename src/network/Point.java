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

    public static int idCounter = 0;

    public Point(Coords coords) {
        this.coords = coords;
        this.parents = new HashSet<>();
        this.zone = coords.name; // if coords has a name, this will be the zone name, otherwise null
    }

    public boolean isZone() {
        return this.zone != null;
    }

    public double distanceTo(Point other) {
        return this.coords.distanceTo(other.coords);
    }

    @Override
    public String toString() {
        return this.coords.toString();
    }
}
