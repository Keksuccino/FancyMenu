package de.keksuccino.fancymenu.customization.profile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * File operations on profile directories. Copies report their progress in bytes and can get cancelled between buffer writes.<br>
 * Symbolic links never get followed, so a profile can't make FancyMenu copy or delete files outside of it.
 */
public final class ProfileFiles {

    private static final Logger LOGGER = LogManager.getLogger();

    static final int BUFFER_SIZE = 64 * 1024;

    private ProfileFiles() {
    }

    /**
     * Sums up the size of all regular files in a directory.
     *
     * @param filter decides which top-level entries count. Nested entries of accepted entries always count.
     */
    public static long measureSize(@NotNull Path directory, @NotNull Predicate<Path> filter) throws IOException {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) return 0L;
        AtomicLong size = new AtomicLong();
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) {
                if (!dir.equals(directory) && !filter.test(dir)) return FileVisitResult.SKIP_SUBTREE;
                return FileVisitResult.CONTINUE;
            }
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                if (attrs.isRegularFile() && filter.test(file)) size.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }
            @Override
            public @NotNull FileVisitResult visitFileFailed(@NotNull Path file, @NotNull IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
        return size.get();
    }

    /**
     * Copies the content of a directory into another directory. The target directory gets created if needed.
     *
     * @param filter decides which entries get copied.
     */
    public static void copyDirectoryContent(@NotNull Path source, @NotNull Path target, @NotNull Predicate<Path> filter, @NotNull ProfileTaskProgress progress) throws IOException {
        Files.createDirectories(target);
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
                progress.throwIfCancelled();
                if (dir.equals(source)) return FileVisitResult.CONTINUE;
                if (!filter.test(dir)) return FileVisitResult.SKIP_SUBTREE;
                Files.createDirectories(target.resolve(source.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                if (!attrs.isRegularFile() || !filter.test(file)) return FileVisitResult.CONTINUE;
                copyFile(file, target.resolve(source.relativize(file).toString()), progress);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Copies a single file with progress updates and keeps its modification time.
     */
    public static void copyFile(@NotNull Path source, @NotNull Path target, @NotNull ProfileTaskProgress progress) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream in = Files.newInputStream(source); OutputStream out = Files.newOutputStream(target)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                progress.throwIfCancelled();
                out.write(buffer, 0, read);
                progress.addProcessedBytes(read);
            }
        }
        try {
            Files.setLastModifiedTime(target, Files.getLastModifiedTime(source));
        } catch (IOException ignored) {}
    }

    /**
     * Deletes a file or a whole directory tree. Symbolic links get deleted without touching their targets.
     */
    public static void deleteRecursively(@NotNull Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return;
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            Files.deleteIfExists(path);
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public @NotNull FileVisitResult postVisitDirectory(@NotNull Path dir, @Nullable IOException exc) throws IOException {
                if (exc != null) throw exc;
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Deletes a path and only logs failures. Meant for cleaning up temporary data.
     */
    public static void deleteQuietly(@Nullable Path path) {
        if (path == null) return;
        try {
            deleteRecursively(path);
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to delete temporary profile data: " + path, ex);
        }
    }

    /**
     * Moves the given top-level entries into a directory. If any move fails, all already moved entries get moved back
     * before the exception gets rethrown, so a failed call leaves both directories as they were.
     *
     * @param conflictHandler gets called when the target directory already has an entry with the same name. Without handler, conflicts fail the move.
     */
    public static void moveEntries(@NotNull List<Path> entries, @NotNull Path targetDirectory, @Nullable ConflictHandler conflictHandler) throws IOException {
        Files.createDirectories(targetDirectory);
        List<Path[]> moved = new ArrayList<>();
        try {
            for (Path entry : entries) {
                Path target = targetDirectory.resolve(entry.getFileName().toString());
                if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                    if (conflictHandler == null) throw new IOException("Can't move " + entry + ", because the target already exists: " + target);
                    if (!conflictHandler.resolveConflict(entry, target)) continue;
                }
                Files.move(entry, target);
                moved.add(new Path[] {entry, target});
            }
        } catch (IOException | RuntimeException ex) {
            for (int i = moved.size() - 1; i >= 0; i--) {
                Path[] pair = moved.get(i);
                try {
                    Files.move(pair[1], pair[0]);
                } catch (Exception rollbackEx) {
                    LOGGER.error("[FANCYMENU] Failed to move profile entry back while rolling back a failed move: " + pair[1] + " -> " + pair[0], rollbackEx);
                    ex.addSuppressed(rollbackEx);
                }
            }
            throw ex;
        }
    }

    /**
     * Moves all entries of a directory into another one, and deletes the then empty source directory.
     */
    public static void moveDirectoryContent(@NotNull Path sourceDirectory, @NotNull Path targetDirectory, @Nullable ConflictHandler conflictHandler) throws IOException {
        List<Path> entries = new ArrayList<>();
        if (Files.isDirectory(sourceDirectory, LinkOption.NOFOLLOW_LINKS)) {
            try (var stream = Files.list(sourceDirectory)) {
                stream.forEach(entries::add);
            }
        }
        moveEntries(entries, targetDirectory, conflictHandler);
        deleteEmptyDirectory(sourceDirectory);
    }

    /**
     * Deletes a directory if it is empty.
     */
    public static void deleteEmptyDirectory(@NotNull Path directory) throws IOException {
        try {
            Files.deleteIfExists(directory);
        } catch (DirectoryNotEmptyException ex) {
            throw new IOException("Directory is not empty: " + directory, ex);
        } catch (NoSuchFileException ignored) {}
    }

    /**
     * Renames a directory. Fails if the target already exists.
     */
    public static void renameDirectory(@NotNull Path source, @NotNull Path target) throws IOException {
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Can't rename " + source + ", because the target already exists: " + target);
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    }

    @FunctionalInterface
    public interface ConflictHandler {

        /**
         * Resolves a name conflict, either by making the target path free or by dropping the source entry.
         *
         * @return TRUE if the target path got freed and the source should get moved, FALSE if the source got dropped and should get skipped.
         */
        boolean resolveConflict(@NotNull Path source, @NotNull Path target) throws IOException;

    }

}
