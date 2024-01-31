package turf;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.SAXException;
import kml.KML;
import map.Coords;
import map.Line;
import util.Logging;

// Represents a collection of real zones, user-defined zones (optionally), and connections between them
//   all extracted from the given KML file
public class Turf extends Logging {
    
    public Set<Zone> zones;
    public Set<Connection> connections;

    private Map<String, Zone> zoneNames;

    // Initialize zones and connections from the given KML file
    // Username is required but can be set to null
    // Give layer names of layers containing real zones, crossings, and connections for the
    //   program to search through
    // Crossings are optional, set to null to ignore them
    // Set a layer name to "!ALL" to search through all layers
    public Turf(Path kmlPath, String realZoneLayer, String crossingLayer, String connectionLayer)
    throws IOException, InterruptedException, SAXException, ParserConfigurationException {

        log("Turf: Initializing KML at " + kmlPath + "...");
        KML kml = new KML(kmlPath);

        // Init zones
        this.zones = new HashSet<>();
        this.zoneNames = new HashMap<>();
        for (Coords coords : kml.getPoints(realZoneLayer)) {
            Zone zone = new Zone(coords);
            boolean added = zones.add(zone);
            if (!added) {
                warn("WARNING: Duplicate zone " + zone);
            }
            zoneNames.put(zone.name, zone);
        }
        log("Turf: Found " + zones.size() + " zones");


        // Init points
        log("Turf: Getting points from API...");
        
        // Create a JSON body with all zone names
        // [{"name": "zonea"}, {"name": "zoneb"}, ...]
        JSONArray requestJson = new JSONArray();
        for (Zone zone : zones) {
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
        if (resultJson.length() != zones.size()) {
            warn("WARNING: API response contains less zones than requested! Investigating...");
            Set<Zone> foundZones = new HashSet<>();
            for (int i = 0; i < resultJson.length(); i++) {
                JSONObject zoneInfo = resultJson.getJSONObject(i);
                String name = zoneInfo.getString("name").toLowerCase();
                Zone zone = getZone(name);
                foundZones.add(zone);
            }
            for (Zone zone : zones) {
                if (!foundZones.contains(zone)) {
                    warn("ERROR: Zone " + zone.name + " does not exist in the API");
                }
            }
            throw new RuntimeException("Tried to set points for nonexistant zones");
        }

        // Set points values to Zone objects
        for (int i = 0; i < resultJson.length(); i++) {
            JSONObject zoneInfo = resultJson.getJSONObject(i);
            String name = zoneInfo.getString("name").toLowerCase();
            Zone zone = getZone(name);
            zone.initPoints(zoneInfo);
        }
        log("Turf: Points set");


        // Init crossings only if crossingLayer is given
        if (crossingLayer != null) {
            int oldSize = zones.size(); // temporary
            for (Coords coords : kml.getPoints(crossingLayer)) {
                Zone zone = new Zone(coords);
                boolean added = zones.add(zone);
                if (!added) {
                    warn("WARNING: Duplicate crossing " + zone);
                }
                zoneNames.put(zone.name, zone);
            }
            log("Turf: Found " + (zones.size() - oldSize) + " crossings");
        }


        // Init connections
        this.connections = new HashSet<>();
        for (Line line : kml.getLines(connectionLayer)) {
            Zone leftZone = closestZoneTo(line.left);
            Zone rightZone = closestZoneTo(line.right);
            Connection connection = new Connection(line.distance, leftZone, rightZone);
            boolean added = connections.add(connection);
            if (!added) {
                warn("WARNING: Duplicate connection " + connection);
            }
        }
        log("Turf: Found " + connections.size() + " connections");
    }

    /* Utility functions */

    // Find a zone by name
    public Zone getZone(String name) {
        return zoneNames.get(name);
    }

    // Returns the closest Zone to a given Coords
    public Zone closestZoneTo(Coords coords) {
        Zone closestZone = null;
        double closestDistance = Double.MAX_VALUE;
        for (Zone zone : zones) {
            double distance = coords.distanceTo(zone.coords);
            if (distance < closestDistance) {
                closestZone = zone;
                closestDistance = distance;
            }
        }
        return closestZone;
    }

    // Helper function to get a path to a file in the root directory
    public static Path getRootFilePath(String filename) {
        return FileSystems.getDefault().getPath(".", filename);
    }
}
