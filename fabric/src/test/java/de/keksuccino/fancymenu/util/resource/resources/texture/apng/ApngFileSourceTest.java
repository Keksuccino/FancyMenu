package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApngFileSourceTest {

    @TempDir
    Path directory;

    @Test
    void tempCopyHoldsTheStreamAndGetsDeletedOnClose() throws IOException {
        byte[] bytes = new byte[200_000];
        new Random(1).nextBytes(bytes);
        Path tempDirectory = Files.createDirectory(this.directory.resolve("apng_textures"));

        ApngFileSource source = ApngFileSource.copyToTempFile(new ByteArrayInputStream(bytes), tempDirectory);
        try {
            assertEquals(bytes.length, source.size());
            assertArrayEquals(bytes, readAll(source));
        } finally {
            source.close();
        }

        assertEquals(List.of(), list(tempDirectory));
    }

    @Test
    void imageFromATempCopyPlaysAndDeletesItsCopyWhenClosed() throws IOException {
        byte[] file = ApngFrameStreamTest.fourFrames();
        Path tempDirectory = Files.createDirectory(this.directory.resolve("apng_textures"));

        ApngImage image = ApngReader.read(ApngFileSource.copyToTempFile(new ByteArrayInputStream(file), tempDirectory), "test.png");
        assertArrayEquals(ApngCompositorTest.composeAll(file), ApngCompositorTest.composeAll(image));
        image.close();

        assertEquals(List.of(), list(tempDirectory));
    }

    @Test
    void failedTempCopyLeavesNoFile() throws IOException {
        Path tempDirectory = Files.createDirectory(this.directory.resolve("apng_textures"));
        InputStream brokenDownload = new InputStream() {
            private int count;
            @Override
            public int read() throws IOException {
                if (this.count++ >= 100_000) throw new IOException("Connection lost");
                return 7;
            }
        };

        assertThrows(IOException.class, () -> ApngFileSource.copyToTempFile(brokenDownload, tempDirectory));

        assertEquals(List.of(), list(tempDirectory));
    }

    @Test
    void imageThatFailsToParseDeletesItsTempCopy() throws IOException {
        Path tempDirectory = Files.createDirectory(this.directory.resolve("apng_textures"));

        assertThrows(IOException.class, () -> ApngReader.read(ApngFileSource.copyToTempFile(new ByteArrayInputStream(new byte[100]), tempDirectory), "broken.png"));

        assertEquals(List.of(), list(tempDirectory));
    }

    @Test
    void localFileGetsReadInPlaceWithoutACopy() throws IOException {
        byte[] file = ApngFrameStreamTest.fourFrames();
        Path path = Files.write(this.directory.resolve("image.apng"), file);

        try (ApngImage image = ApngReader.read(ApngFileSource.open(path), path.toString())) {
            assertArrayEquals(ApngCompositorTest.composeAll(file), ApngCompositorTest.composeAll(image));
            assertEquals(List.of(path), list(this.directory));
            // Frames read the file itself, so they notice when it gets changed in place
            Files.write(path, ApngFrameStreamTest.changedFrameData(file, 2));
            assertThrows(ApngSourceChangedException.class, () -> ApngCompositorTest.composeAll(image));
        }
    }

    @Test
    void localFileCanGetRenamedAndDeletedWhileItPlays() throws IOException {
        byte[] file = ApngFrameStreamTest.fourFrames();
        int[][] expected = ApngCompositorTest.composeAll(file);
        Path path = Files.write(this.directory.resolve("image.apng"), file);

        try (ApngImage image = ApngReader.read(ApngFileSource.open(path), path.toString())) {
            Path renamed = Files.move(path, this.directory.resolve("renamed.apng"));
            assertArrayEquals(expected, ApngCompositorTest.composeAll(image));
            Files.delete(renamed);
            assertArrayEquals(expected, ApngCompositorTest.composeAll(image));
        }

        assertEquals(List.of(), list(this.directory));
    }

    @Test
    void localFileThatGetsShorterWhileItPlaysCountsAsChanged() throws IOException {
        byte[] file = ApngFrameStreamTest.fourFrames();
        Path path = Files.write(this.directory.resolve("image.apng"), file);

        try (ApngImage image = ApngReader.read(ApngFileSource.open(path), path.toString())) {
            Files.write(path, new byte[file.length / 2]);
            assertThrows(ApngSourceChangedException.class, () -> ApngCompositorTest.composeAll(image));
        }
    }

    private static byte[] readAll(ApngSource source) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate((int) source.size());
        while (buffer.hasRemaining()) {
            if (source.read(buffer, buffer.position()) < 0) break;
        }
        return buffer.array();
    }

    private static List<Path> list(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.collect(Collectors.toList());
        }
    }

}
