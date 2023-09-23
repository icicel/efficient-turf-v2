import java.util.Set;
import kml.KML;
import kml.KMLParser;
import turf.Connection;
import turf.Zone;

public class Test {
    public static void main(String[] args) throws Exception {
        KML localKml = EfficientTurf.getKML("example.kml");
        KMLParser kml = new KMLParser(localKml);
        Set<Zone> zones = kml.getZones("Zones");
        Set<Zone> crossings = kml.getZones("Crossings");
        Set<Connection> connections = kml.getConnections("Connections");

        System.out.println(zones.size());
        System.out.println(crossings.size());
        System.out.println(connections.size());
        
        for (Zone zone : zones) {
            System.out.println(zone);
        }
        for (Zone crossing : crossings) {
            System.out.println(crossing);
        }
        for (Connection connection : connections) {
            System.out.println(connection);
            System.out.println(connection.distance);
        }
    }
}
