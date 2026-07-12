package de.keksuccino.fancymenu.customization.requirement.internal;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequirementInstanceTest {

    @Test
    void assumptionMatchesOnlyTheExactInstance() {
        CountingRequirement requirement = new CountingRequirement(false);
        RequirementContainer container = new RequirementContainer();
        RequirementInstance instance = new RequirementInstance(requirement, null, RequirementInstance.RequirementMode.IF, container);
        RequirementInstance equalButDistinctInstance = instance.copy(false);
        equalButDistinctInstance.parent = container;

        assertEquals(instance, equalButDistinctInstance);
        assertTrue(instance.testWithThisRequirementAssumedMet(instance::requirementMet));
        assertFalse(instance.testWithThisRequirementAssumedMet(equalButDistinctInstance::requirementMet));
        assertEquals(1, requirement.evaluationCount.get());
    }

    @Test
    void nestedAssumptionsRemainActiveUntilTheOutermostScopeEnds() {
        CountingRequirement requirement = new CountingRequirement(false);
        RequirementInstance instance = new RequirementInstance(requirement, null, RequirementInstance.RequirementMode.IF, new RequirementContainer());

        assertTrue(instance.testWithThisRequirementAssumedMet(() -> {
            assertTrue(instance.requirementMet());
            return instance.testWithThisRequirementAssumedMet(instance::requirementMet);
        }));
        assertFalse(instance.requirementMet());
        assertEquals(1, requirement.evaluationCount.get());
    }

    @Test
    void nestedDistinctAssumptionsAreRemovedIndependently() {
        CountingRequirement requirement = new CountingRequirement(false);
        RequirementInstance outer = new RequirementInstance(requirement, null, RequirementInstance.RequirementMode.IF, new RequirementContainer());
        RequirementInstance inner = new RequirementInstance(requirement, null, RequirementInstance.RequirementMode.IF, new RequirementContainer());

        assertTrue(outer.testWithThisRequirementAssumedMet(() -> {
            assertTrue(inner.testWithThisRequirementAssumedMet(() -> outer.requirementMet() && inner.requirementMet()));
            assertTrue(outer.requirementMet());
            return !inner.requirementMet();
        }));
        assertFalse(outer.requirementMet());
        assertFalse(inner.requirementMet());
        assertEquals(3, requirement.evaluationCount.get());
    }

    @Test
    void exceptionalProbeCleansUpTheAssumption() {
        CountingRequirement requirement = new CountingRequirement(false);
        RequirementInstance instance = new RequirementInstance(requirement, null, RequirementInstance.RequirementMode.IF, new RequirementContainer());

        assertThrows(IllegalStateException.class, () -> instance.testWithThisRequirementAssumedMet(() -> {
            throw new IllegalStateException("probe failed");
        }));
        assertFalse(instance.requirementMet());
        assertEquals(1, requirement.evaluationCount.get());
    }

    @Test
    void exceptionalNestedProbePreservesTheOuterAssumption() {
        CountingRequirement requirement = new CountingRequirement(false);
        RequirementInstance instance = new RequirementInstance(requirement, null, RequirementInstance.RequirementMode.IF, new RequirementContainer());

        assertTrue(instance.testWithThisRequirementAssumedMet(() -> {
            assertThrows(IllegalStateException.class, () -> instance.testWithThisRequirementAssumedMet(() -> {
                throw new IllegalStateException("nested probe failed");
            }));
            return instance.requirementMet();
        }));
        assertFalse(instance.requirementMet());
        assertEquals(1, requirement.evaluationCount.get());
    }

    @Test
    void assumptionIsIsolatedToTheCallingThread() throws Exception {
        CountingRequirement requirement = new CountingRequirement(false);
        RequirementInstance instance = new RequirementInstance(requirement, null, RequirementInstance.RequirementMode.IF, new RequirementContainer());
        CountDownLatch assumptionActive = new CountDownLatch(1);
        CountDownLatch releaseProbe = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> probeResult = executor.submit(() -> instance.testWithThisRequirementAssumedMet(() -> {
                assumptionActive.countDown();
                await(releaseProbe);
                return instance.requirementMet();
            }));
            assertTrue(assumptionActive.await(5, TimeUnit.SECONDS));
            Future<Boolean> otherThreadResult = executor.submit(instance::requirementMet);
            assertFalse(otherThreadResult.get(5, TimeUnit.SECONDS));
            releaseProbe.countDown();
            assertTrue(probeResult.get(5, TimeUnit.SECONDS));
        } finally {
            releaseProbe.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        assertEquals(1, requirement.evaluationCount.get());
    }

    @Test
    void assumedInvertedRequirementDoesNotApplyItsModeAgain() {
        CountingRequirement requirement = new CountingRequirement(false);
        RequirementInstance instance = new RequirementInstance(requirement, null, RequirementInstance.RequirementMode.IF_NOT, new RequirementContainer());

        assertTrue(instance.requirementMet());
        assertEquals(1, requirement.evaluationCount.get());
        assertTrue(instance.testWithThisRequirementAssumedMet(instance::requirementMet));
        assertEquals(1, requirement.evaluationCount.get());
        assertTrue(instance.requirementMet());
        assertEquals(2, requirement.evaluationCount.get());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for test coordination");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for test coordination", e);
        }
    }

    private static final class CountingRequirement extends Requirement {

        private final boolean result;
        private final AtomicInteger evaluationCount = new AtomicInteger();

        private CountingRequirement(boolean result) {
            super("test_counting_requirement");
            this.result = result;
        }

        @Override
        public boolean checkAsync() {
            return true;
        }

        @Override
        public boolean hasValue() {
            return false;
        }

        @Override
        public boolean isRequirementMet(@Nullable String value) {
            this.evaluationCount.incrementAndGet();
            return this.result;
        }

        @Override
        public @NotNull Component getDisplayName() {
            return Component.empty();
        }

        @Override
        public @Nullable Component getDescription() {
            return null;
        }

        @Override
        public @Nullable String getCategory() {
            return null;
        }

        @Override
        public @Nullable Component getValueDisplayName() {
            return null;
        }

        @Override
        public @Nullable String getValuePreset() {
            return null;
        }

        @Override
        public @Nullable List<TextEditorFormattingRule> getValueFormattingRules() {
            return null;
        }
    }
}
