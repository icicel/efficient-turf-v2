package turf;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import kml.KML;
import zone.ConnectionSet;
import zone.ZoneSet;

// Represents a collection of real zones, user-defined zones (optionally), and connections between them
//   all extracted from the given KML file
public class Turf {
    
    public ZoneSet zones;
    public ConnectionSet connections;

    // Initialize zones and connections from the given KML file
    // Username is required but can be set to null
    // Give layer names of layers containing real zones, crossings, and connections for the
    //   program to search through
    // Crossings are optional, set to null to ignore them
    // Set a layer name to "!ALL" to search through all layers
    public Turf(Path kmlPath, String realZoneLayer, String crossingLayer, String connectionLayer)
    throws IOException, InterruptedException, SAXException, ParserConfigurationException {
        KML kml = new KML(kmlPath);
        ZoneSet realZones = kml.getZones(realZoneLayer);
        realZones.initPoints();

        if (crossingLayer == null) {
            // crossings remains null
            zones = realZones;
            connections = kml.getConnections(connectionLayer, zones);
            return;
        }

        ZoneSet crossings = kml.getZones(crossingLayer);
        zones = ZoneSet.union(realZones, crossings);
        connections = kml.getConnections(connectionLayer, zones);
    }

    // Helper function to get a path to a file in the root directory
    public static Path getRootFilePath(String filename) {
        return FileSystems.getDefault().getPath(".", filename);
    }
}
