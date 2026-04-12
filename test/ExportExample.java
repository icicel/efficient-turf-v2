import java.nio.file.Path;
import java.nio.file.Paths;
import turf.Export;
import turf.Turf;

public class ExportExample {
    public static void main(String[] args) throws Exception {
        String home = System.getProperty("user.home");
        Path path = Paths.get(home, "Downloads", "example.kml");
        Turf turf = new Turf(path, "Zones", "Crossings", "Connections");
        Path exportPath = Paths.get(home, "Downloads", "export.csv");
    
        Export.exportZonesWithPoints(turf, exportPath, "user", true);
    }
}
