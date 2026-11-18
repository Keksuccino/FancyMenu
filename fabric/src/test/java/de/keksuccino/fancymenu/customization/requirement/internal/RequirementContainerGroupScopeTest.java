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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequirementContainerGroupScopeTest {

    private static final ScopeRequirement REQUIREMENT = new ScopeRequirement();

    @BeforeAll
    static void registerTestRequirement() {
        RequirementRegistry.register(REQUIREMENT);
    }

    @Test
    void containersInOnePropertyContainerKeepTheirOwnGroupsWithTheSameIdentifier() {
        RequirementContainer first = new RequirementContainer();
        RequirementGroup firstGroup = first.createAndAddGroup("group_1", RequirementGroup.GroupMode.AND);
        assertNotNull(firstGroup);
        firstGroup.addInstance(requirement(first, RequirementInstance.RequirementMode.IF));
        RequirementGroup firstNested = new RequirementGroup("group_2", RequirementGroup.GroupMode.OR, first);
        firstNested.addInstance(requirement(first, RequirementInstance.RequirementMode.IF));
        assertTrue(firstGroup.addGroup(firstNested));

        RequirementContainer second = new RequirementContainer();
        RequirementGroup secondGroup = second.createAndAddGroup("group_1", RequirementGroup.GroupMode.OR);
        assertNotNull(secondGroup);
        secondGroup.addInstance(requirement(second, RequirementInstance.RequirementMode.IF_NOT));
        secondGroup.addInstance(requirement(second, RequirementInstance.RequirementMode.IF_NOT));
        RequirementGroup secondNested = new RequirementGroup("group_2", RequirementGroup.GroupMode.AND, second);
        assertTrue(secondGroup.addGroup(secondNested));
        assertTrue(second.addInstance(requirement(second, RequirementInstance.RequirementMode.IF)));

        PropertyContainer serialized = new PropertyContainer("test");
        first.serializeToExistingPropertyContainer(serialized);
        second.serializeToExistingPropertyContainer(serialized);

        RequirementContainer firstRestored = RequirementContainer.deserializeWithIdentifier(first.identifier, serialized);
        RequirementContainer secondRestored = RequirementContainer.deserializeWithIdentifier(second.identifier, serialized);
        assertEquals(first, firstRestored);
        assertEquals(second, secondRestored);
        assertNotNull(firstRestored);
        assertNotNull(secondRestored);
        assertNotSame(firstRestored.getGroup("group_2"), secondRestored.getGroup("group_2"));
        assertLinked(firstRestored);
        assertLinked(secondRestored);
    }

    @Test
    void groupKeysStillStartWithTheGroupIdentifier() {
        RequirementContainer container = new RequirementContainer();
        RequirementGroup group = container.createAndAddGroup("group_1", RequirementGroup.GroupMode.AND);
        assertNotNull(group);
        group.addInstance(requirement(container, RequirementInstance.RequirementMode.IF));

        PropertyContainer serialized = container.serialize();

        int groupKeys = 0;
        for (String key : serialized.getProperties().keySet()) {
            if (!key.startsWith("[loading_requirement_group:")) continue;
            groupKeys++;
            assertTrue(key.startsWith("[loading_requirement_group:group_1]"), key);
            RequirementGroup parsed = RequirementGroup.deserializeRequirementGroup(key, serialized.getProperties().get(key), container);
            assertNotNull(parsed);
            assertEquals("group_1", parsed.identifier);
        }
        assertEquals(1, groupKeys);
    }

    @Test
    void groupsWithoutContainerTagStillLoad() {
        PropertyContainer serialized = new PropertyContainer("test");
        serialized.putProperty("[loading_requirement_container_meta:owner]", "[groups:legacy_group;][instances:direct;]");
        serialized.putProperty("[loading_requirement_group:legacy_group]", "[group_mode:or]");
        serialized.putProperty("[loading_requirement:" + ScopeRequirement.IDENTIFIER + "][requirement_mode:if][group:legacy_group][req_id:grouped]", "");
        serialized.putProperty("[loading_requirement:" + ScopeRequirement.IDENTIFIER + "][requirement_mode:if_not][req_id:direct]", "");

        RequirementContainer container = RequirementContainer.deserializeWithIdentifier("owner", serialized);

        assertNotNull(container);
        assertEquals(1, container.getGroups().size());
        RequirementGroup group = container.getGroups().get(0);
        assertEquals("legacy_group", group.identifier);
        assertEquals(RequirementGroup.GroupMode.OR, group.mode);
        assertEquals(1, group.getInstances().size());
        assertEquals("grouped", group.getInstances().get(0).instanceIdentifier);
        assertEquals(1, container.getInstances().size());
        assertEquals("direct", container.getInstances().get(0).instanceIdentifier);
        assertLinked(container);
    }

    @Test
    void ownGroupWinsOverLegacyGroupWithTheSameIdentifier() {
        RequirementContainer container = new RequirementContainer();
        RequirementGroup group = container.createAndAddGroup("group_1", RequirementGroup.GroupMode.AND);
        assertNotNull(group);
        group.addInstance(requirement(container, RequirementInstance.RequirementMode.IF));
        PropertyContainer serialized = container.serialize();
        serialized.putProperty("[loading_requirement_group:group_1]", "[group_mode:or]");
        serialized.putProperty("[loading_requirement:" + ScopeRequirement.IDENTIFIER + "][requirement_mode:if_not][group:group_1][req_id:legacy_grouped]", "");

        RequirementContainer restored = RequirementContainer.deserializeWithIdentifier(container.identifier, serialized);

        assertEquals(container, restored);
    }

    @Test
    void metadataListingGroupsOfAnotherContainerStillFindsThem() {
        RequirementContainer source = new RequirementContainer();
        RequirementGroup group = source.createAndAddGroup("group_1", RequirementGroup.GroupMode.OR);
        assertNotNull(group);
        group.addInstance(requirement(source, RequirementInstance.RequirementMode.IF));
        PropertyContainer serialized = source.serialize();
        serialized.putProperty("[loading_requirement_container_meta:copy_owner]", serialized.getValue("[loading_requirement_container_meta:" + source.identifier + "]"));

        RequirementContainer sourceRestored = RequirementContainer.deserializeWithIdentifier(source.identifier, serialized);
        RequirementContainer copyOwner = RequirementContainer.deserializeWithIdentifier("copy_owner", serialized);

        assertNotNull(sourceRestored);
        assertNotNull(copyOwner);
        assertEquals(source.getGroups(), copyOwner.getGroups());
        assertNotSame(sourceRestored.getGroups().get(0), copyOwner.getGroups().get(0));
        assertLinked(sourceRestored);
        assertLinked(copyOwner);
    }

    @Test
    void singleContainerDeserializationReadsTaggedGroups() {
        RequirementContainer container = new RequirementContainer();
        RequirementGroup group = container.createAndAddGroup("group_1", RequirementGroup.GroupMode.OR);
        assertNotNull(group);
        group.addInstance(requirement(container, RequirementInstance.RequirementMode.IF));
        RequirementGroup nested = new RequirementGroup("group_2", RequirementGroup.GroupMode.AND, container);
        nested.addInstance(requirement(container, RequirementInstance.RequirementMode.IF_NOT));
        assertTrue(group.addGroup(nested));

        RequirementContainer restored = RequirementContainer.deserializeToSingleContainer(container.serialize());

        assertEquals(container.getGroups(), restored.getGroups());
        assertLinked(restored);
    }

    private static void assertLinked(@NotNull RequirementContainer container) {
        for (RequirementGroup group : container.getAllGroups()) {
            assertSame(container, group.parent, group.identifier);
            for (RequirementInstance instance : group.getInstances()) {
                assertSame(container, instance.parent, instance.instanceIdentifier);
                assertSame(group, instance.group, instance.instanceIdentifier);
            }
        }
        for (RequirementInstance instance : container.getInstances()) {
            assertSame(container, instance.parent, instance.instanceIdentifier);
        }
    }

    @NotNull
    private static RequirementInstance requirement(@NotNull RequirementContainer container, @NotNull RequirementInstance.RequirementMode mode) {
        return new RequirementInstance(REQUIREMENT, null, mode, container);
    }

    private static final class ScopeRequirement extends Requirement {

        private static final String IDENTIFIER = "test_group_scope_requirement";

        private ScopeRequirement() {
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
