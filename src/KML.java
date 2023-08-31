import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

// Stores a KML InputStream
public class KML {

    InputStream stream;

    public KML(Path path) throws IOException {
        this.stream = Files.newInputStream(path);
    }

    public KML(InputStream stream) {
        this.stream = stream;
    }

}
