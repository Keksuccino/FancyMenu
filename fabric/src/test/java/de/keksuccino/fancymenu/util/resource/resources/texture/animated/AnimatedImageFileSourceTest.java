package de.keksuccino.fancymenu.util.resource.resources.texture.animated;

import de.keksuccino.fancymenu.util.resource.resources.texture.apng.ApngTestFiles;
import de.keksuccino.fancymenu.util.resource.resources.texture.apng.ApngImage;
import de.keksuccino.fancymenu.util.resource.resources.texture.apng.ApngReader;
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

class AnimatedImageFileSourceTest {

    @TempDir
    Path directory;

    @Test
    void tempCopyHoldsTheStreamAndGetsDeletedOnClose() throws IOException {
        byte[] bytes = new byte[200_000];
        new Random(1).nextBytes(bytes);
        Path tempDirectory = Files.createDirectory(this.directory.resolve("animated_textures"));

        AnimatedImageFileSource source = AnimatedImageFileSource.copyToTempFile(new ByteArrayInputStream(bytes), tempDirectory, "apng");
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
        byte[] file = ApngTestFiles.fourFrames();
        Path tempDirectory = Files.createDirectory(this.directory.resolve("animated_textures"));

        ApngImage image = ApngReader.read(AnimatedImageFileSource.copyToTempFile(new ByteArrayInputStream(file), tempDirectory, "apng"), "test.png");
        assertArrayEquals(ApngTestFiles.composeAll(file), AnimatedImageTestSupport.composeAll(image));
        image.close();

        assertEquals(List.of(), list(tempDirectory));
    }

    @Test
    void failedTempCopyLeavesNoFile() throws IOException {
        Path tempDirectory = Files.createDirectory(this.directory.resolve("animated_textures"));
        InputStream brokenDownload = new InputStream() {
            private int count;
            @Override
            public int read() throws IOException {
                if (this.count++ >= 100_000) throw new IOException("Connection lost");
                return 7;
            }
        };

        assertThrows(IOException.class, () -> AnimatedImageFileSource.copyToTempFile(brokenDownload, tempDirectory, "apng"));

        assertEquals(List.of(), list(tempDirectory));
    }

    @Test
    void imageThatFailsToParseDeletesItsTempCopy() throws IOException {
        Path tempDirectory = Files.createDirectory(this.directory.resolve("animated_textures"));

        assertThrows(IOException.class, () -> ApngReader.read(AnimatedImageFileSource.copyToTempFile(new ByteArrayInputStream(new byte[100]), tempDirectory, "apng"), "broken.png"));

        assertEquals(List.of(), list(tempDirectory));
    }

    @Test
    void localFileGetsReadInPlaceWithoutACopy() throws IOException {
        byte[] file = ApngTestFiles.fourFrames();
        Path path = Files.write(this.directory.resolve("image.apng"), file);

        try (ApngImage image = ApngReader.read(AnimatedImageFileSource.open(path), path.toString())) {
            assertArrayEquals(ApngTestFiles.composeAll(file), AnimatedImageTestSupport.composeAll(image));
            assertEquals(List.of(path), list(this.directory));
            // Frames read the file itself, so they notice when it gets changed in place
            Files.write(path, ApngTestFiles.changedFrameData(file, 2));
            assertThrows(AnimatedImageSourceChangedException.class, () -> AnimatedImageTestSupport.composeAll(image));
        }
    }

    @Test
    void localFileCanGetRenamedAndDeletedWhileItPlays() throws IOException {
        byte[] file = ApngTestFiles.fourFrames();
        int[][] expected = ApngTestFiles.composeAll(file);
        Path path = Files.write(this.directory.resolve("image.apng"), file);

        try (ApngImage image = ApngReader.read(AnimatedImageFileSource.open(path), path.toString())) {
            Path renamed = Files.move(path, this.directory.resolve("renamed.apng"));
            assertArrayEquals(expected, AnimatedImageTestSupport.composeAll(image));
            Files.delete(renamed);
            assertArrayEquals(expected, AnimatedImageTestSupport.composeAll(image));
        }

        assertEquals(List.of(), list(this.directory));
    }

    @Test
    void localFileThatGetsShorterWhileItPlaysCountsAsChanged() throws IOException {
        byte[] file = ApngTestFiles.fourFrames();
        Path path = Files.write(this.directory.resolve("image.apng"), file);

        try (ApngImage image = ApngReader.read(AnimatedImageFileSource.open(path), path.toString())) {
            Files.write(path, new byte[file.length / 2]);
            assertThrows(AnimatedImageSourceChangedException.class, () -> AnimatedImageTestSupport.composeAll(image));
        }
    }

    private static byte[] readAll(AnimatedImageSource source) throws IOException {
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
