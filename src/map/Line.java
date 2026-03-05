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

        // Convert to Coords
        Coords[] coordinates = new Coords[coordinateStrings.length];
        for (int i = 0; i < coordinateStrings.length; i++) {
            coordinates[i] = new Coords(coordinateStrings[i]);
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
