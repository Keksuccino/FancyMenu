package de.keksuccino.fancymenu.customization.requirement.internal;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.RequirementRegistry;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class RequirementContainerDeserializationTest {

    private static final TestRequirement TEST_REQUIREMENT = new TestRequirement();
    private static final String FIRST_CONTAINER_ID = "deserialization_owner_first";
    private static final String SECOND_CONTAINER_ID = "deserialization_owner_second";
    private static final String GROUP_ID = "deserialization_shared_group";
    private static final String GROUP_INSTANCE_ID = "deserialization_group_instance";
    private static final String STANDALONE_INSTANCE_ID = "deserialization_standalone_instance";

    @BeforeAll
    static void registerRequirement() {
        RequirementRegistry.register(TEST_REQUIREMENT);
    }

    @Test
    void identifiedContainersOwnIndependentCopiesWithRestoredParents() {
        List<RequirementContainer> containers = RequirementContainer.deserializeAll(createSharedMetadata());
        RequirementContainer first = container(containers, FIRST_CONTAINER_ID);
        RequirementContainer second = container(containers, SECOND_CONTAINER_ID);
        RequirementGroup firstGroup = first.getGroup(GROUP_ID);
        RequirementGroup secondGroup = second.getGroup(GROUP_ID);
        RequirementInstance firstGrouped = firstGroup.getInstances().get(0);
        RequirementInstance secondGrouped = secondGroup.getInstances().get(0);
        RequirementInstance firstStandalone = first.getInstances().get(0);
        RequirementInstance secondStandalone = second.getInstances().get(0);

        assertNotSame(firstGroup, secondGroup);
        assertSame(first, firstGroup.parent);
        assertSame(second, secondGroup.parent);
        assertNotSame(firstGrouped, secondGrouped);
        assertSame(first, firstGrouped.parent);
        assertSame(firstGroup, firstGrouped.group);
        assertSame(second, secondGrouped.parent);
        assertSame(secondGroup, secondGrouped.group);
        assertNotSame(firstStandalone, secondStandalone);
        assertSame(first, firstStandalone.parent);
        assertSame(second, secondStandalone.parent);
        assertNull(firstStandalone.group);
        assertNull(secondStandalone.group);
        assertEquals(GROUP_INSTANCE_ID, firstGrouped.instanceIdentifier);
        assertEquals(GROUP_INSTANCE_ID, secondGrouped.instanceIdentifier);
        assertEquals(STANDALONE_INSTANCE_ID, firstStandalone.instanceIdentifier);
        assertEquals(STANDALONE_INSTANCE_ID, secondStandalone.instanceIdentifier);

        firstGrouped.value = "mutated";
        firstStandalone.value = "mutated";

        assertEquals("group_value", secondGrouped.value);
        assertEquals("standalone_value", secondStandalone.value);
    }

    @Test
    void duplicateAndMissingMetadataReferencesAreIgnoredDeterministically() {
        PropertyContainer serialized = createSharedMetadata();
        serialized.putProperty("[loading_requirement_container_meta:deserialization_owner_malformed]", "[groups:" + GROUP_ID + ";" + GROUP_ID + ";missing_group;][instances:" + STANDALONE_INSTANCE_ID + ";" + STANDALONE_INSTANCE_ID + ";missing_instance;]");

        RequirementContainer malformed = container(RequirementContainer.deserializeAll(serialized), "deserialization_owner_malformed");

        assertEquals(1, malformed.getGroups().size());
        assertEquals(1, malformed.getInstances().size());
        assertEquals(GROUP_ID, malformed.getGroups().get(0).identifier);
        assertEquals(STANDALONE_INSTANCE_ID, malformed.getInstances().get(0).instanceIdentifier);
    }

    @Test
    void legacyPayloadWithoutMetadataKeepsTheCombinedContainerGraph() {
        PropertyContainer serialized = createSourceContainer().serialize();
        List<String> metaKeys = new ArrayList<>();
        for (String key : serialized.getProperties().keySet()) {
            if (key.startsWith("[loading_requirement_container_meta:")) metaKeys.add(key);
        }
        metaKeys.forEach(serialized::removeProperty);

        List<RequirementContainer> containers = RequirementContainer.deserializeAll(serialized);
        RequirementContainer legacy = containers.get(0);
        RequirementGroup group = legacy.getGroup(GROUP_ID);
        RequirementInstance grouped = group.getInstances().get(0);
        RequirementInstance standalone = legacy.getInstances().get(0);

        assertEquals(1, containers.size());
        assertSame(legacy, group.parent);
        assertSame(legacy, grouped.parent);
        assertSame(group, grouped.group);
        assertSame(legacy, standalone.parent);
        assertNull(standalone.group);
    }

    private static PropertyContainer createSharedMetadata() {
        PropertyContainer serialized = createSourceContainer().serialize();
        String firstMetaKey = "[loading_requirement_container_meta:" + FIRST_CONTAINER_ID + "]";
        serialized.putProperty("[loading_requirement_container_meta:" + SECOND_CONTAINER_ID + "]", serialized.getValue(firstMetaKey));
        return serialized;
    }

    private static RequirementContainer createSourceContainer() {
        RequirementContainer source = new RequirementContainer();
        source.identifier = FIRST_CONTAINER_ID;
        RequirementGroup group = source.createAndAddGroup(GROUP_ID, RequirementGroup.GroupMode.AND);
        RequirementInstance grouped = new RequirementInstance(TEST_REQUIREMENT, "group_value", RequirementInstance.RequirementMode.IF, source);
        grouped.instanceIdentifier = GROUP_INSTANCE_ID;
        group.addInstance(grouped);
        RequirementInstance standalone = new RequirementInstance(TEST_REQUIREMENT, "standalone_value", RequirementInstance.RequirementMode.IF, source);
        standalone.instanceIdentifier = STANDALONE_INSTANCE_ID;
        source.addInstance(standalone);
        return source;
    }

    private static RequirementContainer container(List<RequirementContainer> containers, String identifier) {
        return containers.stream().filter(container -> container.identifier.equals(identifier)).findFirst().orElseThrow();
    }

    private static final class TestRequirement extends Requirement {

        private TestRequirement() {
            super("requirement_container_deserialization_test");
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
            return true;
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
            return "value";
        }

        @Override
        public List<TextEditorFormattingRule> getValueFormattingRules() {
            return null;
        }

    }

}
