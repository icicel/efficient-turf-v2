import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

// Stores a KML InputStream
// It actually doesn't have to be a KML
public class KML {

    private InputStream stream;

    public KML(Path path) throws IOException {
        this.stream = Files.newInputStream(path);
    }
    public KML(InputStream stream) {
        this.stream = stream;
    }

    public InputStream asStream() {
        return stream;
    }

    public String asString() throws IOException {
        byte[] bytes = stream.readAllBytes();
        return new String(bytes);
    }


}
