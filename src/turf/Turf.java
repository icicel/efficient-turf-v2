package turf;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import org.json.JSONArray;
import org.json.JSONObject;
import kml.KML;
import kml.XML;
import nu.xom.ParsingException;
import util.Logging;

// Represents a collection of zones and crossings, and connections between them
// Crossings can either be given directly, or be implied from where connections overlap
public class Turf extends Logging {

    public Set<Point> crossings;
    public Set<Point> zones;
    public Set<Connection> connections;

    public boolean compressed = false;

    // Get zones and connections from the given KML file, no crossings
    // Only use if you don't actually use any crossings!
    public Turf(Path kmlPath, String zoneLayer, String connectionLayer)
    throws IOException, InterruptedException, ParsingException {
        this(kmlPath, zoneLayer, null, connectionLayer);
    }

    // Get zones, crossings and connections from the given KML file
    public Turf(Path kmlPath, String zoneLayer, String crossingLayer, String connectionLayer)
    throws IOException, InterruptedException, ParsingException {
        log("Turf: *** Initializing...");

        log("Turf: Reading KML at " + kmlPath + "...");
        KML kml = new KML(kmlPath);

        // Init zones
        this.zones = kml.points.get(zoneLayer);
        initZones(this.zones);

        // Init crossings only if crossingLayer is given
        if (crossingLayer != null) {
            this.crossings = kml.points.get(crossingLayer);
            log("Turf: Found " + crossings.size() + " crossings");
        }

        // Init connections
        log("Turf: Initializing connections...");
        this.connections = kml.lines.get(connectionLayer);
        Set<Point> allPoints = new HashSet<>(this.crossings);
        allPoints.addAll(this.zones);
        for (Connection connection : this.connections) {
            Point leftPoint = closestPoint(allPoints, connection.left);
            Point rightPoint = closestPoint(allPoints, connection.right);
            connection.overrideEndpoints(leftPoint, rightPoint);
            connections.add(connection);
        }

        log("Turf: *** Initialized with " + crossings.size() + " crossings and " + connections.size() + " connections");
    }

    // Get zones from the given KML file
    // Get connections from Overpass API
    // Imply crossings
    public Turf(Path zoneKml)
    throws IOException, InterruptedException, ParsingException {
        this(zoneKml, null);
    }

