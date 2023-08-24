import java.util.*;

public class Zone {

    public int points;
    public Set<Connection> connections;

    public Zone(int points) {
        this.points = points;
        connections = new HashSet<>();
    }
}
