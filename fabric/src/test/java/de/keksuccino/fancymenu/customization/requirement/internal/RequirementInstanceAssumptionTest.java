package de.keksuccino.fancymenu.customization.requirement.internal;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequirementInstanceAssumptionTest {

    private static final Requirement NEVER_MET = new TestRequirement(false);
    private static final Requirement ALWAYS_MET = new TestRequirement(true);

    @Test
    void assumptionBypassesNegationUntilTheOuterEvaluationAppliesIt() {
        RequirementInstance instance = createInstance(ALWAYS_MET, RequirementInstance.RequirementMode.IF_NOT);

        assertFalse(instance.requirementMet());
        assertTrue(instance.testWithThisRequirementAssumedMet(instance::requirementMet));
        assertFalse(instance.requirementMet());
    }

    @Test
    void assumptionUsesExactObjectIdentity() {
        RequirementInstance assumed = createInstance(NEVER_MET, RequirementInstance.RequirementMode.IF);
        RequirementInstance equalButDistinct = createInstance(NEVER_MET, RequirementInstance.RequirementMode.IF);
        equalButDistinct.instanceIdentifier = assumed.instanceIdentifier;

        assertTrue(assumed.testWithThisRequirementAssumedMet(() -> assertExactIdentity(assumed, equalButDistinct)));
    }

    @Test
    void nestedReentryKeepsTheOuterAssumptionUntilItsProbeEnds() {
        RequirementInstance instance = createInstance(NEVER_MET, RequirementInstance.RequirementMode.IF);

        assertTrue(instance.testWithThisRequirementAssumedMet(() -> assertNestedReentry(instance)));

        assertFalse(instance.requirementMet());
    }

    @Test
    void thrownProbeAlwaysCleansThreadLocalState() {
        RequirementInstance instance = createInstance(NEVER_MET, RequirementInstance.RequirementMode.IF);

        assertThrows(IllegalStateException.class, () -> instance.testWithThisRequirementAssumedMet(() -> { throw new IllegalStateException("expected"); }));

        assertFalse(instance.requirementMet());
    }

    @Test
    void assumptionDoesNotLeakToAnotherThread() throws Exception {
        RequirementInstance instance = createInstance(NEVER_MET, RequirementInstance.RequirementMode.IF);
        CountDownLatch assumptionActive = new CountDownLatch(1);
        CountDownLatch releaseProbe = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Boolean> assumedThread = executor.submit(() -> runAssumedThread(instance, assumptionActive, releaseProbe));
        Future<Boolean> otherThread = executor.submit(() -> runUnassumedThread(instance, assumptionActive, releaseProbe));

        try {
            assertFalse(otherThread.get(5L, TimeUnit.SECONDS));
            assertTrue(assumedThread.get(5L, TimeUnit.SECONDS));
        } finally {
            releaseProbe.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
        }
    }

    private static RequirementInstance createInstance(Requirement requirement, RequirementInstance.RequirementMode mode) {
        return new RequirementInstance(requirement, null, mode, new RequirementContainer());
    }

    private static boolean assertExactIdentity(RequirementInstance assumed, RequirementInstance equalButDistinct) {
        assertTrue(assumed.requirementMet());
        assertFalse(equalButDistinct.requirementMet());
        return true;
    }

    private static boolean assertNestedReentry(RequirementInstance instance) {
        assertTrue(instance.testWithThisRequirementAssumedMet(instance::requirementMet));
        assertTrue(instance.requirementMet());
        return true;
    }

    private static boolean runAssumedThread(RequirementInstance instance, CountDownLatch assumptionActive, CountDownLatch releaseProbe) {
        return instance.testWithThisRequirementAssumedMet(() -> finishAssumedThread(instance, assumptionActive, releaseProbe));
    }

    private static boolean finishAssumedThread(RequirementInstance instance, CountDownLatch assumptionActive, CountDownLatch releaseProbe) {
        assumptionActive.countDown();
        await(releaseProbe);
        return instance.requirementMet();
    }

    private static boolean runUnassumedThread(RequirementInstance instance, CountDownLatch assumptionActive, CountDownLatch releaseProbe) throws InterruptedException {
        assertTrue(assumptionActive.await(5L, TimeUnit.SECONDS));
        try {
            return instance.requirementMet();
        } finally {
            releaseProbe.countDown();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for requirement probe coordination");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for requirement probe coordination", ex);
        }
    }

    private static final class TestRequirement extends Requirement {

        private final boolean result;

        private TestRequirement(boolean result) {
            super("assumption_test_" + result);
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
            return this.result;
        }

        @Override
        public Component getDisplayName() {
            return Component.literal("test");
        }

        @Override
        public Component getDescription() {
            return null;
        }

        @Override
        public String getCategory() {
            return null;
        }

        @Override
        public Component getValueDisplayName() {
            return null;
        }

        @Override
        public String getValuePreset() {
            return null;
        }

        @Override
        public List<TextEditorFormattingRule> getValueFormattingRules() {
            return null;
        }

    }

}
