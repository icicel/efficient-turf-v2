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
import map.Line;
import map.Zone;
import turf.ZoneFinder;

public class EfficientTurf {
    public static void main(String[] args) throws Exception {
        KML localKml = getKML("example.kml");
        KMLParser kml = new KMLParser(localKml);

        /* Get zones and connections */

        // There are two types of zones: a true zone and a crossing
        //   Crossings are zones that are not actually zones, but "helper" zones
        //   They are worth 0 points and are usually used to reduce the amount of connections
        Set<Zone> trueZones = kml.getZones("Zones");
        Set<Zone> crossings = kml.getZones("Crossings");
        Set<Line> lines = kml.getLines("Connections");
        
        // Collect all zones into a single set
        Set<Zone> allZones = union(trueZones, crossings);

        // Convert lines to connections
        Set<Connection> connections = fromLines(lines, allZones);


        /* Initialize zone points */
        
        JSONArray zoneJson = getZoneJSON(trueZones);
        
        ZoneFinder finder = new ZoneFinder(allZones);

        // Set points for each zone
        for (int i = 0; i < zoneJson.length(); i++) {
            JSONObject zoneInfo = zoneJson.getJSONObject(i);
            String name = zoneInfo.getString("name").toLowerCase();
            Zone zone = finder.get(name);
            zone.setPoints(zoneInfo);
        }

        // use depth first search with a single route object
        // if it's valid and finished then copy and save
    }

    // Convert a set of lines to a set of connections, using a set of zones
    public static Set<Connection> fromLines(Set<Line> lines, Set<Zone> zones) {
        Set<Connection> connections = new HashSet<>();
        for (Line line : lines) {
            if (!connections.add(line.leftConnection(zones))) {
                System.out.println("WARNING: Duplicate connection " + line.leftConnection(zones));
            }
            if (!connections.add(line.rightConnection(zones))) {
                System.out.println("WARNING: Duplicate connection " + line.leftConnection(zones));
            }
        }
        return connections;
    }

    // Get points values of zones from the Turf API
    // All supplied zone names must correspond to an actual zone
    public static JSONArray getZoneJSON(Set<Zone> zones) throws IOException, InterruptedException {
        
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

        JSONArray result = new JSONArray(response.body());

        // Check that the response contains the same number of zones as the request
        // Should always be less - the API will ignore nonexistant zones
        if (result.length() != zones.size()) {
            System.out.println("WARNING: API response contains less zones than requested");
            findFakeZones(zones, result);
        }

        return result;
    }

    // Find zones in a set that aren't real, a.k.a. don't exist in the API
    public static void findFakeZones(Set<Zone> zones, JSONArray zoneJson) {
        ZoneFinder finder = new ZoneFinder(zones);
        Set<Zone> foundZones = new HashSet<>();
        for (int i = 0; i < zoneJson.length(); i++) {
            JSONObject zoneInfo = zoneJson.getJSONObject(i);
            String name = zoneInfo.getString("name").toLowerCase();
            Zone zone = finder.get(name);
            if (zone != null) {
                foundZones.add(zone);
            }
        }
        boolean fakeZone = false;
        for (Zone zone : zones) {
            if (!foundZones.contains(zone)) {
                System.out.println("ERROR: Zone " + zone.name + " does not exist in the API");
                fakeZone = true;
            }
        }
        if (fakeZone) {
            throw new RuntimeException("Found fake zones");
        }
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
