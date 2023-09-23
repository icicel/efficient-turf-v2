package turf;

import java.util.HashMap;
import java.util.Set;

import map.Zone;

// A simple hash map class to find a zone by name
public class ZoneFinder {
    private HashMap<String, Zone> zoneMap;

    public ZoneFinder(Set<Zone> zones) {
        this.zoneMap = new HashMap<>();
        for (Zone zone : zones) {
            zoneMap.put(zone.name, zone);
        }
    }

    public Zone get(String name) {
        return zoneMap.get(name);
    }
}
