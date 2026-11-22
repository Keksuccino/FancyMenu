package de.keksuccino.fancymenu.customization.profile;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static de.keksuccino.fancymenu.customization.profile.ProfileTestSetup.*;
import static org.junit.jupiter.api.Assertions.*;

class ProfileSwitcherTest {

    @TempDir
    Path tempDir;
    private ProfileStorage storage;
    private ProfileSwitcher switcher;
    private Path alpha;
    private Path beta;
    private Path stash;

    @BeforeEach
    void setUp() {
        this.storage = new ProfileStorage(this.tempDir);
        this.switcher = new ProfileSwitcher(this.storage);
        this.alpha = this.storage.getStoredProfileDirectory("alpha");
        this.beta = this.storage.getStoredProfileDirectory("beta");
        this.stash = this.switcher.getStashDirectory("alpha");
    }

    @AfterEach
    void tearDown() {
        ProfileFileFaults.uninstall();
    }

    @Test
    void switchSwapsTheActiveProfileWithTheTarget() throws Exception {
        this.createAlphaActiveAndBetaStored();

        assertTrue(this.switcher.switchTo("alpha", "beta", true));

        assertTrue(isCompleteProfile(this.storage, this.tempDir, "beta", "beta options"));
        assertTrue(isCompleteProfile(this.storage, this.alpha, "alpha", "alpha options"));
        assertFalse(Files.exists(this.beta));
        assertFalse(Files.exists(this.stash));
        assertFalse(this.switcher.isSwitchPending());
        assertEquals(1, this.storage.listStoredProfileDirectories().size());
    }

    @Test
    void switchingBackRestoresTheFirstProfileWithItsChanges() throws Exception {
        this.createAlphaActiveAndBetaStored();

        this.switcher.switchTo("alpha", "beta", true);
        Files.writeString(this.tempDir.resolve("options.txt"), "beta edited");
        this.switcher.switchTo("beta", "alpha", true);

        assertEquals("alpha options", read(this.tempDir.resolve("options.txt")));
        assertEquals("beta edited", read(this.beta.resolve("options.txt")));
    }

    @Test
    void switchWithoutKeepingDeletesTheActiveProfile() throws Exception {
        createProfile(this.storage, this.tempDir, "alpha", "old");
        createProfile(this.storage, this.storage.getStoredProfileDirectory("import_123456"), "alpha", "imported");

        assertTrue(this.switcher.switchTo("alpha", "import_123456", false));

        assertEquals("imported", read(this.tempDir.resolve("options.txt")));
        assertFalse(Files.exists(this.alpha));
        assertFalse(Files.exists(this.stash));
        assertTrue(this.storage.listStoredProfileDirectories().isEmpty());
    }

    @Test
    void switchRefusesToOverwriteAStoredProfile() throws Exception {
        createProfile(this.storage, this.tempDir, "alpha", "active");
        createProfile(this.storage, this.alpha, "alpha", "stored");
        createProfile(this.storage, this.beta, "beta", "beta");

        assertThrows(Exception.class, () -> this.switcher.switchTo("alpha", "beta", true));

        assertEquals("active", read(this.tempDir.resolve("options.txt")));
        assertEquals("stored", read(this.alpha.resolve("options.txt")));
    }

    @Test
    void failedUnpackRollsEverythingBack() throws Exception {
        this.createAlphaActiveAndBetaStored();
        AtomicInteger unpacked = new AtomicInteger();
        ProfileFileFaults.install().failMoves((source, target) -> source.getParent().equals(this.beta) && (unpacked.incrementAndGet() == 2), 1);

        assertThrows(Exception.class, () -> this.switcher.switchTo("alpha", "beta", true));

        assertTrue(isCompleteProfile(this.storage, this.tempDir, "alpha", "alpha options"));
        assertTrue(isCompleteProfile(this.storage, this.beta, "beta", "beta options"));
        assertFalse(this.switcher.isSwitchPending());
        assertFalse(Files.exists(this.stash));
    }

    @Test
    void failedStashGetsRolledBackAndLeavesNothingPending() throws Exception {
        this.createAlphaActiveAndBetaStored();
        AtomicInteger stashed = new AtomicInteger();
        ProfileFileFaults.install().failMoves((source, target) -> target.getParent().equals(this.stash) && (stashed.incrementAndGet() == 2), 1);

        assertThrows(Exception.class, () -> this.switcher.switchTo("alpha", "beta", true));

        assertTrue(isCompleteProfile(this.storage, this.tempDir, "alpha", "alpha options"));
        assertTrue(isCompleteProfile(this.storage, this.beta, "beta", "beta options"));
        assertFalse(this.switcher.isSwitchPending());
        assertFalse(Files.exists(this.stash));
    }

