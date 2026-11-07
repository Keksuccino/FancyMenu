package de.keksuccino.fancymenu.customization.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ProfileSwitcherTest {

    @TempDir
    Path tempDir;

    @Test
    void switchSwapsTheActiveProfileWithTheTarget() throws Exception {
        ProfileStorage storage = new ProfileStorage(this.tempDir);
        this.createProfile(storage.getRootDirectory(), "alpha", "alpha options");
        this.createProfile(storage.getStoredProfileDirectory("beta"), "beta", "beta options");

        new ProfileSwitcher(storage).switchTo("alpha", "beta", true);

        assertEquals("beta options", Files.readString(this.tempDir.resolve("options.txt")));
        assertEquals("beta", storage.readMetadata(this.tempDir, "x").getId());
        assertEquals("alpha options", Files.readString(storage.getStoredProfileDirectory("alpha").resolve("options.txt")));
        assertFalse(Files.exists(storage.getStoredProfileDirectory("beta")));
        assertFalse(Files.exists(this.journal(storage)));
        assertEquals(1, storage.listStoredProfileDirectories().size());
    }

    @Test
    void switchingBackRestoresTheFirstProfileWithItsChanges() throws Exception {
        ProfileStorage storage = new ProfileStorage(this.tempDir);
        ProfileSwitcher switcher = new ProfileSwitcher(storage);
        this.createProfile(storage.getRootDirectory(), "alpha", "alpha options");
        this.createProfile(storage.getStoredProfileDirectory("beta"), "beta", "beta options");

        switcher.switchTo("alpha", "beta", true);
        Files.writeString(this.tempDir.resolve("options.txt"), "beta edited");
        switcher.switchTo("beta", "alpha", true);

        assertEquals("alpha options", Files.readString(this.tempDir.resolve("options.txt")));
        assertEquals("beta edited", Files.readString(storage.getStoredProfileDirectory("beta").resolve("options.txt")));
    }

    @Test
    void switchWithoutKeepingDeletesTheActiveProfile() throws Exception {
        ProfileStorage storage = new ProfileStorage(this.tempDir);
        this.createProfile(storage.getRootDirectory(), "alpha", "old");
        this.createProfile(storage.getStoredProfileDirectory("import_123456"), "alpha", "imported");

        new ProfileSwitcher(storage).switchTo("alpha", "import_123456", false);

        assertEquals("imported", Files.readString(this.tempDir.resolve("options.txt")));
        assertFalse(Files.exists(storage.getStoredProfileDirectory("alpha")));
        assertTrue(storage.listStoredProfileDirectories().isEmpty());
    }

    @Test
    void switchRefusesToOverwriteAStoredProfile() throws Exception {
        ProfileStorage storage = new ProfileStorage(this.tempDir);
        this.createProfile(storage.getRootDirectory(), "alpha", "active");
        this.createProfile(storage.getStoredProfileDirectory("alpha"), "alpha", "stored");
        this.createProfile(storage.getStoredProfileDirectory("beta"), "beta", "beta");

        assertThrows(Exception.class, () -> new ProfileSwitcher(storage).switchTo("alpha", "beta", true));

        assertEquals("active", Files.readString(this.tempDir.resolve("options.txt")));
        assertEquals("stored", Files.readString(storage.getStoredProfileDirectory("alpha").resolve("options.txt")));
    }

    @Test
    void failedUnpackRollsEverythingBack() throws Exception {
        ProfileStorage storage = new ProfileStorage(this.tempDir);
        this.createProfile(storage.getRootDirectory(), "alpha", "alpha options");
        Path beta = storage.getStoredProfileDirectory("beta");
        this.createProfile(beta, "beta", "beta options");
        // Entries of a directory without write permission can't get moved out of it
        assumeTrue(beta.toFile().setWritable(false), "Can't change directory permissions on this system");
        try {
            assertThrows(Exception.class, () -> new ProfileSwitcher(storage).switchTo("alpha", "beta", true));
        } finally {
            beta.toFile().setWritable(true);
        }

        assertEquals("alpha options", Files.readString(this.tempDir.resolve("options.txt")));
        assertEquals("alpha", storage.readMetadata(this.tempDir, "x").getId());
        assertEquals("beta options", Files.readString(beta.resolve("options.txt")));
        assertFalse(Files.exists(this.journal(storage)));
        assertFalse(Files.exists(new ProfileSwitcher(storage).getStashDirectory("alpha")));
    }

    @Test
    void recoveryRollsBackAnInterruptedStash() throws Exception {
        ProfileStorage storage = new ProfileStorage(this.tempDir);
        ProfileSwitcher switcher = new ProfileSwitcher(storage);
        this.createProfile(storage.getRootDirectory(), "alpha", "alpha options");
        this.createProfile(storage.getStoredProfileDirectory("beta"), "beta", "beta options");
        // Simulates a crash after the options file got stashed, but before the metadata got stashed
        Path stash = switcher.getStashDirectory("alpha");
        Files.createDirectories(stash);
        Files.move(this.tempDir.resolve("options.txt"), stash.resolve("options.txt"));
        this.writeJournal(storage, "alpha", "beta", "STASH");

        assertTrue(switcher.recoverInterruptedSwitch());

        assertEquals("alpha options", Files.readString(this.tempDir.resolve("options.txt")));
        assertEquals("alpha", storage.readMetadata(this.tempDir, "x").getId());
        assertFalse(Files.exists(stash));
        assertFalse(Files.exists(this.journal(storage)));
        assertTrue(Files.isDirectory(storage.getStoredProfileDirectory("beta")));
    }

    @Test
    void recoveryCompletesAnInterruptedUnpack() throws Exception {
        ProfileStorage storage = new ProfileStorage(this.tempDir);
        ProfileSwitcher switcher = new ProfileSwitcher(storage);
        Path stash = switcher.getStashDirectory("alpha");
        this.createProfile(stash, "alpha", "alpha options");
        Path beta = storage.getStoredProfileDirectory("beta");
        this.createProfile(beta, "beta", "beta options");
        // Simulates a crash after the metadata of beta got unpacked, plus a late write of alpha
        Files.move(beta.resolve(ProfileStorage.METADATA_FILE_NAME), this.tempDir.resolve(ProfileStorage.METADATA_FILE_NAME));
        Files.writeString(this.tempDir.resolve("options.txt"), "late alpha write");
        this.writeJournal(storage, "alpha", "beta", "UNPACK");

        assertTrue(switcher.recoverInterruptedSwitch());

        assertEquals("beta options", Files.readString(this.tempDir.resolve("options.txt")));
        assertEquals("beta", storage.readMetadata(this.tempDir, "x").getId());
        assertEquals("late alpha write", Files.readString(storage.getStoredProfileDirectory("alpha").resolve("options.txt")));
        assertFalse(Files.exists(beta));
        assertFalse(Files.exists(stash));
        assertFalse(Files.exists(this.journal(storage)));
    }

    @Test
    void recoveryDoesNothingWithoutJournal() throws Exception {
        ProfileStorage storage = new ProfileStorage(this.tempDir);
        this.createProfile(storage.getRootDirectory(), "alpha", "alpha options");

        assertFalse(new ProfileSwitcher(storage).recoverInterruptedSwitch());
        assertEquals("alpha options", Files.readString(this.tempDir.resolve("options.txt")));
    }

    private void createProfile(Path directory, String id, String options) throws Exception {
        Files.createDirectories(directory.resolve("customization"));
        Files.writeString(directory.resolve("customization").resolve("layout.txt"), id + " layout");
        Files.writeString(directory.resolve("options.txt"), options);
        new ProfileStorage(this.tempDir).writeMetadata(directory, new ProfileMetadata(id));
    }

    private void writeJournal(ProfileStorage storage, String from, String to, String phase) throws Exception {
        Files.createDirectories(storage.getStoreDirectory());
        Files.writeString(this.journal(storage), "{\"from\":\"" + from + "\",\"to\":\"" + to + "\",\"keep_active\":true,\"phase\":\"" + phase + "\"}", StandardCharsets.UTF_8);
    }

    private Path journal(ProfileStorage storage) {
        return storage.getStoreDirectory().resolve(ProfileSwitcher.JOURNAL_FILE_NAME);
    }

}
