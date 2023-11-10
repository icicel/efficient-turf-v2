import java.nio.file.Path;
import java.nio.file.Paths;
import turf.Turf;
import zone.Zone;
import zone.ZoneType;

public class Export {
    public static void main(String[] args) throws Exception {
        String home = System.getProperty("user.home");
        Path path = Paths.get(home, "Downloads", "example.kml");
        Turf turf = new Turf(path, "Zones", "Crossings", "Connections");
    
        exportZones(turf);
    }

    public static void exportZonesPoints(Turf turf) {
        System.out.println("WKT,name,description,Points");
        for (Zone zone : turf.zones) {
            if (zone.type != ZoneType.CROSSING)
                System.out.println("\"POINT (" + zone.coords.lon + " " + zone.coords.lat + ")\"," + zone.name + ",," + zone.getPoints("name", true));
        }
    }

    public static void exportZones(Turf turf) {
        System.out.println("WKT,name");
        for (Zone zone : turf.zones) {
            if (zone.type != ZoneType.CROSSING)
                System.out.println("\"POINT (" + zone.coords.lon + " " + zone.coords.lat + ")\"," + zone.name);
        }
    }

    public static void exportCrossings(Turf turf) {
        System.out.println("WKT,name");
        for (Zone zone : turf.zones) {
            if (zone.type == ZoneType.CROSSING)
                System.out.println("\"POINT (" + zone.coords.lon + " " + zone.coords.lat + ")\"," + zone.name);
        }
    }
}
