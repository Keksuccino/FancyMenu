package de.keksuccino.fancymenu.customization.requirement.internal;

import de.keksuccino.fancymenu.customization.requirement.RequirementRegistry;
import de.keksuccino.fancymenu.customization.requirement.TestRequirement;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequirementContainerTest {

    private static final String REQUIREMENT_IDENTIFIER = "test_deserialized_container_ownership";

    @BeforeAll
    static void registerTestRequirement() {
        RequirementRegistry.register(new TestRequirement(REQUIREMENT_IDENTIFIER, value -> true));
    }

    @Test
    void identifiedContainersReceiveIndependentOwnedCopies() {
        PropertyContainer serialized = new PropertyContainer("test");
        serialized.putProperty("[loading_requirement_container_meta:first]", "[groups:shared_group;shared_group;][instances:shared_direct;shared_direct;]");
        serialized.putProperty("[loading_requirement_container_meta:second]", "[groups:shared_group;][instances:shared_direct;]");
        serialized.putProperty("[loading_requirement_group:shared_group]", "[group_mode:and]");
        serialized.putProperty("[loading_requirement:" + REQUIREMENT_IDENTIFIER + "][requirement_mode:if][group:shared_group][req_id:shared_grouped]", "grouped");
        serialized.putProperty("[loading_requirement:" + REQUIREMENT_IDENTIFIER + "][requirement_mode:if][req_id:shared_direct]", "direct");

        List<RequirementContainer> containers = RequirementContainer.deserializeAll(serialized);
        assertEquals(2, containers.size());
        RequirementContainer first = containers.get(0);
        RequirementContainer second = containers.get(1);
        RequirementGroup firstGroup = first.getGroup("shared_group");
        RequirementGroup secondGroup = second.getGroup("shared_group");
        assertEquals(1, first.getGroups().size());
        assertEquals(1, first.getInstances().size());
        RequirementInstance firstGrouped = firstGroup.getInstances().get(0);
        RequirementInstance secondGrouped = secondGroup.getInstances().get(0);
        RequirementInstance firstDirect = first.getInstances().get(0);
        RequirementInstance secondDirect = second.getInstances().get(0);

        assertNotSame(firstGroup, secondGroup);
        assertNotSame(firstGrouped, secondGrouped);
        assertNotSame(firstDirect, secondDirect);
        assertSame(first, firstGroup.parent);
        assertSame(second, secondGroup.parent);
        assertSame(first, firstGrouped.parent);
        assertSame(second, secondGrouped.parent);
        assertSame(firstGroup, firstGrouped.group);
        assertSame(secondGroup, secondGrouped.group);
        assertSame(first, firstDirect.parent);
        assertSame(second, secondDirect.parent);
        assertNull(firstDirect.group);
        assertNull(secondDirect.group);

        first.addValuePlaceholder("first_owner_only", () -> "first");
        assertTrue(firstGroup.getValuePlaceholders().containsKey("first_owner_only"));
        assertTrue(firstGrouped.getValuePlaceholders().containsKey("first_owner_only"));
        assertTrue(firstDirect.getValuePlaceholders().containsKey("first_owner_only"));
        assertFalse(secondGroup.getValuePlaceholders().containsKey("first_owner_only"));
        assertFalse(secondGrouped.getValuePlaceholders().containsKey("first_owner_only"));
        assertFalse(secondDirect.getValuePlaceholders().containsKey("first_owner_only"));

        firstGroup.mode = RequirementGroup.GroupMode.OR;
        firstGrouped.value = "changed grouped";
        firstDirect.value = "changed direct";
        assertEquals(RequirementGroup.GroupMode.AND, secondGroup.mode);
        assertEquals("grouped", secondGrouped.value);
        assertEquals("direct", secondDirect.value);
    }

}
