package de.keksuccino.fancymenu.util.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleasableFileInputStreamTest {

    private static final long TIMEOUT_SECONDS = 10L;

    @TempDir
    Path directory;

    @Test
    void readsTheWholeFileInPiecesOfAtMostTheReadLimit() throws IOException {
        byte[] bytes = this.randomBytes(3 * ReleasableFileInputStream.MAX_READ_BYTES + 123);
        Path file = Files.write(this.directory.resolve("image.png"), bytes);
        ByteArrayOutputStream read = new ByteArrayOutputStream();

        try (ReleasableFileInputStream in = new ReleasableFileInputStream(file.toFile())) {
            byte[] buffer = new byte[bytes.length];
            int count;
            while ((count = in.read(buffer, 0, buffer.length)) >= 0) {
                assertTrue(count <= ReleasableFileInputStream.MAX_READ_BYTES, "read " + count + " bytes at once");
                read.write(buffer, 0, count);
            }
        }

        assertArrayEquals(bytes, read.toByteArray());
    }

    @Test
    void closingOnAnotherThreadMakesTheNextReadOfTheReaderFail() throws Exception {
        Path file = Files.write(this.directory.resolve("image.png"), this.randomBytes(4 * ReleasableFileInputStream.MAX_READ_BYTES));
        ReleasableFileInputStream in = new ReleasableFileInputStream(file.toFile());
        CountDownLatch firstReadDone = new CountDownLatch(1);
        CountDownLatch closed = new CountDownLatch(1);
        AtomicReference<Throwable> readerFailure = new AtomicReference<>();
        Thread reader = new Thread(() -> {
            byte[] buffer = new byte[1024];
            try {
                in.read(buffer);
                firstReadDone.countDown();
                assertTrue(closed.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                in.read(buffer);
            } catch (Throwable throwable) {
                readerFailure.set(throwable);
            }
        }, "Releasable stream test reader");
        reader.start();
        assertTrue(firstReadDone.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        in.close();
        closed.countDown();

        reader.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        assertFalse(reader.isAlive());
        assertInstanceOf(IOException.class, readerFailure.get());
    }

    @Test
    void everyReadFailsAfterClosingAndClosingTwiceDoesNothing() throws IOException {
        Path file = Files.write(this.directory.resolve("image.png"), this.randomBytes(100));
        ReleasableFileInputStream in = new ReleasableFileInputStream(file.toFile());

        in.close();
        in.close();

        assertThrows(IOException.class, in::read);
        assertThrows(IOException.class, () -> in.read(new byte[10], 0, 10));
        assertThrows(IOException.class, () -> in.skip(10L));
        assertThrows(IOException.class, in::available);
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new Random(length).nextBytes(bytes);
        return bytes;
    }

}
