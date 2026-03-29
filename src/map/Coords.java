package map;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

// Represents a coordinate pair
public class Coords {
    
    public double lat;
    public double lon;

    // Optional
    public String name;
    
    public Coords(String coordinateString, String name) {
        String[] coordinates = coordinateString.split(",");
        this.lon = Double.parseDouble(coordinates[0]);
        this.lat = Double.parseDouble(coordinates[1]);
        this.name = name;
    }
    public Coords(String coordinateString) {
        this(coordinateString, null);
    }
    public Coords(double lat, double lon, String name) {
        this.lat = lat;
        this.lon = lon;
        this.name = name;
    }
    public Coords(double lat, double lon) {
        this(lat, lon, null);
    }

    // Calculates geodesic distance in meters to another Coords object
    public double distanceTo(Coords other) {
        GeodesicData data = Geodesic.WGS84.Inverse(this.lat, this.lon, other.lat, other.lon);
        return data.s12;
    }

    @Override
    public String toString() {
        if (name != null) {
            return name;
        }
        return "(" + lat + "," + lon + ")";
    }

    @Override
    public int hashCode() {
        return Double.hashCode(lat) * 31 + Double.hashCode(lon);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Coords other = (Coords) obj;
        return Double.compare(lat, other.lat) == 0 && Double.compare(lon, other.lon) == 0;
    }
}
