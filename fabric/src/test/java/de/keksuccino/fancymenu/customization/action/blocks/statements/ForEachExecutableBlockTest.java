package de.keksuccino.fancymenu.customization.action.blocks.statements;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.ActionRegistry;
import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.ExecutableBlockDeserializer;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.action.ui.ActionScriptEntryMover;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.testing.TestPlaceholders;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The loops get their lists from a map instead of FancyMenu's variables, whose store writes to disk. Like the store, the map holds
 * immutable lists that get replaced when a list changes.
 */
@ResourceLock("PlaceholderParser global state")
class ForEachExecutableBlockTest {

    private static final long TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(WhileExecutableBlock.TIMEOUT_MILLIS);
    /** Whole milliseconds in nanoseconds are always divisible by 4, so contents taking this long run exactly 4 times per timeout. */
    private static final long QUARTER_TIMEOUT_NANOS = TIMEOUT_NANOS / 4L;
    /** System.nanoTime() has no fixed origin, so the tests don't start the clock at 0. */
    private static final long START_NANOS = 123_456_789L;
    /** Registered, so actions of serialized scripts can get deserialized. The registry has no removal API, so it stays registered for the whole test JVM. */
    private static final RecordingAction RECORDER = registerRecorder();

    private final Map<String, List<String>> lists = new HashMap<>();
    private final AtomicInteger listReads = new AtomicInteger();
    private final AtomicLong now = new AtomicLong(START_NANOS);
    private PlaceholderParser.PlaceholderCachingController originalCachingController;

    @BeforeEach
    void setUp() {
        this.originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
        RECORDER.values.clear();
        MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
    }

    @AfterEach
    void tearDown() {
        PlaceholderParser.setPlaceholderCachingController(this.originalCachingController);
        MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        assertNull(ForEachExecutableBlock.captureLoopValues(), "Values of a loop are still there after it ended");
    }

    @Test
    void contentsRunOnceForEveryEntryInOrderWithTheEntryAndItsIndex() {
        this.lists.put("fruits", List.of("apple", "banana", "cherry"));
        ForEachExecutableBlock loop = this.loop("fruits");
        loop.addExecutable(record("$$list_index:$$list_entry"));

        loop.execute();

        assertEquals(List.of("0:apple", "1:banana", "2:cherry"), RECORDER.values);
    }

    @Test
    void editingTheListInsideTheLoopDoesNotChangeTheEntriesItGoesThrough() {
        this.lists.put("fruits", List.of("apple", "banana"));
        ForEachExecutableBlock loop = this.loop("fruits");
        loop.addExecutable(record("$$list_entry"));
        // Like a list action in the loop, which gives the list other entries
        loop.addExecutable(new RunnableExecutable(() -> this.lists.put("fruits", List.of("x", "y", "z"))));

        loop.execute();
        assertEquals(List.of("apple", "banana"), RECORDER.values);
        assertEquals(1, this.listReads.get());

        // The next run goes through the list as it is then
        loop.execute();
        assertEquals(List.of("apple", "banana", "x", "y", "z"), RECORDER.values);
    }

    @Test
    void missingAndTextVariablesEmptyListsAndEmptyNamesRunNothing() {
        // The variables give no entries for missing variables and text variables
        this.lists.put("empty", List.of());
        for (String name : new String[] {"missing", "empty", "", "   "}) {
            ForEachExecutableBlock loop = this.loop(name);
            loop.addExecutable(record("$$list_entry"));
            loop.execute();
        }

        assertTrue(RECORDER.values.isEmpty());
        // Empty names don't even get looked up
        assertEquals(2, this.listReads.get());
    }

