package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import de.keksuccino.fancymenu.util.resource.resources.texture.ReadGate;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads real AFMA files and checks that the archive file gets released right away, also while another thread reads from it.
 */
class AfmaDecoderArchiveFileTest {

    private static final int FRAMES = 3;

    @TempDir
    Path directory;

    @Test
    void containerPayloadsGetReadInPiecesThatHoldTheFileLockShortly() throws IOException {
        Path file = AfmaTestFiles.writeContainer(this.directory.resolve("noise.afma"), FRAMES);
        try (GatedAfmaDecoder decoder = new GatedAfmaDecoder()) {
            decoder.read(file.toFile());
            decoder.clearPayloadCache();

            for (int frame = 0; frame < FRAMES; frame++) {
                assertArrayEquals(AfmaTestFiles.framePayload(frame), payload(decoder, frame), "frame " + frame);
            }
            // Noise doesn't compress, so the chunk is bigger than a single piece
            assertTrue(decoder.payloadChunkDescriptors.get(0).compressedLength() > AfmaDecoder.MAX_LOCKED_READ_BYTES);
            assertTrue(decoder.requireOpenedFile().largestRead() <= AfmaDecoder.MAX_LOCKED_READ_BYTES);
        }
    }

    @Test
    void legacyZipArchivesStillReadAllPayloads() throws IOException {
        Path file = AfmaTestFiles.writeLegacyZip(this.directory.resolve("legacy.afma"), FRAMES);
        try (GatedAfmaDecoder decoder = new GatedAfmaDecoder()) {
            decoder.read(file.toFile());
            decoder.clearPayloadCache();

            assertTrue(decoder.legacyZipArchive);
            for (int frame = 0; frame < FRAMES; frame++) {
                assertArrayEquals(AfmaTestFiles.framePayload(frame), payload(decoder, frame), "frame " + frame);
            }
        }
    }

    @Test
    void closingTheArchiveFileWaitsForTheReadOfAnotherThreadAndMakesLaterReadsFail() throws Exception {
        Path file = AfmaTestFiles.writeContainer(this.directory.resolve("noise.afma"), FRAMES);
        try (GatedAfmaDecoder decoder = new GatedAfmaDecoder()) {
            decoder.read(file.toFile());
            decoder.clearPayloadCache();
            GatedAfmaDecoder.GatedFile archiveFile = decoder.requireOpenedFile();
            ReadGate gate = new ReadGate();
            archiveFile.readGate = gate;
            AtomicReference<Object> readResult = new AtomicReference<>();
            Thread reader = new Thread(() -> {
                try {
                    readResult.set(payload(decoder, 1));
                } catch (Throwable throwable) {
                    readResult.set(throwable);
                }
            }, "AFMA test reader");
            reader.start();
            gate.awaitReached();

            Thread closer = new Thread(decoder::closeArchiveFile, "AFMA test closer");
            closer.start();
            ReadGate.awaitBlocked(closer);
            // The read holds the file lock, so the file stays open until the read is done
            assertTrue(archiveFile.isOpen());
            gate.open();
            ReadGate.join(closer);

            assertFalse(archiveFile.isOpen());
            ReadGate.join(reader);
            assertFalse(archiveFile.wasReadAfterClose());
            // The payload needs several pieces, the closer can get the lock between two of them, so the read either completed or failed, but never read a closed file
            Object result = readResult.get();
            if (result instanceof byte[] bytes) {
                assertArrayEquals(AfmaTestFiles.framePayload(1), bytes);
            } else {
                assertInstanceOf(IOException.class, result);
            }
            assertThrows(IOException.class, () -> payload(decoder, 2));
        }
    }

    @Test
    void closingTheArchiveFileOfALegacyZipArchiveMakesLaterReadsFail() throws IOException {
        Path file = AfmaTestFiles.writeLegacyZip(this.directory.resolve("legacy.afma"), FRAMES);
        try (GatedAfmaDecoder decoder = new GatedAfmaDecoder()) {
            decoder.read(file.toFile());
            decoder.clearPayloadCache();

            decoder.closeArchiveFile();

            assertNull(decoder.zipFile);
            assertThrows(IOException.class, () -> payload(decoder, 0));
        }
    }

    @Test
    void decoderWhoseFileGotReleasedNeverOpensIt() throws IOException {
        Path file = AfmaTestFiles.writeContainer(this.directory.resolve("noise.afma"), 1);
        try (GatedAfmaDecoder decoder = new GatedAfmaDecoder()) {
            decoder.closeArchiveFile();

            assertThrows(IOException.class, () -> decoder.read(file.toFile()));
            assertNull(decoder.openedFile());
        }
    }

    @Test
    void closingTheDecoderReleasesTheArchiveFile() throws IOException {
        Path file = AfmaTestFiles.writeContainer(this.directory.resolve("noise.afma"), 1);
        GatedAfmaDecoder decoder = new GatedAfmaDecoder();
        decoder.read(file.toFile());
        GatedAfmaDecoder.GatedFile archiveFile = decoder.requireOpenedFile();
        assertTrue(archiveFile.isOpen());

        decoder.close();

        assertFalse(archiveFile.isOpen());
    }

    @NotNull
    private static byte[] payload(@NotNull AfmaDecoder decoder, int frame) throws IOException {
        AfmaFrameDescriptor descriptor = decoder.getFrame(frame);
        AfmaDecoder.PayloadBytes payload = decoder.readPayloadBytes(descriptor.getPrimaryPayloadPath());
        return Arrays.copyOfRange(payload.bytes(), payload.offset(), payload.offset() + payload.length());
    }

}
