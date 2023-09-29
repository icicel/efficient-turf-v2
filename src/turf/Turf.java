package turf;
import java.io.IOException;
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

    // Give layer names of layers containing real zones, crossings, and connections for the
    //   program to search through
    // Set a layer name to null to search through all layers
    public Turf(String kmlFile, String realZoneLayer, String crossingLayer, String connectionLayer)
    throws IOException, InterruptedException, SAXException, ParserConfigurationException {
        KML kml = new KML(kmlFile);
        ZoneSet realZones = kml.getZones(realZoneLayer);
        realZones.initPoints();
        ZoneSet crossings = kml.getZones(crossingLayer);
        this.zones = ZoneSet.union(realZones, crossings);

        this.connections = kml.getConnections(connectionLayer, zones);
    }
    
    // Give layer names of layers containing real zones and connections for the
    //   program to search through
    // Set a layer name to null to search through all layers
    public Turf(String kmlFile, String zoneLayer, String connectionLayer)
    throws IOException, InterruptedException, SAXException, ParserConfigurationException {
        KML kml = new KML(kmlFile);
        this.zones = kml.getZones(zoneLayer);
        zones.initPoints();

        this.connections = kml.getConnections(connectionLayer, zones);
    }
}
