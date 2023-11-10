import java.nio.file.Path;
import turf.Conditions;
import turf.Scenario;
import turf.Turf;
import zone.Connection;
import zone.Zone;

public class Example {
    public static void main(String[] args) throws Exception {
        Path kmlPath = Turf.getRootFilePath("example.kml");
        Turf turf = new Turf(kmlPath, "Zones", "Crossings", "Connections");
        Conditions conditions = new Conditions("k-klassrum", "k-nösnäs", 90.0);
        conditions.speed = 64.0;
        conditions.username = "user";
        conditions.blacklist = new String[] {"black", "list"};
        conditions.priority = new String[] {"priority", "zones"};
        Scenario scenario = new Scenario(turf, conditions);
        // Solver solver = new BruteForceSolver();
        // solver.solve(scenario);
        
        for (Zone zone : scenario.zones) {
            System.out.println(zone + " " + scenario.points.get(zone));
        }
        for (Connection connection : scenario.connections) {
            System.out.println(connection + " " + connection.distance);
        }
    }
}
