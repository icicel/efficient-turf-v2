package turf;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import util.ListSet;
import zone.Connection;
import zone.Zone;

// Represents a combination of a Turf object (Zone and Link data)
//   and a set of Conditions that specify the problem definition
public class Scenario {

    // Base Turf/Conditions data

    public Set<Node> nodes;
    public Set<Link> links;

    public Node start;
    public Node end;
    public double timeLimit;

    public double speed;
    public double waitTime;

    public Set<Node> priority;

    // node names -> nodes
    private Map<String, Node> nodeName;
    
    public Scenario(Turf turf, Conditions conditions) {
        // Create a Node for each Zone in the Turf
        // Also create a temporary map from Zones to respective Nodes
        this.nodes = new ListSet<>();
        this.nodeName = new HashMap<>();
        Map<Zone, Node> childNode = new HashMap<>();
        for (Zone zone : turf.zones) {
            Node node = new Node(zone, conditions.username, conditions.infiniteRounds);
            this.nodes.add(node);
            this.nodeName.put(node.name, node);
            childNode.put(zone, node);
        }

        // Create a Link for each Connection
        this.links = new ListSet<>();
        for (Connection connection : turf.connections) {
            Node leftNode = childNode.get(connection.left);
            Node rightNode = childNode.get(connection.right);
            Link leftLink = new Link(connection.distance, leftNode, rightNode);
            Link rightLink = new Link(connection.distance, rightNode, leftNode);
            leftLink.reverse = rightLink;
            rightLink.reverse = leftLink;
            this.links.add(leftLink);
            this.links.add(rightLink);
        }

        // Fill in other things
        this.start = getNode(conditions.start);
        this.end = getNode(conditions.end);
        this.timeLimit = conditions.timeLimit;
        this.speed = conditions.speed;
        this.waitTime = conditions.waitTime;
        if (conditions.whitelist != null) {
            inverseRemoveNodes(getNodes(conditions.whitelist));
        } else if (conditions.blacklist != null) {
            removeNodes(getNodes(conditions.blacklist));
        }
        this.priority = getNodes(conditions.priority);
    }

    /* Utility functions */
    
    // Find a zone by name
    public Node getNode(String name) {
        return this.nodeName.get(name);
    }

    // Convert an array of names to a set of nodes
    public Set<Node> getNodes(String[] names) {
        Set<Node> nodes = new HashSet<>();
        if (names == null) {
            return nodes;
        }
        for (String name : names) {
            Node node = getNode(name);
            nodes.add(node);
        }
        return nodes;
    }

    // Remove all nodes in a set
    private void removeNodes(Set<Node> nodes) {
        for (Node node : nodes) {
            removeNode(node);
        }
    }

    // Remove all nodes EXCEPT those in a set
    private void inverseRemoveNodes(Set<Node> safeNodes) {
        for (Node node : nodes) {
            if (!safeNodes.contains(node)) {
                removeNode(node);
            }
        }
    }

    // Completely remove all references to a node
    // Includes removing all connections to/from the node
    private void removeNode(Node node) {
        if (node == null) {
            return;
        }
        this.nodes.remove(node);
        for (Link connection : node.links) {
            this.links.remove(connection);
            this.links.remove(connection.reverse);
        };
    }
}
