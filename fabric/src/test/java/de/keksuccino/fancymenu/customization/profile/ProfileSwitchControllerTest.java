package de.keksuccino.fancymenu.customization.profile;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static de.keksuccino.fancymenu.customization.profile.ProfileTestSetup.*;
import static org.junit.jupiter.api.Assertions.*;

class ProfileSwitchControllerTest {

    @TempDir
    Path tempDir;
    private ProfileStorage storage;
    private ProfileSwitcher switcher;
    private ProfileCatalog catalog;
    private ProfileSwitchController controller;
    private Path alpha;
    private Path beta;
    private Path stash;
    private boolean taskRunning = false;
    private final List<Integer> walkStops = new ArrayList<>();
    @Nullable
    private ProfileFileFaults faults;

    @BeforeEach
    void setUp() throws Exception {
        this.storage = new ProfileStorage(this.tempDir);
        this.switcher = new ProfileSwitcher(this.storage);
        this.catalog = new ProfileCatalog(this.storage, this.switcher);
        // Like ProfileHandler, which reads the active ID through ProfileHandler#getActiveProfile()
        this.controller = new ProfileSwitchController(this.switcher, () -> this.walkStops.add(this.operationCount()), () -> this.taskRunning,
                () -> this.catalog.readActiveProfile(this.controller.tryCompletePendingSwitch(), "My Profile").getId());
        this.alpha = this.storage.getStoredProfileDirectory("alpha");
        this.beta = this.storage.getStoredProfileDirectory("beta");
        this.stash = this.switcher.getStashDirectory("alpha");
        createProfile(this.storage, this.tempDir, "alpha", "alpha options");
        createProfile(this.storage, this.beta, "beta", "beta options");
        this.faults = ProfileFileFaults.install();
    }

    @AfterEach
    void tearDown() {
        ProfileFileFaults.uninstall();
    }

    @Test
    void switchSucceedsAndReloadsAsChanged() throws Exception {
        Run run = this.startSwitch("beta");

        assertTrue(run.done);
        assertNull(run.error);
        assertEquals(List.of("suspend", "resume changed"), run.runtime);
        assertTrue(isCompleteProfile(this.storage, this.tempDir, "beta", "beta options"));
        assertTrue(isCompleteProfile(this.storage, this.alpha, "alpha", "alpha options"));
        assertFalse(this.controller.isSwitching());
    }

    @Test
    void failedCleanupStillReportsSuccessAndGetsFinishedInTheBackground() throws Exception {
        this.faults.failMoves((source, target) -> source.equals(this.stash), 3);
        long revision = this.controller.getRevision();

        Run run = this.startSwitch("beta");

        assertTrue(run.done);
        assertNull(run.error);
        assertEquals(List.of("suspend", "resume changed"), run.runtime);
        assertTrue(isCompleteProfile(this.storage, this.tempDir, "beta", "beta options"));
        assertTrue(this.switcher.isSwitchPending());

        // The first retries still fail, then the cleanup works
        this.tick(ProfileSwitchController.CLEANUP_RETRY_DELAY_TICKS * 3);

        assertFalse(this.switcher.isSwitchPending());
        assertTrue(isCompleteProfile(this.storage, this.alpha, "alpha", "alpha options"));
        assertFalse(Files.exists(this.stash));
        assertFalse(Files.exists(this.beta));
        assertTrue(this.controller.getRevision() > revision);
    }

    @Test
    void pendingCleanupNeverBlocksTheNextSwitch() throws Exception {
        this.faults.failMoves((source, target) -> source.equals(this.stash), -1);
        assertNull(this.startSwitch("beta").error);
        assertTrue(this.switcher.isSwitchPending());
        this.faults.clear();

        Run back = this.startSwitch("alpha");

        assertTrue(back.done);
        assertNull(back.error);
        assertTrue(isCompleteProfile(this.storage, this.tempDir, "alpha", "alpha options"));
        assertTrue(isCompleteProfile(this.storage, this.beta, "beta", "beta options"));
        assertFalse(this.switcher.isSwitchPending());
    }