    // Get zones from the given KML file
    // Get connections from the given Overpass XML file
    // Imply crossings
    public Turf(Path zoneKml, Path networkXml)
    throws IOException, InterruptedException, ParsingException {
        log("Turf: *** Initializing...");

        log("Turf: Reading KML at " + zoneKml + "...");
        KML kml = new KML(zoneKml);

        // Init zones
        this.zones = kml.points.get("Turf Zones");
        for (Point zone : this.zones) {
            // Remove " POI" from end of name
            zone.name = zone.name.substring(0, zone.name.length() - 4).toLowerCase();
        }
        initZones(this.zones);


        // Find zones' bounding box
        double minLat = Double.POSITIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;
        for (Point zone : zones) {
            minLat = Math.min(minLat, zone.lat);
            maxLat = Math.max(maxLat, zone.lat);
            minLon = Math.min(minLon, zone.lon);
            maxLon = Math.max(maxLon, zone.lon);
        }
        // A small buffer
        minLat -= 0.01;
        maxLat += 0.01;
        minLon -= 0.01;
        maxLon += 0.01;
        // If an XML is provided, use it instead of calling the API
        XML xml;
        if (networkXml == null) {
            log("Turf: Getting XML from Overpass API...");
            xml = new XML(minLat, minLon, maxLat, maxLon);
        } else {
            log("Turf: Reading XML at " + networkXml + "...");
            xml = new XML(networkXml);
        }


        // Init connections and crossings
        log("Turf: Initializing connections...");
        this.connections = xml.ways;
        // Get rid of duplicates, map each Point to the single Point object that represents it
        // Ironically means each key maps to itself
        Map<Point, Point> existing = new HashMap<>();
        for (Connection connection : this.connections) {
            // Get existing point at position if available, otherwise place our point there
            // Wow, a rare use case for the identity function!
            Point left = existing.computeIfAbsent(connection.left, Function.identity());
            Point right = existing.computeIfAbsent(connection.right, Function.identity());
            if (left.isNeighbor(right)) {
                // There is already a connection here
                // Having identical connections is bad, so make them slightly different
                // If they actually are different, this won't cause any issues
                // Kinda hacky...
                connection.distance += 0.001;
            }
            connection.overrideEndpoints(left, right);
        }
        this.crossings = new HashSet<>(existing.values());


        // Find the network (connected component) with the most crossings, and remove
        //  everything else
        log("Turf: Pruning smaller networks...");
        List<Set<Point>> networks = new ArrayList<>();
        Set<Point> visited = new HashSet<>();
        for (Point crossing : crossings) {
            if (visited.contains(crossing)) {
                continue;
            }
            Set<Point> network = new HashSet<>();
            Queue<Point> queue = new LinkedList<>();
            queue.add(crossing);
            while (!queue.isEmpty()) {
                Point current = queue.remove();
                if (visited.contains(current)) {
                    continue;
                }
                visited.add(current);
                network.add(current);
                for (Connection connection : current.parents) {
                    Point neighbor = connection.other(current);
                    if (!visited.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
            networks.add(network);
        }
        Set<Point> largestNetwork = networks.stream()
            .max(Comparator.comparingInt(Set::size))
            .orElseThrow();
        for (Point crossing : new HashSet<>(crossings)) {
            if (!largestNetwork.contains(crossing)) {
                crossings.remove(crossing);
                connections.removeAll(crossing.parents);
            }
        }


        // Create direct connections between all zones and their nearest crossing
        // Identify each crossing with its hundredth-degree lat/lon quadrant
        log("Turf: Connecting zones...");
        Map<String, Set<Point>> quadrantMap = new HashMap<>();
        for (Point crossing : this.crossings) {
            String quadrantKey = getQuadrantKey(crossing);
            quadrantMap.computeIfAbsent(quadrantKey, k -> new HashSet<>()).add(crossing);
        }
        for (Point zone : this.zones) {
            // Find closest point in the same quadrant as the zone
            String quadrantKey = getQuadrantKey(zone);
            Set<Point> crossingsInQuadrant = quadrantMap.get(quadrantKey);
            if (crossingsInQuadrant != null) {
                Point closestCrossing = closestPoint(crossingsInQuadrant, zone);
                // Only accept this point if it's closer than the edge of the quadrant
                //  meaning it's closer than any point in a different quadrant
                if (zone.distanceTo(closestCrossing) < distanceToQuadrantEdge(zone)) {
                    Connection connection = new Connection(zone, closestCrossing);
                    connections.add(connection);
                    continue;
                }
            }
            // Failure, search globally
            // This shouldn't be too common since zones are usually close to OSM highways
            Point closestCrossing = closestPoint(this.crossings, zone);
            Connection connection = new Connection(zone, closestCrossing);
            connections.add(connection);
        }


        // Remove connection chains/linear intersections/cases where a point has only 2 parents
        // This will be the case for most connections
        log("Turf: Simplifying " + connections.size() + " connections...");
        Set<Point> toCheck = new HashSet<>(crossings);
        for (Point point : toCheck) {
            if (point.parents.size() != 2) {
                continue;
            }
            mergeOverPivot(point);
        }

        log("Turf: *** Initialized with " + crossings.size() + " crossings and " + connections.size() + " connections");
    }

    /* Zone points */

    // Initialize Zones using the Turf API
    // Take each Point's name and link it to the corresponding Zone
    private void initZones(Set<Point> zonePoints)
    throws IOException, InterruptedException {
        // Gather the Points per zone name
        Map<String, Point> pointsByName = new HashMap<>();
        for (Point zone : zonePoints) {
            pointsByName.put(zone.name, zone);
        }
        Set<String> zoneNames = pointsByName.keySet();
        log("Turf: Found " + pointsByName.size() + " zones, getting points from API...");

        // Create a JSON body with all zone names
        // [{"name": "zonea"}, {"name": "zoneb"}, ...]
        JSONArray requestJson = new JSONArray();
        for (String zoneName : zoneNames) {
            JSONObject object = new JSONObject();
            object.put("name", zoneName);
            requestJson.put(object);
        }

        // Post the JSON to the API and get a response containing zone information
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.turfgame.com/v5/zones"))
            .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString()))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Turf API request failed with status code " + response.statusCode());
        }

        JSONArray resultJson = new JSONArray(response.body());

        // Check that the response contains the same number of zones as the request
        // Should always be less - the API will ignore nonexistant zones
        // If not, tries to find those nonexistant zones
        if (resultJson.length() != zoneNames.size()) {
            warn("WARNING: API response contains less zones than requested! Investigating...");
            Set<String> returnedZones = new HashSet<>();
            for (int i = 0; i < resultJson.length(); i++) {
                JSONObject zoneInfo = resultJson.getJSONObject(i);
                String name = zoneInfo.getString("name").toLowerCase();
                returnedZones.add(name);
            }
            for (String zoneName : zoneNames) {
                if (!returnedZones.contains(zoneName)) {
                    warn("ERROR: Zone " + zoneName + " does not exist in the API");
                }
            }
            throw new RuntimeException("Tried to set points for nonexistant zones");
        }

        // Compile zones
        Set<Zone> zones = new HashSet<>();
        for (int i = 0; i < resultJson.length(); i++) {
            JSONObject zoneInfo = resultJson.getJSONObject(i);
            Zone zone = new Zone(zoneInfo);
            boolean added = zones.add(zone);
            if (!added) {
                warn("WARNING: Duplicate zone " + zone.name);
            }
        }

        // Compile points
        for (Zone zone : zones) {
            Point zonePoint = pointsByName.get(zone.name);
            zonePoint.zone = zone;
        }
        log("Turf: Points set");
    }

