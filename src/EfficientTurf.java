import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class EfficientTurf {
    public static void main(String[] args) throws Exception {
        return;

        // use depth first search with a single path object
        // if it's valid and finished then copy and save
    }

    // Get a KML file located in the root directory
    public static String getKML(String file) throws IOException {
        Path path = FileSystems.getDefault().getPath(".", file);
        byte[] encoded = Files.readAllBytes(path);
        return new String(encoded);
    }

    // Getting a KML by http request and a Google My Maps "mid" (map id)
    // Not working yet
    // Will probably have to pretend to be Firefox to get it to work
    public static String getWebKML(String mapId) throws IOException, InterruptedException {
        String urlString = "http://www.google.com/maps/d/kml?forcekml=1&mid=" + mapId;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(urlString))
            .build();
        HttpResponse<String> response = 
            client.send(request, BodyHandlers.ofString());
        
        return response.body();
    }
}
