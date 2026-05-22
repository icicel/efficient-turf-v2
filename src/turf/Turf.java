package turf;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
public class Turf extends Logging implements Serializable {

    public Set<Point> crossings;
    public Set<Point> zones;
    public Set<Connection> connections;

    private Turf() {}

    // Deserialize
    public static Turf importTurf(Path path) throws IOException {
        log("Turf: *** Initializing...");

        InputStream in = Files.newInputStream(path);
        ObjectInputStream objIn = new ObjectInputStream(in);
        List<?> pointList;
        List<?> connectionList;
        List<?> crossingList;
        List<?> zoneList;
        try {
            pointList = (List<?>) objIn.readObject();
            connectionList = (List<?>) objIn.readObject();
            crossingList = (List<?>) objIn.readObject();
            zoneList = (List<?>) objIn.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("ERROR: Turf not found during import");
        } finally {
            objIn.close();
        }

        // Actually construct the Turf object
        log("Turf: Creating points...");
        Turf turf = new Turf();
        turf.crossings = new HashSet<>();
        turf.zones = new HashSet<>();
        turf.connections = new HashSet<>();
        List<Point> points = new ArrayList<>();
        for (Object o : pointList) {
            Point point = (Point) o;
            point.parents = new HashSet<>();
            points.add(point);
        }
        for (Object o : crossingList) {
            turf.crossings.add(points.get((Integer) o));
        }
        for (Object o : zoneList) {
            turf.zones.add(points.get((Integer) o));
        }

        // Connection time, these are lists of indices into the points list
        log("Turf: Creating connections...");
        for (Object o : connectionList) {
            List<Integer> indices = new ArrayList<>();
            for (Object i : (List<?>) o) {
                indices.add((Integer) i);
            }
            Point[] connection = new Point[indices.size()];
            for (int i = 0; i < indices.size(); i++) {
                connection[i] = points.get(indices.get(i));
            }
            turf.connections.add(new Connection(connection));
        }

        log("Turf: *** Initialized with " + turf.crossings.size() + " crossings and " + turf.connections.size() + " connections");
        return turf;
    }

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
                connection.weightedDistance += 0.001;
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
        // Identify each crossing with a lat/lon quadrant
        log("Turf: Connecting zones...");
        Map<String, Set<Point>> quadrantMap = new HashMap<>();
        for (Point crossing : this.crossings) {
            String quadrantKey = getQuadrantKey(crossing);
            quadrantMap.computeIfAbsent(quadrantKey, k -> new HashSet<>()).add(crossing);
        }
        int c = 1;
        for (Point zone : new HashSet<>(this.zones)) {
            System.out.print("Finding connections... (" + c++ + "/" + zones.size() + ")\r");
            // Find closest point in the same quadrant as the zone
            String quadrantKey = getQuadrantKey(zone);
            Set<Point> crossingsInQuadrant = quadrantMap.get(quadrantKey);
            Point closestCrossing = closestPoint(crossingsInQuadrant, zone);
            // Only accept this point if it's closer than the edge of the quadrant
            //  meaning it's closer than any point in a different quadrant
            if (closestCrossing == null || zone.distanceTo(closestCrossing) > distanceToQuadrantEdge(zone)) {
                // Failure, search globally
                // This shouldn't be too common since zones are usually close to OSM highways
                closestCrossing = closestPoint(this.crossings, zone);
            }
            // Connect to the zone
            Connection connection = new Connection(zone, closestCrossing);
            connections.add(connection);
        }


