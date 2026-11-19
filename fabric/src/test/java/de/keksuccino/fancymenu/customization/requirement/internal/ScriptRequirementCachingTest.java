package de.keksuccino.fancymenu.customization.requirement.internal;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseIfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.IfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.WhileExecutableBlock;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.testing.TestPlaceholders;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Action scripts with requirement caching turned on. The clock of the requirement cache stands still,
 * so every cached result stays usable for rendering during the whole test.
 * Conditions whose values contain placeholders or {@code $$} references get checked with placeholder caching turned on too.
 */
@ResourceLock("RequirementContainer caching settings")
class ScriptRequirementCachingTest {

    /** Fails a test whose loop never stops, instead of letting it run into the timeout of the loop. */
    private static final int MAX_LOOP_RUNS = 100;

    private RequirementContainer.CachingSettings originalSettings;

    @BeforeEach
    void cacheRequirementsWithAClockThatStandsStill() {
        this.originalSettings = RequirementContainer.getCachingSettings();
        RequirementContainer.setCachingSettings(new RequirementContainer.CachingSettings(() -> 60_000L, () -> 123_456_789L));
    }

    @AfterEach
    void restoreCachingSettings() {
        RequirementContainer.setCachingSettings(this.originalSettings);
    }

    @Test
    void whileLoopWhoseConditionDependsOnItsContentsStops() {
        AtomicInteger counter = new AtomicInteger();
        CountingTestRequirement requirement = new CountingTestRequirement(() -> counter.get() < 3);
        RequirementContainer condition = requirement.newContainer();
        // Rendering cached "met" right before the script runs
        assertTrue(condition.requirementsMet());
        WhileExecutableBlock loop = new WhileExecutableBlock(condition);
        loop.addExecutable(action(() -> {
            if (counter.incrementAndGet() > MAX_LOOP_RUNS) throw new AssertionError("The WHILE loop never stopped");
        }));

        loop.execute();

        assertEquals(3, counter.get());
        // The check of the rendering, then 3 met checks of the loop and the one that ends it
        assertEquals(5, requirement.checks());
        assertEquals(1, requirement.checksOutsideFreshValuesScope());
        // Rendering gets the last result of the loop from the cache
        assertFalse(condition.requirementsMet());
        assertEquals(5, requirement.checks());
    }

    @Test
    @ResourceLock("PlaceholderParser global state")
    void whileLoopWhoseConditionParsesAPlaceholderItsContentsChangeStops() {
        String key = "script_requirement_caching_while_placeholder";
        AtomicInteger counter = new AtomicInteger();
        TestPlaceholders.setValue(key, "0");

        assertWhileLoopStopsWithPlaceholderCaching(TestPlaceholders.value(key), counter, () -> TestPlaceholders.setValue(key, Integer.toString(counter.incrementAndGet())));
    }

    @Test
    @ResourceLock("PlaceholderParser global state")
    void whileLoopWhoseConditionParsesAVariableReferenceItsContentsChangeStops() {
        String reference = "$$script_requirement_caching_while_counter";
        AtomicInteger counter = new AtomicInteger();
        // Resolves the reference at the same point as the processor of FancyMenu's variables, which can't be used here because its store writes to disk
        long processorId = PlaceholderParser.addParsingProcessor(PlaceholderParser.ParsingProcessorTiming.AFTER_REPLACING_PLACEHOLDERS, in -> in.replace(reference, Integer.toString(counter.get())));
        try {
            assertWhileLoopStopsWithPlaceholderCaching(reference, counter, counter::incrementAndGet);
        } finally {
            PlaceholderParser.removeParsingProcessor(processorId);
        }
    }

