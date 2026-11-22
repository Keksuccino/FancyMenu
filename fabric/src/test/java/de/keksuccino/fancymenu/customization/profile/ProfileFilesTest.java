package de.keksuccino.fancymenu.customization.profile;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ProfileFilesTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        ProfileFileFaults.uninstall();
    }

    @Test
    void failedMoveMovesEverythingBack() throws Exception {
        Path source = Files.createDirectories(this.tempDir.resolve("source"));
        Path target = Files.createDirectories(this.tempDir.resolve("target"));
        Path first = Files.writeString(source.resolve("a.txt"), "a");
        Path second = Files.writeString(source.resolve("b.txt"), "b");
        Files.writeString(target.resolve("b.txt"), "existing");

        assertThrows(IOException.class, () -> ProfileFiles.moveEntries(List.of(first, second), target, null));

        assertEquals("a", Files.readString(first));
        assertEquals("b", Files.readString(second));
        assertFalse(Files.exists(target.resolve("a.txt")));
        assertEquals("existing", Files.readString(target.resolve("b.txt")));
    }

    @Test
    void conflictHandlerCanSkipEntries() throws Exception {
        Path source = Files.createDirectories(this.tempDir.resolve("source"));
        Path target = Files.createDirectories(this.tempDir.resolve("target"));
        Files.writeString(source.resolve("a.txt"), "old");
        Files.writeString(target.resolve("a.txt"), "newer");

        ProfileFiles.moveDirectoryContent(source, target, (from, to) -> {
            ProfileFiles.deleteRecursively(from);
            return false;
        });

        assertEquals("newer", Files.readString(target.resolve("a.txt")));
        assertFalse(Files.exists(source));
    }

    @Test
    void bestEffortMoveMovesEverythingItCanAndReportsTheFailure() throws Exception {
        Path source = Files.createDirectories(this.tempDir.resolve("source"));
        Path target = Files.createDirectories(this.tempDir.resolve("target"));
        Path first = Files.writeString(source.resolve("a.txt"), "a");
        Path stuck = Files.writeString(source.resolve("b.txt"), "b");
        Path third = Files.writeString(source.resolve("c.txt"), "c");
        ProfileFileFaults.install().failMoves((from, to) -> from.equals(stuck), -1);

        assertThrows(IOException.class, () -> ProfileFiles.moveEntriesBestEffort(List.of(first, stuck, third), target, null));

        assertEquals("a", Files.readString(target.resolve("a.txt")));
        assertEquals("c", Files.readString(target.resolve("c.txt")));
        assertEquals("b", Files.readString(stuck));
        assertFalse(Files.exists(first));
    }

    @Test
    void movingDirectoryContentNeverDeletesASourceWithEntriesLeft() throws Exception {
        Path source = Files.createDirectories(this.tempDir.resolve("source"));
        Path target = Files.createDirectories(this.tempDir.resolve("target"));
        Files.writeString(source.resolve("a.txt"), "a");
        Path stuck = Files.writeString(source.resolve("b.txt"), "b");
        ProfileFileFaults.install().failMoves((from, to) -> from.equals(stuck), -1);

        assertThrows(IOException.class, () -> ProfileFiles.moveDirectoryContent(source, target, null));

        assertEquals("b", Files.readString(stuck));
        assertEquals("a", Files.readString(target.resolve("a.txt")));
    }

    @Test
    void mergingKeepsEverythingThatExistsOnOnlyOneSide() throws Exception {
        Path source = Files.createDirectories(this.tempDir.resolve("source").resolve("sub"));
        Path target = Files.createDirectories(this.tempDir.resolve("target").resolve("sub"));
        Files.writeString(source.resolve("only_source.txt"), "source");
        Files.writeString(source.resolve("both.txt"), "source version");
        Files.writeString(target.resolve("only_target.txt"), "target");
        Files.writeString(target.resolve("both.txt"), "target version");

        ProfileFiles.moveMerging(source.getParent(), target.getParent(), false);

        assertFalse(Files.exists(source.getParent()));
        assertEquals("source", Files.readString(target.resolve("only_source.txt")));
        assertEquals("target", Files.readString(target.resolve("only_target.txt")));
        assertEquals("target version", Files.readString(target.resolve("both.txt")));
    }

    @Test
    void mergingCanLetTheSourceWin() throws Exception {
        Path source = Files.createDirectories(this.tempDir.resolve("source"));
        Path target = Files.createDirectories(this.tempDir.resolve("target"));
        Files.writeString(source.resolve("both.txt"), "source version");
        Files.writeString(target.resolve("both.txt"), "target version");
        Files.writeString(target.resolve("only_target.txt"), "target");

        ProfileFiles.moveMerging(source, target, true);

        assertFalse(Files.exists(source));
        assertEquals("source version", Files.readString(target.resolve("both.txt")));
        assertEquals("target", Files.readString(target.resolve("only_target.txt")));
    }

    @Test
    void measuringStopsOnceCancelled() throws Exception {
        Path source = Files.createDirectories(this.tempDir.resolve("source"));
        Files.write(source.resolve("data.bin"), new byte[100]);
        ProfileTaskProgress cancellation = new ProfileTaskProgress();
        cancellation.requestCancel();

        assertThrows(CancellationException.class, () -> ProfileFiles.measureSize(source, path -> true, cancellation));
    }

    @Test
    void copyReportsProgressAndHonorsTheFilter() throws Exception {
        Path source = Files.createDirectories(this.tempDir.resolve("source"));
        Files.createDirectories(source.resolve("keep"));
        Files.write(source.resolve("keep").resolve("data.bin"), new byte[300_000]);
        Files.createDirectories(source.resolve("skip"));
        Files.write(source.resolve("skip").resolve("data.bin"), new byte[100]);
        Path target = this.tempDir.resolve("target");
        ProfileTaskProgress progress = new ProfileTaskProgress();
        Path skipped = source.resolve("skip");

        progress.setTotalBytes(ProfileFiles.measureSize(source, path -> !path.startsWith(skipped), progress));
        ProfileFiles.copyDirectoryContent(source, target, path -> !path.startsWith(skipped), progress);

        assertEquals(300_000, progress.getTotalBytes());
        assertEquals(300_000, progress.getProcessedBytes());
        assertEquals(300_000, Files.size(target.resolve("keep").resolve("data.bin")));
        assertFalse(Files.exists(target.resolve("skip")));
    }

    @Test
    void cancelledCopyStops() throws Exception {
        Path source = Files.createDirectories(this.tempDir.resolve("source"));
        Files.write(source.resolve("data.bin"), new byte[300_000]);
        ProfileTaskProgress progress = new ProfileTaskProgress();
        progress.requestCancel();

        assertThrows(CancellationException.class, () -> ProfileFiles.copyDirectoryContent(source, this.tempDir.resolve("target"), path -> true, progress));
    }

    @Test
    void deletingNeverFollowsSymbolicLinks() throws Exception {
        Path outside = Files.createDirectories(this.tempDir.resolve("outside"));
        Path precious = Files.writeString(outside.resolve("precious.txt"), "keep me");
        Path profile = Files.createDirectories(this.tempDir.resolve("profile"));
        try {
            Files.createSymbolicLink(profile.resolve("link"), outside);
        } catch (UnsupportedOperationException | IOException ex) {
            assumeTrue(false, "Symbolic links are not supported here");
        }

        ProfileFiles.deleteRecursively(profile);

        assertFalse(Files.exists(profile));
        assertEquals("keep me", Files.readString(precious));
    }

    @Test
    void estimationNeedsEnoughDataFirst() {
        long[] now = {0L};
        ProfileTaskProgress progress = new ProfileTaskProgress(() -> now[0]);
        progress.setTotalBytes(1000);
        progress.addProcessedBytes(100);

        assertEquals(-1L, progress.getEstimatedRemainingMs());

        now[0] = 1000L;
        assertEquals(9000L, progress.getEstimatedRemainingMs());
        assertEquals(0.1F, progress.getProgress(), 0.0001F);

        progress.addProcessedBytes(900);
        assertEquals(0L, progress.getEstimatedRemainingMs());
    }

}
