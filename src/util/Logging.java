package util;
import java.time.LocalTime;

// A simple logging utility
// Extend to enable logging for a class
public class Logging {

    private static Level level = Level.NONE;

    // NONE = only critical errors
    // INFO = above plus program flow information
    // WARN = above plus non-critical errors
    // DEBUG = above plus information about objects
    // TRACE = above plus detailed information
    public enum Level { 
        NONE, INFO, WARN, DEBUG, TRACE;
    }

    public static void init(Level level) {
        Logging.level = level;
    }

    // log messages at the different levels
    public static void info(String message) {
        if (level != Level.NONE) {
            System.out.println(withTimestamp(message));
        }
    }
    public static void warn(String message) {
        if (level != Level.NONE && level != Level.INFO) {
            System.out.println(withTimestamp(message));
        }
    }
    public static void debug(String message) {
        if (level == Level.DEBUG || level == Level.TRACE) {
            System.out.println(withTimestamp(message));
        }
    }
    public static void trace(String message) {
        if (level == Level.TRACE) {
            System.out.println(withTimestamp(message));
        }
    }

    // An infernal creation sanctioned by Satan himself
    // string handling!!!!!! :DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD
    private static String withTimestamp(String message) {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();
        int second = now.getSecond();
        String micro = (Integer.toString(now.getNano()) + "00")
            .substring(0, 3);
        StringBuilder result = new StringBuilder();
        result.append("[")
            .append(hour < 10 ? "0" : "").append(hour)
            .append(minute < 10 ? ":0" : ":").append(minute)
            .append(second < 10 ? ":0" : ":").append(second)
            .append(".").append(micro)
            .append("] ").append(message);
        return result.toString();
    }
}
