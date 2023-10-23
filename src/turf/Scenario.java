package turf;
import zone.ConnectionSet;
import zone.Zone;
import zone.ZoneSet;

// Represents a combination of a Turf object (Zone and Connection data)
//   and a set of Conditions that specify the problem definition
public class Scenario {

    public ZoneSet zones;
    public ConnectionSet connections;

    public Zone start;
    public Zone end;
    public double timeLimit;

    public double speed;
    public double waitTime;
    public String username;

    public ZoneSet priority;
    
    public Scenario(Turf turf, Conditions conditions) {
        // TODO: copy instead
        this.zones = turf.zones;
        this.connections = turf.connections;

        this.start = this.zones.findByName(conditions.start);
        this.end = this.zones.findByName(conditions.end);
        this.timeLimit = conditions.timeLimit;

        this.speed = conditions.speed;
        this.waitTime = conditions.waitTime;
        this.username = new String(conditions.username);

        deleteZones(namesToZones(conditions.blacklist));
        if (conditions.whitelist != null)
            inverseDeleteZones(namesToZones(conditions.whitelist));
        this.priority = namesToZones(conditions.priority);
    }

    private ZoneSet namesToZones(String[] names) {
        ZoneSet zones = new ZoneSet();
        for (String name : names) {
            Zone zone = this.zones.findByName(name.toLowerCase());
            zones.add(zone);
        }
        return zones;
    }

    // Delete all Zones in a ZoneSet
    private void deleteZones(ZoneSet zones) {
        for (Zone zone : zones) {
            deleteZone(zone);
        }
    }

    // Delete all Zones except those in a ZoneSet
    private void inverseDeleteZones(ZoneSet safeZones) {
        for (Zone zone : this.zones) {
            if (!safeZones.contains(zone)) {
                deleteZone(zone);
            }
        }
    }

    // Completely remove all references to a Zone from this.zones and this.connections
    private void deleteZone(Zone zone) {
        if (zone == null) {
            return;
        }
        // TODO
    }
}
