package turf;
import map.Coords;

// Represents a one-way connection from a parent zone to its neighbor
public class Connection {
    
    // meters
    public double distance;

    public Zone parent;
    public Zone neighbor;
    
    // Coordinates are used to calculate the parent and neighbor
    public Coords start;
    public Coords end;

    // Receive a list of coordinates as points defining a line
    public Connection(String coordinateListString) {

        // Split into individual coordinate strings
        String[] coordinateStrings = coordinateListString.split("\n");

        // Convert to Coords, ignoring the first and last coordinateStrings (as they are empty strings)
        Coords[] coordinates = new Coords[coordinateStrings.length - 2];
        for (int i = 1; i < coordinateStrings.length - 1; i++) {
            coordinates[i - 1] = new Coords(coordinateStrings[i]);
        }

        this.start = coordinates[0];
        this.end = coordinates[coordinates.length - 1];
        
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
        if (parent == null) {
            return start + " -> " + end;
        }
        return parent.name + " -> " + neighbor.name + " (" + distance + ")";
    }
}