    @Test
    void failedCleanupStillCountsAsSwitchedAndCanGetFinishedLater() throws Exception {
        this.createAlphaActiveAndBetaStored();
        ProfileFileFaults faults = ProfileFileFaults.install().failMoves((source, target) -> source.equals(this.stash), -1);

        boolean finished = assertDoesNotThrow(() -> this.switcher.switchTo("alpha", "beta", true));

        assertFalse(finished);
        assertTrue(isCompleteProfile(this.storage, this.tempDir, "beta", "beta options"));
        assertTrue(isCompleteProfile(this.storage, this.stash, "alpha", "alpha options"));
        assertTrue(this.switcher.isSwitchPending());
        assertFalse(this.switcher.isActiveProfileIncomplete());
        assertThrows(Exception.class, () -> this.switcher.completePendingSwitch());

        faults.clear();
        this.switcher.completePendingSwitch();

        assertTrue(isCompleteProfile(this.storage, this.alpha, "alpha", "alpha options"));
        assertTrue(isCompleteProfile(this.storage, this.tempDir, "beta", "beta options"));
        assertFalse(Files.exists(this.stash));
        assertFalse(Files.exists(this.beta));
        assertFalse(this.switcher.isSwitchPending());
    }

    @Test
    void failedDeleteOfTheEmptiedTargetStillCountsAsSwitched() throws Exception {
        this.createAlphaActiveAndBetaStored();
        ProfileFileFaults faults = ProfileFileFaults.install().failDeletes(path -> path.equals(this.beta), -1);

        assertFalse(this.switcher.switchTo("alpha", "beta", true));
        assertTrue(isCompleteProfile(this.storage, this.tempDir, "beta", "beta options"));
        assertTrue(this.switcher.isSwitchPending());

        faults.clear();
        this.switcher.completePendingSwitch();

        assertTrue(isCompleteProfile(this.storage, this.alpha, "alpha", "alpha options"));
        assertFalse(Files.exists(this.beta));
        assertFalse(this.switcher.isSwitchPending());
    }

    @Test
    void failedFinishJournalLeavesASwitchThatGetsCompleted() throws Exception {
        this.createAlphaActiveAndBetaStored();
        Path journalTemp = journal(this.storage).resolveSibling(ProfileSwitchJournal.FILE_NAME + ".tmp");
        AtomicInteger journalWrites = new AtomicInteger();
        ProfileFileFaults faults = ProfileFileFaults.install().failMoves((source, target) -> source.equals(journalTemp) && (journalWrites.incrementAndGet() == 3), 1);

        assertThrows(Exception.class, () -> this.switcher.switchTo("alpha", "beta", true));
        assertTrue(this.switcher.isActiveProfileIncomplete());

        faults.clear();
        this.switcher.completePendingSwitch();

        assertTrue(isCompleteProfile(this.storage, this.tempDir, "beta", "beta options"));
        assertTrue(isCompleteProfile(this.storage, this.alpha, "alpha", "alpha options"));
        assertFalse(this.switcher.isSwitchPending());
    }

    @Test
    void completingTheCleanupNeverUnpacksAgain() throws Exception {
        // The state from issue #1867: the cleanup failed, and older versions wrote metadata into the emptied directory of the target profile
        createProfile(this.storage, this.tempDir, "beta", "beta options");
        createProfile(this.storage, this.stash, "alpha", "alpha options");
        ProfileMetadata recreated = new ProfileMetadata("beta");
        recreated.setTitle("beta");
        this.storage.writeMetadata(this.beta, recreated);
        writeJournal(this.storage, "alpha", "beta", "FINISH", true);

        this.switcher.completePendingSwitch();

        assertTrue(isCompleteProfile(this.storage, this.alpha, "alpha", "alpha options"));
        assertTrue(isCompleteProfile(this.storage, this.tempDir, "beta", "beta options"));
        assertFalse(Files.exists(this.beta));
        assertFalse(Files.exists(this.stash));
        assertFalse(this.switcher.isSwitchPending());
        assertEquals(List.of(this.alpha), this.storage.listStoredProfileDirectories());
    }