    @Test
    void leftoverOfTheIssueGetsCompletedByTheNextSwitch() throws Exception {
        // What FancyMenu 3.10 left behind in issue #1867: a pending cleanup, the stashed default profile and metadata in the emptied directory
        Path gamma = this.storage.getStoredProfileDirectory("gamma");
        createProfile(this.storage, gamma, "gamma", "gamma options");
        ProfileFiles.deleteRecursively(this.tempDir.resolve("customization"));
        Files.move(this.tempDir.resolve("options.txt"), Files.createDirectories(this.stash).resolve("options.txt"));
        Files.move(this.storage.getMetadataFile(this.tempDir), this.storage.getMetadataFile(this.stash));
        Files.createDirectories(this.stash.resolve("customization"));
        Files.writeString(this.stash.resolve("customization").resolve("layout.txt"), "alpha layout");
        ProfileFiles.moveEntries(this.storage.listContentEntries(this.beta), this.tempDir, null);
        ProfileMetadata recreated = new ProfileMetadata("beta");
        recreated.setTitle("beta");
        this.storage.writeMetadata(this.beta, recreated);
        writeJournal(this.storage, "alpha", "beta", "FINISH", true);

        Run run = this.startSwitch("gamma");

        assertNull(run.error);
        assertTrue(isCompleteProfile(this.storage, this.tempDir, "gamma", "gamma options"));
        assertTrue(isCompleteProfile(this.storage, this.alpha, "alpha", "alpha options"));
        assertTrue(isCompleteProfile(this.storage, this.beta, "beta", "beta options"));
        assertFalse(this.switcher.isSwitchPending());
    }

    @Test
    void filesInUseGetRetriedUntilTheSwitchWorks() throws Exception {
        // The first move of the first two attempts fails
        this.faults.failMoves((source, target) -> target.getParent().equals(this.stash), 2);

        Run run = this.startSwitch("beta");
        assertFalse(run.done);
        assertTrue(this.controller.isSwitching());
        this.tick(ProfileSwitchController.SWITCH_RETRY_DELAY_TICKS * 3);

        assertTrue(run.done);
        assertNull(run.error);
        assertEquals(List.of("suspend", "resume changed"), run.runtime);
        assertTrue(isCompleteProfile(this.storage, this.tempDir, "beta", "beta options"));
        assertTrue(isCompleteProfile(this.storage, this.alpha, "alpha", "alpha options"));
    }

    @Test
    void failedSwitchWithCompleteRollbackReloadsAsUnchanged() throws Exception {
        this.faults.failMoves((source, target) -> source.getParent().equals(this.beta), -1);

        Run run = this.startSwitchAndWait("beta");

        assertNotNull(run.error);
        assertEquals(List.of("suspend", "resume unchanged"), run.runtime);
        assertTrue(isCompleteProfile(this.storage, this.tempDir, "alpha", "alpha options"));
        assertTrue(isCompleteProfile(this.storage, this.beta, "beta", "beta options"));
        assertFalse(this.switcher.isSwitchPending());
    }

    @Test
    void failedSwitchWithIncompleteRollbackReloadsAsChangedAndGetsCompletedLater() throws Exception {
        AtomicInteger stashed = new AtomicInteger();
        this.faults.failMoves((source, target) -> target.getParent().equals(this.stash) && (stashed.incrementAndGet() == 2), 1);
        this.faults.failMoves((source, target) -> source.getParent().equals(this.stash), -1);

        Run run = this.startSwitchAndWait("beta");

        assertNotNull(run.error);
        assertEquals(List.of("suspend", "resume changed"), run.runtime);
        assertTrue(this.switcher.isActiveProfileIncomplete());
        // FancyMenu uses the incomplete profile now, so completing it later has to keep the stashed data
        assertTrue(this.switcher.readJournal().activeProfileInUse());
        // Listing must not complete it, because that changes files FancyMenu uses
        assertFalse(this.controller.tryCompletePendingSwitch());
        assertEquals(1, ProfileFiles.listEntries(this.stash).size());

        this.faults.clear();
        Run retry = this.startSwitch("beta");

        assertNull(retry.error);
        assertEquals(List.of("suspend", "resume changed"), retry.runtime);
        assertTrue(isCompleteProfile(this.storage, this.tempDir, "beta", "beta options"));
        assertTrue(isCompleteProfile(this.storage, this.alpha, "alpha", "alpha options"));
        assertFalse(this.switcher.isSwitchPending());
    }

    @Test
    void everyAttemptStopsProfileWalksBeforeTouchingFiles() throws Exception {
        this.faults.failMoves((source, target) -> source.getParent().equals(this.beta), 1);
        int operationsBefore = this.operationCount();

        Run run = this.startSwitchAndWait("beta");

        assertNull(run.error);
        assertEquals(2, this.walkStops.size());
        assertEquals(operationsBefore, this.walkStops.get(0));
    }

