package network;
import java.util.ArrayList;
import java.util.List;
import map.Coords;

// A pathway between two Points (left and right) along any number of Coords including none
public class Way {
    
    public Point left;
    public Point right;

    public List<Coords> middle;

    public double distance;

    public Way(Point left, Point right) {
        this.left = left;
        this.right = right;
        left.parents.add(this);
        right.parents.add(this);
        this.middle = new ArrayList<>();
        this.distance = left.distanceTo(right);
    }

    // Return the other endpoint
    public Point other(Point endpoint) {
        if (endpoint == left) return right;
        if (endpoint == right) return left;
        return null;
    }

    // Return a view of middle from the perspective of the endpoint, closest to furthest
    public List<Coords> middleFromPOVOf(Point endpoint) {
        if (endpoint == left) return middle;
        if (endpoint == right) return middle.reversed();
        return null;
    }
}
