import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import kml.KML;
import map.Zone;
import turf.Connections;
import turf.Zones;

public class EfficientTurf {
    public static void main(String[] args) throws Exception {
        KML kml = new KML("example.kml");

        /* Get zones and connections */

        // There are two types of zones: a true zone and a crossing
        //   Crossings are zones that are not actually zones, but "helper" zones
        //   They are worth 0 points and are usually used to reduce the amount of connections
        Zones trueZones = kml.getZones("Zones");
        Zones crossings = kml.getZones("Crossings");
        
        // Collect all zones into a single set
        Zones allZones = Zones.union(trueZones, crossings);

        // Get connections
        Connections connections = kml.getConnections("Connections", allZones);


        /* Initialize zone points */
        
        JSONArray zoneJson = getZoneJSON(trueZones);

        // Set points for each zone
        for (int i = 0; i < zoneJson.length(); i++) {
            JSONObject zoneInfo = zoneJson.getJSONObject(i);
            String name = zoneInfo.getString("name").toLowerCase();
            Zone zone = trueZones.findByName(name);
            zone.setPoints(zoneInfo);
        }

        // use depth first search with a single route object
        // if it's valid and finished then copy and save
    }

    // Get points values of zones from the Turf API
    // All supplied zone names must correspond to an actual zone
    public static JSONArray getZoneJSON(Zones zones) throws IOException, InterruptedException {
        
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
    public static void findFakeZones(Zones zones, JSONArray zoneJson) {
        Set<Zone> foundZones = new HashSet<>();
        for (int i = 0; i < zoneJson.length(); i++) {
            JSONObject zoneInfo = zoneJson.getJSONObject(i);
            String name = zoneInfo.getString("name").toLowerCase();
            Zone zone = zones.findByName(name);
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
}
