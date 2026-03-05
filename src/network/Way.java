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
}
