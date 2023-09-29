package map;

// Represents a primitive connection (two-way) between two Coords (left and right)
public class Line {

    // meters
    public double distance;

    public Coords left;
    public Coords right;

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

    @Override
    public String toString() {
        return left + " -> " + right;
    }
}
