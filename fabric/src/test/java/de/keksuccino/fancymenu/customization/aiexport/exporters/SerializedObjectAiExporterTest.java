package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.ActionRegistry;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.aiexport.AiExportDefaults;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.RequirementRegistry;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerializedObjectAiExporterTest {

    private static final TestAction ACTION = new TestAction();
    private static final TestRequirement REQUIREMENT = new TestRequirement();

    @BeforeAll
    static void registerTestTypes() {
        if (ActionRegistry.getAction(TestAction.IDENTIFIER) == null) ActionRegistry.register(ACTION);
        if (RequirementRegistry.getRequirement(TestRequirement.IDENTIFIER) == null) RequirementRegistry.register(REQUIREMENT);
    }

    @Test
    void onlyPropertiesThatDifferFromTheDefaultsCountAsChanged() {
        PropertyContainer serialized = new PropertyContainer("element");
        serialized.putProperty("element_type", "some_type");
        serialized.putProperty("label", "changed label");
        serialized.putProperty("same_as_default", "default value");
        serialized.putProperty("not_in_defaults", "value");
        serialized.putProperty("[bracket_key:abc]", "serialized script content");

        SerializedObjectAiExporter.Analysis analysis = SerializedObjectAiExporter.analyze(serialized, defaults(), Set.of("element_type"));

        assertEquals(List.of("label", "not_in_defaults"), analysis.changedKeys());
        assertFalse(analysis.isUntouched());
    }

    @Test
    void unknownDefaultsListEveryProperty() {
        PropertyContainer serialized = new PropertyContainer("element");
        serialized.putProperty("label", "default label");
        serialized.putProperty("same_as_default", "default value");

        SerializedObjectAiExporter.Analysis analysis = SerializedObjectAiExporter.analyze(serialized, AiExportDefaults.TypeDefaults.UNKNOWN, Set.of());

        assertEquals(List.of("label", "same_as_default"), analysis.changedKeys());
    }

    @Test
    void requirementsAndScriptsReferencedByPropertiesGetFound() {
        PropertyContainer serialized = new PropertyContainer("element");

        RequirementContainer requirements = new RequirementContainer();
        requirements.addInstance(new RequirementInstance(REQUIREMENT, null, RequirementInstance.RequirementMode.IF, requirements));
        serialized.putProperty("my_requirements_identifier", requirements.identifier);
        requirements.serializeToExistingPropertyContainer(serialized);

        RequirementContainer emptyRequirements = new RequirementContainer();
        serialized.putProperty("my_empty_requirements_identifier", emptyRequirements.identifier);
        emptyRequirements.serializeToExistingPropertyContainer(serialized);

        GenericExecutableBlock script = new GenericExecutableBlock();
        script.addExecutable(new ActionInstance(ACTION, "action value"));
        serialized.putProperty("my_script_identifier", script.getIdentifier());
        script.serializeToExistingPropertyContainer(serialized);

        GenericExecutableBlock emptyScript = new GenericExecutableBlock();
        serialized.putProperty("my_empty_script_identifier", emptyScript.getIdentifier());
        emptyScript.serializeToExistingPropertyContainer(serialized);

        SerializedObjectAiExporter.Analysis analysis = SerializedObjectAiExporter.analyze(serialized, defaults(), Set.of());

        assertEquals(Set.of("my_requirements_identifier"), analysis.requirements().keySet());
        assertEquals(1, analysis.requirements().get("my_requirements_identifier").getInstances().size());
        assertEquals(Set.of("my_script_identifier"), analysis.scripts().keySet());
        assertEquals(1, analysis.scripts().get("my_script_identifier").getExecutables().size());
        assertTrue(analysis.changedKeys().isEmpty(), "Identifiers of requirements and scripts are no changed properties: " + analysis.changedKeys());
    }

    @Test
    void objectsWithOnlyDefaultsAndEmptyReferencesAreUntouched() {
        PropertyContainer serialized = new PropertyContainer("element");
        serialized.putProperty("instance_identifier", "some_id");
        serialized.putProperty("label", "default label");
        RequirementContainer emptyRequirements = new RequirementContainer();
        serialized.putProperty("my_empty_requirements_identifier", emptyRequirements.identifier);
        emptyRequirements.serializeToExistingPropertyContainer(serialized);

        SerializedObjectAiExporter.Analysis analysis = SerializedObjectAiExporter.analyze(serialized, defaults(), Set.of("instance_identifier"));

        assertTrue(analysis.isUntouched());
    }

    @NotNull
    private static AiExportDefaults.TypeDefaults defaults() {
        PropertyContainer defaults = new PropertyContainer("element");
        defaults.putProperty("label", "default label");
        defaults.putProperty("same_as_default", "default value");
        return new AiExportDefaults.TypeDefaults(defaults, Map.of());
    }

    private static final class TestAction extends Action {

        private static final String IDENTIFIER = "test_ai_export_serialized_action";

        private TestAction() {
            super(IDENTIFIER);
        }

        @Override
        public boolean hasValue() {
            return true;
        }

        @Override
        public void execute(@Nullable String value) {
        }

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

    private static final class TestRequirement extends Requirement {

        private static final String IDENTIFIER = "test_ai_export_serialized_requirement";

        private TestRequirement() {
            super(IDENTIFIER);
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
            return true;
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
