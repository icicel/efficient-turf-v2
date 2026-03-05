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
    Map<Coords, Point> points;

    public Network(Document xml) {
        this.ways = new HashSet<>();
        this.points = new HashMap<>();
        
        log("Parsing XML...");
        Element root = xml.getRootElement();
        for (Element element : root.getChildElements("way")) {
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

        log("Finished parsing XML. " + ways.size() + " ways, " + this.points.size() + " points.");
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
