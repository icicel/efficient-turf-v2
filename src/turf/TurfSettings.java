package turf;

// A class to store settings for a Turf object
// It is recommended to not set settings directly but to use the methods instead
//   (allows chaining which looks cool)
public class TurfSettings {

    /* Route settings */

    // Time limit in minutes
    public Double timeLimit;

    // Start and end zone
    public String startZone;
    public String endZone;

    // Blacklist (zones that cannot be visited)
    public String[] blacklist;

    // Whitelist (zones that must be visited)
    public String[] whitelist;


    /* Personal settings */

    // Speed in meters per minute (ignoring wait times at zones)
    public Double speed = 60.0;

    // Time in minutes to wait at each zone
    public Double waitTime = 1.0;

    // Turf username, relevant for calculating revisit points
    public String username;


    // Constructor and setters

    public TurfSettings() {}

    public TurfSettings setTimeLimit(Double timeLimit) {
        this.timeLimit = timeLimit; return this;
    }
    public TurfSettings setStartZone(String startZone) {
        this.startZone = startZone; return this;
    }
    public TurfSettings setEndZone(String endZone) {
        this.endZone = endZone; return this;
    }
    public TurfSettings setBlacklist(String[] blacklist) {
        this.blacklist = blacklist; return this;
    }
    public TurfSettings setWhitelist(String[] whitelist) {
        this.whitelist = whitelist; return this;
    }
    public TurfSettings setSpeed(Double speed) {
        this.speed = speed; return this;
    }
    public TurfSettings setWaitTime(Double waitTime) {
        this.waitTime = waitTime; return this;
    }
    public TurfSettings setUsername(String username) {
        this.username = username; return this;
    }
}
