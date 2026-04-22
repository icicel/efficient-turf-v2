package turf;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import map.Coords;
import scenario.Node;
import scenario.Route;
import turf.Turf.TurfRoute;

public class Export {

    // My Maps can accept max 2000 items per file, this handles that
    private static <I> void exportGeneric(
        Path path, String headers, Iterable<I> items, Function<I, String> toData
    ) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(headers).append("\n");
        int fileCount = 1;
        int count = 1;
        for (I item : items) {
            sb.append(toData.apply(item)).append("\n");
            count++;
            if (count > 2000) {
                // Reset for next file
                writeSubfile(path, fileCount++, sb.toString());
                sb = new StringBuilder();
                sb.append(headers).append("\n");
                count = 1;
            }
        }
        if (fileCount == 1) {
            Files.writeString(path, sb.toString());
        } else if (count > 1) {
            writeSubfile(path, fileCount, sb.toString());
        }
    }

    // Export as CSV
    // Creates multiple files if there are more than 2000 connections
    public static void exportConnections(Turf turf, Path path) throws IOException {
        exportGeneric(
            path,
            "WKT,name",
            turf.connections,
            connection -> {
                StringBuilder sb = new StringBuilder();
                sb.append(connection.isLoop() ? "\"POLYGON ((" : "\"LINESTRING (");
                sb.append(connection.left.coords.lon).append(" ").append(connection.left.coords.lat);
                for (Coords middleCoords : connection.middle) {
                    sb.append(", ");
                    sb.append(middleCoords.lon).append(" ").append(middleCoords.lat);
                }
                sb.append(", ");
                sb.append(connection.right.coords.lon).append(" ").append(connection.right.coords.lat);
                sb.append(connection.isLoop() ? "))\"," : ")\",");
                sb.append(connection.left).append("-").append(connection.right).append("\n");
                return sb.toString();
            }
        );
    }

    public static void exportCrossings(Turf turf, Path path) throws IOException {
        exportGeneric(
            path,
            "WKT,name",
            turf.crossings,
            crossing -> {
                StringBuilder sb = new StringBuilder();
                sb.append("\"POINT (");
                sb.append(crossing.coords.lon).append(" ").append(crossing.coords.lat);
                sb.append(")\",");
                sb.append(crossing).append("\n");
                return sb.toString();
            }
        );
    }
    
    public static void exportZones(Turf turf, Path path) throws IOException {
        exportGeneric(
            path,
            "WKT,name",
            turf.zones,
            zone -> {
                StringBuilder sb = new StringBuilder();
                sb.append("\"POINT (");
                sb.append(zone.coords.lon).append(" ").append(zone.coords.lat);
                sb.append(")\",");
                sb.append(zone).append("\n");
                return sb.toString();
            }
        );
    }

    public static void exportZonesWithPoints(Turf turf, Path path, String username, boolean isNow) throws IOException {
        exportGeneric(
            path,
            "WKT,name,points",
            turf.zones,
            zone -> {
                StringBuilder sb = new StringBuilder();
                sb.append("\"POINT (");
                sb.append(zone.coords.lon).append(" ").append(zone.coords.lat);
                sb.append(")\",");
                sb.append(zone).append(",");
                sb.append(zone.zone.getPoints(username, isNow)).append("\n");
                return sb.toString();
            }
        );
    }

    private static void writeSubfile(Path path, int fileId, String content) throws IOException {
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

    /* Export Routes using Turfs */

    public static void exportRoute(Route route, Path path, Turf turf) throws IOException {
        StringBuilder sb = new StringBuilder();
        // Get the Points for the Route's Nodes
        List<Point> points = new ArrayList<>();
        Set<Point> allPoints = new HashSet<>(turf.zones);
        allPoints.addAll(turf.crossings);
        for (Node node : route.getNodes()) {
            Point point = allPoints.stream()
                .filter(p -> p.toString().equals(node.name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No point found for node " + node.name));
            points.add(point);
        }
        // Get the Connections for every leg (Link) of the Route
        // Note a Link can correspond to multiple Connections, thus the double list
        List<List<Connection>> connectionRoute = new ArrayList<>();
        // every pair of points
        for (int i = 0; i < points.size() - 1; i++) {
            Point start = points.get(i);
            Point end = points.get(i + 1);
            connectionRoute.add(pathfind(turf, start, end));
        }
        connectionRoute.add(new ArrayList<>()); // No connections for last point
        // Export!
        // Iterate over both lists in parallel
        sb.append("WKT,name").append("\n");
        for (int i = 0; i < route.getNodes().size(); i++) {
            Point point = points.get(i);
            List<Connection> connections = connectionRoute.get(i);
            // Point
            sb.append("\"POINT (");
            sb.append(point.coords.lon).append(" ").append(point.coords.lat);
            sb.append(")\",");
            sb.append(point).append("\n");
            if (connections.isEmpty()) {
                continue;
            }
            // Connections
            Point current = point;
            sb.append("\"LINESTRING (");
            for (Connection connection : connections) {
                // get the correct view of the connection (left->right or right->left)
                List<Coords> middle = connection.middleFromPOVOf(current);
                sb.append(current.coords.lon).append(" ").append(current.coords.lat).append(", ");
                for (Coords middleCoords : middle) {
                    sb.append(middleCoords.lon).append(" ").append(middleCoords.lat).append(", ");
                }
                current = connection.other(current);
            }
            sb.append(current.coords.lon).append(" ").append(current.coords.lat);
            sb.append(")\",");
            sb.append(point).append("-").append(current).append("\n");
        }
        Files.writeString(path, sb.toString());
    }

    // Prints a Google Maps URL with the route's points as waypoints
    public static void exportRouteAsUrl(Route route, Turf turf) {
        StringBuilder sb = new StringBuilder("https://www.google.com/maps/dir/");
        Set<Point> allPoints = new HashSet<>(turf.zones);
        allPoints.addAll(turf.crossings);
        for (Node node : route.getNodes()) {
            Point point = allPoints.stream()
                .filter(p -> p.toString().equals(node.name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No point found for node " + node.name));
            sb.append(point.coords.lat).append(",");
            sb.append(point.coords.lon).append("/");
        }
        System.out.println(sb.toString());
    }

    /* Inconspicuous pathfinder */

    // Heavily copied from Turf.optimize
    public static List<Connection> pathfind(Turf turf, Point start, Point end) {
        // Prioritize paths by its last point's distance to start
        PriorityQueue<TurfRoute> queue = new PriorityQueue<>(
            Comparator.comparingDouble(route -> route.length)
        );
        Set<Point> visited = new HashSet<>();
        // Start with a route at the end point, and build backwards towards the start
        queue.add(turf.new TurfRoute(end));
        while (!queue.isEmpty()) {
            TurfRoute route = queue.remove();
            Point current = route.point;
            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);
            // Finish route if we reach start
            // Build list of connections in reverse (this is why we start from end)
            if (current.equals(start)) {
                List<Connection> connections = new ArrayList<>();
                while (route.previous != null) {
                    connections.add( route.connectionFromPrevious);
                    route = route.previous;
                }
                return connections;
            }
            // Extend the route with all connections from the current point
            for (Connection extension : current.parents) {
                TurfRoute nextRoute = turf.new TurfRoute(extension, route);
                queue.add(nextRoute);
            }
        }
        throw new RuntimeException("No path found from " + start + " to " + end);
    }
}
