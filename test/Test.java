import map.Connection;
import map.Zone;
import turf.Turf;

public class Test {
    public static void main(String[] args) throws Exception {
        Turf turf = new Turf("example.kml", "Zones", "Crossings", "Connections");
        
        for (Zone zone : turf.zones) {
            System.out.println(zone + " " + zone.points);
        }
        for (Connection connection : turf.connections) {
            System.out.println(connection + " " + connection.distance);
        }
    }
}