    // Extract an array of unique zones from the source of https://turfa.nu/map/unique
    // These are zones that the user has taken before
    public static String[] getUniqueZones(Path sourcePath) throws IOException {
        // Find the single-line window.MapConfig object in the HTML file
        String html = Files.readString(sourcePath);
        String marker = "window.MapConfig = ";
        int startIndex = html.indexOf(marker) + marker.length();
        int endIndex = html.indexOf(";\r\n", startIndex);
        JSONObject mapConfig = new JSONObject(html.substring(startIndex, endIndex));
        JSONArray features = mapConfig.getJSONObject("data")
            .getJSONObject("sources")
            .getJSONObject("unique")
            .getJSONObject("data")
            .getJSONArray("features");
        // Collect
        String[] zoneNames = new String[features.length()];
        for (int i = 0; i < features.length(); i++) {
            String zoneName = features.getJSONObject(i)
                .getJSONObject("properties")
                .getString("title").toLowerCase();
            zoneNames[i] = zoneName;
        }
        return zoneNames;
    }

    /* Network optimization */

    // Attempt to reduce the size of the network as much as possible without affecting it,
    //  by merging/removing "superfluous" points and connections, like dead ends, loops, and
    //  connections that are too long to matter
    public void compress() {
        log("Turf: Compressing...");
        // Remove all dead ends, loops, and longcuts
        Set<Connection> toCheck = new HashSet<>(this.connections);
        for (Connection connection : toCheck) {
            checkConnection(connection);
        }
        this.compressed = true;
        log("Turf: *** Compressed to " + crossings.size() + " crossings and " + connections.size() + " connections");
    }

