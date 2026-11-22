package de.keksuccino.fancymenu.util.file;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReleasableZipFileTest {

    @TempDir
    Path directory;

    @Test
    void readsEntriesOfEverySizeCompletely() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("empty.bin", new byte[0]);
        entries.put("small.bin", randomBytes(100, 1));
        entries.put("one_piece.bin", randomBytes(ReleasableZipFile.MAX_LOCKED_READ_BYTES, 2));
        entries.put("pieces.bin", randomBytes(3 * ReleasableZipFile.MAX_LOCKED_READ_BYTES + 7, 3));
        Path file = this.writeZip(entries, true);
        Path storedFile = this.writeZip(entries, false);

        for (Path zip : new Path[] {file, storedFile}) {
            try (ReleasableZipFile zipFile = open(zip)) {
                for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                    assertArrayEquals(entry.getValue(), zipFile.readEntry(find(zipFile, entry.getKey())), entry.getKey() + " of " + zip.getFileName());
                }
            }
        }
    }

    @Test
    void readsTheStartOfEntries() throws IOException {
        byte[] bytes = randomBytes(2 * ReleasableZipFile.MAX_LOCKED_READ_BYTES, 4);
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("long.bin", bytes);
        entries.put("short.bin", Arrays.copyOf(bytes, 10));
        try (ReleasableZipFile zipFile = open(this.writeZip(entries, true))) {
            byte[] start = new byte[33];
            assertEquals(33, zipFile.readEntryStart(find(zipFile, "long.bin"), start));
            assertArrayEquals(Arrays.copyOf(bytes, 33), start);

            byte[] shortStart = new byte[33];
            assertEquals(10, zipFile.readEntryStart(find(zipFile, "short.bin"), shortStart));
            assertArrayEquals(Arrays.copyOf(bytes, 10), Arrays.copyOf(shortStart, 10));
        }
    }

    @Test
    void allReadsFailAfterClosing() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("frame.bin", randomBytes(100, 5));
        ReleasableZipFile zipFile = open(this.writeZip(entries, true));
        ZipArchiveEntry entry = find(zipFile, "frame.bin");

        zipFile.close();
        zipFile.close();

        assertThrows(IOException.class, () -> zipFile.readEntry(entry));
        assertThrows(IOException.class, () -> zipFile.readEntryStart(entry, new byte[4]));
        assertThrows(IOException.class, zipFile::getEntries);
    }

    @SuppressWarnings("deprecation")
    @NotNull
    private static ReleasableZipFile open(@NotNull Path file) throws IOException {
        return new ReleasableZipFile(new ZipFile(file.toFile()));
    }

    @NotNull
    private static ZipArchiveEntry find(@NotNull ReleasableZipFile zipFile, @NotNull String name) throws IOException {
        var entries = zipFile.getEntries();
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            if (entry.getName().equals(name)) return entry;
        }
        throw new IllegalStateException("No entry " + name);
    }

    @NotNull
    private Path writeZip(@NotNull Map<String, byte[]> entries, boolean deflated) throws IOException {
        Path file = this.directory.resolve(deflated ? "deflated.zip" : "stored.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(file))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                if (!deflated) {
                    CRC32 crc = new CRC32();
                    crc.update(entry.getValue());
                    zipEntry.setMethod(ZipEntry.STORED);
                    zipEntry.setSize(entry.getValue().length);
                    zipEntry.setCrc(crc.getValue());
                }
                out.putNextEntry(zipEntry);
                out.write(entry.getValue());
                out.closeEntry();
            }
        }
        return file;
    }

    @NotNull
    private static byte[] randomBytes(int length, long seed) {
        byte[] bytes = new byte[length];
        new Random(seed).nextBytes(bytes);
        return bytes;
    }

}
