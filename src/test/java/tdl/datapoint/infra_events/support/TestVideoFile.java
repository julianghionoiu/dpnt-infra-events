package tdl.datapoint.infra_events.support;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestVideoFile {
    private final Path resourcePath;

    public TestVideoFile(String name) {
        resourcePath = Paths.get("src/test/resources/", name);
    }

    public File asFile() {
        return resourcePath.toFile();
    }
}
