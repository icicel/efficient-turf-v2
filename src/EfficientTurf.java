import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import kml.KML;

public class EfficientTurf {
    public static void main(String[] args) throws Exception {
        return;

        // use depth first search with a single path object
        // if it's valid and finished then copy and save
    }

    // Get a KML file located in the root directory
    public static KML getKML(String file) throws IOException {
        Path path = FileSystems.getDefault().getPath(".", file);
        return new KML(path);
    }
}
