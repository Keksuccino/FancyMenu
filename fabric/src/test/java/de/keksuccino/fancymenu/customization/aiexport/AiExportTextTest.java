package de.keksuccino.fancymenu.customization.aiexport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiExportTextTest {

    @TempDir
    Path tempDir;

    @Test
    void pathsHaveNoDotSegments() {
        File file = new File(new File(tempDir.toFile(), "."), "folder/../file.txt");
        assertEquals(tempDir.toAbsolutePath().normalize().resolve("file.txt").toString(), AiExportText.path(file));
    }

    @Test
    void relativePathsBecomeAbsolute() {
        assertEquals(Path.of("").toAbsolutePath().resolve("config").toString(), AiExportText.path(new File("./config")));
    }

}
