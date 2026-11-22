package de.keksuccino.fancymenu.util.level;

import net.minecraft.util.DirectoryLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeldDirectoryLocksTest {

    @TempDir
    Path tempDir;

    @Test
    void heldLocksGetReportedUntilTheyGetReleased() throws IOException {
        Path world = this.tempDir.resolve("World");
        DirectoryLock lock = DirectoryLock.create(world);
        HeldDirectoryLocks.add(world, lock);

        assertTrue(HeldDirectoryLocks.isHeld(world));
        assertTrue(HeldDirectoryLocks.isHeld(this.tempDir.resolve("Other World").resolve("..").resolve("World")));

        lock.close();
        assertFalse(HeldDirectoryLocks.isHeld(world));
    }

    @Test
    void foldersWithoutHeldLockDontGetReported() throws IOException {
        Path world = this.tempDir.resolve("World");
        try (DirectoryLock lock = DirectoryLock.create(world)) {
            HeldDirectoryLocks.add(world, lock);
            assertFalse(HeldDirectoryLocks.isHeld(this.tempDir.resolve("Other World")));
        }
    }

}
