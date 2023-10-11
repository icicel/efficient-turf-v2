import turf.Turf;
import zone.Zone;
import zone.ZoneType;

public class Export {
    public static void main(String[] args) throws Exception {
        Turf turf = new Turf("icicle", "example.kml", "Zones", "Crossings", "Connections");
    
        exportZones(turf);
    }

    public static void exportZonesPoints(Turf turf) {
        System.out.println("WKT,name,description,Points");
        for (Zone zone : turf.zones) {
            if (zone.type != ZoneType.CROSSING)
                System.out.println("\"POINT (" + zone.coords.lon + " " + zone.coords.lat + ")\"," + zone.name + ",," + zone.points);
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
