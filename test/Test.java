import kml.KML;
import map.Connection;
import map.Zone;
import turf.ConnectionSet;
import turf.ZoneSet;

public class Test {
    public static void main(String[] args) throws Exception {
        KML kml = new KML("example.kml");
        ZoneSet realZones = kml.getZones("Zones");
        ZoneSet crossings = kml.getZones("Crossings");
        ZoneSet allZones = ZoneSet.union(realZones, crossings);
        ConnectionSet connections = kml.getConnections("Connections", allZones);

        realZones.initPoints();

        System.out.println(realZones.size());
        System.out.println(crossings.size());
        System.out.println(connections.size());
        
        for (Zone zone : allZones) {
            System.out.println(zone + " " + zone.points);
        }
        for (Connection connection : connections) {
            System.out.println(connection + " " + connection.distance);
        }
    }
}
