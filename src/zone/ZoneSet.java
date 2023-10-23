package zone;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import map.Coords;

// Represents a set of Zones
public class ZoneSet extends AbstractSet<Zone> {

    private Map<String, Zone> nameMap;

    public ZoneSet(Set<Zone> zones) {
        super(zones);

        // Build name map
        this.nameMap = new HashMap<>();
        for (Zone zone : zones) {
            nameMap.put(zone.name, zone);
        }
    }
    public ZoneSet() {
        super();
        this.nameMap = new HashMap<>();
    }

    // Get points values of zones from the Turf API
    // Only use on Zones objects that contain real zones
    public void initPoints(String username) throws IOException, InterruptedException {
        
        // Create a JSON body with all zone names
        // [{"name": "zonea"}, {"name": "zoneb"}, ...]
        JSONArray requestJson = new JSONArray();
        for (Zone zone : this) {
            JSONObject object = new JSONObject();
            object.put("name", zone.name);
            requestJson.put(object);
        }

        // Post the JSON to the API and get a response containing zone information
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.turfgame.com/v4/zones"))
            .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString()))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JSONArray resultJson = new JSONArray(response.body());

        // Check that the response contains the same number of zones as the request
        // Should always be less - the API will ignore nonexistant zones
        // If not, tries to find those nonexistant zones
        if (resultJson.length() != this.size()) {
            System.out.println("WARNING: API response contains less zones than requested");
            findFakeZones(resultJson);
        }

        // Set points values to Zone objects
        for (int i = 0; i < resultJson.length(); i++) {
            JSONObject zoneInfo = resultJson.getJSONObject(i);
            String name = zoneInfo.getString("name").toLowerCase();
            Zone zone = findByName(name);
            zone.setPoints(zoneInfo, username);
        }
    }

    // Find Zones that aren't real, a.k.a. don't exist in the API
    private void findFakeZones(JSONArray resultJson) {
        ZoneSet foundZones = new ZoneSet();
        for (int i = 0; i < resultJson.length(); i++) {
            JSONObject zoneInfo = resultJson.getJSONObject(i);
            String name = zoneInfo.getString("name").toLowerCase();
            Zone zone = findByName(name);
            foundZones.add(zone);
        }
        boolean foundFakeZone = false;
        for (Zone zone : this) {
            if (!foundZones.contains(zone)) {
                System.out.println("ERROR: Zone " + zone.name + " does not exist in the API");
                foundFakeZone = true;
            }
        }
        if (foundFakeZone) {
            throw new RuntimeException("Found fake zones");
        }
    }

    // Returns the closest Zone to a given Coords
    public Zone closestZoneTo(Coords coords) {
        Zone closestZone = null;
        double closestDistance = Double.MAX_VALUE;
        for (Zone zone : this) {
            double distance = coords.distanceTo(zone.coords);
            if (distance < closestDistance) {
                closestZone = zone;
                closestDistance = distance;
            }
        }
        return closestZone;
    }

    // Find a zone by name
    public Zone findByName(String name) {
        return nameMap.get(name);
    }

    @Override
    public boolean add(Zone zone) {
        if (!super.add(zone)) {
            return false;
        }
        nameMap.put(zone.name, zone);
        return true;
    }
    @Override
    public boolean remove(Zone zone) {
        if (!super.remove(zone)) {
            return false;
        }
        nameMap.remove(zone.name);
        return true;
    }

    // Merge two ZoneSets
    public static ZoneSet union(ZoneSet a, ZoneSet b) {
        ZoneSet union = new ZoneSet();
        union.addAll(a);
        union.addAll(b);
        return union;
    }
}
