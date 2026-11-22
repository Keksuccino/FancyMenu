package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import de.keksuccino.fancymenu.util.file.ReleasableZipFile;
import de.keksuccino.fancymenu.util.resource.resources.texture.GatedZipChannel;
import de.keksuccino.fancymenu.util.resource.resources.texture.ReadGate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads real FMA files and checks that the archive file gets released right away, also while another thread reads from it.
 */
class FmaDecoderArchiveFileTest {

    private static final int FRAMES = 3;

    @TempDir
    Path directory;

    @Test
    void framesGetReadInPiecesThatHoldTheFileLockShortly() throws IOException {
        Path file = FmaTestFiles.write(this.directory.resolve("noise.fma"), FRAMES);
        try (GatedFmaDecoder decoder = this.decoder()) {
            decoder.read(file.toFile());
            GatedZipChannel archive = decoder.requireOpenedChannel();
            archive.readLengths.clear();

            for (int frame = 0; frame < FRAMES; frame++) {
                assertArrayEquals(FmaTestFiles.framePng(frame), decoder.readFrame(frame), "frame " + frame);
            }
            assertNull(decoder.readFrame(FRAMES));
            // Noise doesn't compress, so a frame is bigger than a single piece
            assertTrue(FmaTestFiles.framePng(0).length > ReleasableZipFile.MAX_LOCKED_READ_BYTES);
            assertTrue(archive.largestRead() <= ReleasableZipFile.MAX_LOCKED_READ_BYTES);
        }
    }

    @Test
    void closingTheArchiveFileWaitsForTheReadOfAnotherThreadAndMakesLaterReadsFail() throws Exception {
        Path file = FmaTestFiles.write(this.directory.resolve("noise.fma"), FRAMES);
        try (GatedFmaDecoder decoder = this.decoder()) {
            decoder.read(file.toFile());
            GatedZipChannel archive = decoder.requireOpenedChannel();
            ReadGate gate = new ReadGate();
            archive.readGate = gate;
            AtomicReference<Object> readResult = new AtomicReference<>();
            Thread reader = new Thread(() -> {
                try {
                    readResult.set(decoder.readFrame(1));
                } catch (Throwable throwable) {
                    readResult.set(throwable);
                }
            }, "FMA test reader");
            reader.start();
            gate.awaitReached();

            Thread closer = new Thread(decoder::closeArchiveFile, "FMA test closer");
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
            ReadGate.join(reader);
            assertFalse(archive.wasReadAfterClose());
            // The frame needs several pieces, the closer can get the lock between two of them, so the read either completed or failed, but never read a closed archive
            Object result = readResult.get();
            if (result instanceof byte[] bytes) {
                assertArrayEquals(FmaTestFiles.framePng(1), bytes);
            } else {
                assertInstanceOf(IOException.class, result);
            }
            assertThrows(IOException.class, () -> decoder.readFrame(2));
        }
    }

    @Test
    void releasingTheArchiveFileDeletesTheSpooledCopyOfAStream() throws IOException {
        Path file = FmaTestFiles.write(this.directory.resolve("noise.fma"), 1);
        File spoolDirectory = Files.createDirectories(this.directory.resolve("spooled")).toFile();
        try (GatedFmaDecoder decoder = new GatedFmaDecoder(spoolDirectory)) {
            decoder.read(Files.newInputStream(file));
            assertEquals(1, this.spooledFiles(spoolDirectory));

            decoder.closeArchiveFile();

            assertFalse(decoder.requireOpenedChannel().isOpen());
            assertEquals(0, this.spooledFiles(spoolDirectory));
            assertThrows(IOException.class, () -> decoder.readFrame(0));
        }
    }

    @Test
    void decoderWhoseFileGotReleasedNeverOpensIt() throws IOException {
        Path file = FmaTestFiles.write(this.directory.resolve("noise.fma"), 1);
        try (GatedFmaDecoder decoder = this.decoder()) {
            decoder.closeArchiveFile();

            assertThrows(IOException.class, () -> decoder.read(file.toFile()));
            assertNull(decoder.openedChannel());
        }
    }

    @Test
    void closingTheDecoderReleasesTheArchiveFile() throws IOException {
        Path file = FmaTestFiles.write(this.directory.resolve("noise.fma"), 1);
        GatedFmaDecoder decoder = this.decoder();
        decoder.read(file.toFile());
        GatedZipChannel archive = decoder.requireOpenedChannel();
        assertTrue(archive.isOpen());

        decoder.close();

        assertFalse(archive.isOpen());
    }

    private GatedFmaDecoder decoder() {
        return new GatedFmaDecoder(this.directory.toFile());
    }

    private int spooledFiles(File spoolDirectory) {
        String[] files = spoolDirectory.list();
        return (files != null) ? files.length : 0;
    }

}
