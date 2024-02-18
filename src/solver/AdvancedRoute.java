package solver;
import scenario.Link;
import scenario.Node;
import scenario.Route;

// A route that carries more information than a regular Route
public class AdvancedRoute extends Route {

    public int points;
    public Node lastCapture;
    public double distanceSinceLastCapture;
    
    public AdvancedRoute(Node root) {
        super(root);
        this.points = root.points;
        this.lastCapture = root;
        this.distanceSinceLastCapture = 0.0;
    }

    public AdvancedRoute(Link extension, AdvancedRoute previous) {
        super(extension, previous);
        this.points = previous.points + extension.neighbor.points;
        if (this.node.isZone && !previous.hasVisited(node)) {
            this.lastCapture = this.node;
            this.distanceSinceLastCapture = 0.0;
        } else {
            this.lastCapture = previous.lastCapture;
            this.distanceSinceLastCapture = previous.distanceSinceLastCapture + extension.distance;
        }
    }
}
