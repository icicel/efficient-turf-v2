package kml;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import map.Line;
import map.Zone;

// Takes and parses a KML file
public class KMLParser {

    public String kml;

    private XMLReader xmlReader;
    private KMLHandler handler;
    
    // Takes a KML file located in the root directory
    // Stores an XMLReader object to do the reading, and sets a KMLHandler object
    //   as both content and error handler
    public KMLParser(String file) throws ParserConfigurationException, SAXException, IOException {
        Path path = FileSystems.getDefault().getPath(".", file);

        this.kml = Files.readString(path);
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
        StringReader kmlStream = new StringReader(kml);
        InputSource kmlSource = new InputSource(kmlStream);
        xmlReader.parse(kmlSource);
    }

    // Get all zones or connections in layerName
    // Submit null as layerName to parse every layer
    public Set<Zone> getZones(String layerName) throws IOException, SAXException {
        parse(layerName);
        return handler.getZones();
    }
    public Set<Line> getLines(String layerName) throws IOException, SAXException {
        parse(layerName);
        return handler.getLines();
    }
}
