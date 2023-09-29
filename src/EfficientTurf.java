import kml.KML;
import turf.ConnectionSet;
import turf.ZoneSet;

public class EfficientTurf {
    public static void main(String[] args) throws Exception {
        KML kml = new KML("example.kml");

        /* Get zones and connections */

        // There are two types of zones: a true zone and a crossing
        //   Crossings are zones that are not actually zones, but "helper" zones
        //   They are worth 0 points and are usually used to reduce the amount of connections
        ZoneSet trueZones = kml.getZones("Zones");
        ZoneSet crossings = kml.getZones("Crossings");
        
        // Collect all zones into a single set
        ZoneSet allZones = ZoneSet.union(trueZones, crossings);

        // Get connections
        ConnectionSet connections = kml.getConnections("Connections", allZones);

        // Get points
        trueZones.initPoints();

        // use depth first search with a single route object
        // if it's valid and finished then copy and save
    }
}