        log("Turf: Simplifying " + connections.size() + " connections...");
        // Remove zone connections that are too short by placing the zone directly at its neighbor
        for (Point zone : new HashSet<>(this.zones)) {
            if (zone.parents.size() != 1) {
                continue;
            }
            Connection connection = zone.parents.iterator().next();
            if (connection.distance > 30) {
                continue;
            }
            Point neighbor = connection.other(zone);
            neighbor.zone = zone.zone;
            neighbor.name = zone.name;
            zones.remove(zone);
            crossings.remove(neighbor);
            zones.add(neighbor);
            neighbor.parents.remove(connection);
            connections.remove(connection);
        }
        // Remove connection chains/linear intersections/cases where a point has only 2 parents
        // This will be the case for most connections
        for (Point crossing : new HashSet<>(this.crossings)) {
            if (crossing.parents.size() == 2 && !zones.contains(crossing)) {
                mergeOverPivot(crossing);
            }
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
        neighbor.weightedDistance += connection.weightedDistance;
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

    /* Pathfinding */

    // Get routes from a point to all points
    public Map<Point, TurfRoute> routesFrom(Point start) {
        Map<Point, TurfRoute> routes = new HashMap<>();
        PriorityQueue<TurfRoute> queue = new PriorityQueue<>(
            Comparator.comparingDouble(route -> route.weightedDistance)
        );
        Set<Point> visited = new HashSet<>();
        queue.add(new TurfRoute(start));
        while (!queue.isEmpty()) {
            TurfRoute route = queue.remove();
            Point current = route.point;
            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);
            // Save route to current
            routes.put(current, route);
            // Extend the route with all connections from the current point
            for (Connection extension : current.parents) {
                TurfRoute nextRoute = new TurfRoute(extension, route);
                queue.add(nextRoute);
            }
        }
        return routes;
    }