    @Test
    void listNameGetsResolvedEveryTimeTheLoopStarts() {
        String key = "for_each_list_name";
        this.lists.put("fruits", List.of("apple"));
        this.lists.put("vegetables", List.of("carrot"));
        ForEachExecutableBlock loop = this.loop(TestPlaceholders.value(key));
        loop.addExecutable(record("$$list_entry"));

        TestPlaceholders.setValue(key, "fruits");
        loop.execute();
        // Whitespace around the resolved name doesn't count, like for all variable names
        TestPlaceholders.setValue(key, " vegetables ");
        loop.execute();

        assertEquals(List.of("apple", "carrot"), RECORDER.values);
    }

    @Test
    void innerLoopValuesWinForItsContentsAndTheOuterValuesComeBackAfterIt() {
        this.lists.put("fruits", List.of("apple", "banana"));
        this.lists.put("numbers", List.of("one", "two"));
        ForEachExecutableBlock outer = this.loop("fruits");
        ForEachExecutableBlock inner = this.loop("numbers");
        // The inner loop gets added before its contents, so its contents get the value placeholders of both loops
        outer.addExecutable(inner);
        inner.addExecutable(record("inner $$list_index:$$list_entry"));
        outer.addExecutable(record("outer $$list_index:$$list_entry"));

        outer.execute();

        assertEquals(List.of("inner 0:one", "inner 1:two", "outer 0:apple", "inner 0:one", "inner 1:two", "outer 1:banana"), RECORDER.values);
    }

    @Test
    void innerLoopCanGoThroughTheListThatTheOuterEntryNames() {
        this.lists.put("groups", List.of("fruits", "vegetables"));
        this.lists.put("fruits", List.of("apple"));
        this.lists.put("vegetables", List.of("carrot", "leek"));
        ForEachExecutableBlock inner = this.loop("$$list_entry");
        inner.addExecutable(record("$$list_entry"));
        ForEachExecutableBlock outer = this.loop("groups");
        outer.addExecutable(inner);

        outer.execute();

        assertEquals(List.of("apple", "carrot", "leek"), RECORDER.values);
    }

    @Test
    void valuePlaceholdersFromOutsideKeepWorkingInsideTheLoop() {
        this.lists.put("fruits", List.of("apple", "banana"));
        ForEachExecutableBlock loop = this.loop("fruits");
        loop.addExecutable(record("$$pressed_key/$$list_entry"));
        GenericExecutableBlock script = new GenericExecutableBlock();
        script.addExecutable(loop);
        // Like listeners, which add their values after the script got built
        script.addValuePlaceholder("pressed_key", () -> "K");

        script.execute();

        assertEquals(List.of("K/apple", "K/banana"), RECORDER.values);
    }

    @Test
    void valuePlaceholdersFromOutsideWithTheNamesOfTheLoopValuesDoNotReachTheContents() {
        this.lists.put("fruits", List.of("apple", "banana"));
        ForEachExecutableBlock loop = this.loop("fruits");
        loop.addExecutable(record("$$list_entry"));
        GenericExecutableBlock script = new GenericExecutableBlock();
        script.addExecutable(record("$$list_entry"));
        script.addExecutable(loop);
        script.addValuePlaceholder("list_entry", () -> "outside");

        script.execute();

        assertEquals(List.of("outside", "apple", "banana"), RECORDER.values);
    }

    @Test
    void referencesStayAsTheyAreWhenNoLoopRuns() {
        this.lists.put("fruits", List.of("apple"));
        ForEachExecutableBlock loop = this.loop("fruits");
        ActionInstance inside = record("$$list_entry|$$list_index");
        loop.addExecutable(inside);

        // A copy of an action that got copied out of the loop still has the value placeholders of the loop
        inside.copy(true).execute();

        assertEquals(List.of("$$list_entry|$$list_index"), RECORDER.values);
    }

