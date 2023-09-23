package map;
import java.util.Set;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

// Represents a coordinate pair
public class Coords {
    
    public double lat;
    public double lon;
    
    public Coords(String coordinateString) {
        String[] coordinates = coordinateString.split(",");
        this.lon = Double.parseDouble(coordinates[0]);
        this.lat = Double.parseDouble(coordinates[1]);
    }

    // Calculates geodesic distance in meters to another Coords object
    public double distanceTo(Coords other) {
        GeodesicData data = Geodesic.WGS84.Inverse(this.lat, this.lon, other.lat, other.lon);
        return data.s12;
    }

    // Returns the closest zone out of a set
    public Zone closestZoneFrom(Set<Zone> zones) {
        Zone closestZone = null;
        double closestDistance = Double.MAX_VALUE;
        for (Zone zone : zones) {
            double distance = zone.coords.distanceTo(this);
            if (distance < closestDistance) {
                closestZone = zone;
                closestDistance = distance;
            }
        }
        return closestZone;
    }

    @Override
    public String toString() {
        return "(" + lat + "," + lon + ")";
    }
}
