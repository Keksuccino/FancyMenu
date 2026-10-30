package de.keksuccino.fancymenu.util.resource.resources.texture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimatedTextureDirectSourceLoopCountTest {

    @TempDir
    Path tempDir;

    @Test
    void localGifSourceUsesLoopCountOfFile() throws IOException {
        GifTexture texture = new GifTexture();
        texture.sourceFile = this.writeFile("three_plays.gif", gifWithLoops(3));

        assertEquals(3, GifTexture.readDirectSourceLoopCount(texture, "three_plays.gif"));
    }

    @Test
    void localGifSourceWithoutLoopExtensionPlaysOnce() throws IOException {
        GifTexture texture = new GifTexture();
        texture.sourceFile = this.writeFile("once.gif", gifWithLoops(-1));

        assertEquals(1, GifTexture.readDirectSourceLoopCount(texture, "once.gif"));
    }

    @Test
    void unreadableGifSourceLoopsForever() {
        GifTexture texture = new GifTexture();
        texture.sourceFile = this.tempDir.resolve("missing.gif").toFile();

        assertEquals(AnimatedImageLoopCountReader.INFINITE_LOOPS, GifTexture.readDirectSourceLoopCount(texture, "missing.gif"));
    }

    @Test
    void apngSourceUsesPlayCountOfFile() throws IOException {
        ApngTexture texture = new ApngTexture();
        texture.sourceFile = this.writeFile("two_plays.png", apngWithPlays(2));

        assertEquals(2, ApngTexture.readDirectSourceLoopCount(texture, "two_plays.png"));
    }

    private File writeFile(String name, byte[] data) throws IOException {
        return Files.write(this.tempDir.resolve(name), data).toFile();
    }

    private static byte[] gifWithLoops(int loops) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("GIF89a".getBytes(StandardCharsets.US_ASCII));
        out.writeBytes(new byte[] {1, 0, 1, 0, (byte) 0x80, 0, 0});
        out.writeBytes(new byte[6]);
        if (loops >= 0) {
            out.writeBytes(new byte[] {0x21, (byte) 0xFF, 11});
            out.writeBytes("NETSCAPE2.0".getBytes(StandardCharsets.US_ASCII));
            out.writeBytes(new byte[] {3, 1, (byte) loops, 0, 0});
        }
        out.writeBytes(new byte[] {0x2C, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 2, 0x4C, 0x01, 0, 0x3B});
        return out.toByteArray();
    }

    private static byte[] apngWithPlays(int plays) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        out.writeBytes(new byte[] {0, 0, 0, 13, 'I', 'H', 'D', 'R'});
        out.writeBytes(new byte[13 + 4]);
        out.writeBytes(new byte[] {0, 0, 0, 8, 'a', 'c', 'T', 'L', 0, 0, 0, 2, 0, 0, 0, (byte) plays});
        out.writeBytes(new byte[4]);
        return out.toByteArray();
    }

}
