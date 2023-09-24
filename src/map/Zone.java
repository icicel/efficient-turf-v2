package map;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;

public class Zone {

    public String name;
    public double points;
    public Set<Connection> connections;

    public Coords coords;

    public Zone(String name, String coordinates) {
        this.name = name.toLowerCase();
        this.coords = new Coords(coordinates);
        this.connections = new HashSet<>();
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
        double hoursExisted = (currentTime - creationTime) / 1000 / 60 / 60;

        // Find the expected amount of hours the zone will be held for
        // Adding 1 to total takeovers because we count when the zone was created
        // A "takeover from nonexistance", if you will - it makes the math work out
        //   for zones with few/no takeovers
        int totalTakeovers = info.getInt("totalTakeovers") + 1;
        double expectedHoursHeld = hoursExisted / totalTakeovers;

        // Calculate the total points!
        int pointsPerHour = info.getInt("pointsPerHour");
        int takeoverPoints = info.getInt("takeoverPoints");
        this.points = expectedHoursHeld * pointsPerHour + takeoverPoints;
    }

    // Parses a timestamp string into a long (ms)
    // Example: "2020-06-18T19:04:42+0000"
    private static long parseTimestamp(String timestamp) {
        LocalDateTime datetime = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
        Date date = java.sql.Date.valueOf(datetime.toLocalDate());
        return date.getTime();
    }

    @Override
    public String toString() {
        return name;
    }
}
