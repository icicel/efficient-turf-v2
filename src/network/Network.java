package network;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import util.Logging;

// A collection of Ways representing physical routes between Points
public class Network extends Logging {

    public Network() throws IOException, InterruptedException, ParsingException {
        StringBuilder data = new StringBuilder();
        data.append("[bbox:57.98,11.71,58.16,11.89];");
        data.append("way[\"highway\"][\"highway\"!=\"motorway\"][\"highway\"!=\"primary\"][\"highway\"!=\"secondary\"];");
        data.append("out skel geom;");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://overpass-api.de/api/interpreter"))
            .POST(HttpRequest.BodyPublishers.ofString("data=" + data.toString()))
            .build();
        
        // Retry the request until it succeeds
        HttpResponse<String> response;
        log("Requesting data from Overpass API...");
        while (true) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            // Check if error page
            if (response.body().contains("!DOCTYPE html")) {
                log("Error, retrying...");
                Thread.sleep(1000);
            } else {
                break;
            }
        }

        Builder builder = new Builder();
        Document xml = builder.build(response.body(), null);

        parseXML(xml);
    }
    
    public Network(Path path) throws IOException, ParsingException {
        Builder builder = new Builder();
        Document xml = builder.build(path.toFile());
        parseXML(xml);
    }

    private void parseXML(Document xml) {
        log("Parsing XML...");
        Element root = xml.getRootElement();
        for (Element element : root.getChildElements("way")) {
            String id = element.getAttributeValue("id");
            System.out.println("Way " + id);
        }
    }
}
