package map;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

// Represents a coordinate pair
public class Coords {
    
    public double lat;
    public double lon;

    // Optional
    public String name;
    
    public Coords(String coordinateString) {
        String[] coordinates = coordinateString.split(",");
        this.lon = Double.parseDouble(coordinates[0]);
        this.lat = Double.parseDouble(coordinates[1]);
    }
    public Coords(String name, String coordinateString) {
        this(coordinateString);
        this.name = name.toLowerCase();
    }

    // Calculates geodesic distance in meters to another Coords object
    public double distanceTo(Coords other) {
        GeodesicData data = Geodesic.WGS84.Inverse(this.lat, this.lon, other.lat, other.lon);
        return data.s12;
    }

    @Override
    public String toString() {
        return "(" + lat + "," + lon + ")";
    }
}
