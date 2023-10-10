import turf.Turf;
import zone.Connection;
import zone.Zone;

public class Test {
    public static void main(String[] args) throws Exception {
        Turf turf = new Turf("icicle", "example.kml", "Zones", "Crossings", "Connections");
        turf.setSpeed(64.0);
        
        for (Zone zone : turf.zones) {
            System.out.println(zone + " " + zone.points);
        }
        for (Connection connection : turf.connections) {
            System.out.println(connection + " " + connection.distance);
        }
    }
}
