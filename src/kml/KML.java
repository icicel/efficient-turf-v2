package kml;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import map.Coords;
import map.Line;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

// Takes and parses a KML file
public class KML {

    private static final String KML_NAMESPACE = "http://www.opengis.net/kml/2.2";

    // Map of layer name to set of points/lines in that layer
    public Map<String, Set<Coords>> points;
    public Map<String, Set<Line>> lines;

    // Takes a path to a KML file and parses it, initializing the points and lines collections
    public KML(Path path) throws ParsingException, ValidityException, IOException {
        this.points = new HashMap<>();
        this.lines = new HashMap<>();
        Builder builder = new Builder();
        Document kml = builder.build(path.toFile());
        Element root = kml.getRootElement();
        Element document = root.getFirstChildElement("Document", KML_NAMESPACE);

        // Google My Maps-exported KMLs use <Folder> to store each layer
        // Objects in layers are stored in <Placemark> elements in these folders that contain
        //  either <Point> or <LineString>
        // Both <Point> and <LineString> elements contain <coordinates>
        // Both <Folder> and <Placemark> elements contain <name>
        
        for (Element folder : document.getChildElements("Folder", KML_NAMESPACE)) {
            String folderName = getName(folder);
            Set<Coords> points = new HashSet<>();
            Set<Line> lines = new HashSet<>();
            this.points.put(folderName, points);
            this.lines.put(folderName, lines);
            for (Element placemark : folder.getChildElements("Placemark", KML_NAMESPACE)) {
                String placemarkName = getName(placemark);
                Element point = placemark.getFirstChildElement("Point", KML_NAMESPACE);
                Element lineString = placemark.getFirstChildElement("LineString", KML_NAMESPACE);
                if (point != null) {
                    String coordinates = getCoordinates(point);
                    points.add(new Coords(coordinates, placemarkName));
                }
                if (lineString != null) {
                    String coordinateListString = getCoordinates(lineString);
                    String[] coordinateStrings = coordinateListString.split("\n");
                    Coords[] coordinates = new Coords[coordinateStrings.length];
                    for (int i = 0; i < coordinateStrings.length; i++) {
                        coordinates[i] = new Coords(coordinateStrings[i]);
                    }
                    lines.add(new Line(coordinates));
                }
            }
        }
    }

    // Small cleanup methods
    private String getName(Element element) {
        return element.getFirstChildElement("name", KML_NAMESPACE).getValue().strip();
    }
    private String getCoordinates(Element element) {
        return element.getFirstChildElement("coordinates", KML_NAMESPACE).getValue().strip();
    }
}
