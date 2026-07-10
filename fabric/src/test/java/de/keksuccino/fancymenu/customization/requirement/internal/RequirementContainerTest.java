package de.keksuccino.fancymenu.customization.requirement.internal;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.RequirementRegistry;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class RequirementContainerTest {

    private static final TestRequirement TEST_REQUIREMENT = new TestRequirement();

    @BeforeAll
    static void registerTestRequirement() {
        RequirementRegistry.register(TEST_REQUIREMENT);
    }

    @Test
    void identifiedDeserializationCopiesSelectedGraphsPerOwningContainer() {
        PropertyContainer serialized = buildSharedMetadataGraph();

        List<RequirementContainer> containers = RequirementContainer.deserializeAll(serialized);
        RequirementContainer first = getByIdentifier(containers, "owner_a");
        RequirementContainer second = getByIdentifier(containers, "owner_b");

        assertEquals(1, first.getGroups().size());
        assertEquals(1, first.getInstances().size());
        assertEquals(1, second.getGroups().size());
        assertEquals(1, second.getInstances().size());

        RequirementGroup firstGroup = first.getGroups().get(0);
        RequirementGroup secondGroup = second.getGroups().get(0);
        RequirementInstance firstGroupedInstance = firstGroup.getInstances().get(0);
        RequirementInstance secondGroupedInstance = secondGroup.getInstances().get(0);
        RequirementInstance firstStandaloneInstance = first.getInstances().get(0);
        RequirementInstance secondStandaloneInstance = second.getInstances().get(0);

        assertNotSame(firstGroup, secondGroup);
        assertNotSame(firstGroupedInstance, secondGroupedInstance);
        assertNotSame(firstStandaloneInstance, secondStandaloneInstance);
        assertSame(first, firstGroup.parent);
        assertSame(first, firstGroupedInstance.parent);
        assertSame(firstGroup, firstGroupedInstance.group);
        assertSame(first, firstStandaloneInstance.parent);
        assertNull(firstStandaloneInstance.group);
        assertSame(second, secondGroup.parent);
        assertSame(second, secondGroupedInstance.parent);
        assertSame(secondGroup, secondGroupedInstance.group);
        assertSame(second, secondStandaloneInstance.parent);
        assertNull(secondStandaloneInstance.group);

        firstGroup.mode = RequirementGroup.GroupMode.OR;
        firstGroupedInstance.value = "changed_group_value";
        firstStandaloneInstance.value = "changed_standalone_value";

        assertEquals(RequirementGroup.GroupMode.AND, secondGroup.mode);
        assertEquals("group_value", secondGroupedInstance.value);
        assertEquals("standalone_value", secondStandaloneInstance.value);
    }

    @NotNull
    private static PropertyContainer buildSharedMetadataGraph() {
        PropertyContainer serialized = new PropertyContainer("test_requirements");
        serialized.putProperty("[loading_requirement_container_meta:owner_a]", "[groups:shared_group;shared_group;][instances:standalone_instance;standalone_instance;]");
        serialized.putProperty("[loading_requirement_container_meta:owner_b]", "[groups:shared_group;][instances:standalone_instance;]");
        serialized.putProperty("[loading_requirement_group:shared_group]", "[group_mode:and]");
        serialized.putProperty("[loading_requirement:" + TEST_REQUIREMENT.getIdentifier() + "][requirement_mode:if][group:shared_group][req_id:group_instance]", "group_value");
        serialized.putProperty("[loading_requirement:" + TEST_REQUIREMENT.getIdentifier() + "][requirement_mode:if][req_id:standalone_instance]", "standalone_value");
        return serialized;
    }

    @NotNull
    private static RequirementContainer getByIdentifier(@NotNull List<RequirementContainer> containers, @NotNull String identifier) {
        return containers.stream().filter(container -> identifier.equals(container.identifier)).findFirst().orElseThrow();
    }

    private static final class TestRequirement extends Requirement {

        private TestRequirement() {
            super("fancymenu_test_deserialization_ownership");
        }

        @Override
        public boolean hasValue() {
            return true;
        }

        @Override
        public boolean isRequirementMet(@Nullable String value) {
            return true;
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

    }

}
