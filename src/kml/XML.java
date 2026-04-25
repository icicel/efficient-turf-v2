package kml;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;
import turf.Connection;
import turf.Point;

// Takes and parses an OSM XML file
public class XML {

    public Set<Connection> ways;

    // Load from path
    public XML(Path path) throws IOException, ParsingException {
        Builder builder = new Builder();
        Document xml = builder.build(path.toFile());
        parse(xml);
    }

    private void parse(Document xml) {
        this.ways = new HashSet<>();
        Element root = xml.getRootElement();

        for (Element element : root.getChildElements("way")) {
            // Create a Point for each node
            Elements nodes = element.getChildElements("nd");
            Point[] points = new Point[nodes.size()];
            for (int i = 0; i < nodes.size(); i++) {
                Element node = nodes.get(i);
                String ref = node.getAttributeValue("ref");
                Double lat = Double.parseDouble(node.getAttributeValue("lat"));
                Double lon = Double.parseDouble(node.getAttributeValue("lon"));
                points[i] = new Point(lat, lon, ref);
            }
            // Create Connections between the Points
            for (int i = 0; i < points.length - 1; i++) {
                Connection connection = new Connection(points[i], points[i + 1]);
                ways.add(connection);
            }
        }
    }

    // Load from Overpass API with a bounding box
    public XML(double south, double west, double north, double east) throws IOException, ParsingException, InterruptedException {
        StringBuilder data = new StringBuilder();
        data.append("[bbox:" + south + "," + west + "," + north + "," + east + "];");
        data.append("(");
        data.append("way[\"highway\"][\"highway\"!=\"motorway\"][\"highway\"!=\"trunk\"][\"highway\"!=\"primary\"][\"highway\"!=\"secondary\"][\"access\"!=\"private\"];");
        data.append("way[\"route\"=\"ferry\"];");
        data.append(");");
        data.append("out skel geom;");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://overpass-api.de/api/interpreter"))
            .POST(HttpRequest.BodyPublishers.ofString("data=" + data.toString()))
            .build();

        // Retry the request until it succeeds
        HttpResponse<String> response;
        while (true) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            // Check if error page
            if (!response.body().contains("!DOCTYPE html")) {
                break;
            }
        }
        Builder builder = new Builder();
        Document xml = builder.build(response.body(), null);
        parse(xml);
    }

}
