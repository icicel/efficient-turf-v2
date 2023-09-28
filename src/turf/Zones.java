package turf;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import map.Zone;

// Represents a set of Zones
public class Zones implements Iterable<Zone> {
    
    private Set<Zone> zones;

    private Map<String, Zone> nameMap;

    public Zones(Set<Zone> zones) {
        this.zones = zones;

        // Build name map
        this.nameMap = new HashMap<>();
        for (Zone zone : zones) {
            nameMap.put(zone.name, zone);
        }
    }

    public Zone findByName(String name) {
        return nameMap.get(name);
    }

    // Merge and return a new Zones object
    public static Zones union(Zones a, Zones b) {
        Set<Zone> result = new HashSet<>(a.zones);
        result.addAll(b.zones);
        return new Zones(result);
    }

    public int size() {
        return zones.size();
    }

    @Override
    public Iterator<Zone> iterator() {
        return zones.iterator();
    }
}
