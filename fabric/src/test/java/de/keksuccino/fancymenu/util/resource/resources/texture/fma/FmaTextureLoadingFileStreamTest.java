package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import de.keksuccino.fancymenu.util.file.ReleasableFileInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FmaTextureLoadingFileStreamTest {

    @TempDir
    Path directory;

    @Test
    void closingTheTextureClosesTheFileThatStillGetsCopied() throws IOException {
        FmaTexture texture = new FmaTexture() {};
        ReleasableFileInputStream stream = new ReleasableFileInputStream(Files.write(this.directory.resolve("image.fma"), new byte[1000]).toFile());
        texture.loadingFileStream = stream;

        texture.close();

        assertThrows(IOException.class, stream::read);
    }

}
