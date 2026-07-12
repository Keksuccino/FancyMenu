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

class RequirementContainerDeserializationTest {

    private static final String REQUIREMENT_IDENTIFIER = "test_deserialized_ownership";

    @BeforeAll
    static void registerTestRequirement() {
        RequirementRegistry.register(new DeserializationRequirement());
    }

    @Test
    void identifiedContainersOwnIndependentCopiesWithoutDuplicateSelections() {
        PropertyContainer serialized = new PropertyContainer("test");
        serialized.putProperty("[loading_requirement_group:shared_group]", "[group_mode:and]");
        serialized.putProperty("[loading_requirement:" + REQUIREMENT_IDENTIFIER + "][requirement_mode:if][group:shared_group][req_id:group_instance]", "group value");
        serialized.putProperty("[loading_requirement:" + REQUIREMENT_IDENTIFIER + "][requirement_mode:if][req_id:direct_instance]", "direct value");
        serialized.putProperty("[loading_requirement_container_meta:first_owner]", "[groups:shared_group;shared_group;][instances:direct_instance;direct_instance;]");
        serialized.putProperty("[loading_requirement_container_meta:second_owner]", "[groups:shared_group;][instances:direct_instance;]");

        List<RequirementContainer> containers = RequirementContainer.deserializeAll(serialized);

        assertEquals(2, containers.size());
        RequirementContainer first = containers.get(0);
        RequirementContainer second = containers.get(1);
        assertEquals("first_owner", first.identifier);
        assertEquals("second_owner", second.identifier);
        assertEquals(1, first.getGroups().size());
        assertEquals(1, first.getInstances().size());
        assertEquals(1, second.getGroups().size());
        assertEquals(1, second.getInstances().size());

        RequirementGroup firstGroup = first.getGroups().get(0);
        RequirementGroup secondGroup = second.getGroups().get(0);
        assertEquals(1, firstGroup.getInstances().size());
        assertEquals(1, secondGroup.getInstances().size());
        RequirementInstance firstGroupedInstance = firstGroup.getInstances().get(0);
        RequirementInstance secondGroupedInstance = secondGroup.getInstances().get(0);
        RequirementInstance firstDirectInstance = first.getInstances().get(0);
        RequirementInstance secondDirectInstance = second.getInstances().get(0);

        assertNotSame(firstGroup, secondGroup);
        assertNotSame(firstGroupedInstance, secondGroupedInstance);
        assertNotSame(firstDirectInstance, secondDirectInstance);
        assertSame(first, firstGroup.parent);
        assertSame(second, secondGroup.parent);
        assertSame(first, firstGroupedInstance.parent);
        assertSame(second, secondGroupedInstance.parent);
        assertSame(firstGroup, firstGroupedInstance.group);
        assertSame(secondGroup, secondGroupedInstance.group);
        assertSame(first, firstDirectInstance.parent);
        assertSame(second, secondDirectInstance.parent);
        assertNull(firstDirectInstance.group);
        assertNull(secondDirectInstance.group);

        firstGroup.mode = RequirementGroup.GroupMode.OR;
        firstGroupedInstance.value = "changed group value";
        firstDirectInstance.value = "changed direct value";
        assertEquals(RequirementGroup.GroupMode.AND, secondGroup.mode);
        assertEquals("group value", secondGroupedInstance.value);
        assertEquals("direct value", secondDirectInstance.value);
    }

    private static final class DeserializationRequirement extends Requirement {

        private DeserializationRequirement() {
            super(REQUIREMENT_IDENTIFIER);
        }

        @Override
        public boolean hasValue() {
            return true;
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
