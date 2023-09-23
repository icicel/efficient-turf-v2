import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import kml.KML;
import kml.KMLParser;
import map.Connection;
import map.Zone;
import turf.ZoneFinder;

public class EfficientTurf {
    public static void main(String[] args) throws Exception {
        KML localKml = getKML("example.kml");
        KMLParser kml = new KMLParser(localKml);

        // Get zones and connections
        // There are two types of zones: a true zone and a crossing
        //   Crossings are zones that are not actually zones, but "helper" zones
        //   They are worth 0 points and are usually used to reduce the amount of connections
        Set<Zone> trueZones = kml.getZones("Zones");
        Set<Zone> crossings = kml.getZones("Crossings");
        Set<Connection> connections = kml.getConnections("Connections");
        
        // Collect all zones into a single set
        // Check for duplicates and initialize a ZoneFinder on it
        Set<Zone> allZones = union(trueZones, crossings);
        checkForDuplicates(allZones);
        ZoneFinder finder = new ZoneFinder(allZones);

        // Add the reverse of all connections to the set
        Set<Connection> reversedConnections = new HashSet<>();
        for (Connection connection : connections) {
            reversedConnections.add(connection.reversed());
        }
        connections.addAll(reversedConnections);

        // Connect crossings to their closest zone
        for (Connection connection : connections) {
            connection.completeOn(allZones);
        }

        // rounds start at 12:00 swedish time the first sunday of every month

        // use depth first search with a single route object
        // if it's valid and finished then copy and save
    }

    // Find duplicate zone names
    public static void checkForDuplicates(Set<Zone> zones) {
        Set<String> names = new HashSet<>();
        for (Zone zone : zones) {
            if (names.contains(zone.name)) {
                System.out.println("WARNING: Duplicate zone name " + zone.name);
            }
            names.add(zone.name);
        }
    }

    // Get points values of zones from the Turf API
    // Takes into account both on-capture points and hourly points by calculating the expected
    //  number of hours the zone will be held
    // All supplied zone names must correspond to an actual zone
    public static void getZonesPoints(Set<Zone> zones) throws IOException, InterruptedException {
        
        // Create a JSON body with all zone names
        // [{"name": "zonea"}, {"name": "zoneb"}, ...]
        JSONArray json = new JSONArray();
        for (Zone zone : zones) {
            JSONObject object = new JSONObject();
            object.put("name", zone.name);
            json.put(object);
        }

        // Post the JSON to the API
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.turfgame.com/v4/zones"))
            .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());
    }

    // Set union
    public static <T> Set<T> union(Set<T> a, Set<T> b) {
        Set<T> result = new HashSet<>(a);
        result.addAll(b);
        return result;
    }

    // Get a KML file located in the root directory
    public static KML getKML(String file) throws IOException {
        Path path = FileSystems.getDefault().getPath(".", file);
        return new KML(path);
    }
}
