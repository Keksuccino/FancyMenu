package de.keksuccino.fancymenu.customization.requirement.internal;

import de.keksuccino.fancymenu.customization.requirement.TestRequirement;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequirementInstanceTest {

    @Test
    void assumptionBypassesOnlyTheExactInstanceWithoutApplyingNegation() {
        TestRequirement requirement = new TestRequirement("test_assumption_identity", value -> false);
        RequirementContainer parent = new RequirementContainer();
        RequirementInstance positive = new RequirementInstance(requirement, null, RequirementInstance.RequirementMode.IF, parent);
        RequirementInstance inverted = new RequirementInstance(requirement, null, RequirementInstance.RequirementMode.IF_NOT, parent);
        RequirementInstance equalCopy = positive.copy(false);

        assertTrue(positive.testWithThisRequirementAssumedMet(positive::requirementMet));
        assertTrue(inverted.testWithThisRequirementAssumedMet(inverted::requirementMet));
        assertFalse(positive.testWithThisRequirementAssumedMet(equalCopy::requirementMet));
        assertEquals(1, requirement.getEvaluationCount());
    }

    @Test
    void nestedSameInstanceScopesRemainActiveUntilTheOutermostProbeEnds() {
        TestRequirement requirement = new TestRequirement("test_nested_assumption", value -> false);
        RequirementInstance instance = new RequirementInstance(requirement, null, RequirementInstance.RequirementMode.IF, new RequirementContainer());

        assertTrue(instance.testWithThisRequirementAssumedMet(() -> instance.testWithThisRequirementAssumedMet(instance::requirementMet) && instance.requirementMet()));
        assertEquals(0, requirement.getEvaluationCount());
        assertFalse(instance.requirementMet());
        assertEquals(1, requirement.getEvaluationCount());
    }

    @Test
    void assumptionIsRemovedAfterProbeFailure() {
        TestRequirement requirement = new TestRequirement("test_assumption_failure_cleanup", value -> false);
        RequirementInstance instance = new RequirementInstance(requirement, null, RequirementInstance.RequirementMode.IF, new RequirementContainer());

        assertThrows(IllegalStateException.class, () -> instance.testWithThisRequirementAssumedMet(() -> {
            throw new IllegalStateException("probe failed");
        }));
        assertFalse(instance.requirementMet());
        assertEquals(1, requirement.getEvaluationCount());
    }

    @Test
    void assumptionDoesNotLeakToAnotherThread() throws Exception {
        TestRequirement requirement = new TestRequirement("test_assumption_thread_scope", value -> false);
        RequirementInstance instance = new RequirementInstance(requirement, null, RequirementInstance.RequirementMode.IF, new RequirementContainer());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            assertFalse(instance.testWithThisRequirementAssumedMet(() -> {
                try {
                    return executor.submit(instance::requirementMet).get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
        assertEquals(1, requirement.getEvaluationCount());
    }

}
