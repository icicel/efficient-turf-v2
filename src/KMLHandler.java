import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class KMLHandler extends DefaultHandler {

    KMLParser parent;

    public KMLHandler(KMLParser parent) {
        this.parent = parent;
    }

    // Returns information to parent
    public void getEvents() {
        return;
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