    // Merge two neighboring connections across a pivot crossing
    // The pivot must have no other parents besides the two connections being merged
    // Return the resulting connection
    private Connection mergeOverPivot(Point pivot) {
        if (pivot.parents.size() != 2) {
            throw new IllegalArgumentException("Can only merge over a pivot with exactly 2 parents");
        }
        if (zones.contains(pivot)) {
            throw new IllegalArgumentException("Cannot merge over a pivot that is a zone");
        }
        Iterator<Connection> iterator = pivot.parents.iterator();
        Connection connection = iterator.next();
        Connection neighbor = iterator.next();
        Point leftEnd = neighbor.other(pivot);
        Point rightEnd = connection.other(pivot);
        // If one of them are a loop, delete it and return the other
        // The only affected point is the pivot
        if (connection.isLoop()) {
            removeConnection(connection);
            return neighbor;
        } else if (neighbor.isLoop()) {
            removeConnection(neighbor);
            return connection;
        }
        // The chain looks like leftEnd-neighbor-pivot-connection-rightEnd
        // Convert this to leftEnd-neighbor-rightEnd, removing pivot and connection
        // The direction of neighbor may be flipped
        List<Point> newMiddle = new ArrayList<>();
        newMiddle.addAll(neighbor.middleFromPOVOf(leftEnd));
        newMiddle.add(pivot);
        newMiddle.addAll(connection.middleFromPOVOf(pivot));
        neighbor.left = leftEnd;
        neighbor.middle = newMiddle;
        neighbor.right = rightEnd;
        // Update neighbor's connections
        neighbor.distance += connection.distance;
        rightEnd.parents.add(neighbor);
        // Remove pivot and connection from the network
        this.crossings.remove(pivot);
        this.connections.remove(connection);
        rightEnd.parents.remove(connection);
        return neighbor;
    }

    private void removeConnection(Connection connection) {
        this.connections.remove(connection);
        connection.left.parents.remove(connection);
        connection.right.parents.remove(connection);
    }

    private void mergeAndCheck(Point pivot) {
        Connection mergedConnection = mergeOverPivot(pivot);
        checkConnection(mergedConnection);
    }

    private void removeAndCheck(Connection connection) {
        removeConnection(connection);
        checkCrossing(connection.left);
        checkCrossing(connection.right);
    }

    private void checkConnection(Connection connection) {
        if (!this.connections.contains(connection)) {
            return;
        }
        if (connection.isLoop()) {
            removeAndCheck(connection);
        } else if (connection.left.isDeadEnd() || connection.right.isDeadEnd()) {
            removeAndCheck(connection);
        } else if (isLongcut(connection)) {
            removeAndCheck(connection);
        }
    }

    private void checkCrossing(Point crossing) {
        if (!this.crossings.contains(crossing)) {
            return;
        }
        if (crossing.parents.size() == 0) {
            this.crossings.remove(crossing);
        } else if (crossing.parents.size() == 1) {
            removeAndCheck(crossing.parents.iterator().next());
        } else if (crossing.parents.size() == 2) {
            mergeAndCheck(crossing);
        }
    }

    private boolean isLongcut(Connection connection) {
        Point start = connection.left;
        Point end = connection.right;
        Map<Point, Double> distances = new HashMap<>();
        PriorityQueue<Point> queue = new PriorityQueue<>(
            Comparator.comparingDouble(distances::get)
        );
        distances.put(start, 0.0);
        queue.add(start);
        while (true) {
            Point current = queue.remove();
            if (current == end) {
                break;
            }
            for (Connection nextConnection : current.parents) {
                Point next = nextConnection.other(current);
                double distanceToNext = distances.get(current) + nextConnection.distance;
                if (!distances.containsKey(next)) {
                    distances.put(next, distanceToNext);
                    queue.add(next);
                } else if (distanceToNext < distances.get(next)) {
                    distances.put(next, distanceToNext);
                }
            }
        }
        return !(distances.get(end) == connection.distance);
    }

