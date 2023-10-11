import java.nio.file.Path;
import turf.Turf;
import zone.Connection;
import zone.Zone;

public class Test {
    public static void main(String[] args) throws Exception {
        Path kmlPath = Turf.getRootFilePath("example.kml");
        Turf turf = new Turf("icicle", kmlPath, "Zones", "Crossings", "Connections");
        turf.speed = 64.0;
        
        for (Zone zone : turf.zones) {
            System.out.println(zone + " " + zone.points);
        }
        for (Connection connection : turf.connections) {
            System.out.println(connection + " " + connection.distance);
        }
    }
}
