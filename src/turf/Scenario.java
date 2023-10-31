package turf;
import java.util.Map;
import zone.Connection;
import zone.ConnectionSet;
import zone.Zone;
import zone.ZoneSet;

// Represents a combination of a Turf object (Zone and Connection data)
//   and a set of Conditions that specify the problem definition
public class Scenario {

    // Base Turf/Conditions data

    public ZoneSet zones;
    public ConnectionSet connections;

    public Zone start;
    public Zone end;
    public double timeLimit;

    public double speed;
    public double waitTime;

    public ZoneSet priority;

    // Scenario-specific information

    public Map<Zone, Integer> points;
    
    public Scenario(Turf turf, Conditions conditions) {
        this.zones = new ZoneSet(turf.zones);
        this.connections = new ConnectionSet(turf.connections);

        this.start = this.zones.findByName(conditions.start);
        this.end = this.zones.findByName(conditions.end);
        this.timeLimit = conditions.timeLimit;
        this.speed = conditions.speed;
        this.waitTime = conditions.waitTime;
        if (conditions.whitelist != null) {
            inverseRemoveZones(namesToZones(conditions.whitelist));
        } else if (conditions.blacklist != null) {
            removeZones(namesToZones(conditions.blacklist));
        }
        this.priority = namesToZones(conditions.priority);

        for (Zone zone : this.zones) {
            points.put(zone, zone.getPoints(conditions.username, conditions.infiniteRounds));
        }
    }

    // Convert an array of zone names to a ZoneSet
    private ZoneSet namesToZones(String[] names) {
        ZoneSet zones = new ZoneSet();
        for (String name : names) {
            Zone zone = this.zones.findByName(name.toLowerCase());
            zones.add(zone);
        }
        return zones;
    }

    // Remove all Zones in a ZoneSet from this.zones and this.connections
    private void removeZones(ZoneSet zones) {
        for (Zone zone : zones) {
            removeZone(zone);
        }
    }

    // Remove all Zones in this except those in a ZoneSet
    private void inverseRemoveZones(ZoneSet safeZones) {
        for (Zone zone : this.zones) {
            if (!safeZones.contains(zone)) {
                removeZone(zone);
            }
        }
    }

    // Completely remove all references to a Zone from this.zones and this.connections
    private void removeZone(Zone zone) {
        if (zone == null) {
            return;
        }
        this.zones.remove(zone);
        for (Connection connection : zone.connections) {
            this.connections.remove(connection);
            this.connections.remove(connection.reverse);
        };
    }
}
