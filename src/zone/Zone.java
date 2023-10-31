package zone;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.json.JSONObject;
import map.Coords;

public class Zone {

    public String name;
    public ConnectionSet connections;

    public ZoneType type;

    public Coords coords;

    // Points-related values

    // One time point payout for taking over the zone
    private int takeoverPoints;
    // Hourly point payout for holding the zone
    private int hourlyPoints;
    // Expected amount of hours the zone will be held for
    private double expectedHoursHeld;
    // expectedHoursHeld, but without taking into account Turf rounds ending
    private double naïveHoursHeld;
    // Can be revisited by owner
    private boolean revisitable;
    // The user who controls the zone
    private String owner;

    // Create a zone from a Point
    // type defaults to CROSSING until points are set
    public Zone(String name, String coordinates) {
        this.name = name.toLowerCase();
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
    public void initPoints(JSONObject info) {
        
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
        if (firstSunday.isBefore(now)) {
            firstSunday = firstSunday.plusMonths(1);
        }
        // This is then guaranteed to be the next round end datetime
        long roundEndTime = parseDatetime(firstSunday);
        double hoursLeftInRound = asHours(roundEndTime - currentTime);

        // Adding 1 to total takeovers because we count when the zone was created
        // A "takeover from nonexistance", if you will - it makes the math work out
        //   for zones with few/no takeovers
        int totalTakeovers = info.getInt("totalTakeovers") + 1;

        // Final calculations
        naïveHoursHeld = hoursExisted / (double) totalTakeovers;
        expectedHoursHeld = Math.min(naïveHoursHeld, hoursLeftInRound);

        /* Calculate other stuff */
        
        takeoverPoints = info.getInt("takeoverPoints");
        hourlyPoints = info.getInt("pointsPerHour");

        String lastTakenTimestamp = info.getString("dateLastTaken");
        long lastTakenTime = parseTimestamp(lastTakenTimestamp);
        double hoursSinceTaken = asHours(currentTime - lastTakenTime);
        if (hoursSinceTaken > 23) {
            revisitable = true;
        } else {
            revisitable = false;
        }

        if (info.has("currentOwner")) {
            owner = info.getJSONObject("currentOwner").getString("name");
        } else {
            owner = null;
        }

        this.type = ZoneType.REAL;
    }

    // Returns the amount of points the zone is worth for the given user
    // If naïve is true, ignores the fact that Turf rounds end
    public int getPoints(String username, boolean naïve) {
        if (type == ZoneType.CROSSING) {
            return 0;
        }
        double hoursHeld;
        if (naïve) {
            hoursHeld = naïveHoursHeld;
        } else {
            hoursHeld = expectedHoursHeld;
        }

        int points = 0;

        if (owner == null) {
            points += takeoverPoints;
            points += hourlyPoints * hoursHeld;
            points += 50;
        } else if (!owner.equals(username)) {
            points += takeoverPoints;
            points += hourlyPoints * hoursHeld;
        } else if (revisitable) {
            points += takeoverPoints / 2; // integer division
        }

        return points;
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
    // Assumes datetimes (and by extension, API timestamps) are in Swedish time
    private static long parseDatetime(LocalDateTime datetime) {
        ZonedDateTime swedishDatetime = datetime.atZone(ZoneId.of("Europe/Stockholm"));
        return swedishDatetime.toInstant().toEpochMilli();
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
