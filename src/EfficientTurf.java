import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import kml.KML;
import kml.KMLParser;
import turf.Connection;
import turf.Zone;

public class EfficientTurf {

    public static void main(String[] args) throws Exception {
        KML localKml = EfficientTurf.getKML("example.kml");
        KMLParser kml = new KMLParser(localKml);

        // Get zones and connections
        // There are two types of zones: a true zone and a crossing
        //   Crossings are zones that are not actually zones, but "helper" zones
        //   They are worth 0 points and are used to reduce the amount of connections
        Set<Zone> trueZones = kml.getZones("Zones");
        Set<Zone> crossings = kml.getZones("Crossings");
        Set<Connection> connections = kml.getConnections("Connections");
        
        Set<Zone> allZones = union(trueZones, crossings);

        // Connect crossings to their closest zone
        for (Connection connection : connections) {
            connection.completeOn(allZones);
        }

        // use depth first search with a single route object
        // if it's valid and finished then copy and save
    }

    // Set union
    public static <T> Set<T> union(Set<T> a, Set<T> b) {
        Set<T> result = new HashSet<>(a);
        result.addAll(b);
        return result;
    }

    // Get a KML file located in the root directory
    public static KML getKML(String file) throws IOException {
        Path path = FileSystems.getDefault().getPath(".", file);
        return new KML(path);
    }
}
