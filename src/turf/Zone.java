package turf;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.json.JSONObject;

public class Zone {

    // Zone name, should be unique
    public String name;
    // One time point payout for taking over the zone
    private int takeoverPoints;
    // Hourly point payout for holding the zone
    private int hourlyPoints;
    // Expected amount of hours the zone will be held for, if taken over now
    private double expectedHoursHeldFromNow;
    // Expected amount of hours the zone will be held for, if taken over at the start of a 5-week round
    private double expectedHoursHeldMax;
    // Can be revisited by owner
    private boolean revisitable;
    // The user who controls the zone
    private String owner;

    // Defines the zone and its statistics from the given API JSON object
    // Takes into account both on-capture points and hourly points by calculating the expected
    //  number of hours the zone will be held
    // Example object:
    // {
    //     "dateCreated": "2010-09-04T16:41:37+0000",
    //     "dateLastTaken": "2013-06-08T12:59:19+0000",
    //     "currentOwner": {"id": 14022, "name": "Pelle494"},
    //     "name": "Plattan",
    //     "id": 138,
    //     "totalTakeovers": 6278,
    //     "region": {
    //         "area": {"name": "Stockholms kommun", "id": 1828},
    //         "country": "se",
    //         "id": 141,
    //         "name": "Stockholm"
    //     },
    //     "pointsPerHour": 9,
    //     "latitude": 59.33225,
    //     "longitude": 18.064098,
    //     "takeoverPoints": 65,
    //     "type": {"id": 9, "name": "Holy"}
    // }
    public Zone(JSONObject info) {
        this.name = info.getString("name").toLowerCase();

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
        double naïveHoursHeld = hoursExisted / (double) totalTakeovers;
        // Expected hours held will be limited by rounds ending so take that into account
        this.expectedHoursHeldMax = Math.min(naïveHoursHeld, 840.0); // 5 weeks - the max possible round length
        this.expectedHoursHeldFromNow = Math.min(naïveHoursHeld, hoursLeftInRound);

        /* Calculate other stuff */
        
        // Raw points values
        this.takeoverPoints = info.getInt("takeoverPoints");
        this.hourlyPoints = info.getInt("pointsPerHour");

        // Owner and revisitability
        if (info.has("currentOwner")) {
            this.owner = info.getJSONObject("currentOwner").getString("name");
            String lastTakenTimestamp = info.getString("dateLastTaken");
            long lastTakenTime = parseTimestamp(lastTakenTimestamp);
            double hoursSinceTaken = asHours(currentTime - lastTakenTime);
            this.revisitable = hoursSinceTaken > 23;
        } else {
            this.owner = null;
            this.revisitable = false;
        }
    }

    // Returns the amount of points the zone is worth for the given user
    // If isNow is false, ignores premature end-of-round resets, revisitability, and the neutral bonus
    public int getPoints(String username, boolean isNow) {
        if (!isNow) {
            return takeoverPoints + (int) (hourlyPoints * expectedHoursHeldMax);
        } else if (owner == null) { // neutral bonus
            return takeoverPoints + (int) (hourlyPoints * expectedHoursHeldFromNow) + 50;
        } else if (!owner.equals(username)) {
            return takeoverPoints + (int) (hourlyPoints * expectedHoursHeldFromNow);
        } else if (revisitable) {
            return takeoverPoints / 2; // integer division
        } else {
            return 0;
        }
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Zone other = (Zone) obj;
        return this.name.equals(other.name);
    }
}