    // Get routes from a point to another point
    public TurfRoute pathfind(Point start, Point end) {
        PriorityQueue<TurfRoute> queue = new PriorityQueue<>(
            Comparator.comparingDouble(route -> route.weightedDistance)
        );
        Set<Point> visited = new HashSet<>();
        queue.add(new TurfRoute(start));
        while (!queue.isEmpty()) {
            TurfRoute route = queue.remove();
            Point current = route.point;
            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);
            // Finish route if we reach end
            if (current.equals(end)) {
                return route;
            }
            // Extend the route with all connections from the current point
            for (Connection extension : current.parents) {
                TurfRoute nextRoute = new TurfRoute(extension, route);
                queue.add(nextRoute);
            }
        }
        throw new RuntimeException("No path found from " + start + " to " + end);
    }

    // Get routes from a point over a subset of points to all reachable points
    // Subset must include start
    public Map<Point, TurfRoute> routesOverSubset(Point start, Set<Point> subset) {
        Map<Point, TurfRoute> routes = new HashMap<>();
        PriorityQueue<TurfRoute> queue = new PriorityQueue<>(
            Comparator.comparingDouble(route -> route.weightedDistance)
        );
        Set<Point> visited = new HashSet<>();
        queue.add(new TurfRoute(start));
        while (!queue.isEmpty()) {
            TurfRoute route = queue.remove();
            Point current = route.point;
            if (visited.contains(current) || !subset.contains(current)) {
                continue;
            }
            visited.add(current);
            // Save route to current
            routes.put(current, route);
            // Extend the route with all connections from the current point
            for (Connection extension : current.parents) {
                TurfRoute nextRoute = new TurfRoute(extension, route);
                queue.add(nextRoute);
            }
        }
        return routes;
    }

    // Get distances from a point to all points
    public Map<Point, Double> distancesFrom(Point start) {
        Map<Point, Double> distances = new HashMap<>();
        PriorityQueue<TurfRoute> queue = new PriorityQueue<>(
            Comparator.comparingDouble(route -> route.distance)
        );
        Set<Point> visited = new HashSet<>();
        queue.add(new TurfRoute(start));
        while (!queue.isEmpty()) {
            TurfRoute route = queue.remove();
            Point current = route.point;
            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);
            // Save distance to current
            distances.put(current, route.distance);
            // Extend the route with all connections from the current point
            for (Connection extension : current.parents) {
                TurfRoute nextRoute = new TurfRoute(extension, route);
                queue.add(nextRoute);
            }
        }
        return distances;
    }

    // Remove all connections except those who are part of some shortest path between
    //  any two zones
    // Works similarly to Node.findFastestRoutes and uses a similar Route class
    public void optimize() {
        log("Turf: Optimizing...");
        Set<Connection> optimalConnections = new HashSet<>();
        Set<Point> hasBeenStart = new HashSet<>();
        int c = 1;
        for (Point start : zones) {
            System.out.print("Finding routes... (" + c++ + "/" + zones.size() + ")\r");
            hasBeenStart.add(start);
            Map<Point, TurfRoute> routes = routesFrom(start);
            for (Point end : zones) {
                // If end has already been a start point, then start->end has already been processed
                if (hasBeenStart.contains(end)) {
                    continue;
                }
                // Backtrack the route and add all connections along the way to optimalConnections
                TurfRoute backtrack = routes.get(end);
                while (backtrack.previous != null) {
                    optimalConnections.add(backtrack.connectionFromPrevious);
                    backtrack = backtrack.previous;
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
        for (Point crossing : new HashSet<>(this.crossings)) {
            if (crossing.parents.size() == 0) {
                crossings.remove(crossing);
            } else if (crossing.parents.size() == 2 && !zones.contains(crossing)) {
                mergeOverPivot(crossing);
            }
        }
        log("Turf: *** Optimized to " + crossings.size() + " crossings and " + connections.size() + " connections");
    }

    // Turf equivalent of scenario.Route - a linked list with extra steps
    public class TurfRoute {
        public Point point;
        public Connection connectionFromPrevious;
        public TurfRoute previous;
        public double distance;
        public double weightedDistance;

        public TurfRoute(Point point) {
            this.point = point;
            this.connectionFromPrevious = null;
            this.previous = null;
            this.distance = 0.0;
            this.weightedDistance = 0.0;
        }

        public TurfRoute(Connection extension, TurfRoute previous) {
            this.point = extension.other(previous.point);
            this.connectionFromPrevious = extension;
            this.previous = previous;
            this.distance = previous.distance + extension.distance;
            this.weightedDistance = previous.weightedDistance + extension.weightedDistance;
        }

        public List<Point> getPoints() {
            List<Point> points;
            if (this.previous == null) {
                points = new ArrayList<>();
            } else {
                points = this.previous.getPoints();
            }
            points.add(this.point);
            return points;
        }
    }

    /* Customization */

    // Insert a dummy zone with no points
    public void insertZone(double lat, double lon, String name) {
        Point zone = new Point(lat, lon, name);
        zone.zone = new Zone();
        Point closest = closestPoint(this.crossings, zone);
        Connection connection = new Connection(zone, closest);
        this.zones.add(zone);
        this.connections.add(connection);
    }

    /* Connecting zones helpers */

    // Returns the closest Point in the given set to a given Point
    public static Point closestPoint(Set<Point> points, Point point) {
        if (points == null || points.isEmpty()) {
            return null;
        }
        return points.stream()
            .min(Comparator.comparingDouble(p -> point.distanceTo(p)))
            .orElse(null);
    }

    // Get the key for the quadrant that the given Point falls into
    private static String getQuadrantKey(Point point) {
        int latQuadrant = (int) Math.floor(point.lat * 50);
        int lonQuadrant = (int) Math.floor(point.lon * 50);
        return latQuadrant + "" + lonQuadrant;
    }

    // Shortest distance to the edge of the quadrant
    private static double distanceToQuadrantEdge(Point point) {
        double latQuadrantEdge = Math.round(point.lat * 50) / 50.0;
        double lonQuadrantEdge = Math.round(point.lon * 50) / 50.0;
        return Math.min(
            point.distanceTo(point.lat, lonQuadrantEdge), 
            point.distanceTo(latQuadrantEdge, point.lon)
        );
    }
}
