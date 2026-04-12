package turf;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import map.Coords;

public class Export {

    // My Maps can accept max 2000 items per file, this handles that
    private static <I> void exportGeneric(
        Path path, String headers, Iterable<I> items, Function<I, String> toData
    ) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(headers).append("\n");
        int fileCount = 1;
        int count = 1;
        for (I item : items) {
            sb.append(toData.apply(item)).append("\n");
            count++;
            if (count > 2000) {
                // Reset for next file
                writeSubfile(path, fileCount++, sb.toString());
                sb = new StringBuilder();
                sb.append(headers).append("\n");
                count = 1;
            }
        }
        if (fileCount == 1) {
            Files.writeString(path, sb.toString());
        } else if (count > 1) {
            writeSubfile(path, fileCount, sb.toString());
        }
    }

    // Export as CSV
    // Creates multiple files if there are more than 2000 connections
    public static void exportConnections(Turf turf, Path path) throws IOException {
        exportGeneric(
            path,
            "WKT,name",
            turf.connections,
            connection -> {
                StringBuilder sb = new StringBuilder();
                sb.append(connection.isLoop() ? "\"POLYGON ((" : "\"LINESTRING (");
                sb.append(connection.left.coords.lon).append(" ").append(connection.left.coords.lat);
                for (Coords middleCoords : connection.middle) {
                    sb.append(", ");
                    sb.append(middleCoords.lon).append(" ").append(middleCoords.lat);
                }
                sb.append(", ");
                sb.append(connection.right.coords.lon).append(" ").append(connection.right.coords.lat);
                sb.append(connection.isLoop() ? "))\"," : ")\",");
                sb.append(connection.left).append("-").append(connection.right).append("\n");
                return sb.toString();
            }
        );
    }

    public static void exportCrossings(Turf turf, Path path) throws IOException {
        exportGeneric(
            path,
            "WKT,name",
            turf.crossings,
            crossing -> {
                StringBuilder sb = new StringBuilder();
                sb.append("\"POINT (");
                sb.append(crossing.coords.lon).append(" ").append(crossing.coords.lat);
                sb.append(")\",");
                sb.append(crossing).append("\n");
                return sb.toString();
            }
        );
    }
    
    public static void exportZones(Turf turf, Path path) throws IOException {
        exportGeneric(
            path,
            "WKT,name",
            turf.zones,
            zone -> {
                StringBuilder sb = new StringBuilder();
                sb.append("\"POINT (");
                sb.append(zone.coords.lon).append(" ").append(zone.coords.lat);
                sb.append(")\",");
                sb.append(zone).append("\n");
                return sb.toString();
            }
        );
    }

    public static void exportZonesWithPoints(Turf turf, Path path, String username, boolean isNow) throws IOException {
        exportGeneric(
            path,
            "WKT,name,points",
            turf.zones,
            zone -> {
                StringBuilder sb = new StringBuilder();
                sb.append("\"POINT (");
                sb.append(zone.coords.lon).append(" ").append(zone.coords.lat);
                sb.append(")\",");
                sb.append(zone).append(",");
                sb.append(zone.zone.getPoints(username, isNow)).append("\n");
                return sb.toString();
            }
        );
    }

    private static void writeSubfile(Path path, int fileId, String content) throws IOException {
        String filename = path.getFileName().toString();
        String extension = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex != -1) {
            extension = filename.substring(dotIndex);
            filename = filename.substring(0, dotIndex);
        }
        Path subfilePath = path.resolveSibling(filename + "_" + fileId + extension);
        Files.writeString(subfilePath, content);
    }
}
