import java.util.HashSet;
import java.util.Set;

public class Zone {

    public String name;
    public int points;
    public Set<Connection> connections;

    public float latitude;
    public float longitude;

    public Zone(String name, String coordinates) {
        this.name = name;
        connections = new HashSet<>();
    }
}
