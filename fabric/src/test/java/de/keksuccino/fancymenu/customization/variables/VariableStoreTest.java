package de.keksuccino.fancymenu.customization.variables;

import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariableStoreTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void concurrentMutationsAndObserversPublishEveryUpdateAsOneValidDatabase() throws Exception {
        Path target = this.temporaryDirectory.resolve("user_variables.db");
        VariableStore store = createStore(target);
        int writerCount = 6;
        int variablesPerWriter = 6;
        CountDownLatch ready = new CountDownLatch(writerCount + 1);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            try {
                for (int writer = 0; writer < writerCount; writer++) {
                    int writerIndex = writer;
                    futures.add(executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        for (int variable = 0; variable < variablesPerWriter; variable++) {
                            store.setVariable("writer_" + writerIndex + "_variable_" + variable, "value_" + writerIndex + "_" + variable);
                        }
                        return null;
                    }));
                }
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    for (int iteration = 0; iteration < 250; iteration++) {
                        List<UserVariableSnapshot> snapshots = store.getVariableSnapshots();
                        assertEquals(snapshots.size(), new HashSet<>(snapshots.stream().map(UserVariableSnapshot::name).toList()).size());
                        for (Variable variable : store.getVariables()) variable.snapshot();
                    }
                    return null;
                }));
                boolean allTasksReady = ready.await(5L, TimeUnit.SECONDS);
                start.countDown();
                assertTrue(allTasksReady);
                for (Future<?> future : futures) future.get(20L, TimeUnit.SECONDS);
            } finally {
                start.countDown();
            }
        } finally {
            executor.shutdownNow();
        }

        Map<String, UserVariableSnapshot> memory = snapshotsByName(store.getVariableSnapshots());
        Map<String, UserVariableSnapshot> disk = readSnapshots(target);
        assertAll(() -> assertEquals(writerCount * variablesPerWriter, memory.size()), () -> assertEquals(memory, disk), () -> assertEquals(List.of(target.getFileName().toString()), listFileNames(target.getParent())));
    }

    @Test
    void bulkSnapshotsNeverExposeMixedReplacementGenerations() throws Exception {
        Path target = this.temporaryDirectory.resolve("user_variables.db");
        VariableStore store = createStore(target);
        store.replaceVariables(generation(0));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            try {
                Future<?> writer = executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    for (int generation = 1; generation <= 60; generation++) store.replaceVariables(generation(generation));
                    return null;
                });
                Future<?> reader = executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    for (int iteration = 0; iteration < 500; iteration++) {
                        Map<String, UserVariableSnapshot> snapshots = snapshotsByName(store.getVariableSnapshots());
                        assertEquals(2, snapshots.size());
                        assertEquals(snapshots.get("generation_a").value(), snapshots.get("generation_b").value());
                    }
                    return null;
                });
                boolean allTasksReady = ready.await(5L, TimeUnit.SECONDS);
                start.countDown();
                assertTrue(allTasksReady);
                writer.get(20L, TimeUnit.SECONDS);
                reader.get(20L, TimeUnit.SECONDS);
            } finally {
                start.countDown();
            }
        } finally {
            executor.shutdownNow();
        }

        Map<String, UserVariableSnapshot> disk = readSnapshots(target);
        assertAll(() -> assertEquals("60", disk.get("generation_a").value()), () -> assertEquals(disk.get("generation_a").value(), disk.get("generation_b").value()));
    }

    @Test
    void listenerCanReenterStoreWithoutDeadlockOrLosingNestedMutation() throws Exception {
        Path target = this.temporaryDirectory.resolve("user_variables.db");
        AtomicReference<VariableStore> storeReference = new AtomicReference<>();
        AtomicBoolean nestedMutationStarted = new AtomicBoolean();
        List<String> events = new CopyOnWriteArrayList<>();
        VariableStore store = new VariableStore(new AtomicVariableDatabase(target), (name, oldValue, newValue) -> {
            events.add(name + ":" + oldValue + "->" + newValue);
            if ("outer".equals(name) && nestedMutationStarted.compareAndSet(false, true)) storeReference.get().setVariable("nested", "inside");
        });
        storeReference.set(store);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(() -> store.setVariable("outer", "outside")).get(10L, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        Map<String, UserVariableSnapshot> expected = Map.of("outer", new UserVariableSnapshot("outer", "outside", false), "nested", new UserVariableSnapshot("nested", "inside", false));
        assertAll(() -> assertEquals(List.of("outer:->outside", "nested:->inside"), events), () -> assertEquals(expected, snapshotsByName(store.getVariableSnapshots())), () -> assertEquals(expected, readSnapshots(target)));
    }

    @Test
    void concurrentSetIfAbsentHasExactlyOneWinner() throws Exception {
        Path target = this.temporaryDirectory.resolve("user_variables.db");
        VariableStore store = createStore(target);
        int contenderCount = 12;
        CountDownLatch ready = new CountDownLatch(contenderCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Attempt>> attempts = new ArrayList<>();

        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            try {
                for (int contender = 0; contender < contenderCount; contender++) {
                    String value = "contender_" + contender;
                    attempts.add(executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        return new Attempt(store.setVariableIfAbsent("shared", value), value);
                    }));
                }
                boolean allTasksReady = ready.await(5L, TimeUnit.SECONDS);
                start.countDown();
                assertTrue(allTasksReady);
                List<Attempt> results = new ArrayList<>();
                for (Future<Attempt> attempt : attempts) results.add(attempt.get(10L, TimeUnit.SECONDS));
                List<Attempt> winners = results.stream().filter(Attempt::created).toList();
                assertAll(() -> assertEquals(1, winners.size()), () -> assertEquals(winners.get(0).value(), store.getVariableValue("shared")), () -> assertEquals(winners.get(0).value(), readSnapshots(target).get("shared").value()));
            } finally {
                start.countDown();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void immutableSnapshotsRemainStableAfterLaterMutation() {
        Path target = this.temporaryDirectory.resolve("user_variables.db");
        VariableStore store = createStore(target);
        store.setVariable("stable", "before");
        List<UserVariableSnapshot> before = store.getVariableSnapshots();

        store.setVariable("stable", "after");

        assertAll(() -> assertEquals("before", before.get(0).value()), () -> assertEquals("after", store.getVariableValue("stable")), () -> assertThrows(UnsupportedOperationException.class, () -> before.add(new UserVariableSnapshot("other", "value", false))));
    }

    @Test
    void unreadableExistingDatabaseDoesNotReplaceLiveStateOrRewriteFile() throws Exception {
        Path target = this.temporaryDirectory.resolve("user_variables.db");
        VariableStore store = createStore(target);
        store.setVariable("stable", "memory");
        String malformed = "this is not a variable database\n";
        Files.writeString(target, malformed, StandardCharsets.UTF_8);

        store.readFromFile();

        assertAll(() -> assertEquals(Map.of("stable", new UserVariableSnapshot("stable", "memory", false)), snapshotsByName(store.getVariableSnapshots())), () -> assertEquals(malformed, Files.readString(target, StandardCharsets.UTF_8)));
    }

    @Test
    void readFailureKeepsLiveStateAndExistingBytesUntouched() throws Exception {
        Path target = this.temporaryDirectory.resolve("user_variables.db");
        RecordingStoreFileOperations operations = new RecordingStoreFileOperations();
        VariableStore store = new VariableStore(new AtomicVariableDatabase(target, operations), (name, oldValue, newValue) -> {});
        store.setVariable("stable", "memory");
        String persisted = Files.readString(target, StandardCharsets.UTF_8);
        operations.readFailure = new IOException("simulated read failure");

        store.readFromFile();

        assertAll(() -> assertEquals("memory", store.getVariableValue("stable")), () -> assertEquals(persisted, Files.readString(target, StandardCharsets.UTF_8)));
    }

    @Test
    void indeterminateReadFailureIsNotMisclassifiedAsFirstCreation() throws Exception {
        Path target = Files.writeString(this.temporaryDirectory.resolve("user_variables.db"), "existing bytes must stay untouched", StandardCharsets.UTF_8);
        RecordingStoreFileOperations operations = new RecordingStoreFileOperations();
        operations.readFailure = new AccessDeniedException(target.toString());
        VariableStore store = new VariableStore(new AtomicVariableDatabase(target, operations), (name, oldValue, newValue) -> {});

        store.init();

        assertAll(() -> assertTrue(store.getVariableSnapshots().isEmpty()), () -> assertEquals("existing bytes must stay untouched", Files.readString(target, StandardCharsets.UTF_8)), () -> assertEquals(0, operations.writeCalls));
    }

    @Test
    void truncatedOrUnexpectedTypeDatabaseIsNeverPublishedOrRewritten() throws Exception {
        Path target = this.temporaryDirectory.resolve("user_variables.db");
        VariableStore store = createStore(target);
        store.setVariable("stable", "memory");
        Map<String, UserVariableSnapshot> expectedState = Map.of("stable", new UserVariableSnapshot("stable", "memory", false));

        String truncated = "type = user_variables\n\nvariable {\n  name = lost\n  value = incomplete\n";
        Files.writeString(target, truncated, StandardCharsets.UTF_8);
        store.readFromFile();
        assertAll(() -> assertEquals(expectedState, snapshotsByName(store.getVariableSnapshots())), () -> assertEquals(truncated, Files.readString(target, StandardCharsets.UTF_8)));

        String unexpectedType = "type = unrelated_database\n\nvariable {\n  name = foreign\n  value = data\n}\n";
        Files.writeString(target, unexpectedType, StandardCharsets.UTF_8);
        store.readFromFile();
        assertAll(() -> assertEquals(expectedState, snapshotsByName(store.getVariableSnapshots())), () -> assertEquals(unexpectedType, Files.readString(target, StandardCharsets.UTF_8)));
    }

    @Test
    void initializationResetsMarkedVariablesAndMigratesLegacyDatabase() throws Exception {
        Path modernTarget = this.temporaryDirectory.resolve("modern").resolve("user_variables.db");
        Files.createDirectories(modernTarget.getParent());
        Files.writeString(modernTarget, "type = user_variables\n\nvariable {\n  name = reset_me\n  value = old\n  reset_on_launch = true\n}\n\nvariable {\n  name = keep_me\n  value = Grüße\n  reset_on_launch = false\n}\n", StandardCharsets.UTF_8);
        VariableStore modernStore = createStore(modernTarget);

        modernStore.init();

        Map<String, UserVariableSnapshot> modern = snapshotsByName(modernStore.getVariableSnapshots());
        assertAll(() -> assertEquals(new UserVariableSnapshot("reset_me", "", true), modern.get("reset_me")), () -> assertEquals(new UserVariableSnapshot("keep_me", "Grüße", false), modern.get("keep_me")), () -> assertEquals(modern, readSnapshots(modernTarget)));

        Path legacyTarget = this.temporaryDirectory.resolve("legacy").resolve("user_variables.db");
        Files.createDirectories(legacyTarget.getParent());
        Files.writeString(legacyTarget, "type = cached_variables\n\nvariables {\n  legacy_name = legacy_value\n}\n", StandardCharsets.UTF_8);
        VariableStore legacyStore = createStore(legacyTarget);

        legacyStore.init();

        PropertyContainerSet migratedSet = PropertiesParser.deserializeSetFromFancyString(Files.readString(legacyTarget, StandardCharsets.UTF_8));
        assertAll(() -> assertNotNull(migratedSet), () -> assertEquals("user_variables", migratedSet.getType()), () -> assertEquals(Map.of("legacy_name", new UserVariableSnapshot("legacy_name", "legacy_value", false)), snapshotsByName(legacyStore.getVariableSnapshots())), () -> assertEquals(snapshotsByName(legacyStore.getVariableSnapshots()), readSnapshots(legacyTarget)));
    }

    @Test
    void failedBulkSerializationLeavesPreviousCompleteDatabaseAndPublishesNoSubset() throws Exception {
        Path target = this.temporaryDirectory.resolve("user_variables.db");
        AtomicBoolean failSerialization = new AtomicBoolean();
        VariableStore store = new VariableStore(new AtomicVariableDatabase(target), (name, oldValue, newValue) -> {}, snapshots -> {
            if (failSerialization.get()) throw new IllegalStateException("simulated serialization failure");
            return serializeStoredSnapshots(snapshots);
        });
        store.setVariable("old", "complete");
        String previousDatabase = Files.readString(target, StandardCharsets.UTF_8);
        failSerialization.set(true);

        store.replaceVariables(List.of(new UserVariableSnapshot("new_a", "one", false), new UserVariableSnapshot("new_b", "two", true)));

        Map<String, UserVariableSnapshot> expectedMemory = Map.of("new_a", new UserVariableSnapshot("new_a", "one", false), "new_b", new UserVariableSnapshot("new_b", "two", true));
        assertAll(() -> assertEquals(expectedMemory, snapshotsByName(store.getVariableSnapshots())), () -> assertEquals(previousDatabase, Files.readString(target, StandardCharsets.UTF_8)), () -> assertEquals(Map.of("old", new UserVariableSnapshot("old", "complete", false)), readSnapshots(target)), () -> assertEquals(List.of(target.getFileName().toString()), listFileNames(target.getParent())));
    }

    @Test
    void shutdownFlushesOnceAndMakesEveryLateMutationANoOp() throws Exception {
        Path target = this.temporaryDirectory.resolve("user_variables.db");
        RecordingStoreFileOperations operations = new RecordingStoreFileOperations();
        VariableStore store = new VariableStore(new AtomicVariableDatabase(target, operations), (name, oldValue, newValue) -> {});
        store.setVariable("kept", "value");
        Variable handle = store.getVariable("kept");
        assertNotNull(handle);
        int writesBeforeShutdown = operations.writeCalls;

        store.shutdown();
        store.shutdown();
        store.setVariable("late", "ignored");
        store.removeVariable("kept");
        store.clearVariables();
        handle.setValue("ignored");
        handle.toggleResetOnLaunch();
        store.replaceVariables(List.of(new UserVariableSnapshot("replacement", "ignored", false)));

        assertAll(() -> assertEquals(writesBeforeShutdown + 1, operations.writeCalls), () -> assertEquals(Map.of("kept", new UserVariableSnapshot("kept", "value", false)), snapshotsByName(store.getVariableSnapshots())), () -> assertEquals(Map.of("kept", new UserVariableSnapshot("kept", "value", false)), readSnapshots(target)), () -> assertFalse(store.setVariableIfAbsent("late", "ignored")), () -> assertNull(store.createVariableWithUniqueCopyName("kept", "ignored", false)));
    }

    @Test
    void shutdownWaitsForAdmittedMutationThenFlushesItBeforeClosingAdmission() throws Exception {
        Path target = this.temporaryDirectory.resolve("user_variables.db");
        BlockingWriteFileOperations operations = new BlockingWriteFileOperations();
        VariableStore store = new VariableStore(new AtomicVariableDatabase(target, operations), (name, oldValue, newValue) -> {});
        CountDownLatch shutdownTaskStarted = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            try {
                Future<?> mutation = executor.submit(() -> store.setVariable("admitted", "complete"));
                boolean mutationReachedWrite = operations.blockedWriteStarted.await(5L, TimeUnit.SECONDS);
                if (!mutationReachedWrite) operations.releaseBlockedWrite.countDown();
                assertTrue(mutationReachedWrite);
                Future<?> shutdown = executor.submit(() -> {
                    shutdownTaskStarted.countDown();
                    store.shutdown();
                });
                boolean shutdownAttempted = shutdownTaskStarted.await(5L, TimeUnit.SECONDS);
                boolean mutationWaited = !mutation.isDone();
                boolean shutdownWaited = !shutdown.isDone();
                operations.releaseBlockedWrite.countDown();
                assertAll(() -> assertTrue(shutdownAttempted), () -> assertTrue(mutationWaited), () -> assertTrue(shutdownWaited));
                mutation.get(10L, TimeUnit.SECONDS);
                shutdown.get(10L, TimeUnit.SECONDS);
            } finally {
                operations.releaseBlockedWrite.countDown();
            }
        } finally {
            executor.shutdownNow();
        }

        store.setVariable("late", "ignored");
        Map<String, UserVariableSnapshot> expected = Map.of("admitted", new UserVariableSnapshot("admitted", "complete", false));
        assertAll(() -> assertEquals(expected, snapshotsByName(store.getVariableSnapshots())), () -> assertEquals(expected, readSnapshots(target)), () -> assertEquals(3, operations.writeCalls.get()));
    }

    @Test
    void removedMutableHandleCannotResurrectOrCorruptCurrentMap() throws Exception {
        Path target = this.temporaryDirectory.resolve("user_variables.db");
        VariableStore store = createStore(target);
        store.setVariable("removed", "before");
        Variable staleHandle = store.getVariable("removed");
        assertNotNull(staleHandle);
        store.removeVariable("removed");

        staleHandle.setValue("stale");
        staleHandle.setResetOnLaunch(true);

        assertAll(() -> assertTrue(store.getVariableSnapshots().isEmpty()), () -> assertTrue(readSnapshots(target).isEmpty()), () -> assertEquals("stale", staleHandle.getValue()), () -> assertTrue(staleHandle.isResetOnLaunch()));
    }

    @NotNull
    private static VariableStore createStore(@NotNull Path target) {
        return new VariableStore(new AtomicVariableDatabase(target), (name, oldValue, newValue) -> {});
    }

    @NotNull
    private static List<UserVariableSnapshot> generation(int generation) {
        String value = Integer.toString(generation);
        return List.of(new UserVariableSnapshot("generation_a", value, false), new UserVariableSnapshot("generation_b", value, true));
    }

    @NotNull
    private static Map<String, UserVariableSnapshot> snapshotsByName(@NotNull List<UserVariableSnapshot> snapshots) {
        Map<String, UserVariableSnapshot> byName = new HashMap<>();
        for (UserVariableSnapshot snapshot : snapshots) byName.put(snapshot.name(), snapshot);
        return byName;
    }

    @NotNull
    private static Map<String, UserVariableSnapshot> readSnapshots(@NotNull Path target) throws IOException {
        PropertyContainerSet set = PropertiesParser.deserializeSetFromFancyString(Files.readString(target, StandardCharsets.UTF_8));
        assertNotNull(set);
        Map<String, UserVariableSnapshot> snapshots = new HashMap<>();
        for (PropertyContainer container : set.getContainersOfType("variable")) {
            String name = container.getValue("name");
            assertNotNull(name);
            snapshots.put(name, new UserVariableSnapshot(name, java.util.Objects.requireNonNullElse(container.getValue("value"), ""), "true".equals(container.getValue("reset_on_launch"))));
        }
        return snapshots;
    }

    @NotNull
    private static String serializeStoredSnapshots(@NotNull List<VariableStore.StoredVariableSnapshot> snapshots) {
        PropertyContainerSet set = new PropertyContainerSet("user_variables");
        for (VariableStore.StoredVariableSnapshot snapshot : snapshots) {
            PropertyContainer container = new PropertyContainer("variable");
            container.putProperty("name", snapshot.name());
            container.putProperty("value", snapshot.rawValue());
            container.putProperty("reset_on_launch", Boolean.toString(snapshot.resetOnLaunch()));
            set.putContainer(container);
        }
        return PropertiesParser.serializeSetToFancyString(set);
    }

    private static List<String> listFileNames(@NotNull Path directory) throws IOException {
        try (var paths = Files.list(directory)) {
            return paths.map(path -> path.getFileName().toString()).sorted().toList();
        }
    }

    private record Attempt(boolean created, @NotNull String value) {
    }

    private static final class RecordingStoreFileOperations implements AtomicVariableDatabase.FileOperations {

        private IOException readFailure;
        private int writeCalls;

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
            if (this.readFailure != null) throw this.readFailure;
            return Files.readString(path, StandardCharsets.UTF_8);
        }

        @Override
        public void writeUtf8AndForce(@NotNull Path path, @NotNull String value) throws IOException {
            this.writeCalls++;
            Files.writeString(path, value, StandardCharsets.UTF_8);
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

    private static final class BlockingWriteFileOperations implements AtomicVariableDatabase.FileOperations {

        private final CountDownLatch blockedWriteStarted = new CountDownLatch(1);
        private final CountDownLatch releaseBlockedWrite = new CountDownLatch(1);
        private final AtomicBoolean blockFirstWrite = new AtomicBoolean(true);
        private final AtomicInteger writeCalls = new AtomicInteger();

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
            this.writeCalls.incrementAndGet();
            if (this.blockFirstWrite.compareAndSet(true, false)) {
                this.blockedWriteStarted.countDown();
                try {
                    this.releaseBlockedWrite.await();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while simulating a blocked variable write", ex);
                }
            }
            Files.writeString(path, value, StandardCharsets.UTF_8);
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
