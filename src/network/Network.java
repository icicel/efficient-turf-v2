package network;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kml.KML;
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
    Set<Point> points; // non-zone
    Set<Point> zones;

    // Get network XML from Overpass API
    public Network(Path zoneKml) throws IOException, ParsingException, InterruptedException {
        this(zoneKml, null);
    }

    // Use local XML
    public Network(Path zoneKml, Path networkXml) throws IOException, ParsingException, InterruptedException {
        this.ways = new HashSet<>();
        this.points = new HashSet<>();
        this.zones = new HashSet<>();

        log("Parsing KML...");
        KML kml = new KML(zoneKml);
        for (Coords coords : kml.points.get("Turf Zones")) {
            zones.add(new Point(coords));
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
        
        // If an XML is provided, use it instead of calling the API
        Document xml;
        Builder builder = new Builder();
        if (networkXml == null) {
            xml = builder.build(getFromOverpass(minLat, minLon, maxLat, maxLon), null);
        } else {
            xml = builder.build(networkXml.toFile());
        }

        log("Parsing XML...");
        Element root = xml.getRootElement();
        Map<Coords, Point> pointOverlap = new HashMap<>();
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
                points[i] = pointOverlap.getOrDefault(coords, new Point(coords));
                pointOverlap.put(coords, points[i]);
            }
            // Create Ways between every pair of points
            for (int i = 0; i < points.length - 1; i++) {
                Way way = new Way(points[i], points[i + 1]);
                ways.add(way);
            }
        }
        this.points.addAll(pointOverlap.values());

        log("Connecting zones...");
        for (Point zone : zones) {
            // Find and connect to closest point
            Point closestPoint = points.stream()
                .min((p1, p2) -> Double.compare(zone.distanceTo(p1), zone.distanceTo(p2)))
                .orElseThrow();
            Way way = new Way(zone, closestPoint);
            ways.add(way);
        }

        log("Network initialized with " + ways.size() + " ways, " + points.size() + " points, and " + zones.size() + " zones");

        // Remove way chains/linear intersections/cases where a point has only 2 parents
        // This will be the case for most ways
        Set<Way> waysToCheck = new HashSet<>(ways);
        int sizeBefore = ways.size();
        for (Way way : waysToCheck) {
            if (way.left.parents.size() == 2) {
                mergeWayOverPivot(way, way.left);
            } else if (way.right.parents.size() == 2) {
                mergeWayOverPivot(way, way.right);
            } else {
                continue;
            }
        }
        log("Merged " + (sizeBefore - ways.size()) + " way chains");
    }

    // Merge a way into its neighbor across a pivot point
    // Ideally, pivot has exactly 2 parents, but it will also work if it has more
    //  (in that case, an arbitrary neighbor is chosen)
    public void mergeWayOverPivot(Way way, Point pivot) {
        Way neighbor = pivot.parents.stream().filter(w -> w != way).findFirst().orElseThrow();
        Point leftEnd = neighbor.other(pivot);
        Point rightEnd = way.other(pivot);
        List<Coords> newMiddle = new ArrayList<>();
        // The chain looks like leftEnd-neighbor-pivot-way-rightEnd
        // Convert this to leftEnd-neighbor-rightEnd, removing pivot and way
        // The direction of neighbor may be flipped
        neighbor.left = leftEnd;
        newMiddle.addAll(neighbor.middleFromPOVOf(leftEnd));
        newMiddle.add(pivot.coords);
        newMiddle.addAll(way.middleFromPOVOf(pivot));
        neighbor.right = rightEnd;
        // Update neighbor's connections
        neighbor.middle = newMiddle;
        neighbor.distance += way.distance;
        rightEnd.parents.add(neighbor);
        // Remove pivot and way from the network
        points.remove(pivot);
        ways.remove(way);
        rightEnd.parents.remove(way);
    }

    public static String getFromOverpass(double south, double west, double north, double east) throws IOException, ParsingException, InterruptedException {
        StringBuilder data = new StringBuilder();
        data.append("[bbox:" + south + "," + west + "," + north + "," + east + "];");
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
            if (!response.body().contains("!DOCTYPE html")) {
                return response.body();
            }
            log("Error, retrying...");
            Thread.sleep(1000);
        }
    }
}