    @Test
    void actionMovedIntoALoopInTheEditorGetsTheLoopValues() {
        this.lists.put("fruits", List.of("apple", "banana"));
        ForEachExecutableBlock loop = this.loop("fruits");
        ActionInstance action = record("$$list_entry");
        GenericExecutableBlock script = new GenericExecutableBlock();
        script.addExecutable(loop).addExecutable(action);

        // Moving up enters the expanded loop above
        assertTrue(ActionScriptEntryMover.moveUp(script, action, () -> {}));
        assertSame(action, loop.getExecutables().get(0));
        script.execute();

        assertEquals(List.of("apple", "banana"), RECORDER.values);
    }

    @Test
    void runStopsAtTheTimeoutAndTheNextRunGetsTheFullTimeoutAgain() {
        List<String> numbers = new ArrayList<>();
        for (int i = 0; i < 10; i++) numbers.add("entry " + i);
        this.lists.put("numbers", List.copyOf(numbers));
        ForEachExecutableBlock loop = this.loop("numbers");
        loop.addExecutable(record("$$list_index"));
        loop.addExecutable(new RunnableExecutable(() -> this.now.addAndGet(QUARTER_TIMEOUT_NANOS)));

        loop.execute();
        assertEquals(List.of("0", "1", "2", "3"), RECORDER.values);

        loop.execute();
        assertEquals(List.of("0", "1", "2", "3", "0", "1", "2", "3"), RECORDER.values);
    }

    @Test
    void outerLoopValuesComeBackWhenTheContentsOfAnInnerLoopThrow() {
        this.lists.put("fruits", List.of("apple", "banana"));
        this.lists.put("numbers", List.of("one", "two"));
        ForEachExecutableBlock inner = this.loop("numbers");
        inner.addExecutable(new RunnableExecutable(() -> {
            throw new TestError();
        }));
        ForEachExecutableBlock outer = this.loop("fruits");
        outer.addExecutable(new RunnableExecutable(() -> assertThrows(TestError.class, inner::execute)));
        outer.addExecutable(record("$$list_index:$$list_entry"));

        outer.execute();
        assertEquals(List.of("0:apple", "1:banana"), RECORDER.values);

        assertThrows(TestError.class, inner::execute);
    }

    @Test
    void capturedLoopValuesWorkAfterTheLoopMovedOn() {
        this.lists.put("fruits", List.of("apple", "banana"));
        List<ForEachExecutableBlock.LoopValues> captured = new ArrayList<>();
        ForEachExecutableBlock loop = this.loop("fruits");
        loop.addExecutable(new RunnableExecutable(() -> captured.add(ForEachExecutableBlock.captureLoopValues())));
        ActionInstance action = record("$$list_index:$$list_entry");
        loop.addExecutable(action);
        loop.execute();
        RECORDER.values.clear();

        ForEachExecutableBlock.runWithLoopValues(captured.get(1), action::execute);
        ForEachExecutableBlock.runWithLoopValues(captured.get(0), action::execute);
        ForEachExecutableBlock.runWithLoopValues(null, action::execute);

        assertEquals(List.of("1:banana", "0:apple", "$$list_index:$$list_entry"), RECORDER.values);
    }

    @Test
    void executeLaterBlocksInsideTheLoopRunTheirContentsWithTheEntryOfWhenTheyGotScheduled() throws InterruptedException {
        this.lists.put("fruits", List.of("apple", "banana"));
        ExecuteLaterExecutableBlock later = new ExecuteLaterExecutableBlock("0");
        later.addExecutable(record("$$list_index:$$list_entry"));
        ForEachExecutableBlock loop = this.loop("fruits");
        loop.addExecutable(later);

        loop.execute();
        assertTrue(RECORDER.values.isEmpty());
        // The scheduler thread hands the scheduled contents to the main thread queue, which the game runs after every client tick.
        // Other tests can leave tasks of their own in that queue, so the queue gets run until both scheduled contents ran.
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while ((RECORDER.values.size() < 2) && (System.nanoTime() < deadline)) {
            MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK).forEach(Runnable::run);
            if (RECORDER.values.size() < 2) Thread.sleep(5);
        }

