import java.io.IOException;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

// Parses a KML object
public class KMLParser {

    private XMLReader xmlReader;
    private KML kml;
    private KMLHandler handler;
    
    // Stores an XMLReader object to do the reading, and sets a KMLHandler
    //  linked to itself as both content and error handler
    public KMLParser(KML kml) throws ParserConfigurationException, SAXException, IOException {
        this.kml = kml;
        this.xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        this.handler = new KMLHandler();

        xmlReader.setContentHandler(handler);
        xmlReader.setErrorHandler(handler);
    }

    // Can be used as an empty parse to debug a KMLHandler
    // Resets the KMLHandler first
    // A new InputSource is created each time because the old one is closed after use
    public void parse(String layerName) throws IOException, SAXException {
        handler.reset();
        handler.setTargetLayer(layerName);
        InputSource kmlSource = new InputSource(kml.asReader());
        xmlReader.parse(kmlSource);
    }

    // Get all zones or connections in layerName
    // Submit null as layerName to parse every layer
    public Set<Zone> getZones(String layerName) throws IOException, SAXException {
        parse(layerName);
        return handler.getZones();
    }
    public Set<Connection> getConnections(String layerName) throws IOException, SAXException {
        parse(layerName);
        return handler.getConnections();
    }
}