    @Test
    void unknownFilesInTheEmptiedTargetBecomeAProfileOfTheirOwn() throws Exception {
        createProfile(this.storage, this.tempDir, "beta", "beta options");
        createProfile(this.storage, this.stash, "alpha", "alpha options");
        Files.createDirectories(this.beta);
        Files.writeString(this.beta.resolve("notes.txt"), "keep me");
        Files.writeString(this.beta.resolve("Thumbs.db"), "junk");
        writeJournal(this.storage, "alpha", "beta", "FINISH", true);

        this.switcher.completePendingSwitch();

        assertFalse(Files.exists(this.beta));
        List<Path> stored = this.storage.listStoredProfileDirectories();
        assertEquals(2, stored.size());
        Path leftover = stored.stream().filter(path -> !path.equals(this.alpha)).findFirst().orElseThrow();
        assertEquals("keep me", read(leftover.resolve("notes.txt")));
        assertFalse(Files.exists(leftover.resolve("Thumbs.db")));
        assertTrue(isCompleteProfile(this.storage, this.alpha, "alpha", "alpha options"));
    }

    @Test
    void failedStashRollbackNeverDeletesStashedData() throws Exception {
        this.createAlphaActiveAndBetaStored();
        AtomicInteger stashed = new AtomicInteger();
        ProfileFileFaults faults = ProfileFileFaults.install()
                .failMoves((source, target) -> target.getParent().equals(this.stash) && (stashed.incrementAndGet() == 2), 1)
                .failMoves((source, target) -> source.getParent().equals(this.stash), -1);

        assertThrows(Exception.class, () -> this.switcher.switchTo("alpha", "beta", true));

        // One entry of alpha couldn't get moved back, so it has to stay in the stash
        assertEquals(1, ProfileFiles.listEntries(this.stash).size());
        assertTrue(this.switcher.isSwitchPending());
        assertTrue(this.switcher.isActiveProfileIncomplete());
        assertTrue(isCompleteProfile(this.storage, this.beta, "beta", "beta options"));

        faults.clear();
        this.switcher.completePendingSwitch();

        assertTrue(isCompleteProfile(this.storage, this.tempDir, "alpha", "alpha options"));
        assertTrue(isCompleteProfile(this.storage, this.beta, "beta", "beta options"));
        assertFalse(Files.exists(this.stash));
        assertFalse(this.switcher.isSwitchPending());
    }

    @Test
    void failedUnpackRollbackNeverDeletesStashedData() throws Exception {
        this.createAlphaActiveAndBetaStored();
        AtomicInteger unpacked = new AtomicInteger();
        ProfileFileFaults faults = ProfileFileFaults.install()
                .failMoves((source, target) -> source.getParent().equals(this.beta) && (unpacked.incrementAndGet() == 2), 1)
                .failMoves((source, target) -> target.getParent().equals(this.beta), -1);

        assertThrows(Exception.class, () -> this.switcher.switchTo("alpha", "beta", true));

        // An entry of beta is stuck in the root directory, and the same entry of alpha must survive in the stash
        assertTrue(isCompleteProfile(this.storage, this.stash, "alpha", "alpha options"));
        assertTrue(this.switcher.isActiveProfileIncomplete());

        faults.clear();
        this.switcher.completePendingSwitch();

        assertTrue(isCompleteProfile(this.storage, this.tempDir, "beta", "beta options"));
        assertTrue(isCompleteProfile(this.storage, this.alpha, "alpha", "alpha options"));
        assertFalse(Files.exists(this.stash));
        assertFalse(Files.exists(this.beta));
        assertFalse(this.switcher.isSwitchPending());
    }

    @Test
    void recoveryRollsBackAnInterruptedStash() throws Exception {
        this.createAlphaActiveAndBetaStored();
        // A crash after the options file got stashed, but before the rest got stashed
        Files.createDirectories(this.stash);
        Files.move(this.tempDir.resolve("options.txt"), this.stash.resolve("options.txt"));
        writeJournal(this.storage, "alpha", "beta", "STASH", true);

        this.switcher.completePendingSwitch();

        assertTrue(isCompleteProfile(this.storage, this.tempDir, "alpha", "alpha options"));
        assertTrue(isCompleteProfile(this.storage, this.beta, "beta", "beta options"));
        assertFalse(Files.exists(this.stash));
        assertFalse(this.switcher.isSwitchPending());
    }

