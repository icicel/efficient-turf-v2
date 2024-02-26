import java.nio.file.Path;
import scenario.Conditions;
import scenario.Scenario;
import solver.BruteForceSolver;
import solver.Result;
import solver.Solver;
import turf.Turf;
import util.Logging;

public class Example {
    public static void main(String[] args) throws Exception {
        Logging.init();

        Path kmlPath = Turf.getRootFilePath("example.kml");
        Turf turf = new Turf(kmlPath, "Zones", "Crossings", "Connections");
        Conditions conditions = new Conditions("k-klassrum", "k-nösnäs", 90.0);
        conditions.speed = 64.0;
        conditions.username = "user";
        conditions.blacklist = new String[] {"black", "list"};
        conditions.priority = new String[] {"priority", "zones"};
        Scenario scenario = new Scenario(turf, conditions);
        Solver solver = new BruteForceSolver();
        Result result = solver.solve(scenario);
        
        System.out.println("Result:");
        System.out.println(result);
    }
}
