import java.io.IOException;
import java.util.Set;
import java.util.jar.Attributes;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

// Parses a KML object
// The KMLParser object stores an XMLReader object to do the reading, and sets itself as both
// content handler and error handler for the XMLReader
public class KMLParser extends DefaultHandler {

    private XMLReader xmlReader;
    private InputSource kml;

    // Google My Maps-exported KMLs use a <Folder> tag containing a <name> tag to store layers
    // Objects in layers are stored in <Placemark> tags in these folders
    
    public KMLParser(KML kml) throws ParserConfigurationException, SAXException, IOException {
        this.kml = new InputSource(kml.asStream());

        xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        xmlReader.setContentHandler(this);
        xmlReader.setErrorHandler(this);
    }

    private void startElement(String uri, String localName, String qName, Attributes atts) {
        System.out.println(uri + ", " + localName + ", " + qName + ", " + atts);
    }

    // Parses without returning anything
    // Exclusively for testing callback methods
    public void parse() throws IOException, SAXException {
        xmlReader.parse(kml);
    }

    // Get all zones in a layer
    public Set<Zone> getZones(String layerName) throws IOException, SAXException {
        xmlReader.parse(kml);
        return null;
    }

    // Get all connections in a layer
    public Set<Connection> getConnections(String layerName) throws IOException, SAXException {
        xmlReader.parse(kml);
        return null;
    }

}
