package de.keksuccino.fancymenu.customization.action.blocks.statements;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.ExecutableBlockDeserializer;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("PlaceholderParser global state")
class DelayExecutableBlockTest {

    private static final String DELAY_GATE_DESCRIPTION = "Adds a Delay Gate block. On the first reach, it starts a one-time timer and skips its contained actions. The surrounding script continues immediately.\n\nThis block does not schedule an execution. Its timer is checked only when the script reaches the same block instance again. Reaches before the timer expires skip the contained actions. The first reach at or after expiry opens the gate and runs them; every later reach runs them immediately.\n\nDelay time is in milliseconds. A resolved delay of 0 or less, including invalid values, opens the gate immediately.\n\nThe gate resets only when that action-script instance is reconstructed or replaced.\n\nUse Execute Later instead when every reach should schedule a fresh delayed execution.";
    private static PlaceholderParser.PlaceholderCachingController originalPlaceholderCachingController;

    @BeforeAll
    static void disablePlaceholderCaching() {
        originalPlaceholderCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
    }

    @AfterAll
    static void restorePlaceholderCaching() {
        PlaceholderParser.setPlaceholderCachingController(originalPlaceholderCachingController);
    }

    @Test
    void englishTextNamesAndFullyDescribesThePollingGate() throws IOException {
        JsonObject localizations = readEnglishLocalizations();

        assertEquals("Add DELAY GATE Block", localizations.get("fancymenu.actions.blocks.add.delay").getAsString());
        assertEquals(DELAY_GATE_DESCRIPTION, localizations.get("fancymenu.actions.blocks.add.delay.desc").getAsString());
        assertEquals("DELAY GATE (%s ms)", localizations.get("fancymenu.actions.blocks.delay").getAsString());
        assertEquals("Edit Delay Gate (ms)", localizations.get("fancymenu.actions.blocks.delay.edit").getAsString());
        assertTrue(DELAY_GATE_DESCRIPTION.contains("Use Execute Later instead"));
    }

    @Test
    void localizedRenameKeepsSerializedTypeAndKeysBackwardCompatible() {
        DelayExecutableBlock block = new DelayExecutableBlock("25");
        block.setCollapsed(true);

        PropertyContainer serialized = block.serialize();
        String identifier = block.getIdentifier();

        assertEquals("delay", block.getBlockType());
        assertTrue(serialized.hasProperty("[executable_block:" + identifier + "][type:delay]"));
        assertEquals("25", serialized.getValue("[delay_executable_block_value:" + identifier + "]"));
        assertEquals("true", serialized.getValue("[delay_executable_block_collapsed:" + identifier + "]"));
        AbstractExecutableBlock restored = ExecutableBlockDeserializer.deserializeWithIdentifier(serialized, identifier);
        assertInstanceOf(DelayExecutableBlock.class, restored);
    }

    @Test
    void firstReachStartsTimerWhileSurroundingScriptContinuesAndLaterReachesPollIt() {
        AtomicLong now = new AtomicLong(10_000L);
        List<String> executions = new ArrayList<>();
        GenericExecutableBlock script = new GenericExecutableBlock();
        DelayExecutableBlock gate = new DelayExecutableBlock(now::get);
        gate.setDelayMs("100");
        gate.addExecutable(new RecordingExecutable(executions, "inside"));
        script.addExecutable(new RecordingExecutable(executions, "before"));
        script.addExecutable(gate);
        script.addExecutable(new RecordingExecutable(executions, "after"));

        script.execute();
        assertEquals(List.of("before", "after"), executions);

        executions.clear();
        now.set(10_099L);
        script.execute();
        assertEquals(List.of("before", "after"), executions);

        executions.clear();
        now.set(10_100L);
        script.execute();
        assertEquals(List.of("before", "inside", "after"), executions);

        executions.clear();
        script.execute();
        assertEquals(List.of("before", "inside", "after"), executions);
    }