    @Test
    void lateWritesMergeWithTheirStashedVersionsWhenRollingBack() throws Exception {
        this.createAlphaActiveAndBetaStored();
        Files.createDirectories(this.stash);
        Files.move(this.tempDir.resolve("options.txt"), this.stash.resolve("options.txt"));
        Files.move(this.tempDir.resolve("customization"), this.stash.resolve("customization"));
        // Written by alpha after its entries got stashed, so they are newer
        Files.writeString(this.tempDir.resolve("options.txt"), "late options");
        Files.createDirectories(this.tempDir.resolve("customization"));
        Files.writeString(this.tempDir.resolve("customization").resolve("late.txt"), "late layout");
        writeJournal(this.storage, "alpha", "beta", "STASH", true);

        this.switcher.completePendingSwitch();

        assertEquals("late options", read(this.tempDir.resolve("options.txt")));
        assertEquals("alpha layout", read(this.tempDir.resolve("customization").resolve("layout.txt")));
        assertEquals("late layout", read(this.tempDir.resolve("customization").resolve("late.txt")));
        assertFalse(Files.exists(this.stash));
    }

    @Test
    void stashedDataWinsOverDefaultsOfAnIncompleteProfileInUse() throws Exception {
        this.createAlphaActiveAndBetaStored();
        Files.createDirectories(this.stash);
        Files.move(this.tempDir.resolve("options.txt"), this.stash.resolve("options.txt"));
        Files.move(this.tempDir.resolve("customization"), this.stash.resolve("customization"));
        writeJournal(this.storage, "alpha", "beta", "STASH", true);
        // FancyMenu loads the incomplete profile, creates defaults for what is missing, and the user adds a layout
        this.switcher.markActiveProfileInUse();
        Files.writeString(this.tempDir.resolve("options.txt"), "default options");
        Files.createDirectories(this.tempDir.resolve("customization"));
        Files.writeString(this.tempDir.resolve("customization").resolve("layout.txt"), "default layout");
        Files.writeString(this.tempDir.resolve("customization").resolve("new.txt"), "new layout");

        this.switcher.completePendingSwitch();

        assertEquals("alpha options", read(this.tempDir.resolve("options.txt")));
        assertEquals("alpha layout", read(this.tempDir.resolve("customization").resolve("layout.txt")));
        assertEquals("new layout", read(this.tempDir.resolve("customization").resolve("new.txt")));
        assertFalse(Files.exists(this.stash));
        assertFalse(this.switcher.isSwitchPending());
    }

    @Test
    void recoveryCompletesAnInterruptedUnpack() throws Exception {
        createProfile(this.storage, this.stash, "alpha", "alpha options");
        createProfile(this.storage, this.beta, "beta", "beta options");
        // A crash after the metadata of beta got unpacked, plus a late write of alpha
        Files.move(this.beta.resolve(ProfileStorage.METADATA_FILE_NAME), this.tempDir.resolve(ProfileStorage.METADATA_FILE_NAME));
        Files.writeString(this.tempDir.resolve("options.txt"), "late alpha write");
        writeJournal(this.storage, "alpha", "beta", "UNPACK", true);

        this.switcher.completePendingSwitch();

        assertTrue(isCompleteProfile(this.storage, this.tempDir, "beta", "beta options"));
        assertEquals("late alpha write", read(this.alpha.resolve("options.txt")));
        assertEquals("alpha layout", read(this.alpha.resolve("customization").resolve("layout.txt")));
        assertFalse(Files.exists(this.beta));
        assertFalse(Files.exists(this.stash));
        assertFalse(this.switcher.isSwitchPending());
    }

    @Test
    void strayDirectoriesGetMergedIntoTheStashWhileUnpacking() throws Exception {
        createProfile(this.storage, this.stash, "alpha", "alpha options");
        createProfile(this.storage, this.beta, "beta", "beta options");
        // alpha wrote a new layout after its layouts got stashed
        Files.createDirectories(this.tempDir.resolve("customization"));
        Files.writeString(this.tempDir.resolve("customization").resolve("late.txt"), "late layout");
        writeJournal(this.storage, "alpha", "beta", "UNPACK", true);

        this.switcher.completePendingSwitch();

        assertTrue(isCompleteProfile(this.storage, this.tempDir, "beta", "beta options"));
        assertFalse(Files.exists(this.tempDir.resolve("customization").resolve("late.txt")));
        assertTrue(isCompleteProfile(this.storage, this.alpha, "alpha", "alpha options"));
        assertEquals("late layout", read(this.alpha.resolve("customization").resolve("late.txt")));
    }

    @Test
    void cleanupWithoutKeepingDeletesTheStash() throws Exception {
        createProfile(this.storage, this.tempDir, "beta", "beta options");
        createProfile(this.storage, this.stash, "alpha", "alpha options");
        writeJournal(this.storage, "alpha", "beta", "FINISH", false);

        this.switcher.completePendingSwitch();

        assertFalse(Files.exists(this.stash));
        assertFalse(Files.exists(this.alpha));
        assertFalse(this.switcher.isSwitchPending());
    }