    @Test
    void ifConditionSeesWhatTheActionsBeforeItChanged() {
        AtomicBoolean flag = new AtomicBoolean(false);
        CountingTestRequirement requirement = new CountingTestRequirement(flag::get);
        RequirementContainer condition = requirement.newContainer();
        assertFalse(condition.requirementsMet());
        List<String> executed = new ArrayList<>();
        IfExecutableBlock ifBlock = new IfExecutableBlock(condition);
        ifBlock.addExecutable(action(() -> executed.add("if")));
        GenericExecutableBlock script = new GenericExecutableBlock();
        script.addExecutable(action(() -> flag.set(true)));
        script.addExecutable(ifBlock);

        script.execute();

        assertEquals(List.of("if"), executed);
        assertEquals(1, requirement.checksOutsideFreshValuesScope());
    }

    @Test
    void elseIfConditionSeesWhatTheActionsBeforeItChanged() {
        AtomicInteger mode = new AtomicInteger(1);
        CountingTestRequirement isOne = new CountingTestRequirement(() -> mode.get() == 1);
        CountingTestRequirement isTwo = new CountingTestRequirement(() -> mode.get() == 2);
        RequirementContainer ifCondition = isOne.newContainer();
        RequirementContainer elseIfCondition = isTwo.newContainer();
        assertTrue(ifCondition.requirementsMet());
        assertFalse(elseIfCondition.requirementsMet());
        List<String> executed = new ArrayList<>();
        IfExecutableBlock ifBlock = new IfExecutableBlock(ifCondition);
        ifBlock.addExecutable(action(() -> executed.add("one")));
        ElseIfExecutableBlock elseIfBlock = new ElseIfExecutableBlock(elseIfCondition);
        elseIfBlock.addExecutable(action(() -> executed.add("two")));
        ifBlock.setAppendedBlock(elseIfBlock);
        GenericExecutableBlock script = new GenericExecutableBlock();
        script.addExecutable(action(() -> mode.set(2)));
        script.addExecutable(ifBlock);

        script.execute();

        assertEquals(List.of("two"), executed);
    }

    /**
     * Runs a WHILE loop that counts to 3, with a condition whose value has to get parsed, while placeholder caching is on too.
     * The caching duration is a minute, so every parse result that rendering cached stays usable during the whole test.
     *
     * @param conditionValue The value of the condition, which gets parsed into the current count.
     * @param loopContents Increases the count, so the next parse of the condition value gets the new count.
     */
    private static void assertWhileLoopStopsWithPlaceholderCaching(@NotNull String conditionValue, @NotNull AtomicInteger counter, @NotNull Runnable loopContents) {
        PlaceholderParser.PlaceholderCachingController originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> true, () -> 60000L));
        try {
            CountingTestRequirement requirement = new CountingTestRequirement(value -> Integer.parseInt(value) < 3);
            RequirementContainer condition = requirement.newContainer(conditionValue);
            // Rendering cached the parsed value and "met" right before the script runs
            assertTrue(condition.requirementsMet());
            WhileExecutableBlock loop = new WhileExecutableBlock(condition);
            loop.addExecutable(action(() -> {
                if (counter.get() >= MAX_LOOP_RUNS) throw new AssertionError("The WHILE loop never stopped");
                loopContents.run();
            }));

            loop.execute();

            assertEquals(3, counter.get());
            // The check of the rendering, then 3 met checks of the loop and the one that ends it
            assertEquals(5, requirement.checks());
            assertEquals(1, requirement.checksOutsideFreshValuesScope());
            // Rendering gets the last results of the loop from the caches
            assertFalse(condition.requirementsMet());
            assertEquals("3", PlaceholderParser.replacePlaceholders(conditionValue));
            assertEquals(5, requirement.checks());
        } finally {
            PlaceholderParser.setPlaceholderCachingController(originalCachingController);
        }
    }

    @NotNull
    private static ActionInstance action(@NotNull Runnable runnable) {
        return new ActionInstance(new RunnableAction(runnable), null);
    }

    private static final class RunnableAction extends Action {

        @NotNull
        private final Runnable runnable;

        private RunnableAction(@NotNull Runnable runnable) {
            super("fancymenu_test_runnable_action");
            this.runnable = runnable;
        }

        @Override
        public boolean hasValue() {
            return false;
        }

        @Override
        public void execute(@Nullable String value) {
            this.runnable.run();
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

}
