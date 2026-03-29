package map;
import java.util.ArrayList;
import java.util.List;

// Represents a primitive connection between two Coords (left and right) along
//  any number of middle Coords, including none
public class Line {

    public Coords left;
    public List<Coords> middle;
    public Coords right;

    public double distance; // meters

    public Line(Coords left, Coords right) {
        this.left = left;
        this.right = right;
        this.middle = new ArrayList<>();
        this.distance = left.distanceTo(right);
    }

    public Line(Coords[] coordinates) {
        this.left = coordinates[0];
        this.right = coordinates[coordinates.length - 1];
        this.middle = new ArrayList<>();
        this.distance = 0;
        for (int i = 1; i < coordinates.length - 1; i++) {
            this.middle.add(coordinates[i]);
            this.distance += coordinates[i - 1].distanceTo(coordinates[i]);
        }
        this.distance += coordinates[coordinates.length - 2]
            .distanceTo(coordinates[coordinates.length - 1]);
    }

    @Override
    public String toString() {
        return left + " -> " + middle + " -> " + right;
    }
}
