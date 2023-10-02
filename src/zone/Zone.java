package zone;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.json.JSONObject;
import map.Coords;

public class Zone {

    public String name;
    public int points;
    public ConnectionSet connections;

    public ZoneType type;

    public Coords coords;

    // Create a zone from a Point
    // type defaults to CROSSING until points are set
    public Zone(String name, String coordinates) {
        this.name = name.toLowerCase();
        this.points = 0;
        this.coords = new Coords(coordinates);
        this.connections = new ConnectionSet();
        this.type = ZoneType.CROSSING;
    }

    // Calculates a total point value for the zone from the given API JSON object
    //  and the given Turf username
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
    public void setPoints(JSONObject info, String username) {
        
        /* Calculate the average amount of hours the zone was held for */

        long currentTime = System.currentTimeMillis();

        // find the amount of hours the zone has existed for (current time - creation time)
        String creationTimestamp = info.getString("dateCreated");
        long creationTime = parseTimestamp(creationTimestamp);
        double hoursExisted = asHours(currentTime - creationTime);
        
        // Find how many hours are left in this round
        // A round ends and a new one begins at 12:00 swedish time the first sunday of every month
        LocalDateTime now = LocalDateTime.now();
        // First day of the current month at 12:00
        LocalDateTime firstDay = LocalDateTime.of(now.getYear(), now.getMonth(), 1, 12, 0);
        // First sunday of current month
        // Days until sunday = 7 - weekday number
        LocalDateTime firstSunday = firstDay.plusDays(7 - firstDay.getDayOfWeek().getValue());
        // If the first sunday has already passed, change month to next month
        if (firstSunday.isBefore(now))
            firstSunday = firstSunday.plusMonths(1);
        // This is then guaranteed to be the next round end datetime
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


        /* Everything else */

        boolean userControlsZone = false;
        int neutralPoints = 0;

        // If the zone has no owner ("currentOwner" does not exist), it's neutral and a bonus is added
        boolean hasOwner = info.has("currentOwner");
        if (!hasOwner)
            neutralPoints = 50;
        
        else {
            // If the zone's owner is the user, points are calculated differently
            String owner = info.getJSONObject("currentOwner").getString("name");
            if (owner.equals(username))
                userControlsZone = true;
        }

        // One time point payout for taking over the zone
        int takeoverPoints = info.getInt("takeoverPoints");

        // Ordinary calculation
        if (!userControlsZone) {
            int pointsPerHour = info.getInt("pointsPerHour");

            // Stored as int because Turf doesn't do fractional points B)
            int holdingPoints = (int) expectedHoursHeld * pointsPerHour;

            this.points = takeoverPoints + holdingPoints + neutralPoints;
        }

        // Special calculation for when the user controls the zone
        else {
            // If 23 hours have passed since the zone was taken, grant revisit points, otherwise nothing
            String lastTakenTimestamp = info.getString("dateLastTaken");
            long lastTakenTime = parseTimestamp(lastTakenTimestamp);
            double hoursSinceTaken = asHours(currentTime - lastTakenTime);

            if (hoursSinceTaken > 23)
                this.points = takeoverPoints / 2; // integer division
            else
                this.points = 0;
        }
        
        // Change type
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
    public int hashCode() {
        return this.name.hashCode();
    }
}
