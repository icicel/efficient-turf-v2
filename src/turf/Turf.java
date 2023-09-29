package turf;
import java.io.IOException;
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

    public double timeLimit;
    public Zone startZone;
    public Zone endZone;

    public Zone[] blacklist;
    public Zone[] whitelist;

    public String username;
    public double speed;
    public double waitTime;

    // Give layer names of layers containing real zones, crossings, and connections for the
    //   program to search through
    // Set a layer name to null to search through all layers
    public Turf(TurfSettings settings, String kmlFile, String realZoneLayer, String crossingLayer, String connectionLayer)
    throws IOException, InterruptedException, SAXException, ParserConfigurationException {
        KML kml = new KML(kmlFile);
        ZoneSet realZones = kml.getZones(realZoneLayer);
        realZones.initPoints();
        ZoneSet crossings = kml.getZones(crossingLayer);
        this.zones = ZoneSet.union(realZones, crossings);

        this.connections = kml.getConnections(connectionLayer, zones);

        applySettings(settings);
    }
    
    // Give layer names of layers containing real zones and connections for the
    //   program to search through
    // Set a layer name to null to search through all layers
    public Turf(TurfSettings settings, String kmlFile, String zoneLayer, String connectionLayer)
    throws IOException, InterruptedException, SAXException, ParserConfigurationException {
        KML kml = new KML(kmlFile);
        this.zones = kml.getZones(zoneLayer);
        zones.initPoints();

        this.connections = kml.getConnections(connectionLayer, zones);

        applySettings(settings);
    }

    private void applySettings(TurfSettings settings) {
        // Check for undefined settings
        if (settings.timeLimit == null) throw new IllegalArgumentException("Time limit undefined");
        if (settings.startZone == null) throw new IllegalArgumentException("Start zone undefined");
        if (settings.endZone == null) throw new IllegalArgumentException("End zone undefined");

        // Apply settings
        this.timeLimit = settings.timeLimit;
        this.startZone = zones.findByName(settings.startZone);
        this.endZone = zones.findByName(settings.endZone);

        if (settings.blacklist != null) {
            this.whitelist = namesToZones(settings.blacklist);
        }
        if (settings.whitelist != null) {
            this.whitelist = namesToZones(settings.whitelist);
        }

        this.username = settings.username;
        this.speed = settings.speed;
        this.waitTime = settings.waitTime;
    }

    // Convert an array of zone names to an array of corresponding Zone objects
    private Zone[] namesToZones(String[] names) {
        Zone[] zones = new Zone[names.length];
        for (int i = 0; i < names.length; i++) {
            zones[i] = this.zones.findByName(names[i]);
        }
        return zones;
    }
}
