package turf;
import java.util.Map;
import java.util.Set;
import util.ListSet;
import zone.Connection;
import zone.Zone;

// Represents a combination of a Turf object (Zone and Connection data)
//   and a set of Conditions that specify the problem definition
public class Scenario {

    // Base Turf/Conditions data

    public Set<Zone> zones;
    public Set<Connection> connections;

    public Zone start;
    public Zone end;
    public double timeLimit;

    public double speed;
    public double waitTime;

    public Set<Zone> priority;

    // Scenario-specific information

    public Map<Zone, Integer> points;
    
    public Scenario(Turf turf, Conditions conditions) {
        this.zones = new ListSet<>(turf.zones);
        this.connections = new ListSet<>(turf.connections);

        this.start = turf.getZone(conditions.start);
        this.end = turf.getZone(conditions.end);
        this.timeLimit = conditions.timeLimit;
        this.speed = conditions.speed;
        this.waitTime = conditions.waitTime;
        if (conditions.whitelist != null) {
            inverseRemoveZones(turf.getZones(conditions.whitelist));
        } else if (conditions.blacklist != null) {
            removeZones(turf.getZones(conditions.blacklist));
        }
        this.priority = turf.getZones(conditions.priority);

        this.points = new java.util.HashMap<>();
        for (Zone zone : this.zones) {
            points.put(zone, zone.getPoints(conditions.username, conditions.infiniteRounds));
        }
    }

    // Remove all Zones in a ZoneSet from this.zones and this.connections
    private void removeZones(Set<Zone> zones) {
        for (Zone zone : zones) {
            removeZone(zone);
        }
    }

    // Remove all Zones in this except those in a ZoneSet
    private void inverseRemoveZones(Set<Zone> safeZones) {
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
