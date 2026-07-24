package de.keksuccino.fancymenu.customization.variables;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtomicVariableDatabaseTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void createsMissingParentAndPublishesCompleteUtf8Target() throws Exception {
        Path target = this.temporaryDirectory.resolve("missing").resolve("nested").resolve("user_variables.db");
        AtomicVariableDatabase database = new AtomicVariableDatabase(target);

        database.write("type = user_variables\n\nvariable {\n  name = greeting\n  value = Grüße 世界\n}\n");

        assertAll(() -> assertEquals("type = user_variables\n\nvariable {\n  name = greeting\n  value = Grüße 世界\n}\n", Files.readString(target, StandardCharsets.UTF_8)), () -> assertEquals(List.of(target.getFileName().toString()), listFileNames(target.getParent())));
    }

    @Test
    void createsUniqueSiblingTempsAndCleansThemAfterReplacement() throws Exception {
        Path target = this.temporaryDirectory.resolve("user_variables.db");
        RecordingFileOperations operations = new RecordingFileOperations();
        AtomicVariableDatabase database = new AtomicVariableDatabase(target, operations);

        database.write("first");
        database.write("second");

        Path firstTemp = operations.createdTemps.get(0);
        Path secondTemp = operations.createdTemps.get(1);
        assertAll(() -> assertEquals(target.getParent().toAbsolutePath().normalize(), firstTemp.getParent()), () -> assertEquals(firstTemp.getParent(), secondTemp.getParent()), () -> assertNotEquals(firstTemp, secondTemp), () -> assertFalse(Files.exists(firstTemp)), () -> assertFalse(Files.exists(secondTemp)), () -> assertEquals("second", Files.readString(target)), () -> assertEquals(operations.createdTemps, operations.deletedPaths));
    }

    @Test
    void fallsBackToSameFilesystemReplacementOnlyWhenAtomicMoveIsUnsupported() throws Exception {
        Path target = Files.writeString(this.temporaryDirectory.resolve("user_variables.db"), "old");
        RecordingFileOperations operations = new RecordingFileOperations();
        operations.atomicMoveUnsupported = true;
        AtomicVariableDatabase database = new AtomicVariableDatabase(target, operations);

        database.write("new");

        assertAll(() -> assertEquals("new", Files.readString(target)), () -> assertEquals(1, operations.atomicReplaceCalls), () -> assertEquals(1, operations.replaceCalls), () -> assertFalse(Files.exists(operations.createdTemps.get(0))));
    }

    @Test
    void tempCreationFailurePreservesPreviousTargetWithoutDeletingAnything() throws Exception {
        Path target = Files.writeString(this.temporaryDirectory.resolve("user_variables.db"), "old");
        RecordingFileOperations operations = new RecordingFileOperations();
        operations.createTempFailure = new IOException("simulated temp open failure");
        AtomicVariableDatabase database = new AtomicVariableDatabase(target, operations);

        IOException exception = assertThrows(IOException.class, () -> database.write("new"));

        assertAll(() -> assertEquals("simulated temp open failure", exception.getMessage()), () -> assertEquals("old", Files.readString(target)), () -> assertTrue(operations.createdTemps.isEmpty()), () -> assertTrue(operations.deletedPaths.isEmpty()));
    }

    @Test
    void writeFailurePreservesPreviousTargetAndCleansOnlyItsExactTemp() throws Exception {
        Path target = Files.writeString(this.temporaryDirectory.resolve("user_variables.db"), "old");
        Path unrelated = Files.writeString(this.temporaryDirectory.resolve(".user_variables.db.unrelated.tmp"), "keep");
        RecordingFileOperations operations = new RecordingFileOperations();
        operations.writeFailure = new IOException("simulated write failure");
        AtomicVariableDatabase database = new AtomicVariableDatabase(target, operations);

        IOException exception = assertThrows(IOException.class, () -> database.write("new"));
        Path createdTemp = operations.createdTemps.get(0);

        assertAll(() -> assertEquals("simulated write failure", exception.getMessage()), () -> assertEquals("old", Files.readString(target)), () -> assertFalse(Files.exists(createdTemp)), () -> assertEquals("keep", Files.readString(unrelated)), () -> assertEquals(List.of(createdTemp), operations.deletedPaths));
    }

    @Test
    void atomicMoveFailureDoesNotFallBackOrDamagePreviousTarget() throws Exception {
        Path target = Files.writeString(this.temporaryDirectory.resolve("user_variables.db"), "old");
        RecordingFileOperations operations = new RecordingFileOperations();
        operations.atomicReplaceFailure = new IOException("simulated atomic move failure");
        AtomicVariableDatabase database = new AtomicVariableDatabase(target, operations);

        IOException exception = assertThrows(IOException.class, () -> database.write("new"));

        assertAll(() -> assertEquals("simulated atomic move failure", exception.getMessage()), () -> assertEquals("old", Files.readString(target)), () -> assertEquals(1, operations.atomicReplaceCalls), () -> assertEquals(0, operations.replaceCalls), () -> assertFalse(Files.exists(operations.createdTemps.get(0))));
    }

    @Test
    void fallbackMoveFailurePreservesPreviousTargetAndCleansTemp() throws Exception {
        Path target = Files.writeString(this.temporaryDirectory.resolve("user_variables.db"), "old");
        RecordingFileOperations operations = new RecordingFileOperations();
        operations.atomicMoveUnsupported = true;
        operations.replaceFailure = new IOException("simulated fallback move failure");
        AtomicVariableDatabase database = new AtomicVariableDatabase(target, operations);

        IOException exception = assertThrows(IOException.class, () -> database.write("new"));

        assertAll(() -> assertEquals("simulated fallback move failure", exception.getMessage()), () -> assertEquals("old", Files.readString(target)), () -> assertEquals(1, operations.replaceCalls), () -> assertFalse(Files.exists(operations.createdTemps.get(0))));
    }

    @Test
    void cleanupFailureIsSuppressedWhenWriteFailureIsPrimary() throws Exception {
        Path target = Files.writeString(this.temporaryDirectory.resolve("user_variables.db"), "old");
        RecordingFileOperations operations = new RecordingFileOperations();
        operations.writeFailure = new IOException("simulated write failure");
        operations.deleteFailure = new IOException("simulated cleanup failure");
        AtomicVariableDatabase database = new AtomicVariableDatabase(target, operations);

        IOException exception = assertThrows(IOException.class, () -> database.write("new"));

        assertAll(() -> assertEquals("simulated write failure", exception.getMessage()), () -> assertEquals(1, exception.getSuppressed().length), () -> assertEquals("simulated cleanup failure", exception.getSuppressed()[0].getMessage()), () -> assertEquals("old", Files.readString(target)), () -> assertTrue(Files.exists(operations.createdTemps.get(0))));
    }

    @Test
    void cleanupFailureAfterReplacementReportsThatNewTargetIsAlreadyLive() throws Exception {
        Path target = Files.writeString(this.temporaryDirectory.resolve("user_variables.db"), "old");
        RecordingFileOperations operations = new RecordingFileOperations();
        operations.deleteFailure = new IOException("simulated cleanup failure");
        AtomicVariableDatabase database = new AtomicVariableDatabase(target, operations);

        AtomicVariableDatabase.TemporaryFileCleanupException exception = assertThrows(AtomicVariableDatabase.TemporaryFileCleanupException.class, () -> database.write("new"));

        assertAll(() -> assertTrue(exception.replacementCompleted()), () -> assertEquals("simulated cleanup failure", exception.getCause().getMessage()), () -> assertEquals("new", Files.readString(target)), () -> assertEquals(List.of(operations.createdTemps.get(0)), operations.deletedPaths));
    }

    private static List<String> listFileNames(@NotNull Path directory) throws IOException {
        try (var paths = Files.list(directory)) {
            return paths.map(path -> path.getFileName().toString()).sorted().toList();
        }
    }

    private static final class RecordingFileOperations implements AtomicVariableDatabase.FileOperations {

        private final List<Path> createdTemps = new ArrayList<>();
        private final List<Path> deletedPaths = new ArrayList<>();
        private IOException createTempFailure;
        private IOException writeFailure;
        private IOException atomicReplaceFailure;
        private IOException replaceFailure;
        private IOException deleteFailure;
        private boolean atomicMoveUnsupported;
        private int atomicReplaceCalls;
        private int replaceCalls;

        @Override
        public void createDirectories(@NotNull Path directory) throws IOException {
            Files.createDirectories(directory);
        }

        @Override
        public @NotNull Path createTempFile(@NotNull Path directory, @NotNull String prefix, @NotNull String suffix) throws IOException {
            if (this.createTempFailure != null) throw this.createTempFailure;
            Path path = Files.createTempFile(directory, prefix, suffix);
            this.createdTemps.add(path);
            return path;
        }

        @Override
        public @NotNull String readUtf8(@NotNull Path path) throws IOException {
            return Files.readString(path, StandardCharsets.UTF_8);
        }

        @Override
        public void writeUtf8AndForce(@NotNull Path path, @NotNull String value) throws IOException {
            if (this.writeFailure != null) throw this.writeFailure;
            Files.writeString(path, value, StandardCharsets.UTF_8);
        }

        @Override
        public void atomicReplace(@NotNull Path source, @NotNull Path target) throws IOException {
            this.atomicReplaceCalls++;
            if (this.atomicMoveUnsupported) throw new AtomicMoveNotSupportedException(source.toString(), target.toString(), "simulated unsupported atomic move");
            if (this.atomicReplaceFailure != null) throw this.atomicReplaceFailure;
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }

        @Override
        public void replace(@NotNull Path source, @NotNull Path target) throws IOException {
            this.replaceCalls++;
            if (this.replaceFailure != null) throw this.replaceFailure;
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }

        @Override
        public void deleteIfExists(@NotNull Path path) throws IOException {
            this.deletedPaths.add(path);
            if (this.deleteFailure != null) throw this.deleteFailure;
            Files.deleteIfExists(path);
        }

    }

}
