package de.keksuccino.fancymenu.customization.profile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
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
import java.util.stream.Stream;

/**
 * File operations on profile directories. Copies report their progress in bytes and can get cancelled between buffer writes.<br>
 * Symbolic links never get followed, so a profile can't make FancyMenu copy or delete files outside of it.
 */
public final class ProfileFiles {

    private static final Logger LOGGER = LogManager.getLogger();

    static final int BUFFER_SIZE = 64 * 1024;

    /**
     * Gets told about every move and delete of profile files before it happens, and can make it fail by throwing.
     * Only tests set it, to simulate files that other programs keep open on Windows. Always NULL in production.
     */
    @Nullable
    static volatile OperationHook operationHook;

    private ProfileFiles() {
    }

    /**
     * Sums up the size of all regular files in a directory.
     *
     * @param filter decides which top-level entries count. Nested entries of accepted entries always count.
     * @param cancellation stops the walk with a {@link java.util.concurrent.CancellationException} at the next entry once cancelling got requested.
     */
    public static long measureSize(@NotNull Path directory, @NotNull Predicate<Path> filter, @NotNull ProfileTaskProgress cancellation) throws IOException {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) return 0L;
        AtomicLong size = new AtomicLong();
        // Exceptions of the visitor end the walk and close all directories it has open
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) {
                cancellation.throwIfCancelled();
                if (!dir.equals(directory) && !filter.test(dir)) return FileVisitResult.SKIP_SUBTREE;
                return FileVisitResult.CONTINUE;
            }
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                cancellation.throwIfCancelled();
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
     * Moves or renames a single entry. All moves of profile files go through here, so {@link #operationHook} sees them.
     */
    static void move(@NotNull Path source, @NotNull Path target, @NotNull CopyOption... options) throws IOException {
        OperationHook hook = operationHook;
        if (hook != null) hook.beforeMove(source, target);
        Files.move(source, target, options);
    }

    /**
     * Deletes a single file or empty directory, if it exists. All deletes of profile files go through here, so {@link #operationHook} sees them.
     */
    static void delete(@NotNull Path path) throws IOException {
        OperationHook hook = operationHook;
        if (hook != null) hook.beforeDelete(path);
        Files.deleteIfExists(path);
    }

    /**
     * Deletes a file or a whole directory tree. Symbolic links get deleted without touching their targets.
     */
    public static void deleteRecursively(@NotNull Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return;
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            delete(path);
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public @NotNull FileVisitResult postVisitDirectory(@NotNull Path dir, @Nullable IOException exc) throws IOException {
                if (exc != null) throw exc;
                delete(dir);
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
     * before the exception gets rethrown, so a failed call leaves both directories as they were.<br>
     * Moving an entry back can fail too, which only gets logged and added to the exception as suppressed. Callers that need to know
     * whether everything is back in place have to check the entries.
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
                move(entry, target);
                moved.add(new Path[] {entry, target});
            }
        } catch (IOException | RuntimeException ex) {
            for (int i = moved.size() - 1; i >= 0; i--) {
                Path[] pair = moved.get(i);
                try {
                    move(pair[1], pair[0]);
                } catch (Exception rollbackEx) {
                    LOGGER.error("[FANCYMENU] Failed to move profile entry back while rolling back a failed move: " + pair[1] + " -> " + pair[0], rollbackEx);
                    ex.addSuppressed(rollbackEx);
                }
            }
            throw ex;
        }
    }

    /**
     * Moves the given top-level entries into a directory. Unlike {@link #moveEntries(List, Path, ConflictHandler)}, a failed move doesn't stop it
     * and nothing gets moved back. It moves every entry it can, so after a failure, calling it again only needs to move the rest.
     *
     * @param conflictHandler gets called when the target directory already has an entry with the same name. Without handler, conflicts fail the move of that entry.
     * @throws IOException the first failure, with all others added as suppressed, after it tried to move every entry.
     */
    public static void moveEntriesBestEffort(@NotNull List<Path> entries, @NotNull Path targetDirectory, @Nullable ConflictHandler conflictHandler) throws IOException {
        Files.createDirectories(targetDirectory);
        Exception failure = null;
        for (Path entry : entries) {
            try {
                Path target = targetDirectory.resolve(entry.getFileName().toString());
                if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                    if (conflictHandler == null) throw new IOException("Can't move " + entry + ", because the target already exists: " + target);
                    if (!conflictHandler.resolveConflict(entry, target)) continue;
                }
                move(entry, target);
            } catch (IOException | RuntimeException ex) {
                if (failure == null) {
                    failure = ex;
                } else {
                    failure.addSuppressed(ex);
                }
            }
        }
        if (failure instanceof IOException io) throw io;
        if (failure instanceof RuntimeException runtime) throw runtime;
    }

    /**
     * Moves all entries of a directory into another one like {@link #moveEntriesBestEffort(List, Path, ConflictHandler)},
     * and deletes the source directory once it is empty. A source directory that still has entries never gets deleted.
     */
    public static void moveDirectoryContent(@NotNull Path sourceDirectory, @NotNull Path targetDirectory, @Nullable ConflictHandler conflictHandler) throws IOException {
        if (Files.isDirectory(sourceDirectory, LinkOption.NOFOLLOW_LINKS)) {
            moveEntriesBestEffort(listEntries(sourceDirectory), targetDirectory, conflictHandler);
        }
        deleteEmptyDirectory(sourceDirectory);
    }

    /**
     * Moves an entry to a path that may be taken already, without losing data that exists on only one side. Directories on both sides get merged
     * entry by entry, so only entries that exist on both sides with the same name compete, and one of them gets dropped.
     *
     * @param sourceWins whether an entry of the source replaces the competing entry of the target, or gets dropped instead.
     */
    public static void moveMerging(@NotNull Path source, @NotNull Path target, boolean sourceWins) throws IOException {
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            move(source, target);
            return;
        }
        if (Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS) && Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)) {
            for (Path child : listEntries(source)) {
                moveMerging(child, target.resolve(child.getFileName().toString()), sourceWins);
            }
            delete(source);
            return;
        }
        if (sourceWins) {
            deleteRecursively(target);
            move(source, target);
        } else {
            deleteRecursively(source);
        }
    }

    /**
     * Deletes a directory if it is empty.
     */
    public static void deleteEmptyDirectory(@NotNull Path directory) throws IOException {
        try {
            delete(directory);
        } catch (DirectoryNotEmptyException ex) {
            throw new IOException("Directory is not empty: " + directory, ex);
        } catch (NoSuchFileException ignored) {}
    }

    /**
     * Renames a directory. Fails if the target already exists.
     */
    public static void renameDirectory(@NotNull Path source, @NotNull Path target) throws IOException {
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Can't rename " + source + ", because the target already exists: " + target);
        move(source, target, StandardCopyOption.ATOMIC_MOVE);
    }

    @NotNull
    static List<Path> listEntries(@NotNull Path directory) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            return new ArrayList<>(stream.toList());
        }
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

    /**
     * See {@link #operationHook}.
     */
    interface OperationHook {

        void beforeMove(@NotNull Path source, @NotNull Path target) throws IOException;

        void beforeDelete(@NotNull Path path) throws IOException;

    }

}
