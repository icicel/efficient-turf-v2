package solver;
import java.util.ArrayList;
import java.util.List;
import scenario.Link;
import scenario.Node;
import scenario.Route;
import scenario.Scenario;

// The original solving algorithm, from version 1
// A brute force algorithm that tries every possible route, but slightly
//  optimized by not trying routes that are guaranteed to be invalid
// While the original was breadth-first, this implementation is depth-first
public class BruteForceSolver implements Solver {

    public Scenario scenario;
    public List<AdvancedRoute> finishedRoutes;
    
    public Result solve(Scenario scenario) {
        this.scenario = scenario;
        finishedRoutes = new ArrayList<>();
        search(new AdvancedRoute(scenario.start));
        finishedRoutes.sort(
            (a, b) -> Double.compare(b.points, a.points));
        return new Result(finishedRoutes);
    }

    // Recursively searches for valid, finished routes
    private void search(AdvancedRoute base) {
        for (Link link : base.node.out) {
            int error = invalidRouteExtension(base, link);
            if (error != 0) {
                continue;
            }
            AdvancedRoute next = new AdvancedRoute(link, base);
            if (next.node == this.scenario.end) {
                finishedRoutes.add(next);
            }
            search(next);
        }
    }

    // Returns a nonzero number if the route would be invalid (aka inefficient) if extended via the link
    private int invalidRouteExtension(AdvancedRoute route, Link link) {

        // Can't be finished without exceeding the distance limit
        if (route.length + link.distance + this.scenario.nodeEndDistance.get(link.neighbor) 
                > this.scenario.distanceLimit) {
            return 1;
        }

        // There exists a faster route to this node
        if (this.scenario.nodeFastestRoutes.get(route.lastCapture).get(link.neighbor).length
                < route.distanceSinceLastCapture + link.distance) {
            return 2;
        }

        return 0;
    }

    // A route that carries more information than a regular Route
    public class AdvancedRoute extends Route {

        public Node lastCapture;
        public double distanceSinceLastCapture;
        
        public AdvancedRoute(Node root) {
            super(root);
            this.lastCapture = root;
            this.distanceSinceLastCapture = 0.0;
        }

        public AdvancedRoute(Link extension, AdvancedRoute previous) {
            super(extension, previous);
            if (this.node.isZone && !previous.hasVisited(node)) {
                this.lastCapture = this.node;
                this.distanceSinceLastCapture = 0.0;
            } else {
                this.lastCapture = previous.lastCapture;
                this.distanceSinceLastCapture = previous.distanceSinceLastCapture + extension.distance;
            }
        }
    }
}
