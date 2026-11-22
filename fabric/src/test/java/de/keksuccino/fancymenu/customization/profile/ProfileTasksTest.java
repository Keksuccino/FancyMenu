package de.keksuccino.fancymenu.customization.profile;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static de.keksuccino.fancymenu.customization.profile.ProfileTestSetup.*;
import static org.junit.jupiter.api.Assertions.*;

class ProfileTasksTest {

    @TempDir
    Path tempDir;
    private ProfileStorage storage;
    private Path alpha;
    private Path imported;

    @BeforeEach
    void setUp() throws Exception {
        this.storage = new ProfileStorage(this.tempDir);
        this.alpha = this.storage.getStoredProfileDirectory("alpha");
        this.imported = this.storage.getStoreDirectory().resolve(".tmp_import_12345678");
        createProfile(this.storage, this.alpha, "alpha", "old version");
        createProfile(this.storage, this.imported, "alpha", "imported version");
    }

    @AfterEach
    void tearDown() {
        ProfileFileFaults.uninstall();
    }

    @Test
    void replacingAStoredProfileLeavesOnlyTheImportedOne() throws Exception {
        ProfileTasks.replaceStoredProfile(this.storage, "alpha", this.imported);

        assertEquals("imported version", read(this.alpha.resolve("options.txt")));
        assertEquals(List.of(this.alpha), ProfileFiles.listEntries(this.storage.getStoreDirectory()));
    }

    @Test
    void replacingRestoresTheOldProfileWhenTheImportedOneCantTakeItsPlace() throws Exception {
        ProfileFileFaults.install().failMoves((source, target) -> source.equals(this.imported), -1);

        assertThrows(Exception.class, () -> ProfileTasks.replaceStoredProfile(this.storage, "alpha", this.imported));

        assertEquals("old version", read(this.alpha.resolve("options.txt")));
        assertTrue(Files.isDirectory(this.imported));
        assertEquals(2, ProfileFiles.listEntries(this.storage.getStoreDirectory()).size());
    }

    @Test
    void interruptedReplaceGetsRestoredInsteadOfDeleted() throws Exception {
        // The imported profile couldn't take the place of the old one, and moving the old one back failed too, like in a crash in between
        ProfileFileFaults.install().failMoves((source, target) -> source.equals(this.imported) || target.equals(this.alpha), -1);
        assertThrows(Exception.class, () -> ProfileTasks.replaceStoredProfile(this.storage, "alpha", this.imported));
        assertFalse(Files.exists(this.alpha));
        ProfileFileFaults.uninstall();

        ProfileTasks.cleanUpLeftovers(this.storage);

        assertEquals("old version", read(this.alpha.resolve("options.txt")));
        assertEquals(List.of(this.alpha), ProfileFiles.listEntries(this.storage.getStoreDirectory()));
    }

    @Test
    void backupOfAReplacedProfileGetsDeletedOnceTheImportTookItsPlace() throws Exception {
        Path backup = this.storage.getStoreDirectory().resolve(".replaced_alpha_1234abcd");
        createProfile(this.storage, backup, "alpha", "old version");
        ProfileFiles.deleteRecursively(this.imported);

        ProfileTasks.cleanUpLeftovers(this.storage);

        assertFalse(Files.exists(backup));
        assertEquals("old version", read(this.alpha.resolve("options.txt")));
    }

    @Test
    void cleanupDeletesWorkingDirectories() throws Exception {
        Path deleted = Files.createDirectories(this.storage.getStoreDirectory().resolve(".delete_beta_1234abcd"));

        ProfileTasks.cleanUpLeftovers(this.storage);

        assertFalse(Files.exists(this.imported));
        assertFalse(Files.exists(deleted));
        assertTrue(isCompleteProfile(this.storage, this.alpha, "alpha", "old version"));
    }

}
