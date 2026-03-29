package turf;
import java.util.ArrayList;
import java.util.List;
import map.Coords;
import map.Line;

// A pathway between two Points (left and right) along any number of Coords including none
// Is not one-way, may be a loop
public class Connection {
    
    public Point left;
    public List<Coords> middle;
    public Point right;

    public double distance;

    // Convert a Line to a Connection, overriding left and right with the given Points
    public Connection(Line line, Point left, Point right) {
        this.left = left;
        this.right = right;
        left.parents.add(this);
        right.parents.add(this);
        this.middle = line.middle;
        this.distance = line.distance;
    }

    // A direct connection, with no middle
    public Connection(Point left, Point right) {
        this.left = left;
        this.right = right;
        left.parents.add(this);
        right.parents.add(this);
        this.middle = new ArrayList<>();
        this.distance = left.distanceTo(right);
    }

    public boolean isLoop() {
        return left == right;
    }

    // Return the other endpoint
    public Point other(Point endpoint) {
        if (endpoint == left) return right;
        if (endpoint == right) return left;
        throw new IllegalArgumentException("Endpoint must be either left or right");
    }

    // Return a view of middle from the perspective of the endpoint, closest to furthest
    public List<Coords> middleFromPOVOf(Point endpoint) {
        if (endpoint == left) return middle;
        if (endpoint == right) return middle.reversed();
        throw new IllegalArgumentException("Endpoint must be either left or right");
    }

    @Override
    public String toString() {
        return left + " -> " + middle + " -> " + right;
    }
}
