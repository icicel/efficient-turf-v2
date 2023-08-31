import java.util.jar.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class KMLHandler extends DefaultHandler {

    KMLParser parent;

    public KMLHandler(KMLParser parent) {
        this.parent = parent;
    }

    public void getEvents() {
        return;
    }

    private void startElement(String uri, String localName, String qName, Attributes atts) {
        System.out.println(uri + ", " + localName + ", " + qName + ", " + atts);
    }
}
