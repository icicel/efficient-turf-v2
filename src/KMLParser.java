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

    // Google My Maps-exported KMLs use a <Folder> tag containing a <name> tag to store layers
    // Objects in layers are stored in <Placemark> tags in these folders
    
    // Stores an XMLReader object to do the reading, and sets a KMLHandler
    //  linked to itself as both content and error handler
    public KMLParser(KML kml) throws ParserConfigurationException, SAXException, IOException {
        this.kml = kml;
        this.xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();

        KMLHandler handler = new KMLHandler(this);
        xmlReader.setContentHandler(handler);
        xmlReader.setErrorHandler(handler);
    }

    // Can be used as an empty parse to debug a KMLHandler
    public void parse() throws IOException, SAXException {
        InputSource kmlSource = new InputSource(kml.asReader());
        xmlReader.parse(kmlSource);
    }

    // Get all zones in a layer
    public Set<Zone> getZones(String layerName) throws IOException, SAXException {
        parse();
        return null;
    }

    // Get all connections in a layer
    public Set<Connection> getConnections(String layerName) throws IOException, SAXException {
        parse();
        return null;
    }
}
