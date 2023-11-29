package util;

// A simple logging utility
// Extend to enable logging for a class
public class Logging {

    public static boolean logging = false;

    public static void log(String message) {
        if (logging) {
            System.out.println(message);
        }
    }

    public static void warn(String message) {
        if (logging) {
            System.err.println(message);
        }
    }
}
