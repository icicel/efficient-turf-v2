package map;

// Represents a coordinate pair
public class Coords {
    
    public float lat;
    public float lon;
    
    public Coords(String coordinateString) {
        String[] coordinates = coordinateString.split(",");
        this.lat = Float.parseFloat(coordinates[0]);
        this.lon = Float.parseFloat(coordinates[1]);
    }

    @Override
    public String toString() {
        return "(" + lat + "," + lon + ")";
    }
}
