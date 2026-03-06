package network;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kml.KML;
import kml.XML;
import map.Coords;
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

        log("Getting KML...");
        KML kml = new KML(zoneKml);
        for (Coords coords : kml.points.get("Turf Zones")) {
            zones.add(new Point(coords));
        }

        log("Getting XML...");
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
            xml = new XML(minLat, minLon, maxLat, maxLon);
        } else {
            xml = new XML(networkXml);
        }
        this.ways = xml.ways;
        this.points = xml.points;

        log("Connecting zones...");
        // Identify each point with its hundredth-degree lat/lon quadrant
        Map<String, Set<Point>> quadrantMap = new HashMap<>();
        for (Point point : this.points) {
            String quadrantKey = getQuadrantKey(point.coords);
            quadrantMap.computeIfAbsent(quadrantKey, k -> new HashSet<>()).add(point);
        }
        for (Point zone : zones) {
            // Find closest point in the same quadrant as the zone
            String quadrantKey = getQuadrantKey(zone.coords);
            Set<Point> pointsInQuadrant = quadrantMap.get(quadrantKey);
            if (pointsInQuadrant != null) {
                Point closestPoint = closestPoint(pointsInQuadrant, zone);
                // Only accept this point if it's closer than the edge of the quadrant
                //  meaning it's closer than any point in a different quadrant
                if (zone.distanceTo(closestPoint) < distanceToQuadrantEdge(zone.coords)) {
                    Way way = new Way(zone, closestPoint);
                    ways.add(way);
                    continue;
                }
            }
            // Failure, search globally
            // This shouldn't be too common since zones are usually close to OSM highways
            Point closestPoint = closestPoint(this.points, zone);
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

    /* Quadrant optimization helpers */

    // Return the point in the given set that is closest to the given point
    public static Point closestPoint(Set<Point> points, Point point) {
        return points.stream()
            .min((p1, p2) -> Double.compare(point.distanceTo(p1), point.distanceTo(p2)))
            .orElseThrow();
    }

    // Get the key for the quadrant that the given coords fall into
    // Ex: lat=-37.7749, lon=-122.4194 -> quadrantKey="-3777-12241"
    public static String getQuadrantKey(Coords coords) {
        int latQuadrant = (int) Math.floor(coords.lat * 100);
        int lonQuadrant = (int) Math.floor(coords.lon * 100);
        return latQuadrant + "" + lonQuadrant;
    }

    // Shortest distance to the edge of the quadrant
    // Ex: lat=-37.7749, lon=-122.4194 -> nearestQuadrantEdge=(-37.77,-122.42)
    public static double distanceToQuadrantEdge(Coords coords) {
        double latQuadrantEdge = Math.round(coords.lat * 100) / 100.0;
        double lonQuadrantEdge = Math.round(coords.lon * 100) / 100.0;
        Coords nearestQuadrantEdge = new Coords(latQuadrantEdge, lonQuadrantEdge);
        return coords.distanceTo(nearestQuadrantEdge);
    }
}
