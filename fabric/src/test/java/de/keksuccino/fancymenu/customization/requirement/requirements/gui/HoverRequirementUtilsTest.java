package de.keksuccino.fancymenu.customization.requirement.requirements.gui;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoverRequirementUtilsTest {

    @Test
    void selfHoverRequirementProbesTheOwnersOtherRenderConditions() {
        ElementHoverRequirement requirement = new ElementHoverRequirement();
        TestElement element = new TestElement("self");
        requirement.elements.put("self", element);
        RequirementInstance instance = createInstance(requirement, "self", RequirementInstance.RequirementMode.IF, element.requirementContainer);
        element.renderEligibility = instance::requirementMet;

        assertTrue(element.shouldRender());
    }

    @Test
    void selfHoverNegationIsAppliedOnlyByTheOuterEvaluation() {
        ElementHoverRequirement requirement = new ElementHoverRequirement();
        TestElement element = new TestElement("negated");
        requirement.elements.put("negated", element);
        RequirementInstance instance = createInstance(requirement, "negated", RequirementInstance.RequirementMode.IF_NOT, element.requirementContainer);
        element.renderEligibility = instance::requirementMet;

        assertFalse(element.shouldRender());
    }

    @Test
    void sameTypeOwnerRequirementsKeepTheirExactInstanceContexts() {
        ElementHoverRequirement requirement = new ElementHoverRequirement();
        TestElement element = new TestElement("nested");
        requirement.elements.put("nested", element);
        RequirementInstance first = createInstance(requirement, "nested", RequirementInstance.RequirementMode.IF, element.requirementContainer);
        RequirementInstance second = createInstance(requirement, "nested", RequirementInstance.RequirementMode.IF, element.requirementContainer);
        element.renderEligibility = () -> first.requirementMet() && second.requirementMet();

        assertTrue(element.shouldRender());
    }

    @Test
    void crossElementCycleFailsClosedAndCleansTheActiveProbeSet() {
        ElementHoverRequirement requirement = new ElementHoverRequirement();
        TestElement firstElement = new TestElement("first");
        TestElement secondElement = new TestElement("second");
        requirement.elements.put("first", firstElement);
        requirement.elements.put("second", secondElement);
        RequirementInstance first = createInstance(requirement, "second", RequirementInstance.RequirementMode.IF, firstElement.requirementContainer);
        RequirementInstance second = createInstance(requirement, "first", RequirementInstance.RequirementMode.IF, secondElement.requirementContainer);
        firstElement.renderEligibility = first::requirementMet;
        secondElement.renderEligibility = second::requirementMet;

        assertFalse(firstElement.shouldRender());

        first.value = "first";
        assertTrue(firstElement.shouldRender());
    }

    private static RequirementInstance createInstance(Requirement requirement, String value, RequirementInstance.RequirementMode mode, RequirementContainer parent) {
        RequirementInstance instance = new RequirementInstance(requirement, value, mode, parent);
        parent.addInstance(instance);
        return instance;
    }

    private static final class TestElement extends AbstractElement {

        private BooleanSupplier renderEligibility = () -> true;

        private TestElement(String identifier) {
            super(null);
            this.setInstanceIdentifier(identifier);
        }

        @Override
        public boolean shouldRender() {
            return this.renderEligibility.getAsBoolean();
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

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        }

    }

    private static final class ElementHoverRequirement extends Requirement {

        private final Map<String, TestElement> elements = new HashMap<>();

        private ElementHoverRequirement() {
            super("hover_requirement_utils_test");
        }

        @Override
        public boolean checkAsync() {
            return true;
        }

        @Override
        public boolean hasValue() {
            return true;
        }

        @Override
        public boolean isRequirementMet(@Nullable String value) {
            RequirementInstance currentInstance = this.getCurrentInstance();
            TestElement element = this.elements.get(value);
            return element != null && HoverRequirementUtils.isElementHovered(element, 5, 5, currentInstance);
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
            return Component.literal("value");
        }

        @Override
        public String getValuePreset() {
            return "element";
        }

        @Override
        public List<TextEditorFormattingRule> getValueFormattingRules() {
            return null;
        }

    }

}
