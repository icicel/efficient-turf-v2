import java.util.Set;
import kml.KML;
import kml.KMLParser;
import map.Connection;
import map.Zone;

public class Test {
    public static void main(String[] args) throws Exception {
        KML localKml = EfficientTurf.getKML("example.kml");
        KMLParser kml = new KMLParser(localKml);
        Set<Zone> realZones = kml.getZones("Zones");
        Set<Zone> crossings = kml.getZones("Crossings");
        Set<Connection> connections = kml.getConnections("Connections");

        System.out.println(realZones.size());
        System.out.println(crossings.size());
        System.out.println(connections.size());

        Set<Zone> allZones = EfficientTurf.union(realZones, crossings);
        EfficientTurf.checkForDuplicates(allZones);

        for (Connection connection : connections) {
            connection.completeOn(allZones);
        }
        
        for (Zone zone : allZones) {
            System.out.println(zone);
        }
        for (Connection connection : connections) {
            System.out.println(connection);
        }
    }
}
