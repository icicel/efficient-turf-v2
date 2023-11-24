import java.nio.file.Path;
import scenario.Conditions;
import scenario.Link;
import scenario.Node;
import scenario.Scenario;
import turf.Turf;

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
        
        for (Node node : scenario.nodes) {
            System.out.println(node + " " + node.points);
        }
        for (Link link : scenario.links) {
            System.out.println(link + " " + link.distance);
        }
    }
}