    @Test
    void overlappingGateInstancesTrackTheirTimersAndOpenStateIndependently() {
        AtomicLong now = new AtomicLong(0L);
        AtomicInteger firstExecutions = new AtomicInteger();
        AtomicInteger secondExecutions = new AtomicInteger();
        DelayExecutableBlock first = delayBlock("100", now, firstExecutions);
        DelayExecutableBlock second = delayBlock("200", now, secondExecutions);

        first.execute();
        second.execute();
        assertEquals(0, firstExecutions.get());
        assertEquals(0, secondExecutions.get());

        now.set(100L);
        first.execute();
        second.execute();
        assertEquals(1, firstExecutions.get());
        assertEquals(0, secondExecutions.get());

        now.set(199L);
        first.execute();
        second.execute();
        assertEquals(2, firstExecutions.get());
        assertEquals(0, secondExecutions.get());

        now.set(200L);
        first.execute();
        second.execute();
        assertEquals(3, firstExecutions.get());
        assertEquals(1, secondExecutions.get());
    }

    @Test
    void zeroNegativeInvalidAndNumericOverflowDelaysOpenImmediatelyWithoutReadingClock() {
        for (String delay : List.of("0", "-1", "invalid", "9223372036854775808")) {
            AtomicInteger executions = new AtomicInteger();
            DelayExecutableBlock gate = new DelayExecutableBlock(() -> { throw new AssertionError("An immediately open gate must not read the clock"); });
            gate.setDelayMs(delay);
            gate.addExecutable(new CountingExecutable(executions));

            gate.execute();

            assertEquals(1, executions.get(), "Delay value should open immediately: " + delay);
        }
    }

    @Test
    void deadlineCalculationSaturatesOnOverflowAndUsesInclusiveBoundary() {
        AtomicLong now = new AtomicLong(Long.MAX_VALUE - 5L);
        OneTimeDelayGate gate = new OneTimeDelayGate(now::get);

        assertFalse(gate.shouldExecute(10L));
        now.set(Long.MAX_VALUE - 1L);
        assertFalse(gate.shouldExecute(10L));
        now.set(Long.MAX_VALUE);
        assertTrue(gate.shouldExecute(10L));
        assertTrue(gate.shouldExecute(10L));
    }

    @Test
    void firstPositiveResolutionFixesDeadlineAndNonPositiveResolutionOpensPermanently() {
        AtomicLong now = new AtomicLong(100L);
        AtomicLong resolvedDelay = new AtomicLong(50L);
        AtomicInteger executions = new AtomicInteger();
        MutableDelayExecutableBlock fixedDeadlineGate = new MutableDelayExecutableBlock(now, resolvedDelay);
        fixedDeadlineGate.addExecutable(new CountingExecutable(executions));

        fixedDeadlineGate.execute();
        resolvedDelay.set(500L);
        now.set(149L);
        fixedDeadlineGate.execute();
        assertEquals(0, executions.get());
        now.set(150L);
        fixedDeadlineGate.execute();
        assertEquals(1, executions.get());
        resolvedDelay.set(1000L);
        fixedDeadlineGate.execute();
        assertEquals(2, executions.get());

        AtomicLong secondDelay = new AtomicLong(50L);
        MutableDelayExecutableBlock immediatelyOpenedGate = new MutableDelayExecutableBlock(now, secondDelay);
        immediatelyOpenedGate.addExecutable(new CountingExecutable(executions));
        immediatelyOpenedGate.execute();
        secondDelay.set(0L);
        immediatelyOpenedGate.execute();
        secondDelay.set(1000L);
        immediatelyOpenedGate.execute();
        assertEquals(4, executions.get());
    }

    @Test
    void copyingAnOpenBlockCreatesAFreshClosedGateWithTheSameSerializedIdentity() {
        AtomicLong now = new AtomicLong(100L);
        AtomicInteger executions = new AtomicInteger();
        DelayExecutableBlock original = delayBlock("10", now, executions);

        original.execute();
        now.set(110L);
        original.execute();
        DelayExecutableBlock copy = original.copy(false);

        assertEquals(original.getIdentifier(), copy.getIdentifier());
        original.execute();
        copy.execute();
        assertEquals(2, executions.get());

        now.set(120L);
        copy.execute();
        assertEquals(3, executions.get());
    }

