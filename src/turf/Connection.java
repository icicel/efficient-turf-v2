package turf;

// Represents a one-way connection from a parent zone to its neighbor
public class Connection {
    
    public int distance;
    public Zone parent;
    public Zone neighbor;

    // Should correspond to the coordinates of the parent zone
    public float latitudeStart;
    public float longitudeStart;
    // And these to the neighbor zone
    public float latitudeEnd;
    public float longitudeEnd;

    // Receive a list of coordinates as points defining a line
    public Connection(String coordinates) {
        return;
    }

}
