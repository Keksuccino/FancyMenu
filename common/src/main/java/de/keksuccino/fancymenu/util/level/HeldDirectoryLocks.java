package de.keksuccino.fancymenu.util.level;

import net.minecraft.util.DirectoryLock;
import org.jetbrains.annotations.NotNull;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Knows the folders that this game currently holds a {@link DirectoryLock} for, like the folder of the world that is open in singleplayer.
 *
 * <p>{@link DirectoryLock#isLocked(Path)} can't probe a lock that this game holds itself: the probe fails with an
 * {@link java.nio.channels.OverlappingFileLockException}, and closing the file channel of the probe releases the real lock of the game,
 * so other processes could open the locked world at the same time. This happens because file locks belong to the whole process, not to
 * the channel that got them. {@code MixinDirectoryLock} adds every lock this game creates here and answers probes of these folders
 * without touching their lock files.</p>
 */
public final class HeldDirectoryLocks {

    /** Weak, so locks that never get closed can still get collected, which releases them. */
    private static final Map<Path, WeakReference<DirectoryLock>> LOCKS = new ConcurrentHashMap<>();

    private HeldDirectoryLocks() {
    }

    public static void add(@NotNull Path directory, @NotNull DirectoryLock lock) {
        LOCKS.values().removeIf(reference -> !isValid(reference));
        LOCKS.put(normalize(directory), new WeakReference<>(lock));
    }

    /**
     * @return True if this game holds a lock for the given folder that it didn't release yet.
     */
    public static boolean isHeld(@NotNull Path directory) {
        Path key = normalize(directory);
        WeakReference<DirectoryLock> reference = LOCKS.get(key);
        if (reference == null) return false;
        if (isValid(reference)) return true;
        LOCKS.remove(key, reference);
        return false;
    }

    private static boolean isValid(@NotNull WeakReference<DirectoryLock> reference) {
        DirectoryLock lock = reference.get();
        return (lock != null) && lock.isValid();
    }

    @NotNull
    private static Path normalize(@NotNull Path directory) {
        return directory.toAbsolutePath().normalize();
    }

}
