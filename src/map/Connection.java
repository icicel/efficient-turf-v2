package map;
import java.util.Set;

// Represents a one-way connection from a parent zone to its neighbor
public class Connection {
    
    // meters
    public double distance;

    public Zone parent;
    public Zone neighbor;

    // If the zone has been fully connected to its parent and neighbor
    //  (aka parent and neighbor are defined and it is in parent.connections)
    private boolean completed;
    
    // Coordinates are used to calculate distance, parent and neighbor
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

        this.completed = false;
    }
    // Empty constructor
    private Connection() {}

    // Complete the connection by finding the closest zones to the start and end coords out of a set
    // This does NOT add itself to the neighbor's connections since it is a one-way connection
    public void completeOn(Set<Zone> zones) {
        this.parent = start.closestZoneFrom(zones);
        this.parent.connections.add(this);
        this.neighbor = end.closestZoneFrom(zones);
        this.completed = true;
    }

    // Create an identical connection with parent and neighbor swapped
    public Connection reversed() {
        Connection reversed = new Connection();
        reversed.distance = this.distance;
        reversed.start = this.end;
        reversed.end = this.start;
        if (this.completed) {
            reversed.parent = this.neighbor;
            reversed.neighbor = this.parent;
            reversed.parent.connections.add(reversed);
            reversed.completed = true;
        }
        return reversed;
    }

    @Override
    public String toString() {
        if (parent == null) {
            return start + " -> " + end;
        }
        return parent.name + " -> " + neighbor.name + " (" + distance + ")";
    }
}
