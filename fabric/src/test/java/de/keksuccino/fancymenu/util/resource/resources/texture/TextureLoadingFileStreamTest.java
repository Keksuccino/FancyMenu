package de.keksuccino.fancymenu.util.resource.resources.texture;

import de.keksuccino.fancymenu.util.file.ReleasableFileInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks that closing PNG and JPEG textures closes the stream of the file they still load, so the file is never open anymore once they got closed.
 */
class TextureLoadingFileStreamTest {

    @TempDir
    Path directory;

    @Test
    void closingAPngTextureClosesTheFileItStillLoads() throws IOException {
        PngTexture texture = new PngTexture() {};
        ReleasableFileInputStream stream = this.openFile("image.png");
        texture.loadingFileStream = stream;

        texture.close();

        assertThrows(IOException.class, stream::read);
    }

    @Test
    void closingAJpegTextureClosesTheFileItStillLoads() throws IOException {
        JpegTexture texture = new JpegTexture() {};
        ReleasableFileInputStream stream = this.openFile("image.jpg");
        texture.loadingFileStream = stream;

        texture.close();

        assertThrows(IOException.class, stream::read);
    }

    private ReleasableFileInputStream openFile(String name) throws IOException {
        return new ReleasableFileInputStream(Files.write(this.directory.resolve(name), new byte[1000]).toFile());
    }

}
