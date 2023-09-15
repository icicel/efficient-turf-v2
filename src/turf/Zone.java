package turf;
import java.util.HashSet;
import java.util.Set;
import map.Coords;

public class Zone {

    public String name;
    public int points;
    public Set<Connection> connections;

    public Coords coords;

    public Zone(String name, String coordinates) {
        this.name = name;
        this.coords = new Coords(coordinates);
        this.connections = new HashSet<>();
    }

    @Override
    public String toString() {
        return name;
    }
}
