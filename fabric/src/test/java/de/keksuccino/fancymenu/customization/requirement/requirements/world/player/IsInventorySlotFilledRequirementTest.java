package de.keksuccino.fancymenu.customization.requirement.requirements.world.player;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsInventorySlotFilledRequirementTest {

    @BeforeAll
    static void initializeMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void reportsPositiveCountItemStackAsFilled() {
        assertTrue(IsInventorySlotFilledRequirement.isStackFilled(new ItemStack(Items.STONE, 1)));
    }

    @Test
    void reportsCanonicalAndAirItemStacksAsEmpty() {
        assertFalse(IsInventorySlotFilledRequirement.isStackFilled(ItemStack.EMPTY));
        assertFalse(IsInventorySlotFilledRequirement.isStackFilled(new ItemStack(Items.AIR, 1)));
    }

    @Test
    void reportsNonPositiveCountItemStacksAsEmpty() {
        assertFalse(IsInventorySlotFilledRequirement.isStackFilled(new ItemStack(Items.STONE, 0)));
        assertFalse(IsInventorySlotFilledRequirement.isStackFilled(new ItemStack(Items.STONE, -1)));
    }

    @Test
    void reportsStoredStackAsEmptyAfterRemovingItsLastItemInPlace() {
        ItemStack storedStack = new ItemStack(Items.STONE, 1);
        List<ItemStack> slot = new ArrayList<>(List.of(storedStack));

        assertTrue(IsInventorySlotFilledRequirement.isStackFilled(storedStack));
        ItemStack removedStack = ContainerHelper.removeItem(slot, 0, 1);

        assertEquals(1, removedStack.getCount());
        assertSame(storedStack, slot.get(0));
        assertEquals(0, storedStack.getCount());
        assertFalse(IsInventorySlotFilledRequirement.isStackFilled(storedStack));
    }

}