    @Test
    void serializedRuntimeStateIsDiscardedWhenTheBlockIsReconstructed() {
        AtomicInteger executions = new AtomicInteger();
        DelayExecutableBlock original = new DelayExecutableBlock("0");
        original.addExecutable(new CountingExecutable(executions));
        original.execute();
        original.setDelayMs(Long.toString(Long.MAX_VALUE));
        original.setCollapsed(true);
        original.execute();
        assertEquals(2, executions.get());

        PropertyContainer serialized = original.serialize();
        DelayExecutableBlock restored = DelayExecutableBlock.deserializeEmptyWithIdentifier(serialized, original.getIdentifier());
        restored.addExecutable(new CountingExecutable(executions));
        restored.execute();

        assertEquals(2, executions.get());
        assertEquals(Long.toString(Long.MAX_VALUE), restored.getDelayMsRaw());
        assertTrue(restored.isCollapsed());
    }

    private static DelayExecutableBlock delayBlock(@NotNull String delay, @NotNull AtomicLong now, @NotNull AtomicInteger executions) {
        DelayExecutableBlock block = new DelayExecutableBlock(now::get);
        block.setDelayMs(delay);
        block.addExecutable(new CountingExecutable(executions));
        return block;
    }

    private static JsonObject readEnglishLocalizations() throws IOException {
        try (InputStream stream = DelayExecutableBlockTest.class.getResourceAsStream("/assets/fancymenu/lang/en_us.json")) {
            assertNotNull(stream, "English localization resource is missing from the test runtime classpath");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static class RecordingExecutable implements Executable {

        @NotNull
        private final List<String> executions;
        @NotNull
        private final String value;
        @NotNull
        private final String identifier;

        private RecordingExecutable(@NotNull List<String> executions, @NotNull String value) {
            this(executions, value, UUID.randomUUID().toString());
        }

        private RecordingExecutable(@NotNull List<String> executions, @NotNull String value, @NotNull String identifier) {
            this.executions = executions;
            this.value = value;
            this.identifier = identifier;
        }

        @Override
        public void execute() {
            this.executions.add(this.value);
        }

        @Override
        public @NotNull String getIdentifier() {
            return this.identifier;
        }

        @Override
        public @NotNull Executable copy(boolean unique) {
            return new RecordingExecutable(this.executions, this.value, unique ? UUID.randomUUID().toString() : this.identifier);
        }

        @Override
        public @NotNull PropertyContainer serialize() {
            return new PropertyContainer("recording_test_executable");
        }
    }

    private static final class CountingExecutable implements Executable {

        @NotNull
        private final AtomicInteger executions;
        @NotNull
        private final String identifier;

        private CountingExecutable(@NotNull AtomicInteger executions) {
            this(executions, UUID.randomUUID().toString());
        }

        private CountingExecutable(@NotNull AtomicInteger executions, @NotNull String identifier) {
            this.executions = executions;
            this.identifier = identifier;
        }

        @Override
        public void execute() {
            this.executions.incrementAndGet();
        }

        @Override
        public @NotNull String getIdentifier() {
            return this.identifier;
        }

        @Override
        public @NotNull Executable copy(boolean unique) {
            return new CountingExecutable(this.executions, unique ? UUID.randomUUID().toString() : this.identifier);
        }

        @Override
        public @NotNull PropertyContainer serialize() {
            return new PropertyContainer("counting_test_executable");
        }
    }

    private static final class MutableDelayExecutableBlock extends DelayExecutableBlock {

        @NotNull
        private final AtomicLong resolvedDelay;

        private MutableDelayExecutableBlock(@NotNull AtomicLong now, @NotNull AtomicLong resolvedDelay) {
            super(now::get);
            this.resolvedDelay = resolvedDelay;
        }

        @Override
        public long resolveDelayMs() {
            return this.resolvedDelay.get();
        }
    }

}
