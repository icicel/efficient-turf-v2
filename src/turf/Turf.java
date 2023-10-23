package turf;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import kml.KML;
import zone.ConnectionSet;
import zone.Zone;
import zone.ZoneSet;

// Represents a collection of real zones, user-defined zones (optionally), and connections between them
//   all extracted from the given KML file
public class Turf {
    
    public ZoneSet zones;
    public ConnectionSet connections;

    // In order to refer to zones based on type
    public ZoneSet realZones;
    public ZoneSet crossings;

    // Zones that should not be visited
    public ZoneSet blacklist;

    // Zones that must be visited
    public ZoneSet whitelist;

    // Speed in meters per minute (ignoring wait times at zones)
    public double speed;

    // Time in minutes to wait at each zone
    public double waitTime;

    // Initialize zones and connections from the given KML file
    // Username is required but can be set to null
    // Give layer names of layers containing real zones, crossings, and connections for the
    //   program to search through
    // Crossings are optional, set to null to ignore them
    // Set a layer name to "!ALL" to search through all layers
    public Turf(String username, Path kmlPath, String realZoneLayer, String crossingLayer, String connectionLayer)
    throws IOException, InterruptedException, SAXException, ParserConfigurationException {
        KML kml = new KML(kmlPath);
        realZones = kml.getZones(realZoneLayer);
        realZones.initPoints(username);

        if (crossingLayer == null) {
            // crossings remains null
            zones = realZones;
            connections = kml.getConnections(connectionLayer, zones);
            return;
        }

        crossings = kml.getZones(crossingLayer);
        zones = ZoneSet.union(realZones, crossings);
        connections = kml.getConnections(connectionLayer, zones);

        // Default values
        this.speed = 60.0;
        this.waitTime = 1.0;
    }

    // Set black/whitelist using array of zone names
    public void setBlacklist(String[] blacklist) {
        this.blacklist = namesToZones(blacklist);
    }
    public void setWhitelist(String[] whitelist) {
        this.whitelist = namesToZones(whitelist);
    }

    // Convert an array of zone names to a ZoneSet of corresponding Zone objects
    //   in this.zones
    private ZoneSet namesToZones(String[] names) {
        if (names == null) {
            return null;
        }
        ZoneSet zones = new ZoneSet();
        for (String name : names) {
            Zone zone = this.zones.findByName(name.toLowerCase());
            zones.add(zone);
        }
        return zones;
    }

    // Helper function to get a path to a file in the root directory
    public static Path getRootFilePath(String filename) {
        return FileSystems.getDefault().getPath(".", filename);
    }
}
