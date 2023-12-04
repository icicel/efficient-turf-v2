package util;
import java.time.LocalTime;

// A simple logging utility
// Extend to enable logging for a class
public class Logging {

    private static boolean logging = false;

    public static void init() {
        logging = true;
    }

    public static void log(String message) {
        if (logging) {
            System.out.println(withTimestamp(message));
        }
    }

    public static void warn(String message) {
        if (logging) {
            System.err.println(withTimestamp(message));
        }
    }

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
