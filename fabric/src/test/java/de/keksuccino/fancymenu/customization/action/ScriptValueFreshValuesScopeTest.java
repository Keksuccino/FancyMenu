package de.keksuccino.fancymenu.customization.action;

import de.keksuccino.fancymenu.customization.FreshValuesScope;
import de.keksuccino.fancymenu.customization.action.blocks.statements.DelayExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ExecuteLaterExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ForEachExecutableBlock;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;
import de.keksuccino.fancymenu.testing.TestPlaceholders;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Values that action scripts parse must get parsed inside a {@link FreshValuesScope}, so the placeholder parser
 * never answers them from its result cache.
 */
@ResourceLock("PlaceholderParser global state")
class ScriptValueFreshValuesScopeTest {

    private static final String PROBE_IDENTIFIER = "fancymenu_test_fresh_values_scope_probe";
    /** Gets replaced with 1 when it gets parsed inside a fresh-values scope, otherwise with 0. */
    private static final String PROBE = "{\"placeholder\":\"" + PROBE_IDENTIFIER + "\"}";

    private PlaceholderParser.PlaceholderCachingController originalCachingController;

    @BeforeAll
    static void registerProbe() {
        // The registry has no removal API, so the probe stays registered for the whole test JVM
        if (PlaceholderRegistry.getPlaceholder(PROBE_IDENTIFIER) == null) PlaceholderRegistry.register(new ScopeProbePlaceholder());
    }

    @BeforeEach
    void disableCaching() {
        // Results of the probe must not come from the result cache of earlier parses
        this.originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
    }

    @AfterEach
    void restoreCachingAndCheckThatTheScopeIsClosed() {
        PlaceholderParser.setPlaceholderCachingController(this.originalCachingController);
        assertFalse(FreshValuesScope.isActive());
    }

    @Test
    void probeIsOnlyReplacedWithOneInsideTheScope() {
        assertEquals("0", PlaceholderParser.replacePlaceholders(PROBE));
        try (FreshValuesScope ignored = FreshValuesScope.enter()) {
            assertEquals("1", PlaceholderParser.replacePlaceholders(PROBE));
        }
    }

    @Test
    void actionValuesAndTheActionItselfReadFreshValues() {
        for (boolean rawValue : new boolean[] {false, true}) {
            RecordingAction action = new RecordingAction(rawValue, null);

            new ActionInstance(action, PROBE).execute();

            assertEquals("1", action.lastValue, "Raw value: " + rawValue);
            assertEquals("1", action.probeWhileExecuting, "Raw value: " + rawValue);
            assertFalse(FreshValuesScope.isActive());
        }
    }

    @Test
    void actionValueSeesWhatChangedRightBeforeItWhilePlaceholderCachingIsOn() {
        // Caching duration of a minute, so the result that rendering cached stays usable during the whole test
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> true, () -> 60000L));
        String key = "script_value_fresh_values_changed_value";
        String value = TestPlaceholders.value(key);
        TestPlaceholders.setValue(key, "old");
        assertEquals("old", PlaceholderParser.replacePlaceholders(value));
        // Like an action right before it, for example a Set Variable action
        TestPlaceholders.setValue(key, "new");
        RecordingAction action = new RecordingAction(false, null);

        new ActionInstance(action, value).execute();

        assertEquals("new", action.lastValue);
        // Rendering gets the fresh result from the cache
        TestPlaceholders.setValue(key, "newer");
        assertEquals("new", PlaceholderParser.replacePlaceholders(value));
    }

    @Test
    void scopeOfAnActionGetsClosedWhenTheActionThrows() {
        ActionInstance instance = new ActionInstance(new RecordingAction(false, new TestError()), PROBE);

        assertThrows(TestError.class, instance::execute);

        assertFalse(FreshValuesScope.isActive());
    }

    @Test
    void delayValuesOfDelayAndExecuteLaterBlocksReadFreshValues() {
        assertEquals(1L, new DelayExecutableBlock(PROBE).resolveDelayMs());
        assertFalse(FreshValuesScope.isActive());
        assertEquals(1L, new ExecuteLaterExecutableBlock(PROBE).resolveDelayMsOrFallback());
        assertFalse(FreshValuesScope.isActive());
    }

    @Test
    void listNamesOfForEachBlocksReadFreshValues() {
        assertEquals("1", new ForEachExecutableBlock(PROBE).resolveListVariableName());
        assertFalse(FreshValuesScope.isActive());
    }

    private static final class TestError extends Error {
    }

    private static final class RecordingAction extends Action {

        private final boolean rawValue;
        @Nullable
        private final Error error;
        @Nullable
        private String lastValue;
        @Nullable
        private String probeWhileExecuting;

        private RecordingAction(boolean rawValue, @Nullable Error error) {
            super("fancymenu_test_fresh_values_recording_action");
            this.rawValue = rawValue;
            this.error = error;
        }

        @Override
        public boolean hasValue() {
            return true;
        }

        @Override
        public boolean hasRawValue() {
            return this.rawValue;
        }

        @Override
        public void execute(@Nullable String value) {
            this.lastValue = value;
            // Actions can read values themselves while they execute
            this.probeWhileExecuting = PlaceholderParser.replacePlaceholders(PROBE);
            if (this.error != null) throw this.error;
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

    private static final class ScopeProbePlaceholder extends Placeholder {

        private ScopeProbePlaceholder() {
            super(PROBE_IDENTIFIER);
        }

        @Override
        public String getReplacementFor(DeserializedPlaceholderString dps) {
            return FreshValuesScope.isActive() ? "1" : "0";
        }

        @Override
        public @Nullable List<String> getValueNames() {
            return null;
        }

        @Override
        public @NotNull String getDisplayName() {
            return PROBE_IDENTIFIER;
        }

        @Override
        public List<String> getDescription() {
            return List.of();
        }

        @Override
        public String getCategory() {
            return "test";
        }

        @Override
        public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
            return new DeserializedPlaceholderString(PROBE_IDENTIFIER, new HashMap<>(), PROBE);
        }

        // The default check needs a running Minecraft client to compare threads.
        @Override
        public boolean checkAsync() {
            return true;
        }

    }

}
