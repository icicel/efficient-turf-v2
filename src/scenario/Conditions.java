package scenario;

public class Conditions {
    
    /* Problem definition */

    public String start;
    public String end;
    public double timeLimit;

    /* Turfing variables */

    // Speed in meters per minute (ignoring wait times at zones)
    public double speed;

    // Username to use for zone ownership calculation
    public String username;

    // Count varying factors like revisitability or neutral bonuses
    // Essentially, if true, use data from exactly now, otherwise use more general data
    public boolean isNow;

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
        this.isNow = true;
    }
}
