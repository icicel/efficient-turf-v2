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
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import kml.KML;
import kml.XML;
import map.Coords;
import map.Line;
import nu.xom.ParsingException;
import util.Logging;

// Represents a collection of zones and crossings, and connections between them
// Crossings can either be given directly, or be implied from where connections overlap
public class Turf extends Logging {
    
    public Set<Point> crossings;
    public Set<Point> zones;
    public Set<Connection> connections;
    
    // Get zones and connections from the given KML file, no crossings
    // Only use if you don't actually use any crossings!
    public Turf(Path kmlPath, String zoneLayer, String connectionLayer)
    throws IOException, InterruptedException, ParsingException {
        this(kmlPath, zoneLayer, null, connectionLayer);
    }

    // Get zones, crossings and connections from the given KML file
    public Turf(Path kmlPath, String zoneLayer, String crossingLayer, String connectionLayer)
    throws IOException, InterruptedException, ParsingException {

        log("Turf: Reading KML at " + kmlPath + "...");
        KML kml = new KML(kmlPath);

        // Init zones
        this.zones = initZones(kml.points.get(zoneLayer));


        // Init crossings only if crossingLayer is given
        if (crossingLayer != null) {
            this.crossings = new HashSet<>();
            for (Coords coords : kml.points.get(crossingLayer)) {
                String crossingName = coords.name.toLowerCase();
                Point crossing = new Point(coords);
                boolean added = this.crossings.add(crossing);
                if (!added) {
                    warn("WARNING: Duplicate crossing " + crossingName);
                }
                
            }
            log("Turf: Found " + crossings.size() + " crossings");
        }


        // Init connections
        this.connections = new HashSet<>();
        Set<Point> allPoints = new HashSet<>(this.crossings);
        allPoints.addAll(this.zones);
        for (Line line : kml.lines.get(connectionLayer)) {
            Point leftPoint = closestPoint(allPoints, line.left);
            Point rightPoint = closestPoint(allPoints, line.right);
            Connection connection = new Connection(line, leftPoint, rightPoint);
            connections.add(connection);
        }
        log("Turf: Found " + connections.size() + " connections");
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

        log("Turf: Reading KML at " + zoneKml + "...");
        KML kml = new KML(zoneKml);

        // Init zones
        Set<Coords> zoneCoords = kml.points.get("Turf Zones");
        for (Coords coords : zoneCoords) {
            // Remove " POI" from end of name
            coords.name = coords.name.substring(0, coords.name.length() - 4);
        }
        this.zones = initZones(zoneCoords);


        // Find zones' bounding box
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
        this.connections = new HashSet<>();
        Map<Coords, Point> crossingOverlap = new HashMap<>();
        for (Line line : xml.lines) {
            Coords left = line.left;
            Coords right = line.right;
            // Get matching point from the map if it exists, create a new one if it doesn't
            Point leftPoint = crossingOverlap.computeIfAbsent(left, k -> new Point(k));
            Point rightPoint = crossingOverlap.computeIfAbsent(right, k -> new Point(k));
            Connection connection = new Connection(line, leftPoint, rightPoint);
            connections.add(connection);
        }

        this.crossings = new HashSet<>(crossingOverlap.values());


        // Create direct connections between all zones and their nearest crossing
        // Identify each crossing with its hundredth-degree lat/lon quadrant
        log("Turf: Connecting zones...");
        Map<String, Set<Point>> quadrantMap = new HashMap<>();
        for (Point crossing : this.crossings) {
            String quadrantKey = getQuadrantKey(crossing.coords);
            quadrantMap.computeIfAbsent(quadrantKey, k -> new HashSet<>()).add(crossing);
        }
        for (Point zone : this.zones) {
            // Find closest point in the same quadrant as the zone
            String quadrantKey = getQuadrantKey(zone.coords);
            Set<Point> crossingsInQuadrant = quadrantMap.get(quadrantKey);
            if (crossingsInQuadrant != null) {
                Point closestCrossing = closestPoint(crossingsInQuadrant, zone);
                // Only accept this point if it's closer than the edge of the quadrant
                //  meaning it's closer than any point in a different quadrant
                if (zone.distanceTo(closestCrossing) < distanceToQuadrantEdge(zone.coords)) {
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


        // Remove way chains/linear intersections/cases where a point has only 2 parents
        // This will be the case for most ways
        log("Turf: Simplifying " + connections.size() + " connections...");
        Set<Point> toCheck = new HashSet<>(crossings);
        for (Point point : toCheck) {
            if (point.parents.size() != 2) {
                continue;
            }
            mergeOverPivot(point);
        }
        log("Turf: Implied " + crossings.size() + " crossings over " + connections.size() + " connections");
    }

    /* Zone points */
    
    // Convert a set of named Coords into a set of Points with Zones using the Turf API
    private Set<Point> initZones(Set<Coords> zoneCoords)
    throws IOException, InterruptedException {
        // Gather the Coords per zone name
        Map<String, Coords> coordsByName = new HashMap<>();
        for (Coords coords : zoneCoords) {
            coordsByName.put(coords.name.toLowerCase(), coords);
        }
        Set<String> zoneNames = coordsByName.keySet();
        log("Turf: Found " + coordsByName.size() + " zones, getting points from API...");

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
        Set<Point> zonePoints = new HashSet<>();
        for (Zone zone : zones) {
            Coords coords = coordsByName.get(zone.name);
            Point zonePoint = new Point(coords, zone);
            zonePoints.add(zonePoint);
        }
        log("Turf: Points set");
        
        return zonePoints;
    }

    /* Network optimization */

    // Attempt to reduce the size of the network as much as possible without affecting it,
    //  by merging/removing "superfluous" points and ways, like dead ends, loops, and ways
    //  that are too long to matter
    public void compress() {
        // Remove all dead ends, loops, and longcuts
        Set<Connection> toCheck = new HashSet<>(this.connections);
        for (Connection connection : toCheck) {
            checkConnection(connection);
        }
    }

    // Merge two neighboring ways across a pivot crossing
    // The pivot must have no other parents besides the two connections being merged
    // Return the resulting way
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
        List<Coords> newMiddle = new ArrayList<>();
        newMiddle.addAll(neighbor.middleFromPOVOf(leftEnd));
        newMiddle.add(pivot.coords);
        newMiddle.addAll(connection.middleFromPOVOf(pivot));
        neighbor.left = leftEnd;
        neighbor.middle = newMiddle;
        neighbor.right = rightEnd;
        // Update neighbor's connections
        neighbor.distance += connection.distance;
        rightEnd.parents.add(neighbor);
        // Remove pivot and way from the network
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
        System.out.println("Checking connection " + connection);
        if (!this.connections.contains(connection)) {
            System.out.println("Connection not in network");
            return;
        }
        if (connection.isLoop()) {
            System.out.println("Found loop: " + connection);
            removeAndCheck(connection);
        } else if (connection.left.isDeadEnd() || connection.right.isDeadEnd()) {
            System.out.println("Found dead end: " + connection);
            removeAndCheck(connection);
        } else if (isLongcut(connection)) {
            System.out.println("Found longcut: " + connection);
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

    /* Connecting zones helpers */

    // Returns the closest Point in the given set to a given Point
    public static Point closestPoint(Set<Point> points, Point point) {
        return closestPoint(points, point.coords);
    }

    // Returns the closest Point in the given set to a given Coords
    public static Point closestPoint(Set<Point> points, Coords coords) {
        return points.stream()
            .min((p1, p2) -> Double.compare(coords.distanceTo(p1.coords), coords.distanceTo(p2.coords)))
            .orElseThrow();
    }

    // Get the key for the quadrant that the given coords fall into
    // Ex: lat=-37.7749, lon=-122.4194 -> quadrantKey="-3777-12241"
    private static String getQuadrantKey(Coords coords) {
        int latQuadrant = (int) Math.floor(coords.lat * 100);
        int lonQuadrant = (int) Math.floor(coords.lon * 100);
        return latQuadrant + "" + lonQuadrant;
    }

    // Shortest distance to the edge of the quadrant
    // Ex: lat=-37.7749, lon=-122.4194 -> nearestQuadrantEdge=(-37.77,-122.42)
    private static double distanceToQuadrantEdge(Coords coords) {
        double latQuadrantEdge = Math.round(coords.lat * 100) / 100.0;
        double lonQuadrantEdge = Math.round(coords.lon * 100) / 100.0;
        Coords nearestQuadrantEdge = new Coords(latQuadrantEdge, lonQuadrantEdge);
        return coords.distanceTo(nearestQuadrantEdge);
    }

    /* Export */

    // Export as CSV
    // Creates multiple files if there are more than 2000 ways
    public void exportWays(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("WKT,name\n");
        int connectionId = 1;
        int fileId = 1;
        for (Connection connection : connections) {
            sb.append(connection.isLoop() ? "\"POLYGON ((" : "\"LINESTRING (");
            sb.append(connection.left.coords.lon).append(" ").append(connection.left.coords.lat);
            for (Coords middleCoords : connection.middle) {
                sb.append(", ");
                sb.append(middleCoords.lon).append(" ").append(middleCoords.lat);
            }
            sb.append(", ");
            sb.append(connection.right.coords.lon).append(" ").append(connection.right.coords.lat);
            sb.append(connection.isLoop() ? "))\"," : ")\",");
            sb.append("Line ").append(connectionId++);
            sb.append("\n");
            if (connectionId > 2000) {
                // Reset for next file
                writeSubfile(path, fileId++, sb.toString());
                sb = new StringBuilder();
                sb.append("WKT,name\n");
                connectionId = 1;
            }
        }
        // Write remaining ways
        if (fileId == 1) {
            Files.writeString(path, sb.toString());
        } else if (connectionId > 1) {
            writeSubfile(path, fileId, sb.toString());
        }
    }

    public void exportCrossings(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("WKT,name\n");
        int pointId = 1;
        int fileId = 1;
        for (Point crossing : this.crossings) {
            sb.append("\"POINT (");
            sb.append(crossing.coords.lon).append(" ").append(crossing.coords.lat);
            sb.append(")\",");
            sb.append("Point ").append(pointId++);
            sb.append("\n");
            if (pointId > 2000) {
                // Reset for next file
                writeSubfile(path, fileId++, sb.toString());
                sb = new StringBuilder();
                sb.append("WKT,name\n");
                pointId = 1;
            }
        }
        if (fileId == 1) {
            Files.writeString(path, sb.toString());
        } else if (pointId > 1) {
            writeSubfile(path, fileId, sb.toString());
        }
    }

    public void exportZones(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("WKT,name\n");
        int zoneId = 1;
        int fileId = 1;
        for (Point zone : zones) {
            sb.append("\"POINT (");
            sb.append(zone.coords.lon).append(" ").append(zone.coords.lat);
            sb.append(")\",");
            sb.append(zone.zone);
            sb.append("\n");
            if (zoneId > 2000) {
                // Reset for next file
                writeSubfile(path, fileId++, sb.toString());
                sb = new StringBuilder();
                sb.append("WKT,name\n");
                zoneId = 1;
            }
        }
        if (fileId == 1) {
            Files.writeString(path, sb.toString());
        } else if (zoneId > 1) {
            writeSubfile(path, fileId, sb.toString());
        }
    }

    private void writeSubfile(Path path, int fileId, String content) throws IOException {
        String filename = path.getFileName().toString();
        String extension = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex != -1) {
            extension = filename.substring(dotIndex);
            filename = filename.substring(0, dotIndex);
        }
        Path subfilePath = path.resolveSibling(filename + "_" + fileId + extension);
        Files.writeString(subfilePath, content);
    }

    /* Utility functions */

    // Helper function to get a path to a file in the root directory
    public static Path getRootFilePath(String filename) {
        return FileSystems.getDefault().getPath(".", filename);
    }
}
