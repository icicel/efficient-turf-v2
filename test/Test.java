import turf.Turf;
import turf.TurfSettings;
import zone.Connection;
import zone.Zone;

public class Test {
    public static void main(String[] args) throws Exception {
        // settings
        TurfSettings settings = new TurfSettings()
            .setTimeLimit(60.0)
            .setStartZone("k-klassrum")
            .setEndZone("k-nösnäs")
            .setUsername("icicle")
            .setSpeed(64.0)
            .setWaitTime(1.0)
        ;
        Turf turf = new Turf(
            settings, 
            "example.kml", 
            "Zones", "Crossings", "Connections"
        );
        
        for (Zone zone : turf.zones) {
            System.out.println(zone + " " + zone.points);
        }
        for (Connection connection : turf.connections) {
            System.out.println(connection + " " + connection.distance);
        }
    }
}
