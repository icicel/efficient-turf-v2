package util;

public class Logging {

    public static boolean logging = false;

    public static void log(String message) {
        if (logging) {
            System.out.println(message);
        }
    }
}
