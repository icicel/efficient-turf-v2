package util;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import scenario.Node;
import scenario.Route;
import turf.Connection;
import turf.Point;
import turf.Turf;
import turf.Turf.TurfRoute;

public class Export {

    /* Export Turf features as CSV */

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
                sb.append(connection.left.lon).append(" ").append(connection.left.lat);
                for (Point middlePoint : connection.middle) {
                    sb.append(", ");
                    sb.append(middlePoint.lon).append(" ").append(middlePoint.lat);
                }
                sb.append(", ");
                sb.append(connection.right.lon).append(" ").append(connection.right.lat);
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
                sb.append(crossing.lon).append(" ").append(crossing.lat);
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
                sb.append(zone.lon).append(" ").append(zone.lat);
                sb.append(")\",");
                sb.append(zone).append("\n");
                return sb.toString();
            }
        );
    }

    public static void exportZonesWithPoints(Turf turf, Path path) throws IOException {
        exportZonesWithPoints(turf, path, null, false);
    }

    public static void exportZonesWithPoints(Turf turf, Path path, String username, boolean isNow) throws IOException {
        exportGeneric(
            path,
            "WKT,name,points",
            turf.zones,
            zone -> {
                StringBuilder sb = new StringBuilder();
                sb.append("\"POINT (");
                sb.append(zone.lon).append(" ").append(zone.lat);
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

    /* Export entire Turf */

    // Serialize
    public static void exportTurf(Turf turf, Path path) throws IOException {
        OutputStream out = Files.newOutputStream(path);
        ObjectOutputStream objOut = new ObjectOutputStream(out);
        // Points are references in multiple places so must be
        //  serialized as pointers to a list instead
        Set<Point> allPoints = new HashSet<>(turf.zones);
        allPoints.addAll(turf.crossings);
        for (Connection connection : turf.connections) {
            allPoints.add(connection.left);
            allPoints.add(connection.right);
            allPoints.addAll(connection.middle);
        }
        List<Point> pointList = new ArrayList<>(allPoints);
        // Recreate Connections as lists of indicess
        // Distance can be recalculated from the points
        Map<Point, Integer> index = new HashMap<>();
        for (int i = 0; i < pointList.size(); i++) {
            index.put(pointList.get(i), i);
        }
        List<List<Integer>> connectionList = new ArrayList<>();
        for (Connection connection : turf.connections) {
            List<Integer> connectionData = new ArrayList<>();
            connectionData.add(index.get(connection.left));
            for (Point middlePoint : connection.middle) {
                connectionData.add(index.get(middlePoint));
            }
            connectionData.add(index.get(connection.right));
            connectionList.add(connectionData);
        }
        // Recreate the sets
        List<Integer> crossingList = new ArrayList<>();
        for (Point crossing : turf.crossings) {
            crossingList.add(index.get(crossing));
        }
        List<Integer> zoneList = new ArrayList<>();
        for (Point zone : turf.zones) {
            zoneList.add(index.get(zone));
        }
        // Write
        objOut.writeObject(pointList);
        objOut.writeObject(connectionList);
        objOut.writeObject(crossingList);
        objOut.writeObject(zoneList);
        objOut.close();
    }

    /* Export Routes using their base Turfs */

    public static void exportRouteAsCsv(Route route, Path path, Turf turf) throws IOException {
        List<Point> points = getPoints(route, turf);
        StringBuilder sb = new StringBuilder();
        sb.append("WKT,name").append("\n");
        // Iterate over all nodes except the last one
        for (int i = 0; i < points.size() - 1; i++) {
            Point start = points.get(i);
            Point end = points.get(i + 1);
            // Construct a backwards turfRoute to drain it in the correct order
            TurfRoute turfRoute = turf.pathfind(end, start);
            List<Connection> connections = new LinkedList<>();
            while (turfRoute.previous != null) {
                connections.add(turfRoute.connectionFromPrevious);
                turfRoute = turfRoute.previous;
            }
            // Point
            sb.append("\"POINT (");
            sb.append(start.lon).append(" ").append(start.lat);
            sb.append(")\",");
            sb.append(start).append("\n");
            // Connections
            Point current = start;
            sb.append("\"LINESTRING (");
            for (Connection connection : connections) {
                // get the correct view of the connection (left->right or right->left)
                sb.append(current.lon).append(" ").append(current.lat).append(", ");
                for (Point middlePoint : connection.middleFromPOVOf(current)) {
                    sb.append(middlePoint.lon).append(" ").append(middlePoint.lat).append(", ");
                }
                current = connection.other(current);
            }
            sb.append(current.lon).append(" ").append(current.lat);
            sb.append(")\",");
            sb.append(start).append("-").append(end).append("\n");
        }
        // Last point
        Point last = points.get(points.size() - 1);
        sb.append("\"POINT (");
        sb.append(last.lon).append(" ").append(last.lat);
        sb.append(")\",");
        sb.append(last).append("\n");
        Files.writeString(path, sb.toString());
    }

    // For use in turf.urbangeeks.org
    public static void exportRouteAsGpx(Route route, Path path, Turf turf) throws IOException {
        List<Point> points = getPoints(route, turf);
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<gpx version=\"1.1\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n");
        for (Point point : points) {
            sb.append("<wpt lat=\"").append(point.lat).append("\" lon=\"").append(point.lon).append("\">\n");
            sb.append("<name>").append(point).append("</name>\n");
            sb.append("</wpt>\n");
        }
        sb.append("</gpx>\n");
        Files.writeString(path, sb.toString());
    }

    // Prints a Google Maps URL with the route's points as waypoints
    public static void exportRouteAsUrl(Route route, Turf turf) {
        List<Point> points = getPoints(route, turf);
        StringBuilder sb = new StringBuilder("https://www.google.com/maps/dir/");
        for (Point point : points) {
            sb.append(point.lat).append(",");
            sb.append(point.lon).append("/");
        }
        System.out.println(sb.toString());
    }

    private static List<Point> getPoints(Route route, Turf turf) {
        List<Node> nodes = route.getNodes();
        List<Point> points = new ArrayList<>();
        Set<Point> allPoints = new HashSet<>(turf.zones);
        allPoints.addAll(turf.crossings);
        for (Node node : nodes) {
            Point point = allPoints.stream()
                .filter(p -> p.toString().equals(node.name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No point found for node " + node.name));
            points.add(point);
        }
        return points;
    }
}
