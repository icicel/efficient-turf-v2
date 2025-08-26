package solver;
import java.io.Console;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public Map<Integer, AdvancedRoute> finishedRoutes;
    
    public Result solve(Scenario scenario) {
        this.scenario = scenario;
        this.finishedRoutes = new HashMap<>();
        search(new AdvancedRoute(scenario.start));
        return new Result(finishedRoutes.values(), scenario.speed);
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
                if (finishedRoutes.containsKey(next.points)) {
                    AdvancedRoute existing = finishedRoutes.get(next.points);
                    if (next.length < existing.length) {
                        finishedRoutes.put(next.points, next);
                    }
                } else {
                    finishedRoutes.put(next.points, next);
                }
            }
            search(next);
        }
    }

    // Returns a nonzero number if the route would be invalid (aka inefficient) if extended via the link
    private int invalidRouteExtension(AdvancedRoute route, Link newLink) {
        Node newNode = newLink.neighbor;

        // Can't be finished without exceeding the distance limit
        if (route.length + newLink.distance + this.scenario.nodeEndDistance.get(newNode) 
                > this.scenario.distanceLimit) {
            return 1;
        }

        // Returns to a zone without capturing any new zones
        if (newNode == route.lastCapture) {
            return 2;
        }

        // There exists a faster route to this node from the last captured zone
        if (this.scenario.nodeFastestRoutes.get(route.lastCapture).get(newNode).length
                < route.distanceSinceLastCapture + newLink.distance) {
            return 3;
        }

        // This link has already been used in this route
        if (route.hasVisited(newLink)) {
            return 4;
        }

        // Visiting the last four nodes in another order would've been more efficient
        if (route.nodes >= 3) {
            Node lastNode = route.node;
            Node last2Node = route.previous.node;
            Node last3Node = route.previous.previous.node;
            if (
                this.scenario.nodeFastestRoutes.get(lastNode).get(newNode).length +
                this.scenario.nodeFastestRoutes.get(last3Node).get(last2Node).length >
                this.scenario.nodeFastestRoutes.get(last2Node).get(newNode).length +
                this.scenario.nodeFastestRoutes.get(last3Node).get(lastNode).length
            ) {
                return 5;
            }
        }

        return 0;
    }

    // A route that carries more information than a regular Route
    public static class AdvancedRoute extends Route {

        public Node lastCapture;
        public double distanceSinceLastCapture;
        
        public AdvancedRoute(Node root) {
            super(root);
            this.lastCapture = root;
            this.distanceSinceLastCapture = 0.0;
        }

        public AdvancedRoute(Link extension, AdvancedRoute previous) {
            super(extension, previous);
            if (this.node.isZone() && !previous.hasVisited(node)) {
                this.lastCapture = this.node;
                this.distanceSinceLastCapture = 0.0;
            } else {
                this.lastCapture = previous.lastCapture;
                this.distanceSinceLastCapture = previous.distanceSinceLastCapture + extension.distance;
            }
        }
    }

    // Exploring routes interactively, for debugging
    public void interactive() {
        Console in = System.console();
        AdvancedRoute route = new AdvancedRoute(this.scenario.start);
        System.out.println("Start: " + route.node);
        while (true) {
            System.out.print("-> ");
            String input = in.readLine();
            Node nextNode = scenario.getNode(input);
            if (nextNode == null) {
                System.out.println("No such node");
                continue;
            }
            if (route.node.hasLinkTo(nextNode)) {
                Link nextLink = route.node.getLinkTo(nextNode);
                int error = invalidRouteExtension(route, nextLink);
                if (error != 0) {
                    System.out.println("Invalid move: error " + error);
                    continue;
                }
                route = new AdvancedRoute(nextLink, route);
            } else {
                Route fastestRoute = this.scenario.nodeFastestRoutes.get(route.node).get(nextNode);
                String fastestRouteStringWithoutFirstNode = fastestRoute.toString().split(" ", 2)[1];
                System.out.println("(" + fastestRouteStringWithoutFirstNode + ")");

                List<Link> fastestPath = new ArrayList<>();
                while (fastestRoute.link != null) {
                    fastestPath.add(0, fastestRoute.link);
                    fastestRoute = fastestRoute.previous;
                }

                AdvancedRoute originalRoute = route;
                boolean aborted = false;
                for (Link nextLink : fastestPath) {
                    int error = invalidRouteExtension(route, nextLink);
                    if (error != 0) {
                        System.out.println("Invalid move at " + nextLink + ": error " + error);
                        aborted = true;
                        break;
                    }
                    route = new AdvancedRoute(nextLink, route);
                }
                if (aborted) {
                    route = originalRoute;
                    continue;
                }
            }
        }
    }
}
