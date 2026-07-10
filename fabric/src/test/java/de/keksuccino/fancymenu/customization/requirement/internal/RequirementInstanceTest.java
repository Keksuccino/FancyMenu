package de.keksuccino.fancymenu.customization.requirement.internal;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequirementInstanceTest {

    @Test
    void assumedMetScopeUsesExactInstanceIdentity() {
        TestRequirement requirement = new TestRequirement(false);
        RequirementInstance instance = createInstance(requirement, RequirementInstance.RequirementMode.IF);
        RequirementInstance equalCopy = instance.copy(false);

        assertEquals(instance, equalCopy);
        assertNotSame(instance, equalCopy);
        assertTrue(instance.testWithThisRequirementAssumedMet(() -> {
            assertTrue(instance.requirementMet());
            assertFalse(equalCopy.requirementMet());
            return true;
        }));
        assertEquals(1, requirement.evaluationCount);

        assertFalse(instance.requirementMet());
        assertEquals(2, requirement.evaluationCount);
    }

    @Test
    void nestedScopesKeepTheInstanceAssumedUntilTheOutermostScopeEnds() {
        TestRequirement requirement = new TestRequirement(false);
        RequirementInstance instance = createInstance(requirement, RequirementInstance.RequirementMode.IF);

        assertTrue(instance.testWithThisRequirementAssumedMet(() -> {
            assertTrue(instance.requirementMet());
            assertTrue(instance.testWithThisRequirementAssumedMet(instance::requirementMet));
            return instance.requirementMet();
        }));
        assertEquals(0, requirement.evaluationCount);

        assertFalse(instance.requirementMet());
        assertEquals(1, requirement.evaluationCount);
    }

    @Test
    void exceptionalScopeIsAlwaysCleanedUp() {
        TestRequirement requirement = new TestRequirement(false);
        RequirementInstance instance = createInstance(requirement, RequirementInstance.RequirementMode.IF);

        assertThrows(IllegalStateException.class, () -> instance.testWithThisRequirementAssumedMet(() -> {
            assertTrue(instance.requirementMet());
            throw new IllegalStateException("probe failed");
        }));

        assertFalse(instance.requirementMet());
        assertEquals(1, requirement.evaluationCount);
    }

    @Test
    void assumedMetScopeBypassesNegationInsteadOfApplyingItAgain() {
        TestRequirement requirement = new TestRequirement(true);
        RequirementInstance instance = createInstance(requirement, RequirementInstance.RequirementMode.IF_NOT);

        assertFalse(instance.requirementMet());
        assertTrue(instance.testWithThisRequirementAssumedMet(instance::requirementMet));
        assertEquals(1, requirement.evaluationCount);
    }

    private static RequirementInstance createInstance(@NotNull Requirement requirement, @NotNull RequirementInstance.RequirementMode mode) {
        return new RequirementInstance(requirement, null, mode, new RequirementContainer());
    }

    private static final class TestRequirement extends Requirement {

        private final boolean result;
        private int evaluationCount;

        private TestRequirement(boolean result) {
            super("fancymenu_test_assumed_met_scope");
            this.result = result;
        }

        @Override
        public boolean hasValue() {
            return false;
        }

        @Override
        public boolean isRequirementMet(@Nullable String value) {
            this.evaluationCount++;
            return this.result;
        }

        @NotNull
        @Override
        public Component getDisplayName() {
            return Component.empty();
        }

        @Nullable
        @Override
        public Component getDescription() {
            return null;
        }

        @Nullable
        @Override
        public String getCategory() {
            return null;
        }

        @Nullable
        @Override
        public Component getValueDisplayName() {
            return null;
        }

        @Nullable
        @Override
        public String getValuePreset() {
            return null;
        }

        @Nullable
        @Override
        public List<TextEditorFormattingRule> getValueFormattingRules() {
            return null;
        }

        @Override
        public boolean checkAsync() {
            return true;
        }

    }

}
