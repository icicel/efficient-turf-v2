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

    // Zones that should not be visited
    public Zone[] blacklist;

    // Zones that must be visited
    public Zone[] whitelist;

    // Speed in meters per minute (ignoring wait times at zones)
    public double speed = 60.0;

    // Time in minutes to wait at each zone
    public double waitTime = 1.0;

    // Initialize zones and connections from the given KML file
    // Username is required but can be set to null
    // Give layer names of layers containing real zones, crossings, and connections for the
    //   program to search through
    // Crossings are optional, set to null to ignore them
    // Set a layer name to "!ALL" to search through all layers
    public Turf(String username, Path kmlPath, String realZoneLayer, String crossingLayer, String connectionLayer)
    throws IOException, InterruptedException, SAXException, ParserConfigurationException {
        KML kml = new KML(kmlPath);
        ZoneSet realZones = kml.getZones(realZoneLayer);
        realZones.initPoints(username);

        if (crossingLayer == null) {
            this.zones = realZones;
            this.connections = kml.getConnections(connectionLayer, zones);
            return;
        }

        ZoneSet crossings = kml.getZones(crossingLayer);
        this.zones = ZoneSet.union(realZones, crossings);
        this.connections = kml.getConnections(connectionLayer, zones);
    }

    // Helper function to get a path to a file in the root directory
    public static Path getRootFilePath(String filename) {
        return FileSystems.getDefault().getPath(".", filename);
    }

    /* Setters */

    public void setSpeed(double speed) {
        this.speed = speed;
    }
    public void setWaitTime(double waitTime) {
        this.waitTime = waitTime;
    }

    // List of names of zones 
    public void setBlacklist(String[] blacklist) {
        this.blacklist = namesToZones(blacklist);
    }
    public void setWhitelist(String[] whitelist) {
        this.whitelist = namesToZones(whitelist);
    }

    // Convert an array of zone names to an array of corresponding Zone objects
    private Zone[] namesToZones(String[] names) {
        if (names == null) {
            return null;
        }
        Zone[] zones = new Zone[names.length];
        for (int i = 0; i < names.length; i++) {
            String name = names[i].toLowerCase();
            zones[i] = this.zones.findByName(name);
        }
        return zones;
    }
}
