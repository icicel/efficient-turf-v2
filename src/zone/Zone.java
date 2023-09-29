package zone;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;
import map.Coords;

public class Zone {

    public String name;
    public int points;
    public Set<Connection> connections;

    public ZoneType type;

    public Coords coords;

    // Create a zone from a Point
    // type defaults to CROSSING until points are set
    public Zone(String name, String coordinates) {
        this.name = name.toLowerCase();
        this.points = 0;
        this.coords = new Coords(coordinates);
        this.connections = new HashSet<>();
        this.type = ZoneType.CROSSING;
    }

    // Calculates a total point value for the zone from the given API JSON object
    // Takes into account both on-capture points and hourly points by calculating the expected
    //  number of hours the zone will be held
    // Example object:
    //   [{"dateCreated": "2020-06-18T19:04:42+0000",
    //     "dateLastTaken": "2023-09-09T13:07:18+0000",
    //     "currentOwner": {"name": "user", "id": 1},
    //     "name": "OdlaMedCykel",
    //     "id": 296263,
    //     "totalTakeovers": 188,
    //     "region": {"country": "se", "name": "Västra Götaland", "id": 132},
    //     "pointsPerHour": 1,
    //     "latitude": 58.047462,
    //     "longitude": 11.851104,
    //     "takeoverPoints": 185}
    //      , ...]
    public void setPoints(JSONObject info) {

        // find the amount of hours the zone has existed for (current time - creation time)
        String creationTimestamp = info.getString("dateCreated");
        long creationTime = parseTimestamp(creationTimestamp);
        long currentTime = System.currentTimeMillis();
        double hoursExisted = asHours(currentTime - creationTime);
        
        // Find how many hours are left in this round
        // A round ends and a new one begins at 12:00 swedish time the first sunday of every month
        // So to find the next round end, we get the first sunday of this month, and then simply add
        //   a month if that sunday is in the past
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstDay = LocalDateTime.of(now.getYear(), now.getMonth(), 1, 12, 0);
        // 7 - day of week = days until sunday
        LocalDateTime firstSunday = firstDay.plusDays(7 - firstDay.getDayOfWeek().getValue());
        if (firstSunday.isBefore(now)) {
            firstSunday = firstSunday.plusMonths(1);
        }
        long roundEndTime = parseDatetime(firstSunday);
        double hoursLeftInRound = asHours(roundEndTime - currentTime);

        // Adding 1 to total takeovers because we count when the zone was created
        // A "takeover from nonexistance", if you will - it makes the math work out
        //   for zones with few/no takeovers
        int totalTakeovers = info.getInt("totalTakeovers") + 1;

        // Find the expected amount of hours the zone will be held for
        // The first calculation ignores the fact that the round might end, resetting ownership prematurely
        //   (also I really wanted to use ï in a variable name)
        double naïveHoursHeld = hoursExisted / (double) totalTakeovers;
        double expectedHoursHeld = Math.min(naïveHoursHeld, hoursLeftInRound);

        // Calculate the total points!
        // Stored as int because Turf doesn't do fractional points B)
        int takeoverPoints = info.getInt("takeoverPoints");

        int pointsPerHour = info.getInt("pointsPerHour");
        int holdingPoints = (int) expectedHoursHeld * pointsPerHour;

        this.points = takeoverPoints + holdingPoints;
        this.type = ZoneType.REAL;
    }

    /* Time methods */

    // Parses a timestamp string into a long (ms)
    // Example: "2020-06-18T19:04:42+0000"
    private static long parseTimestamp(String timestamp) {
        // Remove the timezone offset
        String trimmed = timestamp.substring(0, timestamp.length() - 5);
        LocalDateTime datetime = LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return parseDatetime(datetime);
    }
    // Parses a datetime object into a long (ms)
    private static long parseDatetime(LocalDateTime datetime) {
        Date date = java.sql.Date.valueOf(datetime.toLocalDate());
        return date.getTime();
    }
    // Get hours from milliseconds
    private static double asHours(long ms) {
        return ms / 1000 / 60 / 60;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Zone)) {
            return false;
        }
        Zone otherZone = (Zone) other;
        return this.name.equals(otherZone.name);
    }
    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
