package network;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import map.Coords;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;
import util.Logging;

// A collection of Ways representing physical routes between Points
public class Network extends Logging {

    Set<Way> ways;
    Map<Coords, Point> points; // non-zone
    Set<Point> zones;

    public Network(Document networkXml, Path zoneKml) throws IOException, ParsingException {
        this.ways = new HashSet<>();
        this.points = new HashMap<>();
        this.zones = new HashSet<>();

        log("Parsing KML...");
        Builder builder = new Builder();
        Document kml = builder.build(zoneKml.toFile());
        Element kmlRoot = kml.getRootElement();
        Element document = kmlRoot.getFirstChildElement("Document", "http://www.opengis.net/kml/2.2");
        Element folder = document.getFirstChildElement("Folder", "http://www.opengis.net/kml/2.2");
        for (Element placemark : folder.getChildElements("Placemark", "http://www.opengis.net/kml/2.2")) {
            Element point = placemark.getFirstChildElement("Point", "http://www.opengis.net/kml/2.2");
            String name = placemark.getFirstChildElement("name", "http://www.opengis.net/kml/2.2").getValue();
            name = name.substring(0, name.length() - 3); // remove " POI" from the end of the name
            String coordinates = point.getFirstChildElement("coordinates", "http://www.opengis.net/kml/2.2").getValue();
            Coords coords = new Coords(coordinates);
            this.zones.add(new Point(coords, name.toLowerCase()));
        }

        log("Finding bbox...");
        double minLat = Double.POSITIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;
        for (Point zone : zones) {
            minLat = Math.min(minLat, zone.coords.lat);
            maxLat = Math.max(maxLat, zone.coords.lat);
            minLon = Math.min(minLon, zone.coords.lon);
            maxLon = Math.max(maxLon, zone.coords.lon);
        }
        // A small buffer
        minLat -= 0.01;
        maxLat += 0.01;
        minLon -= 0.01;
        maxLon += 0.01;
        
        log("Parsing XML...");
        Element xmlRoot = networkXml.getRootElement();
        for (Element element : xmlRoot.getChildElements("way")) {
            // Create a Point for each node
            Elements nodes = element.getChildElements("nd");
            Point[] points = new Point[nodes.size()];
            for (int i = 0; i < nodes.size(); i++) {
                Element node = nodes.get(i);
                Double lat = Double.parseDouble(node.getAttributeValue("lat"));
                Double lon = Double.parseDouble(node.getAttributeValue("lon"));
                Coords coords = new Coords(lat, lon);
                // Get the existing Point for these coords, create a new one if it doesn't exist
                points[i] = this.points.getOrDefault(coords, new Point(coords, null));
                this.points.put(coords, points[i]);
            }
            // Create Ways between every pair of points
            for (int i = 0; i < points.length - 1; i++) {
                Way way = new Way(points[i], points[i + 1]);
                ways.add(way);
            }
        }

        log("Network initialized with " + ways.size() + " ways, " + points.size() + " points, and " + zones.size() + " zones");
    }

    public static Document fromAPI() throws IOException, ParsingException, InterruptedException {
        StringBuilder data = new StringBuilder();
        data.append("[bbox:57.98,11.71,58.16,11.89];");
        data.append("way[\"highway\"][\"highway\"!=\"motorway\"][\"highway\"!=\"primary\"][\"highway\"!=\"secondary\"];");
        data.append("out skel geom;");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://overpass-api.de/api/interpreter"))
            .POST(HttpRequest.BodyPublishers.ofString("data=" + data.toString()))
            .build();
        
        // Retry the request until it succeeds
        HttpResponse<String> response;
        log("Requesting data from Overpass API...");
        while (true) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            // Check if error page
            if (response.body().contains("!DOCTYPE html")) {
                log("Error, retrying...");
                Thread.sleep(1000);
            } else {
                break;
            }
        }

        Builder builder = new Builder();
        return builder.build(response.body(), null);
    }
    
    public static Document fromFile(Path path) throws IOException, ParsingException {
        Builder builder = new Builder();
        return builder.build(path.toFile());
    }
}
