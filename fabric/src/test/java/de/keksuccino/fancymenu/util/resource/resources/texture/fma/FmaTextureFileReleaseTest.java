package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import de.keksuccino.fancymenu.util.resource.resources.texture.GatedZipChannel;
import de.keksuccino.fancymenu.util.resource.resources.texture.LogCapture;
import de.keksuccino.fancymenu.util.resource.resources.texture.ReadGate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Loads real FMA files into real textures with their real stream threads, and checks that closing and resetting a texture take turns with the reads of its stream thread:
 * closing releases the archive right after a read that runs, without errors, and resetting neither waits for the read nor breaks the archive.
 */
class FmaTextureFileReleaseTest {

    private static final int FRAMES = 3;
    /** How long a reset may take at most, which is far less than a read that waits at a gate. **/
    private static final long MAX_RESET_MILLIS = 2000L;

    @TempDir
    Path directory;
    private FileTexture texture;

    @AfterEach
    void closeTexture() throws InterruptedException {
        if (this.texture == null) return;
        Thread stream = this.texture.streamThread;
        this.texture.close();
        if (stream != null) ReadGate.join(stream);
    }

    @Test
    void closeWaitsForAReadOfTheStreamThreadAndReleasesTheArchiveRightAfterItWithoutErrors() throws Exception {
        try (LogCapture problems = LogCapture.problemsOf(FmaTexture.class, FmaDecoder.class)) {
            this.load();
            GatedZipChannel archive = this.texture.requireDecoder().requireOpenedChannel();
            ReadGate gate = new ReadGate();
            archive.readGate = gate;
            this.texture.startTickerIfNeeded();
            Thread stream = this.texture.streamThread;
            assertNotNull(stream);
            gate.awaitReached();

            Thread closer = new Thread(this.texture::close, "FMA test closer");
            try {
                closer.start();
                ReadGate.awaitBlocked(closer);
                // The read holds the lock of the archive, so the archive stays open until the read is done
                assertTrue(archive.isOpen());
            } finally {
                gate.open();
            }
            ReadGate.join(closer);

            assertFalse(archive.isOpen());
            ReadGate.join(stream);
            assertFalse(archive.wasReadAfterClose());
            assertFalse(this.texture.isLoadingFailed());
            assertEquals(List.of(), problems.messages());
        }
    }

    @Test
    void resetDoesntWaitForAReadOfTheStreamThreadAndTheArchiveKeepsPlaying() throws Exception {
        try (LogCapture problems = LogCapture.problemsOf(FmaTexture.class, FmaDecoder.class)) {
            this.load();
            GatedZipChannel archive = this.texture.requireDecoder().requireOpenedChannel();
            ReadGate gate = new ReadGate();
            archive.readGate = gate;
            this.texture.startTickerIfNeeded();
            Thread stream = this.texture.streamThread;
            assertNotNull(stream);
            gate.awaitReached();

            Thread resetter = new Thread(this.texture::reset, "FMA test resetter");
            try {
                resetter.start();
                resetter.join(MAX_RESET_MILLIS);
                assertFalse(resetter.isAlive(), "The reset waited for the read of the stream thread");
            } finally {
                gate.open();
            }
            ReadGate.join(resetter);
            ReadGate.join(stream);

            // The stream that the reset started shows the first frame again
            this.awaitPendingFrame();
            assertTrue(archive.isOpen());
            assertFalse(this.texture.isLoadingFailed());
            assertEquals(List.of(), problems.messages());
        }
    }

    @Test
    void closeReleasesTheArchiveOfATextureThatIsStillLoading() throws Exception {
        try (LogCapture problems = LogCapture.problemsOf(FmaTexture.class, FmaDecoder.class)) {
            Path file = FmaTestFiles.write(this.directory.resolve("noise.fma"), FRAMES);
            ReadGate gate = new ReadGate();
            this.texture = new FileTexture(this.spoolDirectory(), gate);
            Thread loader = new Thread(() -> this.populate(file), "FMA test loader");
            loader.start();
            gate.awaitReached();
            GatedFmaDecoder decoder = this.texture.requireDecoder();
            GatedZipChannel archive = decoder.requireOpenedChannel();
            assertTrue(archive.isOpen());

            try {
                this.texture.close();
                assertFalse(archive.isOpen());
            } finally {
                gate.open();
            }
            ReadGate.join(loader);

            assertNull(this.texture.decoder);
            assertTrue(this.texture.isLoadingFailed());
            // The spooled copy of the archive got deleted
            try (var spooled = Files.list(this.spoolDirectory().toPath())) {
                assertEquals(0L, spooled.count());
            }
            assertEquals(List.of(), problems.messages());
        }
    }

    @Test
    void closedTextureNeverOpensItsArchive() throws IOException {
        Path file = FmaTestFiles.write(this.directory.resolve("noise.fma"), 1);
        this.texture = new FileTexture(this.spoolDirectory(), null);
        this.texture.close();

        this.populate(file);

        assertNull(this.texture.createdDecoder);
        assertFalse(this.texture.isReady());
    }

    private void load() throws IOException {
        Path file = FmaTestFiles.write(this.directory.resolve("noise.fma"), FRAMES);
        this.texture = new FileTexture(this.spoolDirectory(), null);
        this.populate(file);
        assertTrue(this.texture.isLoadingCompleted());
    }

    private void populate(@NotNull Path file) {
        try {
            FmaTexture.populateTexture(this.texture, Files.newInputStream(file), file.toString());
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @NotNull
    private File spoolDirectory() throws IOException {
        return Files.createDirectories(this.directory.resolve("spooled")).toFile();
    }

    /**
     * Waits until the stream thread published a frame for the render thread.
     */
    private void awaitPendingFrame() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(ReadGate.TIMEOUT_SECONDS);
        while (this.texture.pendingUploadFrame.get() == null) {
            if (this.texture.isLoadingFailed()) fail("The texture failed instead of playing");
            if (System.nanoTime() > deadline) fail("The stream thread didn't publish a frame");
            Thread.sleep(1L);
        }
    }

    /**
     * The real texture, reading its archive with a gated decoder. Frames only get counted instead of uploaded, so playback never touches Minecraft.
     */
    private static final class FileTexture extends FmaTexture {

        @NotNull
        private final File spoolDirectory;
        @Nullable
        private final ReadGate loadingGate;
        @Nullable
        private volatile GatedFmaDecoder createdDecoder;

        private FileTexture(@NotNull File spoolDirectory, @Nullable ReadGate loadingGate) {
            this.spoolDirectory = spoolDirectory;
            this.loadingGate = loadingGate;
        }

        @Override
        protected @NotNull FmaDecoder createDecoder() {
            GatedFmaDecoder decoder = new GatedFmaDecoder(this.spoolDirectory, this.loadingGate);
            this.createdDecoder = decoder;
            return decoder;
        }

        @Override
        protected void uploadPendingFrameToTexture() {
        }

        @NotNull
        private GatedFmaDecoder requireDecoder() {
            GatedFmaDecoder decoder = this.createdDecoder;
            assertNotNull(decoder, "The texture didn't create a decoder");
            return decoder;
        }

    }

}
