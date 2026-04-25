package turf;
import java.util.ArrayList;
import java.util.List;

// A pathway between two Points (left and right) along any number of other Points including none
// Is not one-way, may be a loop
public class Connection {

    public Point left;
    public List<Point> middle; // from left to right, excluding endpoints
    public Point right;

    public double distance; // meters

    // Convert an array of Points to a Connection
    public Connection(Point[] coordinates) {
        if (coordinates.length < 2) {
            throw new IllegalArgumentException("Connection must have at least two coordinates");
        }
        this.left = coordinates[0];
        this.right = coordinates[coordinates.length - 1];
        this.middle = new ArrayList<>();
        this.distance = coordinates[0].distanceTo(coordinates[1]);
        for (int i = 1; i < coordinates.length - 1; i++) {
            Point current = coordinates[i];
            Point next = coordinates[i + 1];
            this.distance += current.distanceTo(next);
            this.middle.add(current);
        }
        left.parents.add(this);
        right.parents.add(this);
    }

    // A direct connection, with no middle
    public Connection(Point left, Point right) {
        this.left = left;
        this.right = right;
        this.middle = new ArrayList<>();
        this.distance = left.distanceTo(right);
        left.parents.add(this);
        right.parents.add(this);
    }

    // Does not update distance
    // Parent sets can handle duplicates, no need to check for if the endpoints
    //   are the same as the current ones
    public void overrideEndpoints(Point left, Point right) {
        this.left = left;
        this.right = right;
        left.parents.add(this);
        right.parents.add(this);
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
    public List<Point> middleFromPOVOf(Point endpoint) {
        if (endpoint == left) return middle;
        if (endpoint == right) return middle.reversed();
        throw new IllegalArgumentException("Endpoint must be either left or right");
    }

    @Override
    public String toString() {
        return left + " -> " + middle + " -> " + right;
    }
}
