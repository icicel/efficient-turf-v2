import kml.KML;
import turf.Connections;
import turf.Zones;

public class EfficientTurf {
    public static void main(String[] args) throws Exception {
        KML kml = new KML("example.kml");

        /* Get zones and connections */

        // There are two types of zones: a true zone and a crossing
        //   Crossings are zones that are not actually zones, but "helper" zones
        //   They are worth 0 points and are usually used to reduce the amount of connections
        Zones trueZones = kml.getZones("Zones");
        Zones crossings = kml.getZones("Crossings");
        
        // Collect all zones into a single set
        Zones allZones = Zones.union(trueZones, crossings);

        // Get connections
        Connections connections = kml.getConnections("Connections", allZones);

        // Get points
        trueZones.initPoints();

        // use depth first search with a single route object
        // if it's valid and finished then copy and save
    }
}
