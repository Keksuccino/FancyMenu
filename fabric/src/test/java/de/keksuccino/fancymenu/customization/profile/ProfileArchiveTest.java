package de.keksuccino.fancymenu.customization.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ProfileArchiveTest {

    @TempDir
    Path tempDir;

    @Test
    void writtenArchiveRestoresTheProfileWithoutTheProfileStore() throws Exception {
        Path root = this.tempDir.resolve("config");
        ProfileStorage storage = new ProfileStorage(root);
        ProfileMetadata metadata = new ProfileMetadata("my_setup");
        metadata.setTitle("My Setup");
        storage.writeMetadata(root, metadata);
        Files.createDirectories(root.resolve("customization"));
        Files.writeString(root.resolve("customization").resolve("layout.txt"), "layout content");
        Files.createDirectories(root.resolve("assets").resolve("empty_folder"));
        Files.write(root.resolve("assets").resolve("image.png"), new byte[200_000]);
        Files.createDirectories(storage.getStoreDirectory().resolve("other"));
        Files.writeString(storage.getStoreDirectory().resolve("other").resolve("secret.txt"), "other profile");
        Path archive = this.tempDir.resolve("export").resolve("my_setup.fmp");
        ProfileTaskProgress exportProgress = new ProfileTaskProgress();

        ProfileArchive.write(root, storage.getContentFilter(root), archive, exportProgress);

        assertTrue(Files.isRegularFile(archive));
        assertFalse(Files.exists(archive.resolveSibling("my_setup.fmp.part")));
        assertEquals(exportProgress.getTotalBytes(), exportProgress.getProcessedBytes());
        assertEquals(1.0F, exportProgress.getProgress());
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            assertNull(zip.getEntry("profiles/other/secret.txt"));
            assertEquals(ProfileArchive.ARCHIVE_COMMENT, zip.getComment());
        }
        assertEquals("My Setup", ProfileArchive.readMetadata(archive, "fallback").getTitle());

        Path target = this.tempDir.resolve("extracted");
        ProfileArchive.extract(archive, target, new ProfileTaskProgress());

        assertEquals("layout content", Files.readString(target.resolve("customization").resolve("layout.txt")));
        assertEquals(200_000, Files.size(target.resolve("assets").resolve("image.png")));
        assertTrue(Files.isDirectory(target.resolve("assets").resolve("empty_folder")));
        assertFalse(Files.exists(target.resolve(ProfileStorage.STORE_DIRECTORY_NAME)));
    }

    @Test
    void entriesOutsideOfTheProfileGetRejected() throws Exception {
        Path archive = this.writeZip("evil.fmp", "profile.json", "{}", "../escaped.txt", "evil");

        assertThrows(ProfileArchive.InvalidProfileArchiveException.class, () -> ProfileArchive.extract(archive, this.tempDir.resolve("target"), new ProfileTaskProgress()));
        assertFalse(Files.exists(this.tempDir.resolve("escaped.txt")));
    }

    @Test
    void absoluteEntriesGetRejected() {
        Path target = this.tempDir.resolve("target").toAbsolutePath().normalize();
        assertThrows(ProfileArchive.InvalidProfileArchiveException.class, () -> ProfileArchive.resolveEntryPath(target, "/etc/passwd"));
        assertThrows(ProfileArchive.InvalidProfileArchiveException.class, () -> ProfileArchive.resolveEntryPath(target, "C:\\Windows\\evil.dll"));
        assertThrows(ProfileArchive.InvalidProfileArchiveException.class, () -> ProfileArchive.resolveEntryPath(target, "a/../../evil"));
    }

    @Test
    void storeEntriesInsideArchivesGetSkipped() throws Exception {
        Path archive = this.writeZip("nested.fmp", "profile.json", "{}", "profiles/nested/options.txt", "nested", "options.txt", "own");
        Path target = this.tempDir.resolve("target");

        ProfileArchive.extract(archive, target, new ProfileTaskProgress());

        assertEquals("own", Files.readString(target.resolve("options.txt")));
        assertFalse(Files.exists(target.resolve(ProfileStorage.STORE_DIRECTORY_NAME)));
    }

    @Test
    void filesWithoutMetadataAreNoProfiles() throws Exception {
        Path noMetadata = this.writeZip("plain.fmp", "options.txt", "x");
        Path noZip = this.tempDir.resolve("text.fmp");
        Files.writeString(noZip, "this is no zip file");

        assertThrows(ProfileArchive.InvalidProfileArchiveException.class, () -> ProfileArchive.readMetadata(noMetadata, "fallback"));
        assertThrows(ProfileArchive.InvalidProfileArchiveException.class, () -> ProfileArchive.extract(noMetadata, this.tempDir.resolve("target"), new ProfileTaskProgress()));
        assertThrows(ProfileArchive.InvalidProfileArchiveException.class, () -> ProfileArchive.readMetadata(noZip, "fallback"));
    }

    @Test
    void cancelledExportLeavesNothingBehind() throws Exception {
        Path root = this.tempDir.resolve("config");
        Files.createDirectories(root);
        Files.write(root.resolve("big.bin"), new byte[1_000_000]);
        Path archive = this.tempDir.resolve("cancelled.fmp");
        ProfileTaskProgress progress = new ProfileTaskProgress();
        progress.requestCancel();

        assertThrows(CancellationException.class, () -> ProfileArchive.write(root, path -> true, archive, progress));
        assertFalse(Files.exists(archive));
        assertFalse(Files.exists(this.tempDir.resolve("cancelled.fmp.part")));
    }

    @Test
    void archiveExtensionGetsDetected() {
        assertTrue(ProfileArchive.hasArchiveExtension(Path.of("Setup.FMP")));
        assertFalse(ProfileArchive.hasArchiveExtension(Path.of("setup.zip")));
    }

    private Path writeZip(String name, String... entriesAndContents) throws Exception {
        Path file = this.tempDir.resolve(name);
        try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
            for (int i = 0; i < entriesAndContents.length; i += 2) {
                zip.putNextEntry(new ZipEntry(entriesAndContents[i]));
                zip.write(entriesAndContents[i + 1].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return file;
    }

}
