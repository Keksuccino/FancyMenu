package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import de.keksuccino.fancymenu.util.resource.resources.texture.GatedZipChannel;
import de.keksuccino.fancymenu.util.resource.resources.texture.LogCapture;
import de.keksuccino.fancymenu.util.resource.resources.texture.ReadGate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
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
 * Loads real AFMA files into real textures with their real loading and stream threads, and checks that closing a texture releases its file right away,
 * like a profile switch needs it before it moves the files of the profile.
 */
class AfmaTextureFileReleaseTest {

    private static final int FRAMES = 3;

    @TempDir
    Path directory;
    private final FileTexture texture = new FileTexture();

    @AfterEach
    void closeTexture() throws InterruptedException {
        Thread stream = this.texture.streamThread;
        this.texture.close();
        if (stream != null) ReadGate.join(stream);
    }

    @Test
    void closeReleasesTheFileWhileTheStreamThreadStillRuns() throws Exception {
        try (LogCapture problems = LogCapture.problemsOf(AfmaTexture.class, AfmaDecoder.class)) {
            this.load();
            GatedAfmaDecoder.GatedFile archiveFile = this.texture.requireDecoder().requireOpenedFile();
            this.texture.startTickerIfNeeded();
            Thread stream = this.texture.streamThread;
            assertNotNull(stream);
            this.awaitPrefetchedFrames();

            this.texture.close();

            assertFalse(archiveFile.isOpen());
            ReadGate.join(stream);
            // The stream thread released the rest of the decoder once it stopped
            assertNull(this.texture.decoder);
            assertFalse(this.texture.isLoadingFailed());
            assertEquals(List.of(), problems.messages());
        }
    }

    @Test
    void closeWaitsForAReadOfTheStreamThreadAndReleasesTheFileRightAfterIt() throws Exception {
        try (LogCapture problems = LogCapture.problemsOf(AfmaTexture.class, AfmaDecoder.class)) {
            this.load();
            GatedAfmaDecoder decoder = this.texture.requireDecoder();
            GatedAfmaDecoder.GatedFile archiveFile = decoder.requireOpenedFile();
            // Loading cached the chunk of the first frame, so the stream thread has to read it from the file again
            decoder.clearPayloadCache();
            ReadGate gate = new ReadGate();
            archiveFile.readGate = gate;
            this.texture.startTickerIfNeeded();
            Thread stream = this.texture.streamThread;
            assertNotNull(stream);
            gate.awaitReached();

            Thread closer = new Thread(this.texture::close, "AFMA test closer");
            closer.start();
            ReadGate.awaitBlocked(closer);
            assertTrue(archiveFile.isOpen());
            gate.open();
            ReadGate.join(closer);

            assertFalse(archiveFile.isOpen());
            ReadGate.join(stream);
            assertFalse(archiveFile.wasReadAfterClose());
            assertNull(this.texture.decoder);
            assertFalse(this.texture.isLoadingFailed());
            assertEquals(List.of(), problems.messages());
        }
    }

    @Test
    void resetWhileTheStreamThreadReadsALegacyArchiveKeepsTheArchiveReadable() throws Exception {
        try (LogCapture problems = LogCapture.problemsOf(AfmaTexture.class, AfmaDecoder.class)) {
            Path file = AfmaTestFiles.writeLegacyZip(this.directory.resolve("legacy.afma"), FRAMES);
            AfmaTexture.populateTexture(this.texture, file.toFile(), file.toString());
            assertTrue(this.texture.isLoadingCompleted());
            GatedAfmaDecoder decoder = this.texture.requireDecoder();
            GatedZipChannel archive = decoder.requireOpenedZipChannel();
            // Loading cached the chunk of the first frame, so the stream thread has to read it from the archive again
            decoder.clearPayloadCache();
            ReadGate gate = new ReadGate();
            archive.readGate = gate;
            this.texture.startTickerIfNeeded();
            Thread stream = this.texture.streamThread;
            assertNotNull(stream);
            gate.awaitReached();

            try {
                this.texture.reset();
            } finally {
                gate.open();
            }
            ReadGate.join(stream);
            // The next render starts the stream of the reset
            this.texture.startTickerIfNeeded();
            this.awaitPrefetchedFrames();

            assertTrue(archive.isOpen());
            assertFalse(this.texture.isLoadingFailed());
            assertEquals(List.of(), problems.messages());
        }
    }

    @Test
    void closeReleasesTheFileOfATextureThatIsStillLoading() throws Exception {
        try (LogCapture problems = LogCapture.problemsOf(AfmaTexture.class, AfmaDecoder.class)) {
            Path file = AfmaTestFiles.writeContainer(this.directory.resolve("noise.afma"), FRAMES);
            ReadGate gate = new ReadGate();
            this.texture.loadingGate = gate;
            Thread loader = new Thread(() -> AfmaTexture.populateTexture(this.texture, file.toFile(), file.toString()), "AFMA test loader");
            loader.start();
            gate.awaitReached();
            GatedAfmaDecoder decoder = this.texture.requireDecoder();
            GatedAfmaDecoder.GatedFile archiveFile = decoder.requireOpenedFile();
            assertTrue(archiveFile.isOpen());

            this.texture.close();

            assertFalse(archiveFile.isOpen());
            gate.open();
            ReadGate.join(loader);
            assertNull(this.texture.decoder);
            assertNull(this.texture.loadingDecoder);
            // The loading thread closed the rest of the decoder once it noticed that its file got released
            assertNull(decoder.getMetadata());
            assertTrue(this.texture.isLoadingFailed());
            assertEquals(List.of(), problems.messages());
        }
    }

    @Test
    void closedTextureNeverOpensItsFile() throws IOException {
        Path file = AfmaTestFiles.writeContainer(this.directory.resolve("noise.afma"), 1);
        this.texture.close();

        AfmaTexture.populateTexture(this.texture, file.toFile(), file.toString());

        assertNull(this.texture.createdDecoder);
        assertFalse(this.texture.isReady());
    }

    private void load() throws IOException {
        Path file = AfmaTestFiles.writeContainer(this.directory.resolve("noise.afma"), FRAMES);
        AfmaTexture.populateTexture(this.texture, file.toFile(), file.toString());
        assertTrue(this.texture.isLoadingCompleted());
    }

    /**
     * Waits until the stream thread decoded frames ahead of playback, which it reads from the file.
     */
    private void awaitPrefetchedFrames() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(ReadGate.TIMEOUT_SECONDS);
        while (true) {
            synchronized (this.texture.streamStateLock) {
                if (this.texture.prefetchedFrames.size() >= 2) return;
            }
            if (this.texture.isLoadingFailed()) fail("The texture failed instead of decoding frames");
            if (System.nanoTime() > deadline) fail("The stream thread didn't decode any frames");
            Thread.sleep(1L);
        }
    }

    /**
     * The real texture, reading its file with a decoder that can hold its loading at a gate. Nothing gets rendered, so playback never touches Minecraft.
     */
    private static final class FileTexture extends AfmaTexture {

        @Nullable
        private volatile ReadGate loadingGate;
        @Nullable
        private volatile GatedAfmaDecoder createdDecoder;

        @Override
        protected @NotNull AfmaDecoder createDecoder() {
            GatedAfmaDecoder decoder = new GatedAfmaDecoder(this.loadingGate);
            this.createdDecoder = decoder;
            return decoder;
        }

        @NotNull
        private GatedAfmaDecoder requireDecoder() {
            GatedAfmaDecoder decoder = this.createdDecoder;
            assertNotNull(decoder, "The texture didn't create a decoder");
            return decoder;
        }

    }

}