    @Test
    void completingAPendingCleanupStopsProfileWalksFirst() throws Exception {
        this.faults.failMoves((source, target) -> source.equals(this.stash), 1);
        this.startSwitch("beta");
        this.walkStops.clear();
        int operationsBefore = this.operationCount();

        assertTrue(this.controller.tryCompletePendingSwitch());

        assertEquals(List.of(operationsBefore), this.walkStops);
    }

    @Test
    void pendingCleanupWaitsForRunningTasks() throws Exception {
        this.faults.failMoves((source, target) -> source.equals(this.stash), 1);
        this.startSwitch("beta");
        this.taskRunning = true;

        assertFalse(this.controller.tryCompletePendingSwitch());
        this.tick(ProfileSwitchController.CLEANUP_RETRY_DELAY_TICKS * 2);
        assertTrue(this.switcher.isSwitchPending());

        this.taskRunning = false;
        this.tick(1);
        assertFalse(this.switcher.isSwitchPending());
    }

    @Test
    void launchCompletesPendingSwitches() throws Exception {
        this.faults.failMoves((source, target) -> source.equals(this.stash), -1);
        this.startSwitch("beta");
        this.faults.clear();
        ProfileSwitchController afterRestart = new ProfileSwitchController(this.switcher, () -> {}, () -> false, () -> "beta");

        afterRestart.completePendingSwitchAtLaunch();

        assertFalse(this.switcher.isSwitchPending());
        assertTrue(isCompleteProfile(this.storage, this.alpha, "alpha", "alpha options"));
    }

    @Test
    void launchThatCantCompleteMarksTheIncompleteProfileAsUsed() throws Exception {
        Files.move(this.tempDir.resolve("options.txt"), Files.createDirectories(this.stash).resolve("options.txt"));
        writeJournal(this.storage, "alpha", "beta", "STASH", true);
        this.faults.failMoves((source, target) -> source.getParent().equals(this.stash), -1);
        ProfileSwitchController afterRestart = new ProfileSwitchController(this.switcher, () -> {}, () -> false, () -> "alpha");

        afterRestart.completePendingSwitchAtLaunch();

        assertTrue(this.switcher.isActiveProfileIncomplete());
        assertTrue(this.switcher.readJournal().activeProfileInUse());
        assertEquals("alpha options", read(this.stash.resolve("options.txt")));
    }

    @Test
    void launchThatCantCleanUpRetriesInTheBackground() throws Exception {
        this.faults.failMoves((source, target) -> source.equals(this.stash), -1);
        this.startSwitch("beta");
        ProfileSwitchController afterRestart = new ProfileSwitchController(this.switcher, () -> {}, () -> false, () -> "beta");
        afterRestart.completePendingSwitchAtLaunch();
        assertTrue(this.switcher.isSwitchPending());
        this.faults.clear();

        for (int i = 0; i < ProfileSwitchController.CLEANUP_RETRY_DELAY_TICKS; i++) afterRestart.tick();

        assertFalse(this.switcher.isSwitchPending());
        assertTrue(isCompleteProfile(this.storage, this.alpha, "alpha", "alpha options"));
    }

    private Run startSwitch(String targetId) {
        Run run = new Run();
        this.controller.startSwitch(targetId, true, new ProfileSwitchController.SwitchRuntime() {
            @Override
            public boolean suspend() {
                run.runtime.add("suspend");
                return true;
            }
            @Override
            public void resume(boolean activeProfileChanged) {
                run.runtime.add(activeProfileChanged ? "resume changed" : "resume unchanged");
            }
        }, error -> {
            run.done = true;
            run.error = error;
        });
        return run;
    }

    private Run startSwitchAndWait(String targetId) {
        Run run = this.startSwitch(targetId);
        this.tick(ProfileSwitchController.SWITCH_RETRY_DELAY_TICKS * ProfileSwitchController.MAX_SWITCH_ATTEMPTS);
        assertTrue(run.done);
        return run;
    }

    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) this.controller.tick();
    }

    private int operationCount() {
        return (this.faults != null) ? this.faults.operations.size() : 0;
    }

    private static final class Run {

        private final List<String> runtime = new ArrayList<>();
        private boolean done = false;
        @Nullable
        private Throwable error;

    }

}
