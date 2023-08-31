import java.io.IOException;
import java.util.Set;
import java.util.jar.Attributes;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class KMLParser extends DefaultHandler {

    private String document;
    private String activeLayer;
    
    public KMLParser(String documentPath) throws ParserConfigurationException, SAXException, IOException {
        XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        DefaultHandler handler = this;
        xmlReader.setContentHandler(handler);
        xmlReader.setErrorHandler(handler);
        xmlReader.parse(documentPath);
    }

    private void startElement(String uri, String localName, String qName, Attributes atts) {
        System.out.println(uri + ", " + localName + ", " + qName + ", " + atts);
    }

    public Set<Zone> getZones(String layerName) {
        return null;
    }

    public Set<Connection> getConnections(String layerName) {
        return null;
    }

}
