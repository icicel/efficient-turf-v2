import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

// Stores a KML String
// It actually doesn't have to be a KML
public class KML {

    private String document;

    public KML(Path path) throws IOException {
        this.document = Files.readString(path);
    }
    public KML(String string) {
        this.document = string;
    }

    public Reader asReader() {
        return new StringReader(document);
    }

    public String asString() {
        return document;
    }
}
