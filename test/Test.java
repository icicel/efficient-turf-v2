import kml.KML;
import map.Connection;
import map.Zone;
import turf.Connections;
import turf.Zones;

public class Test {
    public static void main(String[] args) throws Exception {
        KML kml = new KML("example.kml");
        Zones realZones = kml.getZones("Zones");
        Zones crossings = kml.getZones("Crossings");
        Zones allZones = Zones.union(realZones, crossings);
        Connections connections = kml.getConnections("Connections", allZones);

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
