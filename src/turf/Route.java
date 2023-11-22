package turf;
import java.util.LinkedList;
import java.util.List;

public class Route {

    public List<Node> nodes;
    public List<Link> links;

    public Route() {
        nodes = new LinkedList<>();
        links = new LinkedList<>();
    }
}
