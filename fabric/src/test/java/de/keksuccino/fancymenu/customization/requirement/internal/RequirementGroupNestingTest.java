package de.keksuccino.fancymenu.customization.requirement.internal;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.RequirementRegistry;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequirementGroupNestingTest {

    private static final CountingRequirement REQUIREMENT = new CountingRequirement();

    @BeforeAll
    static void registerTestRequirement() {
        RequirementRegistry.register(REQUIREMENT);
    }

    @BeforeEach
    void resetEvaluationCount() {
        REQUIREMENT.evaluationCount.set(0);
    }

    @Test
    void compoundLogicFollowsTheModesOfNestedGroups() {
        for (int combination = 0; combination < 16; combination++) {
            boolean a = (combination & 1) != 0;
            boolean b = (combination & 2) != 0;
            boolean c = (combination & 4) != 0;
            boolean d = (combination & 8) != 0;
            RequirementContainer container = new RequirementContainer();
            RequirementGroup either = container.createAndAddGroup("either", RequirementGroup.GroupMode.OR);
            assertNotNull(either);
            assertTrue(either.addGroup(group("first_pair", RequirementGroup.GroupMode.AND, container, a, b)));
            assertTrue(either.addGroup(group("second_pair", RequirementGroup.GroupMode.AND, container, c, d)));

            assertEquals((a && b) || (c && d), either.requirementsMet(), "Combination " + combination);
        }
    }

    @Test
    void directRequirementsGetCheckedBeforeNestedGroups() {
        RequirementContainer container = new RequirementContainer();
        RequirementGroup orGroup = container.createAndAddGroup("or_group", RequirementGroup.GroupMode.OR);
        assertNotNull(orGroup);
        assertTrue(orGroup.addGroup(group("nested_or", RequirementGroup.GroupMode.AND, container, true, true)));
        orGroup.addInstance(requirement(true, container));
        assertTrue(orGroup.requirementsMet());
        assertEquals(1, REQUIREMENT.evaluationCount.get());

        REQUIREMENT.evaluationCount.set(0);
        RequirementGroup andGroup = container.createAndAddGroup("and_group", RequirementGroup.GroupMode.AND);
        assertNotNull(andGroup);
        assertTrue(andGroup.addGroup(group("nested_and", RequirementGroup.GroupMode.AND, container, true, true)));
        andGroup.addInstance(requirement(false, container));
        assertFalse(andGroup.requirementsMet());
        assertEquals(1, REQUIREMENT.evaluationCount.get());
    }

    @Test
    void emptyGroupsKeepTheResultOfTheirMode() {
        RequirementContainer container = new RequirementContainer();
        RequirementGroup emptyAnd = new RequirementGroup("empty_and", RequirementGroup.GroupMode.AND, container);
        RequirementGroup emptyOr = new RequirementGroup("empty_or", RequirementGroup.GroupMode.OR, container);
        assertTrue(emptyAnd.requirementsMet());
        assertFalse(emptyOr.requirementsMet());

        RequirementGroup outer = container.createAndAddGroup("outer", RequirementGroup.GroupMode.OR);
        assertNotNull(outer);
        assertTrue(outer.addGroup(emptyAnd));
        assertTrue(outer.requirementsMet());
        assertEquals(0, REQUIREMENT.evaluationCount.get());
    }

    @Test
    void serializationKeepsNestedGroupsAndTheirOwnership() {
        RequirementContainer original = buildNestedContainer();
        PropertyContainer serialized = original.serialize();

        String meta = serialized.getValue("[loading_requirement_container_meta:" + original.identifier + "]");
        assertNotNull(meta);
        assertTrue(meta.contains("[groups:outer;standalone;]"), meta);
        String nestedGroupValue = serialized.getValue("[loading_requirement_group:deepest]" + RequirementContainer.serializeContainerTag(original.identifier));
        assertNotNull(nestedGroupValue);
        assertTrue(nestedGroupValue.contains("[parent_group:second_pair]"), nestedGroupValue);

        RequirementContainer deserialized = RequirementContainer.deserializeWithIdentifier(original.identifier, serialized);
        assertNotNull(deserialized);
        assertEquals(original, deserialized);
        assertTreeIsLinked(deserialized);
        assertGroupPaths(deserialized);

        RequirementContainer single = RequirementContainer.deserializeToSingleContainer(serialized);
        assertTreeIsLinked(single);
        assertGroupPaths(single);
        assertEquals(original.getGroup("outer"), single.getGroup("outer"));
        assertEquals(original.getGroup("standalone"), single.getGroup("standalone"));
    }

    @Test
    void nestedGroupsSerializedBeforeTheirParentStillGetNested() {
        PropertyContainer serialized = new PropertyContainer("test");
        serialized.putProperty("[loading_requirement_group:child]", "[group_mode:and][parent_group:parent]");
        serialized.putProperty("[loading_requirement:" + CountingRequirement.IDENTIFIER + "][requirement_mode:if][group:child][req_id:child_requirement]", "");
        serialized.putProperty("[loading_requirement_group:parent]", "[group_mode:or]");
        serialized.putProperty("[loading_requirement_container_meta:owner]", "[groups:parent;][instances:]");

        RequirementContainer container = RequirementContainer.deserializeWithIdentifier("owner", serialized);
        assertNotNull(container);
        assertEquals(1, container.getGroups().size());
        RequirementGroup parent = container.getGroups().get(0);
        assertEquals("parent", parent.identifier);
        assertEquals(RequirementGroup.GroupMode.OR, parent.mode);
        assertEquals(1, parent.getGroups().size());
        RequirementGroup child = parent.getGroups().get(0);
        assertEquals("child", child.identifier);
        assertEquals(1, child.getInstances().size());
        assertEquals("child_requirement", child.getInstances().get(0).instanceIdentifier);
        assertTreeIsLinked(container);
    }

    @Test
    void groupsWithMissingOrCyclicParentsStayTopLevelGroupsWithTheirRequirements() {
        PropertyContainer serialized = new PropertyContainer("test");
        serialized.putProperty("[loading_requirement_group:cycle_a]", "[group_mode:and][parent_group:cycle_b]");
        serialized.putProperty("[loading_requirement_group:cycle_b]", "[group_mode:and][parent_group:cycle_a]");
        serialized.putProperty("[loading_requirement_group:orphan]", "[group_mode:and][parent_group:missing_group]");
        serialized.putProperty("[loading_requirement_group:self]", "[group_mode:and][parent_group:self]");
        for (String groupId : List.of("cycle_a", "cycle_b", "orphan", "self")) {
            serialized.putProperty("[loading_requirement:" + CountingRequirement.IDENTIFIER + "][requirement_mode:if][group:" + groupId + "][req_id:" + groupId + "_requirement]", "");
        }

        RequirementContainer container = RequirementContainer.deserializeToSingleContainer(serialized);

        assertEquals(4, container.getAllGroups().size());
        for (RequirementGroup group : container.getAllGroups()) {
            assertEquals(1, group.getInstances().size(), group.identifier);
            assertEquals(group.identifier + "_requirement", group.getInstances().get(0).instanceIdentifier);
        }
        RequirementGroup orphan = container.getGroup("orphan");
        RequirementGroup self = container.getGroup("self");
        assertNotNull(orphan);
        assertNotNull(self);
        assertNull(orphan.parentGroup);
        assertNull(self.parentGroup);
        assertTrue(self.getGroups().isEmpty());
        RequirementGroup cycleA = container.getGroup("cycle_a");
        RequirementGroup cycleB = container.getGroup("cycle_b");
        assertNotNull(cycleA);
        assertNotNull(cycleB);
        assertTrue(((cycleA.parentGroup == cycleB) && (cycleB.parentGroup == null)) || ((cycleB.parentGroup == cycleA) && (cycleA.parentGroup == null)));
        assertEquals(3, container.getGroups().size());
        assertTreeIsLinked(container);
    }

    @Test
    void containersSharingOnePropertyContainerKeepTheirOwnNestedGroups() {
        RequirementContainer first = new RequirementContainer();
        RequirementGroup firstOuter = first.createAndAddGroup("first_outer", RequirementGroup.GroupMode.OR);
        assertNotNull(firstOuter);
        assertTrue(firstOuter.addGroup(group("first_inner", RequirementGroup.GroupMode.AND, first, true, false)));
        RequirementContainer second = new RequirementContainer();
        RequirementGroup secondOuter = second.createAndAddGroup("second_outer", RequirementGroup.GroupMode.AND);
        assertNotNull(secondOuter);
        assertTrue(secondOuter.addGroup(group("second_inner", RequirementGroup.GroupMode.OR, second, true)));
        PropertyContainer serialized = new PropertyContainer("test");
        first.serializeToExistingPropertyContainer(serialized);
        second.serializeToExistingPropertyContainer(serialized);

        RequirementContainer firstDeserialized = RequirementContainer.deserializeWithIdentifier(first.identifier, serialized);
        RequirementContainer secondDeserialized = RequirementContainer.deserializeWithIdentifier(second.identifier, serialized);

        assertEquals(first, firstDeserialized);
        assertEquals(second, secondDeserialized);
        assertTreeIsLinked(firstDeserialized);
        assertTreeIsLinked(secondDeserialized);
    }

    @Test
    void copiesKeepTheNestedStructureAndAreIndependent() {
        RequirementContainer original = buildNestedContainer();

        RequirementContainer copy = original.copy(false);
        assertEquals(original, copy);
        assertTreeIsLinked(copy);
        assertGroupPaths(copy);
        RequirementGroup copiedDeepest = copy.getGroup("deepest");
        assertNotNull(copiedDeepest);
        copiedDeepest.mode = RequirementGroup.GroupMode.AND;
        copiedDeepest.getInstances().get(0).mode = RequirementInstance.RequirementMode.IF_NOT;
        RequirementGroup originalDeepest = original.getGroup("deepest");
        assertNotNull(originalDeepest);
        assertEquals(RequirementGroup.GroupMode.OR, originalDeepest.mode);
        assertEquals(RequirementInstance.RequirementMode.IF, originalDeepest.getInstances().get(0).mode);

        RequirementContainer uniqueCopy = original.copy(true);
        assertTreeIsLinked(uniqueCopy);
        assertEquals(original.getAllGroups().size(), uniqueCopy.getAllGroups().size());
        Set<String> originalIdentifiers = new HashSet<>();
        original.getAllGroups().forEach(group -> originalIdentifiers.add(group.identifier));
        Set<String> uniqueIdentifiers = new HashSet<>();
        for (RequirementGroup group : uniqueCopy.getAllGroups()) {
            assertFalse(originalIdentifiers.contains(group.identifier), group.identifier);
            assertTrue(uniqueIdentifiers.add(group.identifier), group.identifier);
        }
        RequirementGroup uniqueOuter = uniqueCopy.getGroups().get(0);
        assertEquals(2, uniqueOuter.getGroups().size());
        assertEquals(1, uniqueOuter.getGroups().get(1).getGroups().size());
    }

    @Test
    void groupIdentifiersAreUniqueInTheWholeGroupTree() {
        RequirementContainer container = buildNestedContainer();

        assertNull(container.createAndAddGroup("deepest", RequirementGroup.GroupMode.AND));
        assertFalse(container.addGroup(new RequirementGroup("first_pair", RequirementGroup.GroupMode.AND, container)));
        RequirementGroup standalone = container.getGroup("standalone");
        assertNotNull(standalone);
        assertFalse(standalone.addGroup(new RequirementGroup("second_pair", RequirementGroup.GroupMode.AND, container)));
        RequirementGroup detached = new RequirementGroup("detached", RequirementGroup.GroupMode.AND, null);
        assertFalse(detached.addGroup(new RequirementGroup("detached", RequirementGroup.GroupMode.AND, null)));
        assertTrue(container.groupExists("deepest"));
        assertEquals(5, container.getAllGroups().size());
    }

    @Test
    void groupsCanNeitherContainThemselvesNorBeNestedTwice() {
        RequirementContainer container = buildNestedContainer();
        RequirementGroup outer = container.getGroup("outer");
        RequirementGroup deepest = container.getGroup("deepest");
        RequirementGroup standalone = container.getGroup("standalone");
        assertNotNull(outer);
        assertNotNull(deepest);
        assertNotNull(standalone);

        assertFalse(outer.addGroup(outer));
        assertFalse(deepest.addGroup(outer));
        assertFalse(standalone.addGroup(deepest));
        assertFalse(container.addGroup(deepest));
        assertSame(outer, container.getGroups().get(0));
        assertTreeIsLinked(container);
    }

    @Test
    void valuePlaceholdersReachRequirementsOfNestedGroups() {
        Supplier<String> supplier = () -> "value";
        RequirementContainer container = buildNestedContainer();
        container.addValuePlaceholder("late_placeholder", supplier);
        RequirementGroup deepest = container.getGroup("deepest");
        assertNotNull(deepest);
        assertSame(supplier, deepest.getInstances().get(0).getValuePlaceholders().get("late_placeholder"));

        RequirementContainer target = new RequirementContainer();
        target.addValuePlaceholder("early_placeholder", supplier);
        RequirementGroup outerCopy = container.getGroups().get(0).copy(false);
        assertTrue(target.addGroup(outerCopy));
        RequirementGroup copiedDeepest = target.getGroup("deepest");
        assertNotNull(copiedDeepest);
        assertSame(supplier, copiedDeepest.getInstances().get(0).getValuePlaceholders().get("early_placeholder"));
        assertTreeIsLinked(target);
    }

    @Test
    void nestedGroupsCanGetRemovedByIdentifier() {
        RequirementContainer container = buildNestedContainer();
        RequirementGroup secondPair = container.getGroup("second_pair");
        RequirementGroup outer = container.getGroup("outer");
        assertNotNull(secondPair);
        assertNotNull(outer);

        assertTrue(container.removeGroupByIdentifier("second_pair"));

        assertNull(secondPair.parentGroup);
        assertEquals(1, outer.getGroups().size());
        assertNull(container.getGroup("second_pair"));
        assertNull(container.getGroup("deepest"));
        assertFalse(container.removeGroupByIdentifier("second_pair"));
    }

    @Test
    void stackedContainersKeepTheirNestedGroups() {
        RequirementContainer first = buildNestedContainer();
        RequirementContainer second = buildNestedContainer();

        RequirementContainer stack = RequirementContainer.stackContainers(first, second);

        assertEquals(10, stack.getAllGroups().size());
        assertEquals(4, stack.getGroups().size());
        assertTreeIsLinked(stack);
    }

    /**
     * Builds {@code outer(OR: first_pair(AND: req, req), second_pair(AND: req, deepest(OR: req)))}, {@code standalone(AND: req)} and one direct requirement.
     */
    @NotNull
    private static RequirementContainer buildNestedContainer() {
        RequirementContainer container = new RequirementContainer();
        RequirementGroup outer = container.createAndAddGroup("outer", RequirementGroup.GroupMode.OR);
        assertNotNull(outer);
        assertTrue(outer.addGroup(group("first_pair", RequirementGroup.GroupMode.AND, container, true, false)));
        RequirementGroup secondPair = group("second_pair", RequirementGroup.GroupMode.AND, container, true);
        assertTrue(outer.addGroup(secondPair));
        assertTrue(secondPair.addGroup(group("deepest", RequirementGroup.GroupMode.OR, container, true)));
        RequirementGroup standalone = container.createAndAddGroup("standalone", RequirementGroup.GroupMode.AND);
        assertNotNull(standalone);
        standalone.addInstance(requirement(true, container));
        assertTrue(container.addInstance(requirement(false, container)));
        return container;
    }

    private static void assertGroupPaths(@NotNull RequirementContainer container) {
        RequirementGroup outer = container.getGroup("outer");
        RequirementGroup firstPair = container.getGroup("first_pair");
        RequirementGroup secondPair = container.getGroup("second_pair");
        RequirementGroup deepest = container.getGroup("deepest");
        RequirementGroup standalone = container.getGroup("standalone");
        assertNotNull(outer);
        assertNotNull(firstPair);
        assertNotNull(secondPair);
        assertNotNull(deepest);
        assertNotNull(standalone);
        assertNull(outer.parentGroup);
        assertNull(standalone.parentGroup);
        assertSame(outer, firstPair.parentGroup);
        assertSame(outer, secondPair.parentGroup);
        assertSame(secondPair, deepest.parentGroup);
        assertEquals(RequirementGroup.GroupMode.OR, deepest.mode);
        assertEquals(2, container.getGroups().size());
        assertEquals(1, container.getInstances().size());
    }

    private static void assertTreeIsLinked(@Nullable RequirementContainer container) {
        assertNotNull(container);
        for (RequirementGroup group : container.getGroups()) {
            assertNull(group.parentGroup, group.identifier);
        }
        for (RequirementGroup group : container.getAllGroups()) {
            assertSame(container, group.parent, group.identifier);
            for (RequirementGroup nested : group.getGroups()) {
                assertSame(group, nested.parentGroup, nested.identifier);
            }
            for (RequirementInstance instance : group.getInstances()) {
                assertSame(container, instance.parent, instance.instanceIdentifier);
                assertSame(group, instance.group, instance.instanceIdentifier);
            }
        }
        for (RequirementInstance instance : container.getInstances()) {
            assertSame(container, instance.parent, instance.instanceIdentifier);
            assertNull(instance.group, instance.instanceIdentifier);
        }
    }

    @NotNull
    private static RequirementGroup group(@NotNull String identifier, @NotNull RequirementGroup.GroupMode mode, @NotNull RequirementContainer container, boolean... requirementResults) {
        RequirementGroup group = new RequirementGroup(identifier, mode, container);
        for (boolean met : requirementResults) {
            group.addInstance(requirement(met, container));
        }
        return group;
    }

    /**
     * The test requirement is always met, so the requirement mode decides whether the instance is met.
     */
    @NotNull
    private static RequirementInstance requirement(boolean met, @NotNull RequirementContainer container) {
        return new RequirementInstance(REQUIREMENT, null, met ? RequirementInstance.RequirementMode.IF : RequirementInstance.RequirementMode.IF_NOT, container);
    }

    private static final class CountingRequirement extends Requirement {

        private static final String IDENTIFIER = "test_nested_group_requirement";

        private final AtomicInteger evaluationCount = new AtomicInteger();

        private CountingRequirement() {
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
            this.evaluationCount.incrementAndGet();
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
