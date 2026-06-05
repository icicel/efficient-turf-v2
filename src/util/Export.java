package util;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import scenario.Link;
import scenario.Node;
import scenario.Route;
import scenario.Scenario;
import turf.Connection;
import turf.Point;
import turf.Turf;
import turf.Trail;

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
        Set<Point> allPoints = turf.allPoints();
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
        List<Double> weightList = new ArrayList<>();
        for (Connection connection : turf.connections) {
            List<Integer> connectionData = new ArrayList<>();
            connectionData.add(index.get(connection.left));
            for (Point middlePoint : connection.middle) {
                connectionData.add(index.get(middlePoint));
            }
            connectionData.add(index.get(connection.right));
            connectionList.add(connectionData);
            double weight = connection.weightedDistance / connection.distance;
            weightList.add(weight);
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
        objOut.writeObject(weightList);
        objOut.writeObject(crossingList);
        objOut.writeObject(zoneList);
        objOut.close();
    }

    /* Export Scenario objects */

    public static void exportNodes(Scenario scenario, Path path) throws IOException {
        exportGeneric(
            path,
            "WKT,name",
            scenario.nodes,
            node -> {
                Point point = node.ancestor;
                StringBuilder sb = new StringBuilder();
                sb.append("\"POINT (");
                sb.append(point.lon).append(" ").append(point.lat);
                sb.append(")\",");
                sb.append(node.name).append("\n");
                return sb.toString();
            }
        );
    }

    private static void exportLinks(Iterable<Link> links, Path path) throws IOException {
        exportGeneric(
            path,
            "WKT,name",
            links,
            link -> {
                Trail trail = link.ancestor;
                StringBuilder sb = new StringBuilder();
                Point current = trail.start();
                sb.append("\"LINESTRING (");
                for (Connection connection : trail.getConnections()) {
                    // get the correct view of the connection (left->right or right->left)
                    sb.append(current.lon).append(" ").append(current.lat).append(", ");
                    for (Point middlePoint : connection.middleFromPOVOf(current)) {
                        sb.append(middlePoint.lon).append(" ").append(middlePoint.lat).append(", ");
                    }
                    current = connection.other(current);
                }
                sb.append(current.lon).append(" ").append(current.lat);
                sb.append(")\",");
                sb.append(link.parent.name).append("-").append(link.neighbor.name).append("\n");
                return sb.toString();
            }
        );
    }

    public static void exportLinks(Scenario scenario, Path path) throws IOException {
        Set<Link> oneWayLinks = new HashSet<>();
        for (Link link : scenario.links) {
            if (oneWayLinks.contains(link.reverse)) {
                continue;
            }
            oneWayLinks.add(link);
        }
        exportLinks(oneWayLinks, path);
    }

    public static void exportNodeNeighborhood(Node node, Path path) throws IOException {
        exportLinks(node.out, path);
    }

    /* Export Routes */

    public static void exportRouteAsCsv(Route route, Path path) throws IOException {
        exportLinks(route.getLinks(), path);
    }

    // For use in turf.urbangeeks.org
    public static void exportRouteAsGpx(Route route, Path path) throws IOException {
        List<Node> nodes = route.getRouteNodes();
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<gpx version=\"1.1\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n");
        for (Node node : nodes) {
            sb.append("<wpt lat=\"").append(node.ancestor.lat).append("\" lon=\"").append(node.ancestor.lon).append("\">\n");
            sb.append("<name>").append(node).append("</name>\n");
            sb.append("</wpt>\n");
        }
        sb.append("</gpx>\n");
        Files.writeString(path, sb.toString());
    }

    // Prints a Google Maps URL with the route's points as waypoints
    public static void exportRouteAsUrl(Route route) {
        List<Node> nodes = route.getRouteNodes();
        StringBuilder sb = new StringBuilder("https://www.google.com/maps/dir/");
        for (Node node : nodes) {
            sb.append(node.ancestor.lat).append(",");
            sb.append(node.ancestor.lon).append("/");
        }
        System.out.println(sb.toString());
    }
}
