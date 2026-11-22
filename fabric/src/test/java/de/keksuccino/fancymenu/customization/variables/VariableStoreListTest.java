package de.keksuccino.fancymenu.customization.variables;

import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariableStoreListTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void listEditsCreateMissingVariablesAsLists() {
        VariableStore store = this.createStore();

        ListEditResult result = store.editList("fruits", entries -> VariableLists.add(entries, "apple", null, false, false));

        assertAll(() -> assertEquals(ListEditResult.CHANGED, result), () -> assertEquals(VariableType.LIST, store.getVariableType("fruits")), () -> assertEquals(List.of("apple"), store.getListEntries("fruits")),
                () -> assertEquals("[\"apple\"]", store.getVariableValue("fruits")));
    }

    @Test
    void listEditsTakeOverEmptyTextButNeverOverwriteText() {
        VariableStore store = this.createStore();
        store.setVariable("empty", "");
        store.setVariable("text", "hello");

        ListEditResult emptyResult = store.editList("empty", entries -> VariableLists.add(entries, "a", null, false, false));
        ListEditResult textResult = store.editList("text", entries -> VariableLists.add(entries, "a", null, false, false));

        assertAll(() -> assertEquals(ListEditResult.CHANGED, emptyResult), () -> assertEquals(List.of("a"), store.getListEntries("empty")), () -> assertEquals(ListEditResult.NOT_A_LIST, textResult),
                () -> assertEquals(VariableType.TEXT, store.getVariableType("text")), () -> assertEquals("hello", store.getVariableValue("text")), () -> assertNull(store.getListEntries("text")));
    }

    @Test
    void editsOfIndexesWithoutEntriesCreateAndConvertNothing() {
        VariableStore store = this.createStore();
        store.setVariable("empty", "");

        ListEditResult missingResult = store.editList("missing", entries -> VariableLists.set(entries, 0, "x"));
        ListEditResult emptyResult = store.editList("empty", entries -> VariableLists.removeAt(entries, -1));

        assertAll(() -> assertEquals(ListEditResult.INDEX_OUT_OF_RANGE, missingResult), () -> assertFalse(store.variableExists("missing")), () -> assertEquals(ListEditResult.INDEX_OUT_OF_RANGE, emptyResult),
                () -> assertEquals(VariableType.TEXT, store.getVariableType("empty")));
    }

    @Test
    void unchangedListEditsScheduleNoWrite() {
        ManualScheduler scheduler = new ManualScheduler();
        VariableStore store = this.createStore(scheduler, new RecordingListener(true));
        store.setListIfAbsent("list", List.of("a"));
        assertTrue(store.flush());
        int tasksBefore = scheduler.tasks.size();
        long revisionBefore = store.getRevision();

        ListEditResult result = store.editList("list", entries -> VariableLists.remove(entries, "missing", true, false));

        assertAll(() -> assertEquals(ListEditResult.UNCHANGED, result), () -> assertEquals(revisionBefore, store.getRevision()), () -> assertEquals(tasksBefore, scheduler.tasks.size()));
    }

    @Test
    void textAssignedToAListKeepsItAList() {
        VariableStore store = this.createStore();
        store.setListIfAbsent("list", List.of("a", "b"));

        store.setVariable("list", "[\"x\",\"y\"]");
        List<String> fromJson = store.getListEntries("list");
        store.setVariable("list", "sword");
        List<String> fromText = store.getListEntries("list");
        Variable variable = store.getVariable("list");
        assertNotNull(variable);
        variable.setValue("   ");

        assertAll(() -> assertEquals(List.of("x", "y"), fromJson), () -> assertEquals(List.of("sword"), fromText), () -> assertEquals(List.of(), store.getListEntries("list")), () -> assertEquals(VariableType.LIST, store.getVariableType("list")),
                () -> assertTrue(variable.isList()), () -> assertEquals("[]", variable.getValue()));
    }

    @Test
    void textVariablesNeverBecomeListsOnTheirOwn() {
        VariableStore store = this.createStore();

        store.setVariable("json_text", "[1, 2]");

        assertAll(() -> assertEquals(VariableType.TEXT, store.getVariableType("json_text")), () -> assertEquals("[1, 2]", store.getVariableValue("json_text")), () -> assertNull(store.getListEntries("json_text")));
    }

    @Test
    void listsPersistWithTheirTypeAndTextVariablesKeepTheOldFormat() throws Exception {
        Path target = this.temporaryDirectory.resolve("user_variables.db");
        VariableStore store = this.createStore(target);
        store.setVariable("name", "Steve");
        store.setListIfAbsent("fruits", List.of("apple", "line\nbreak", "{braces}"));

        assertTrue(store.flush());
        Map<String, PropertyContainer> containers = readContainers(target);
        VariableStore reloaded = this.createStore(target);
        reloaded.init();

        assertAll(() -> assertFalse(containers.get("name").hasProperty("type")), () -> assertEquals("Steve", containers.get("name").getValue("value")), () -> assertEquals("list", containers.get("fruits").getValue("type")),
                () -> assertEquals("[\"apple\",\"line\\nbreak\",\"{braces}\"]", containers.get("fruits").getValue("value")), () -> assertEquals(snapshotsByName(store.getVariableSnapshots()), snapshotsByName(reloaded.getVariableSnapshots())));
    }

    @Test
    void databasesOfOlderVersionsLoadEveryVariableAsText() throws Exception {
        Path target = this.temporaryDirectory.resolve("user_variables.db");
        Files.writeString(target, "type = user_variables\n\nvariable {\n  name = packed\n  value = [\"a\",\"b\"]\n  reset_on_launch = false\n}\n", StandardCharsets.UTF_8);
        VariableStore store = this.createStore(target);

        store.init();

        assertAll(() -> assertEquals(VariableType.TEXT, store.getVariableType("packed")), () -> assertEquals("[\"a\",\"b\"]", store.getVariableValue("packed")));
    }

    @Test
    void listsWithBrokenValuesOrUnknownTypesLoadAsTextWithoutLosingTheirValue() throws Exception {
        Path target = this.temporaryDirectory.resolve("user_variables.db");
        Files.writeString(target, "type = user_variables\n\nvariable {\n  name = broken\n  type = list\n  value = not json\n  reset_on_launch = false\n}\n\nvariable {\n  name = future\n  type = map\n  value = {}\n  reset_on_launch = false\n}\n", StandardCharsets.UTF_8);
        VariableStore store = this.createStore(target);

        store.init();

        assertAll(() -> assertEquals(VariableType.TEXT, store.getVariableType("broken")), () -> assertEquals("not json", store.getVariableValue("broken")), () -> assertEquals(VariableType.TEXT, store.getVariableType("future")),
                () -> assertEquals("{}", store.getVariableValue("future")));
    }

    @Test
    void clearOnLaunchEmptiesListsAndKeepsThemLists() throws Exception {
        Path target = this.temporaryDirectory.resolve("user_variables.db");
        Files.writeString(target, "type = user_variables\n\nvariable {\n  name = session\n  type = list\n  value = [\"a\"]\n  reset_on_launch = true\n}\n", StandardCharsets.UTF_8);
        VariableStore store = this.createStore(target);

        store.init();

        assertAll(() -> assertEquals(VariableType.LIST, store.getVariableType("session")), () -> assertEquals(List.of(), store.getListEntries("session")));
    }

    @Test
    void replacingVariablesWithSnapshotsRestoresListsExactly() {
        VariableStore store = this.createStore();
        List<UserVariableSnapshot> snapshots = List.of(UserVariableSnapshot.ofList("fruits", List.of("apple", "banana"), true), new UserVariableSnapshot("name", "Steve", false), UserVariableSnapshot.ofList("empty", List.of(), false));

        store.replaceVariables(snapshots);

        assertEquals(snapshotsByName(snapshots), snapshotsByName(store.getVariableSnapshots()));
    }

    @Test
    void copiesKeepTheTypeAndEntries() {
        VariableStore store = this.createStore();
        store.setListIfAbsent("fruits", List.of("apple"));
        Variable fruits = store.getVariable("fruits");
        assertNotNull(fruits);

        String copyName = store.createVariableWithUniqueCopyName(fruits.snapshot());
        String secondCopyName = store.createVariableWithUniqueCopyName(fruits.snapshot());

        assertAll(() -> assertEquals("fruits_Copy", copyName), () -> assertEquals("fruits_Copy2", secondCopyName), () -> assertEquals(List.of("apple"), store.getListEntries("fruits_Copy")));
    }

    @Test
    void explicitConversionsChangeTheTypeOfExistingVariablesOnly() {
        VariableStore store = this.createStore();
        store.setVariable("text", "a,b");
        store.setListIfAbsent("list", List.of("x", "y"));

        boolean toList = store.convertToList("text", List.of("a", "b"));
        boolean toText = store.convertToText("list", "x, y");
        boolean missing = store.convertToList("missing", List.of());

        assertAll(() -> assertTrue(toList), () -> assertEquals(List.of("a", "b"), store.getListEntries("text")), () -> assertTrue(toText), () -> assertEquals(VariableType.TEXT, store.getVariableType("list")),
                () -> assertEquals("x, y", store.getVariableValue("list")), () -> assertFalse(missing), () -> assertFalse(store.variableExists("missing")));
    }

    @Test
    void listEntriesStayTheSameImmutableInstanceUntilTheListChanges() {
        VariableStore store = this.createStore();
        store.setListIfAbsent("list", List.of("a"));

        List<String> first = store.getListEntries("list");
        List<String> second = store.getListEntries("list");
        store.editList("list", entries -> VariableLists.add(entries, "b", null, false, false));
        List<String> afterChange = store.getListEntries("list");

        assertAll(() -> assertSame(first, second), () -> assertNotSame(first, afterChange), () -> assertEquals(List.of("a"), first), () -> assertEquals(List.of("a", "b"), afterChange),
                () -> assertThrows(UnsupportedOperationException.class, () -> afterChange.add("c")));
    }

    @Test
    void listEditsOnlyBuildListenerValuesWhileSomebodyListens() {
        RecordingListener listener = new RecordingListener(false);
        VariableStore store = this.createStore(new ManualScheduler(), listener);

        store.editList("list", entries -> VariableLists.add(entries, "a", null, false, false));
        int whileNotListening = listener.updates.size();
        listener.listening.set(true);
        store.editList("list", entries -> VariableLists.add(entries, "b", null, false, false));

        assertAll(() -> assertEquals(0, whileNotListening), () -> assertEquals(List.of("list|[\"a\"]|[\"a\",\"b\"]"), listener.updates));
    }

    @Test
    void listenersGetOldValuesWithRealLineBreaks() {
        RecordingListener listener = new RecordingListener(true);
        VariableStore store = this.createStore(new ManualScheduler(), listener);
        store.setVariable("text", "first\nsecond");

        store.setVariable("text", "new");

        assertEquals("text|first\nsecond|new", listener.updates.get(1));
    }

    @Test
    void referencesResolveToTheTextFormOfLists() {
        VariableStore store = this.createStore();
        store.setListIfAbsent("fruits", List.of("apple", "banana"));

        assertEquals("Fruits: [\"apple\",\"banana\"]", store.resolveReferences("Fruits: $$fruits"));
    }

    @Test
    void arrangingThroughTheStoreChangesTheStoredOrder() {
        VariableStore store = this.createStore();
        store.setListIfAbsent("numbers", List.of("3", "1", "2"));

        ListEditResult result = store.editList("numbers", entries -> VariableLists.arrange(entries, ListArrangement.SORT_NUMBERS_ASCENDING, new Random(1)));

        assertAll(() -> assertEquals(ListEditResult.CHANGED, result), () -> assertEquals(List.of("1", "2", "3"), store.getListEntries("numbers")), () -> assertEquals("[\"1\",\"2\",\"3\"]", store.getVariableValue("numbers")));
    }

    @Test
    void shutDownStoresRejectListEdits() {
        VariableStore store = this.createStore();
        store.shutdown();

        assertAll(() -> assertEquals(ListEditResult.UNAVAILABLE, store.editList("list", entries -> VariableLists.add(entries, "a", null, false, false))), () -> assertFalse(store.setListIfAbsent("list", List.of("a"))),
                () -> assertFalse(store.variableExists("list")));
    }

    @NotNull
    private VariableStore createStore() {
        return this.createStore(this.temporaryDirectory.resolve("user_variables.db"));
    }

    @NotNull
    private VariableStore createStore(@NotNull Path target) {
        return new VariableStore(new AtomicVariableDatabase(target), (name, oldValue, newValue) -> {}, new ManualScheduler());
    }

    @NotNull
    private VariableStore createStore(@NotNull ManualScheduler scheduler, @NotNull RecordingListener listener) {
        return new VariableStore(new AtomicVariableDatabase(this.temporaryDirectory.resolve("user_variables.db")), listener, scheduler);
    }

    @NotNull
    private static Map<String, UserVariableSnapshot> snapshotsByName(@NotNull List<UserVariableSnapshot> snapshots) {
        Map<String, UserVariableSnapshot> byName = new HashMap<>();
        for (UserVariableSnapshot snapshot : snapshots) byName.put(snapshot.name(), snapshot);
        return byName;
    }

    @NotNull
    private static Map<String, PropertyContainer> readContainers(@NotNull Path target) throws Exception {
        PropertyContainerSet set = PropertiesParser.deserializeSetFromFancyString(Files.readString(target, StandardCharsets.UTF_8));
        assertNotNull(set);
        Map<String, PropertyContainer> containers = new HashMap<>();
        for (PropertyContainer container : set.getContainersOfType("variable")) containers.put(container.getValue("name"), container);
        return containers;
    }

    private static final class RecordingListener implements VariableStore.VariableUpdateListener {

        private final AtomicBoolean listening;
        private final List<String> updates = new ArrayList<>();

        private RecordingListener(boolean listening) {
            this.listening = new AtomicBoolean(listening);
        }

        @Override
        public void onVariableUpdated(@NotNull String name, @NotNull String oldValue, @NotNull String newValue) {
            this.updates.add(name + "|" + oldValue + "|" + newValue);
        }

        @Override
        public boolean isListening() {
            return this.listening.get();
        }

    }

    /** Never runs delayed writes on its own, so only explicit flushes write the database. */
    private static final class ManualScheduler implements VariablePersistenceCoordinator.Scheduler {

        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public synchronized @NotNull VariablePersistenceCoordinator.ScheduledTask schedule(@NotNull Runnable task, long delayMillis) {
            this.tasks.add(task);
            return () -> {};
        }

        @Override
        public synchronized void shutdown() {
            this.tasks.clear();
        }

    }

}
