import java.util.HashSet;
import java.util.Set;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

// This class's methods are called by an XMLReader parsing a KML whenever certain
//  events occur, such as at the start or end of a tag (element)
// Google My Maps-exported KMLs use a <Folder> tag to store each layer
// Objects in layers are stored in <Placemark> tags in these folders that contain
//  either <Point> (for Zones) or <LineString> (for Connections)
// Both <Folder> and <Placemark> tags also contain <name> tags
public class KMLHandler extends DefaultHandler {

    // If targetLayer is null, all layers are parsed
    private String targetLayer;

    private Set<Zone> zones;
    private Set<Connection> connections;

    public KMLHandler() {
        resetStorage();
    }

    // Clears parsed zones and connections
    public void resetStorage() {
        zones = new HashSet<>();
        connections = new HashSet<>();
    }

    // A KMLParser uses these methods to get the results of a parse
    public Set<Zone> getZones() {
        return zones;
    }
    public Set<Connection> getConnections() {
        return connections;
    }
    
    public void setTargetLayer(String targetLayer) {
        this.targetLayer = targetLayer;
    }

    @Override
    public void startElement(String uri, String name, String qName, Attributes attributes) {
        System.out.println("Start " + qName);
    }

    @Override
    public void endElement(String uri, String name, String qName) {
        System.out.println("End " + qName);
    }

    @Override
    public void characters(char chars[], int start, int length) {
        String string = new String(chars, start, length);
        System.out.println("Characters " + string);
    }
}
