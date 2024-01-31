package scenario;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import turf.Connection;
import turf.Turf;
import turf.Zone;
import util.ListSet;
import util.Logging;

// Represents a combination of a Turf object (Zone and Link data)
//   and a set of Conditions that specify the problem definition
public class Scenario extends Logging {

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

    // two global counters
    private int removedNodes;
    private int removedLinks;
    
    public Scenario(Turf turf, Conditions conditions) {
        log("Scenario: Initializing...");

        // Create a Node for each Zone in the Turf
        // Also create a temporary map from Zones to respective Nodes
        this.nodes = new ListSet<>();
        this.nodeName = new HashMap<>();
        Map<Zone, Node> childNode = new HashMap<>();
        int nodes = 0;
        for (Zone zone : turf.zones) {
            Node node = new Node(zone, conditions.username, conditions.infiniteRounds);
            this.nodes.add(node);
            this.nodeName.put(node.name, node);
            childNode.put(zone, node);
            nodes++;
        }
        log("Scenario: Created " + nodes + " nodes");

        // Create a Link for each Connection
        this.links = new ListSet<>();
        int links = 0;
        for (Connection connection : turf.connections) {
            Node leftNode = childNode.get(connection.left);
            Node rightNode = childNode.get(connection.right);
            Link leftLink = new Link(connection.distance, leftNode, rightNode);
            Link rightLink = new Link(connection.distance, rightNode, leftNode);
            leftLink.reverse = rightLink;
            rightLink.reverse = leftLink;
            this.links.add(leftLink);
            this.links.add(rightLink);
            links += 2;
        }
        log("Scenario: Created " + links + " links");

        // Fill in other things
        this.start = getNode(conditions.start);
        this.end = getNode(conditions.end);
        this.timeLimit = conditions.timeLimit;
        this.speed = conditions.speed;
        this.waitTime = conditions.waitTime;
        removedNodes = 0;
        removedLinks = 0;
        if (conditions.whitelist != null) {
            log("Scenario: Applying whitelist...");
            inverseRemoveNodes(getNodes(conditions.whitelist));
            log("Scenario: Removed " + removedNodes + " nodes and " + removedLinks + " links");
        } else if (conditions.blacklist != null) {
            log("Scenario: Applying blacklist...");
            removeNodes(getNodes(conditions.blacklist));
            log("Scenario: Removed " + removedNodes + " nodes and " + removedLinks + " links");
        }
        this.priority = getNodes(conditions.priority);

        // Fastest routes
        log("Scenario: Creating fastest routes...");
        for (Node node : this.nodes) {
            if (node.isZone) {
                node.createFastestRoutes();
            }
        }
    }

    /* Utility functions */
    
    // Find a zone by name
    public Node getNode(String name) {
        return this.nodeName.get(name);
    }

    // Convert an array of names to a set of nodes
    public Set<Node> getNodes(String[] names) {
        Set<Node> nodes = new ListSet<>();
        if (names == null) {
            return nodes;
        }
        for (String name : names) {
            Node node = getNode(name);
            nodes.add(node);
        }
        return nodes;
    }

    // Check all links to see if their parent and neighbor still exists

    // Remove all nodes in a set
    private void removeNodes(Set<Node> nodes) {
        for (Node node : nodes) {
            removeNode(node);
        }
    }

    // Remove all nodes EXCEPT those in a set
    private void inverseRemoveNodes(Set<Node> safeNodes) {
        for (Node node : this.nodes) {
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
        if (node == start || node == end) {
            throw new RuntimeException("Tried to remove start or end node");
        }
        this.nodes.remove(node);
        removedNodes++;
        for (Link connection : node.links) {
            this.links.remove(connection);
            removedLinks++;
            if (this.links.remove(connection.reverse)) {
                // If the reverse connection exists, additionally remove it from its parent's links
                removedLinks++;
                connection.reverse.parent.links.remove(connection.reverse); // oh lords
            }
        };
    }
}
