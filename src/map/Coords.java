package map;

// Represents a coordinate pair
public class Coords {
    
    public double lat;
    public double lon;
    
    public Coords(String coordinateString) {
        String[] coordinates = coordinateString.split(",");
        this.lon = Double.parseDouble(coordinates[0]);
        this.lat = Double.parseDouble(coordinates[1]);
    }

    @Override
    public String toString() {
        return "(" + lat + "," + lon + ")";
    }
}