    // Remove all connections except those who are part of some shortest path between
    //  any two zones
    // Works similarly to Node.findFastestRoutes and uses a similar Route class
    public void optimize() {
        if (!compressed) {
            warn("WARNING: Optimize works better on a compressed Turf");
        }
        log("Turf: Optimizing...");
        Set<Connection> optimalConnections = new HashSet<>();
        Set<Point> hasBeenStart = new HashSet<>();
        for (Point start : zones) {
            hasBeenStart.add(start);
            // Prioritize paths by its last point's distance to start
            PriorityQueue<TurfRoute> queue = new PriorityQueue<>(
                Comparator.comparingDouble(route -> route.length)
            );
            Set<Point> visited = new HashSet<>();
            queue.add(new TurfRoute(start));
            // Dijkstra's time!
            while (!queue.isEmpty()) {
                TurfRoute route = queue.remove();
                Point current = route.point;
                if (visited.contains(current)) {
                    continue;
                }
                visited.add(current);
                // If current is a zone, then route is the shortest path
                // Backtrack the route and add all connections along the way to optimalConnections
                // If current has already been a start point, then we have already found the
                //  shortest path to it, so skip
                if (current.isZone() && !hasBeenStart.contains(current)) {
                    TurfRoute backtrack = route;
                    while (backtrack.previous != null) {
                        optimalConnections.add(backtrack.connectionFromPrevious);
                        backtrack = backtrack.previous;
                    }
                }
                // Extend the route with all connections from the current point
                for (Connection extension : current.parents) {
                    TurfRoute nextRoute = new TurfRoute(extension, route);
                    queue.add(nextRoute);
                }
            }
        }
        // Remove!
        for (Connection connection : new HashSet<>(connections)) {
            if (!optimalConnections.contains(connection)) {
                removeConnection(connection);
            }
        }
        // Cleanup
        // Theoretically, no crossing should have only one parent
        for (Point point : new HashSet<>(crossings)) {
            if (point.parents.size() == 0) {
                crossings.remove(point);
            } else if (point.parents.size() == 2) {
                mergeOverPivot(point);
            }
        }
        log("Turf: *** Optimized to " + crossings.size() + " crossings and " + connections.size() + " connections");
    }

    // Exlusively for Turf.optimize
    // Turf equivalent of scenario.Route - a linked list with extra steps
    public class TurfRoute {
        public Point point;
        public Connection connectionFromPrevious;
        public TurfRoute previous;
        public double length;

        public TurfRoute(Point point) {
            this.point = point;
            this.connectionFromPrevious = null;
            this.previous = null;
            this.length = 0.0;
        }

        public TurfRoute(Connection extension, TurfRoute previous) {
            this.point = extension.other(previous.point);
            this.connectionFromPrevious = extension;
            this.previous = previous;
            this.length = previous.length + extension.distance;
        }
    }

    /* Connecting zones helpers */

    // Returns the closest Point in the given set to a given Point
    public static Point closestPoint(Set<Point> points, Point point) {
        return points.stream()
            .min(Comparator.comparingDouble(p -> point.distanceTo(p)))
            .orElseThrow();
    }

    // Get the key for the quadrant that the given Point falls into
    // Ex: lat=-37.7749, lon=-122.4194 -> quadrantKey="-3777-12241"
    private static String getQuadrantKey(Point point) {
        int latQuadrant = (int) Math.floor(point.lat * 100);
        int lonQuadrant = (int) Math.floor(point.lon * 100);
        return latQuadrant + "" + lonQuadrant;
    }

    // Shortest distance to the edge of the quadrant
    // Ex: lat=-37.7749, lon=-122.4194 -> nearestQuadrantEdge=(-37.77,-122.42)
    private static double distanceToQuadrantEdge(Point point) {
        double latQuadrantEdge = Math.round(point.lat * 100) / 100.0;
        double lonQuadrantEdge = Math.round(point.lon * 100) / 100.0;
        Point nearestQuadrantEdge = new Point(latQuadrantEdge, lonQuadrantEdge);
        return point.distanceTo(nearestQuadrantEdge);
    }

    /* Utility functions */

    // Helper function to get a path to a file in the root directory
    public static Path getRootFilePath(String filename) {
        return FileSystems.getDefault().getPath(".", filename);
    }
}