    @Test
    void journalsWithoutKeepActiveKeepTheActiveProfile() throws Exception {
        createProfile(this.storage, this.tempDir, "beta", "beta options");
        createProfile(this.storage, this.stash, "alpha", "alpha options");
        Files.createDirectories(this.storage.getStoreDirectory());
        Files.writeString(journal(this.storage), "{\"from\":\"alpha\",\"to\":\"beta\",\"phase\":\"FINISH\"}");

        this.switcher.completePendingSwitch();

        assertTrue(isCompleteProfile(this.storage, this.alpha, "alpha", "alpha options"));
        assertFalse(this.switcher.isSwitchPending());
    }

    @Test
    void stashGetsANewIdIfItsIdGotTakenMeanwhile() throws Exception {
        createProfile(this.storage, this.tempDir, "beta", "beta options");
        createProfile(this.storage, this.stash, "alpha", "alpha options");
        createProfile(this.storage, this.alpha, "alpha", "other alpha");
        writeJournal(this.storage, "alpha", "beta", "FINISH", true);

        this.switcher.completePendingSwitch();

        assertFalse(this.switcher.isSwitchPending());
        assertEquals("other alpha", read(this.alpha.resolve("options.txt")));
        List<Path> stored = this.storage.listStoredProfileDirectories();
        assertEquals(2, stored.size());
        Path renamed = stored.stream().filter(path -> !path.equals(this.alpha)).findFirst().orElseThrow();
        assertEquals("alpha options", read(renamed.resolve("options.txt")));
    }

    @Test
    void brokenJournalKeepsAllStashedDataVisible() throws Exception {
        createProfile(this.storage, this.tempDir, "beta", "beta options");
        createProfile(this.storage, this.stash, "alpha", "alpha options");
        Files.writeString(journal(this.storage), "{broken");

        this.switcher.completePendingSwitch();

        assertFalse(this.switcher.isSwitchPending());
        assertFalse(Files.exists(this.stash));
        assertTrue(isCompleteProfile(this.storage, this.alpha, "alpha", "alpha options"));
    }

    @Test
    void leftoverStashesWithoutJournalBecomeStoredProfiles() throws Exception {
        // Failed switches of older versions could leave the stash of the active profile behind without a journal
        this.createAlphaActiveAndBetaStored();
        Files.createDirectories(this.stash);
        Files.writeString(this.stash.resolve("old_options.txt"), "leftover");

        this.switcher.completePendingSwitch();

        assertFalse(Files.exists(this.stash));
        assertFalse(Files.exists(this.alpha));
        assertTrue(isCompleteProfile(this.storage, this.tempDir, "alpha", "alpha options"));
        Path leftover = this.storage.listStoredProfileDirectories().stream().filter(path -> !path.equals(this.beta)).findFirst().orElseThrow();
        assertEquals("leftover", read(leftover.resolve("old_options.txt")));
        assertTrue(this.switcher.switchTo("alpha", "beta", true));
    }

    @Test
    void completingWithoutPendingSwitchChangesNothing() throws Exception {
        this.createAlphaActiveAndBetaStored();

        this.switcher.completePendingSwitch();

        assertTrue(isCompleteProfile(this.storage, this.tempDir, "alpha", "alpha options"));
        assertTrue(isCompleteProfile(this.storage, this.beta, "beta", "beta options"));
        assertFalse(this.switcher.isSwitchPending());
    }

    @Test
    void journalKeepsWhetherTheIncompleteProfileGotUsed() throws Exception {
        writeJournal(this.storage, "alpha", "beta", "UNPACK", true);
        assertFalse(this.switcher.readJournal().activeProfileInUse());

        this.switcher.markActiveProfileInUse();

        ProfileSwitchJournal journal = this.switcher.readJournal();
        assertTrue(journal.activeProfileInUse());
        assertEquals("alpha", journal.fromId());
        assertEquals("beta", journal.toId());
        assertTrue(journal.keepActiveProfile());
        assertEquals(ProfileSwitchJournal.Phase.UNPACK, journal.phase());
    }

    private void createAlphaActiveAndBetaStored() throws Exception {
        createProfile(this.storage, this.tempDir, "alpha", "alpha options");
        createProfile(this.storage, this.beta, "beta", "beta options");
    }

}
