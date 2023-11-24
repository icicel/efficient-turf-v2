package scenario;

public class Conditions {
    
    /* Problem definition */

    public String start;
    public String end;
    public double timeLimit;

    /* Turfing variables */

    // Speed in meters per minute (ignoring wait times at zones)
    public double speed;

    // Time in minutes to wait at each zone
    public double waitTime;

    // Username to use for zone ownership calculation
    public String username;

    // Ignore end of Turf rounds when calculating Zone points
    public boolean infiniteRounds;

    /* Zone adjustments */

    // Names of zones to avoid (a.k.a. remove completely)
    public String[] blacklist;

    // Names of zones to NOT avoid (a.k.a. remove all other zones)
    // Ignored if null
    public String[] whitelist;

    // Names of zones to prioritize (a.k.a. must be included in the final Route)
    public String[] priority;

    public Conditions(String start, String end, double timeLimit) {
        this.start = start;
        this.end = end;
        this.timeLimit = timeLimit;

        // Default values
        this.speed = 60.0;
        this.waitTime = 1.0;
        this.infiniteRounds = false;
    }
}
