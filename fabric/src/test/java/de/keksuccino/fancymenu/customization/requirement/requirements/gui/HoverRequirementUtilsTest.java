package de.keksuccino.fancymenu.customization.requirement.requirements.gui;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance.RequirementMode;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoverRequirementUtilsTest {

    @Test
    void selfHoverEvaluatesTheOwningRequirementOnce() {
        TestElement element = new TestElement("self");
        TestHoverRequirement requirement = new TestHoverRequirement(element);
        RequirementInstance instance = addRequirement(element, requirement, element.getInstanceIdentifier(), RequirementMode.IF);

        assertTrue(instance.requirementMet());
        assertEquals(1, requirement.evaluationCount.get());
    }

    @Test
    void invertedSelfHoverPreservesNegationAndEvaluatesOnce() {
        TestElement element = new TestElement("inverted_self");
        TestHoverRequirement requirement = new TestHoverRequirement(element);
        RequirementInstance instance = addRequirement(element, requirement, element.getInstanceIdentifier(), RequirementMode.IF_NOT);

        assertFalse(instance.requirementMet());
        assertEquals(1, requirement.evaluationCount.get());
    }

    @Test
    void selfHoverStillRequiresTheOwnersOtherRenderConditions() {
        TestElement element = new TestElement("gated_self");
        TestHoverRequirement hoverRequirement = new TestHoverRequirement(element);
        RequirementInstance hoverInstance = addRequirement(element, hoverRequirement, element.getInstanceIdentifier(), RequirementMode.IF);
        element.remainingRenderGate = () -> false;

        assertFalse(hoverInstance.requirementMet());
        assertEquals(1, hoverRequirement.evaluationCount.get());
    }

    @Test
    void twoElementHoverCycleFailsClosedAndCleansUpTheGuard() {
        TestElement first = new TestElement("first");
        TestElement second = new TestElement("second");
        TestHoverRequirement requirement = new TestHoverRequirement(first, second);
        RequirementInstance firstInstance = addRequirement(first, requirement, second.getInstanceIdentifier(), RequirementMode.IF);
        addRequirement(second, requirement, first.getInstanceIdentifier(), RequirementMode.IF);

        assertFalse(firstInstance.requirementMet());
        assertFalse(firstInstance.requirementMet());
        assertEquals(6, requirement.evaluationCount.get());
    }

    private static RequirementInstance addRequirement(TestElement owner, Requirement requirement, @Nullable String value, RequirementMode mode) {
        RequirementInstance instance = new RequirementInstance(requirement, value, mode, owner.requirementContainer);
        owner.requirementContainer.addInstance(instance);
        return instance;
    }

    private static class ConstantRequirement extends Requirement {

        private final boolean result;

        private ConstantRequirement(boolean result) {
            super("test_constant_hover_requirement");
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

    private static final class TestHoverRequirement extends ConstantRequirement {

        private final Map<String, TestElement> elementsByIdentifier = new HashMap<>();
        private final AtomicInteger evaluationCount = new AtomicInteger();

        private TestHoverRequirement(TestElement... elements) {
            super(false);
            for (TestElement element : elements) {
                this.elementsByIdentifier.put(element.getInstanceIdentifier(), element);
            }
        }

        @Override
        public boolean hasValue() {
            return true;
        }

        @Override
        public boolean isRequirementMet(@Nullable String value) {
            this.evaluationCount.incrementAndGet();
            RequirementInstance currentInstance = this.getCurrentInstance();
            TestElement element = this.elementsByIdentifier.get(value);
            return (element != null) && HoverRequirementUtils.isElementHovered(element, 5, 5, currentInstance);
        }
    }

    private static final class TestElement extends AbstractElement {

        private BooleanSupplier remainingRenderGate = () -> true;

        private TestElement(String identifier) {
            super(null);
            this.setInstanceIdentifier(identifier);
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        }

        @Override
        public boolean shouldRender() {
            if (!this.remainingRenderGate.getAsBoolean()) return false;
            for (RequirementInstance instance : this.requirementContainer.getInstances()) {
                if (!instance.requirementMet()) return false;
            }
            return true;
        }

        @Override
        public int getAbsoluteX() {
            return 0;
        }

        @Override
        public int getAbsoluteY() {
            return 0;
        }

        @Override
        public int getAbsoluteWidth() {
            return 10;
        }

        @Override
        public int getAbsoluteHeight() {
            return 10;
        }
    }
}
