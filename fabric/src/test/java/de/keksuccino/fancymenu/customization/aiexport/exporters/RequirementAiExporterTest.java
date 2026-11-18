package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.aiexport.AiExportReferences;
import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementGroup;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequirementAiExporterTest {

    @Test
    void requirementsGetCheckedWithTheirMode() {
        RequirementContainer container = new RequirementContainer();
        assertEquals(RequirementAiExporter.CheckResult.MET, RequirementAiExporter.check(instance(new TestRequirement(true, false), RequirementInstance.RequirementMode.IF, container)));
        assertEquals(RequirementAiExporter.CheckResult.NOT_MET, RequirementAiExporter.check(instance(new TestRequirement(false, false), RequirementInstance.RequirementMode.IF, container)));
        assertEquals(RequirementAiExporter.CheckResult.MET, RequirementAiExporter.check(instance(new TestRequirement(false, false), RequirementInstance.RequirementMode.IF_NOT, container)));
    }

    @Test
    void requirementsWithCheckSideEffectsNeverGetChecked() {
        TestRequirement requirement = new TestRequirement(true, true);
        RequirementContainer container = new RequirementContainer();
        container.addInstance(instance(requirement, RequirementInstance.RequirementMode.IF, container));

        assertEquals(RequirementAiExporter.CheckResult.NOT_CHECKED, RequirementAiExporter.check(container));
        assertEquals(0, requirement.checks.get());
    }

    @Test
    void failingRequirementsDoNotBreakTheCheck() {
        RequirementContainer container = new RequirementContainer();
        TestRequirement failing = new TestRequirement(true, false);
        failing.fail = true;
        assertEquals(RequirementAiExporter.CheckResult.FAILED, RequirementAiExporter.check(instance(failing, RequirementInstance.RequirementMode.IF, container)));
    }

    @Test
    void knownResultsDecideBeforeUnknownOnes() {
        List<RequirementAiExporter.CheckResult> mixed = List.of(RequirementAiExporter.CheckResult.NOT_CHECKED, RequirementAiExporter.CheckResult.NOT_MET);
        assertEquals(RequirementAiExporter.CheckResult.NOT_MET, RequirementAiExporter.combine(mixed, false));
        assertEquals(RequirementAiExporter.CheckResult.NOT_CHECKED, RequirementAiExporter.combine(mixed, true));
        List<RequirementAiExporter.CheckResult> metAndUnknown = List.of(RequirementAiExporter.CheckResult.FAILED, RequirementAiExporter.CheckResult.MET);
        assertEquals(RequirementAiExporter.CheckResult.MET, RequirementAiExporter.combine(metAndUnknown, true));
        assertEquals(RequirementAiExporter.CheckResult.NOT_CHECKED, RequirementAiExporter.combine(metAndUnknown, false));
    }

    @Test
    void containerResultsFollowTheModesOfNestedGroups() {
        for (int combination = 0; combination < 8; combination++) {
            boolean a = (combination & 1) != 0;
            boolean b = (combination & 2) != 0;
            boolean c = (combination & 4) != 0;
            RequirementContainer container = new RequirementContainer();
            container.addInstance(instance(new TestRequirement(a, false), RequirementInstance.RequirementMode.IF, container));
            RequirementGroup either = container.createAndAddGroup("either", RequirementGroup.GroupMode.OR);
            assertNotNull(either);
            either.addInstance(instance(new TestRequirement(b, false), RequirementInstance.RequirementMode.IF, container));
            either.addInstance(instance(new TestRequirement(c, false), RequirementInstance.RequirementMode.IF, container));

            RequirementAiExporter.CheckResult expected = (a && (b || c)) ? RequirementAiExporter.CheckResult.MET : RequirementAiExporter.CheckResult.NOT_MET;
            assertEquals(expected, RequirementAiExporter.check(container), "Combination " + combination);
            assertEquals(either.requirementsMet() ? RequirementAiExporter.CheckResult.MET : RequirementAiExporter.CheckResult.NOT_MET, RequirementAiExporter.check(either), "Combination " + combination);
        }
    }

    @Test
    void writingWithoutCheckNeverChecksRequirements() {
        TestRequirement requirement = new TestRequirement(true, false);
        RequirementContainer container = new RequirementContainer();
        container.addInstance(instance(requirement, RequirementInstance.RequirementMode.IF, container));
        AiExportWriter writer = new AiExportWriter();
        AiExportReferences references = new AiExportReferences();

        RequirementAiExporter.writeContainer(writer, "Requirements", container, references, false);

        assertEquals(0, requirement.checks.get());
        assertTrue(writer.build().contains(TestRequirement.IDENTIFIER));
        assertTrue(!references.isEmpty());
    }

    @Test
    void groupsAndValuesShowUpNestedBelowTheContainer() {
        RequirementContainer container = new RequirementContainer();
        RequirementGroup group = container.createAndAddGroup("my_group_id", RequirementGroup.GroupMode.AND);
        assertNotNull(group);
        TestRequirement withValue = new TestRequirement(true, false);
        withValue.hasValue = true;
        RequirementInstance instance = new RequirementInstance(withValue, "my_requirement_value", RequirementInstance.RequirementMode.IF, container);
        group.addInstance(instance);
        AiExportWriter writer = new AiExportWriter();

        RequirementAiExporter.writeContainer(writer, "Requirements", container, new AiExportReferences(), true);

        String[] lines = writer.build().split("\n");
        int groupIndentation = indentationOfLineContaining(lines, "my_group_id");
        int valueIndentation = indentationOfLineContaining(lines, "my_requirement_value");
        assertTrue(groupIndentation > 0);
        assertTrue(valueIndentation > groupIndentation);
    }

    private static int indentationOfLineContaining(String[] lines, String token) {
        for (String line : lines) {
            if (line.contains(token)) return line.length() - line.stripLeading().length();
        }
        throw new AssertionError("No line contains " + token);
    }

    @NotNull
    private static RequirementInstance instance(@NotNull Requirement requirement, @NotNull RequirementInstance.RequirementMode mode, @NotNull RequirementContainer container) {
        return new RequirementInstance(requirement, null, mode, container);
    }

    private static final class TestRequirement extends Requirement {

        private static final String IDENTIFIER = "test_ai_export_requirement";

        private final boolean met;
        private final boolean sideEffects;
        private final AtomicInteger checks = new AtomicInteger();
        private boolean fail = false;
        private boolean hasValue = false;

        private TestRequirement(boolean met, boolean sideEffects) {
            super(IDENTIFIER);
            this.met = met;
            this.sideEffects = sideEffects;
        }

        @Override
        public boolean checkAsync() {
            return true;
        }

        @Override
        public boolean hasCheckSideEffects() {
            return this.sideEffects;
        }

        @Override
        public boolean hasValue() {
            return this.hasValue;
        }

        @Override
        public boolean isRequirementMet(@Nullable String value) {
            this.checks.incrementAndGet();
            if (this.fail) throw new IllegalStateException("Test failure");
            return this.met;
        }

        @Override
        public @NotNull Component getDisplayName() {
            return Component.literal("Test Requirement");
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
