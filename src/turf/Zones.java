package turf;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import map.Zone;
import map.ZoneType;

// Represents a set of Zones
public class Zones implements Iterable<Zone> {
    
    private Set<Zone> zones;

    public ZoneType type;

    private Map<String, Zone> nameMap;

    // Similar to Zone class, type starts at CROSSING until initPoints() is called
    public Zones(Set<Zone> zones) {
        this.zones = zones;
        this.type = ZoneType.CROSSING;

        // Build name map
        this.nameMap = new HashMap<>();
        for (Zone zone : zones) {
            nameMap.put(zone.name, zone);
        }
    }

    // Get points values of zones from the Turf API
    // Only use on Zones objects that contain real zones
    public void initPoints() throws IOException, InterruptedException {
        
        // Create a JSON body with all zone names
        // [{"name": "zonea"}, {"name": "zoneb"}, ...]
        JSONArray json = new JSONArray();
        for (Zone zone : zones) {
            JSONObject object = new JSONObject();
            object.put("name", zone.name);
            json.put(object);
        }

        // Post the JSON to the API and get a response containing zone information
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.turfgame.com/v4/zones"))
            .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JSONArray zoneJson = new JSONArray(response.body());

        // Check that the response contains the same number of zones as the request
        // Should always be less - the API will ignore nonexistant zones
        // If not, tries to find those nonexistant zones
        if (zoneJson.length() != zones.size()) {
            System.out.println("WARNING: API response contains less zones than requested");
            findFakeZones(zoneJson);
        }

        // Set points values to Zone objects
        for (int i = 0; i < zoneJson.length(); i++) {
            JSONObject zoneInfo = zoneJson.getJSONObject(i);
            String name = zoneInfo.getString("name").toLowerCase();
            Zone zone = findByName(name);
            zone.setPoints(zoneInfo);
        }

        this.type = ZoneType.REAL;
    }

    // Find Zones that aren't real, a.k.a. don't exist in the API
    public void findFakeZones(JSONArray zoneJson) {
        Set<Zone> foundZones = new HashSet<>();
        for (int i = 0; i < zoneJson.length(); i++) {
            JSONObject zoneInfo = zoneJson.getJSONObject(i);
            String name = zoneInfo.getString("name").toLowerCase();
            Zone zone = findByName(name);
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

    // Find a zone by name
    public Zone findByName(String name) {
        return nameMap.get(name);
    }

    // Merge and return a new Zones object
    public static Zones union(Zones a, Zones b) {
        Set<Zone> result = new HashSet<>(a.zones);
        result.addAll(b.zones);
        return new Zones(result);
    }

    public int size() {
        return zones.size();
    }

    @Override
    public Iterator<Zone> iterator() {
        return zones.iterator();
    }
}