        // The scheduler can hand them over in any order
        assertEquals(List.of("0:apple", "1:banana"), RECORDER.values.stream().sorted().toList());
    }

    @Test
    void copiesKeepTheListTheSettingsAndTheContentsAndRunTheSame() {
        this.lists.put("fruits", List.of("apple", "banana"));
        this.lists.put("numbers", List.of("one"));
        ForEachExecutableBlock loop = this.loop("fruits");
        loop.setCollapsed(true);
        ForEachExecutableBlock inner = this.loop("numbers");
        inner.addExecutable(record("inner $$list_entry"));
        loop.addExecutable(record("$$list_index:$$list_entry"));
        loop.addExecutable(inner);

        ForEachExecutableBlock unique = loop.copy(true);
        ForEachExecutableBlock same = loop.copy(false);

        assertNotEquals(loop.getIdentifier(), unique.getIdentifier());
        assertEquals(loop.getIdentifier(), same.getIdentifier());
        for (ForEachExecutableBlock copy : List.of(unique, same)) {
            assertEquals("fruits", copy.getListVariableName());
            assertTrue(copy.isCollapsed());
            assertEquals(2, copy.getExecutables().size());
            assertNotSame(loop.getExecutables().get(0), copy.getExecutables().get(0));
        }
        loop.execute();
        List<String> originalValues = List.copyOf(RECORDER.values);
        RECORDER.values.clear();
        unique.execute();
        assertEquals(originalValues, RECORDER.values);
        assertEquals(List.of("0:apple", "inner one", "1:banana", "inner one"), originalValues);
    }

    @Test
    void serializedLoopsLoadWithTheirListTheirSettingsAndTheirContents() {
        ForEachExecutableBlock outer = new ForEachExecutableBlock(" fruits ");
        outer.setCollapsed(true);
        ForEachExecutableBlock inner = new ForEachExecutableBlock("{\"placeholder\":\"some_placeholder\"} | $$list_entry");
        inner.addExecutable(record("inner $$list_entry"));
        outer.addExecutable(inner);
        outer.addExecutable(record("after"));
        GenericExecutableBlock script = new GenericExecutableBlock();
        script.addExecutable(outer);
        PropertyContainerSet set = new PropertyContainerSet("test_script");
        set.putContainer(script.serialize());
        PropertyContainerSet loadedSet = PropertiesParser.deserializeSetFromFancyString(PropertiesParser.serializeSetToFancyString(set));
        assertNotNull(loadedSet);
        PropertyContainer serialized = loadedSet.getContainers().get(0);

        AbstractExecutableBlock loaded = ExecutableBlockDeserializer.deserializeWithIdentifier(serialized, script.getIdentifier());

        assertNotNull(loaded);
        ForEachExecutableBlock loadedOuter = assertInstanceOf(ForEachExecutableBlock.class, loaded.getExecutables().get(0));
        assertEquals("for-each", loadedOuter.getBlockType());
        assertEquals(outer.getIdentifier(), loadedOuter.getIdentifier());
        assertEquals("fruits", loadedOuter.getListVariableName());
        assertTrue(loadedOuter.isCollapsed());
        assertEquals(2, loadedOuter.getExecutables().size());
        ForEachExecutableBlock loadedInner = assertInstanceOf(ForEachExecutableBlock.class, loadedOuter.getExecutables().get(0));
        assertEquals(inner.getListVariableName(), loadedInner.getListVariableName());
        assertFalse(loadedInner.isCollapsed());
        ActionInstance loadedInnerAction = assertInstanceOf(ActionInstance.class, loadedInner.getExecutables().get(0));
        assertEquals("inner $$list_entry", loadedInnerAction.value);
        assertEquals("after", assertInstanceOf(ActionInstance.class, loadedOuter.getExecutables().get(1)).value);
        assertEquals(script.serialize().getProperties(), loaded.serialize().getProperties());

        // The loaded contents get the values of the loop around them
        this.lists.put("fruits", List.of("apple"));
        List<ForEachExecutableBlock.LoopValues> captured = new ArrayList<>();
        ForEachExecutableBlock capturing = this.loop("fruits");
        capturing.addExecutable(new RunnableExecutable(() -> captured.add(ForEachExecutableBlock.captureLoopValues())));
        capturing.execute();
        ForEachExecutableBlock.runWithLoopValues(captured.get(0), loadedInnerAction::execute);
        assertEquals(List.of("inner apple"), RECORDER.values);
    }

    @Test
    void loopsWithoutListOrCollapseStateLoadWithDefaults() {
        ForEachExecutableBlock loop = new ForEachExecutableBlock("fruits");
        loop.setCollapsed(true);
        loop.addExecutable(record("inside"));
        PropertyContainer serialized = loop.serialize();
        serialized.removeProperty("[for_each_executable_block_list:" + loop.getIdentifier() + "]");
        serialized.removeProperty("[for_each_executable_block_collapsed:" + loop.getIdentifier() + "]");

        AbstractExecutableBlock loaded = ExecutableBlockDeserializer.deserializeWithIdentifier(serialized, loop.getIdentifier());

        ForEachExecutableBlock loadedLoop = assertInstanceOf(ForEachExecutableBlock.class, loaded);
        assertEquals("", loadedLoop.getListVariableName());
        assertFalse(loadedLoop.isCollapsed());
        assertEquals(1, loadedLoop.getExecutables().size());
    }

    @NotNull
    private ForEachExecutableBlock loop(@NotNull String listVariableName) {
        ForEachExecutableBlock loop = new ForEachExecutableBlock(this.now::get, name -> {
            this.listReads.incrementAndGet();
            return this.lists.get(name);
        });
        loop.setListVariableName(listVariableName);
        return loop;
    }

    @NotNull
    private static ActionInstance record(@NotNull String value) {
        return new ActionInstance(RECORDER, value);
    }

    @NotNull
    private static RecordingAction registerRecorder() {
        if (ActionRegistry.getAction(RecordingAction.IDENTIFIER) instanceof RecordingAction registered) return registered;
        RecordingAction recorder = new RecordingAction();
        ActionRegistry.register(recorder);
        return recorder;
    }

    private static final class TestError extends Error {
    }

    /** Records the values it gets, with all placeholders resolved. */
    private static final class RecordingAction extends Action {

        private static final String IDENTIFIER = "fancymenu_test_for_each_recording_action";

        private final List<String> values = new ArrayList<>();

        private RecordingAction() {
            super(IDENTIFIER);
        }

        @Override
        public boolean hasValue() {
            return true;
        }

        @Override
        public void execute(@Nullable String value) {
            this.values.add(value);
        }

        // The default check needs a running Minecraft client to compare threads.
        @Override
        public boolean checkAsync() {
            return true;
        }

        @Override
        public Component getDisplayName() {
            return Component.empty();
        }

        @Override
        public Component getDescription() {
            return Component.empty();
        }

        @Override
        public Component getValueDisplayName() {
            return Component.empty();
        }

        @Override
        public String getValuePreset() {
            return "";
        }

    }

    private static final class RunnableExecutable implements Executable {

        @NotNull
        private final Runnable task;
        @NotNull
        private final String identifier = UUID.randomUUID().toString();

        private RunnableExecutable(@NotNull Runnable task) {
            this.task = task;
        }

        @Override
        public void execute() {
            this.task.run();
        }

        @Override
        public @NotNull String getIdentifier() {
            return this.identifier;
        }

        @Override
        public @NotNull Executable copy(boolean unique) {
            return new RunnableExecutable(this.task);
        }

        @Override
        public @NotNull PropertyContainer serialize() {
            return new PropertyContainer("for_each_test_executable");
        }

    }

}
