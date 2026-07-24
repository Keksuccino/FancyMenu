package de.keksuccino.fancymenu.customization.variables;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Owns durable replacement of the variable database. The temporary file must remain beside the target so both the
 * atomic move and its fallback stay on the same filesystem.
 */
final class AtomicVariableDatabase {

    private final Path target;
    private final FileOperations fileOperations;

    AtomicVariableDatabase(@NotNull Path target) {
        this(target, new NioFileOperations());
    }

    AtomicVariableDatabase(@NotNull Path target, @NotNull FileOperations fileOperations) {
        this.target = Objects.requireNonNull(target).toAbsolutePath().normalize();
        this.fileOperations = Objects.requireNonNull(fileOperations);
    }

    @NotNull
    String read() throws IOException {
        return this.fileOperations.readUtf8(this.target);
    }

    void write(@NotNull String serializedVariables) throws IOException {
        Path parent = this.target.getParent();
        if (parent == null) {
            throw new IOException("Variable database has no parent directory: " + this.target);
        }

        Path temporaryFile = null;
        Throwable primaryFailure = null;
        boolean replacementCompleted = false;
        try {
            this.fileOperations.createDirectories(parent);
            temporaryFile = this.fileOperations.createTempFile(parent, "." + this.target.getFileName() + ".", ".tmp");
            this.fileOperations.writeUtf8AndForce(temporaryFile, Objects.requireNonNull(serializedVariables));
            try {
                this.fileOperations.atomicReplace(temporaryFile, this.target);
            } catch (AtomicMoveNotSupportedException ex) {
                this.fileOperations.replace(temporaryFile, this.target);
            }
            replacementCompleted = true;
        } catch (IOException | RuntimeException | Error failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            if (temporaryFile != null) {
                try {
                    this.fileOperations.deleteIfExists(temporaryFile);
                } catch (IOException cleanupFailure) {
                    if (primaryFailure != null) {
                        primaryFailure.addSuppressed(cleanupFailure);
                    } else {
                        throw new TemporaryFileCleanupException(temporaryFile, replacementCompleted, cleanupFailure);
                    }
                }
            }
        }
    }

    @NotNull
    Path getTarget() {
        return this.target;
    }

    interface FileOperations {

        void createDirectories(@NotNull Path directory) throws IOException;

        @NotNull
        Path createTempFile(@NotNull Path directory, @NotNull String prefix, @NotNull String suffix) throws IOException;

        @NotNull
        String readUtf8(@NotNull Path path) throws IOException;

        void writeUtf8AndForce(@NotNull Path path, @NotNull String value) throws IOException;

        void atomicReplace(@NotNull Path source, @NotNull Path target) throws IOException;

        void replace(@NotNull Path source, @NotNull Path target) throws IOException;

        void deleteIfExists(@NotNull Path path) throws IOException;

    }

    static final class TemporaryFileCleanupException extends IOException {

        private final boolean replacementCompleted;

        private TemporaryFileCleanupException(@NotNull Path temporaryFile, boolean replacementCompleted, @NotNull IOException cause) {
            super("Failed to clean the variable database temporary file: " + temporaryFile, cause);
            this.replacementCompleted = replacementCompleted;
        }

        boolean replacementCompleted() {
            return this.replacementCompleted;
        }

    }

    private static final class NioFileOperations implements FileOperations {

        @Override
        public void createDirectories(@NotNull Path directory) throws IOException {
            Files.createDirectories(directory);
        }

        @Override
        public @NotNull Path createTempFile(@NotNull Path directory, @NotNull String prefix, @NotNull String suffix) throws IOException {
            return Files.createTempFile(directory, prefix, suffix);
        }

        @Override
        public @NotNull String readUtf8(@NotNull Path path) throws IOException {
            return Files.readString(path, StandardCharsets.UTF_8);
        }

        @Override
        public void writeUtf8AndForce(@NotNull Path path, @NotNull String value) throws IOException {
            ByteBuffer buffer = StandardCharsets.UTF_8.encode(value);
            try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
        }

        @Override
        public void atomicReplace(@NotNull Path source, @NotNull Path target) throws IOException {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }

        @Override
        public void replace(@NotNull Path source, @NotNull Path target) throws IOException {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }

        @Override
        public void deleteIfExists(@NotNull Path path) throws IOException {
            Files.deleteIfExists(path);
        }

    }

}
