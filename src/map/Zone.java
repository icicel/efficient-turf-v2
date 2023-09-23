package map;
import java.util.HashSet;
import java.util.Set;

public class Zone {

    public String name;
    public int points;
    public Set<Connection> connections;

    public Coords coords;

    public Zone(String name, String coordinates) {
        this.name = name.toLowerCase();
        this.coords = new Coords(coordinates);
        this.connections = new HashSet<>();
    }

    @Override
    public String toString() {
        return name;
    }
}
