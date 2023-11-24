package kml;
import java.util.HashSet;
import java.util.Set;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import map.Line;
import map.Coords;

// This class's methods are called by an XMLReader parsing a KML whenever certain
//  events occur, such as at the start or end of a tag (element)
// Google My Maps-exported KMLs use <Folder> to store each layer
// Objects in layers are stored in <Placemark> elements in these folders that contain
//  either <Point> or <LineString>
// Both <Point> and <LineString> elements contain <coordinates>
// Both <Folder> and <Placemark> elements contain <name>
//
// Folder
// |- name
// |- Placemark
// |  |- name
// |  |- Point
// |     |- coordinates
// |- Placemark
//    |- name
//    |- LineString
//       |- coordinates
public class KMLHandler extends DefaultHandler {

    // If targetLayer is null, all layers are parsed
    private String targetLayer;

    private Set<Coords> points;
    private Set<Line> lines;

    // True if in a <Folder> tag and <name> has not been parsed yet
    private boolean searchingForLayerName;
    // True if in a <Placemark> tag and <name> has not been parsed yet
    private boolean searchingForObjectName;

    // True if in the target layer
    private boolean inTargetLayer;
    // True if in a <Point> element
    private boolean parsingPoint;
    // True if in a <LineString> element
    private boolean parsingLine;

    // Stores the latest parsed characters, cleared at the end of each tag
    // Cannot be null because parsed characters are appended
    private String currentChars;
    // Stores the current object name, cleared at the end of each <Placemark>
    private String objectName;

    public KMLHandler() {
        reset();
    }

    // Clears all variables
    public void reset() {
        targetLayer = null;

        points = new HashSet<>();
        lines = new HashSet<>();

        searchingForLayerName = false;
        searchingForObjectName = false;

        inTargetLayer = false;
        parsingPoint = false;
        parsingLine = false;

        currentChars = "";
        objectName = null;
    }

    // A KMLParser uses these methods to get the results of a parse
    public Set<Coords> getPoints() {
        return points;
    }
    public Set<Line> getLines() {
        return lines;
    }
    
    public void setTargetLayer(String targetLayer) {
        this.targetLayer = targetLayer;
    }

    // Called at the start of a tag
    // uri and localName are probably unused for KMLs
    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) {
        switch (name) {

            case "Folder":
                searchingForLayerName = true;
                break;
            
            case "Placemark":
                if (inTargetLayer) {
                    searchingForObjectName = true;
                }
                break;
            
            case "Point":
                if (inTargetLayer) {
                    parsingPoint = true;
                }
                break;
            
            case "LineString":
                if (inTargetLayer) {
                    parsingLine = true;
                }
                break;

            default:
                break;
        }

        currentChars = "";
    }

    // Called at the end of an element
    // At this point, currentChars contains the characters immediately before the end tag
    //  which will be the element data if the element is <name> or <coordinates>
    @Override
    public void endElement(String uri, String localName, String name) {
        switch (name) {

            case "name":
                if (searchingForLayerName) {
                    if (targetLayer.equals("!ALL") || targetLayer.equals(currentChars)) {
                        inTargetLayer = true;
                    }
                    searchingForLayerName = false;
                } 
                if (searchingForObjectName) {
                    objectName = currentChars.strip();
                    searchingForObjectName = false;
                }
                break;
            
            case "coordinates":
                if (parsingPoint) {
                    Coords newPoint = new Coords(objectName, currentChars);
                    points.add(newPoint);
                } 
                if (parsingLine) {
                    Line newLine = new Line(currentChars);
                    lines.add(newLine);
                }
                break;

            case "Folder":
                inTargetLayer = false;
                break;

            case "Placemark":
                objectName = null;
                parsingLine = false;
                parsingPoint = false;
                break;

            default:
                break;
        }

        currentChars = "";
    }

    // Called between starts and ends of tags, receives the characters inbetween as
    //  parameters (actually a substring of seemingly the full file)
    @Override
    public void characters(char fullFile[], int start, int length) {
        String string = new String(fullFile, start, length);
        currentChars += string;
    }
}
