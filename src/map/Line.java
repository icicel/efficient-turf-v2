package map;
import java.util.Set;

// Represents a primitive connection (two-way) between two Coords (left and right)
public class Line {

    Coords left;
    Coords right;

    double distance;

    public Line(String coordinateListString) {

        // Split into individual coordinate strings
        String[] coordinateStrings = coordinateListString.split("\n");

        // Convert to Coords, ignoring the first and last coordinateStrings (as they are empty strings)
        Coords[] coordinates = new Coords[coordinateStrings.length - 2];
        for (int i = 1; i < coordinateStrings.length - 1; i++) {
            coordinates[i - 1] = new Coords(coordinateStrings[i]);
        }

        this.left = coordinates[0];
        this.right = coordinates[coordinates.length - 1];
        
        // Calculates distance between every coordinate pair, adds them together to form a total distance
        Coords previousCoordinate = null;
        this.distance = 0;
        for (Coords coordinate : coordinates) {
            if (previousCoordinate != null) {
                this.distance += coordinate.distanceTo(previousCoordinate);
            }
            previousCoordinate = coordinate;
        }
    }

    // Convert to a Connection using a set of zones, with a specific coordinate as the parent
    public Connection leftConnection(Set<Zone> zones) {
        return new Connection(this, zones, true);
    }
    public Connection rightConnection(Set<Zone> zones) {
        return new Connection(this, zones, false);
    }

    @Override
    public String toString() {
        return left + " -> " + right + " (" + distance + ")";
    }
}
