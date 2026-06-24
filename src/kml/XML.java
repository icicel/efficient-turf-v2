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
            // Find highway type
            Elements tags = element.getChildElements("tag");
            String highway = null;
            for (Element tag : tags) {
                if (tag.getAttributeValue("k").equals("highway")) {
                    highway = tag.getAttributeValue("v");
                    break;
                }
            }
            // Find layer
            int layer = 0;
            for (Element tag : tags) {
                if (tag.getAttributeValue("k").equals("layer")) {
                    layer = Integer.parseInt(tag.getAttributeValue("v"));
                }
            }
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
            double weight = highwayWeight(highway);
            for (int i = 0; i < points.length - 1; i++) {
                Connection connection = new Connection(points[i], points[i + 1], weight);
                connection.layer = layer;
                ways.add(connection);
            }
        }
    }

    // Load from Overpass API with a bounding box
    public XML(double south, double west, double north, double east) throws IOException, ParsingException, InterruptedException {
        StringBuilder data = new StringBuilder();
        data.append("[bbox:" + south + "," + west + "," + north + "," + east + "];");
        data.append("(");
        data.append("way[highway][highway!~motorway][highway!~trunk][highway!=construction][highway!=proposed][highway!=no];");
        data.append("way[route=ferry];");
        data.append(")->.walkable;");
        data.append("(");
        data.append("way.walkable[foot!=no][access!=no][access!=private][!junction];");
        data.append("way.walkable[foot=yes];");
        data.append("way.walkable[foot=designated];");
        data.append(");");
        data.append("out body geom qt;");
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

    public double highwayWeight(String highway) {
        if (highway == null) {
            return 1;
        }
        switch (highway) {
            // we really don't want to walk on primary/secondary but if we have to, we have to
            case "primary":
            case "primary_link":
            case "secondary":
            case "secondary_link":
                return 20;
            // just a slight deprioritization, allows for shortcuts
            case "tertiary":
            case "tertiary_link":
            case "unclassified":
            case "residential":
            case "service":
            case "road":
            case "corridor":
                return 1.25;
            default:
                return 1;
        }
    }
}
