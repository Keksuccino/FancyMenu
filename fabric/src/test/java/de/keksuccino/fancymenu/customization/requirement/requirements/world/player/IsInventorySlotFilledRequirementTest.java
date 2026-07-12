package de.keksuccino.fancymenu.customization.requirement.requirements.world.player;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsInventorySlotFilledRequirementTest {

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void reportsEmptySingletonAsNotFilled() {
        assertFalse(IsInventorySlotFilledRequirement.isStackFilled(ItemStack.EMPTY));
    }

    @Test
    void reportsAirStackAsNotFilled() {
        assertFalse(IsInventorySlotFilledRequirement.isStackFilled(new ItemStack(Items.AIR)));
    }

    @Test
    void reportsPositiveCountStackAsFilled() {
        ItemStack stack = new ItemStack(Items.STONE, 64);

        assertTrue(IsInventorySlotFilledRequirement.isStackFilled(stack));
    }

    @Test
    void reportsInPlaceDepletedStackAsNotFilledAfterDrop() {
        ItemStack inventoryStack = new ItemStack(Items.STONE);

        assertTrue(IsInventorySlotFilledRequirement.isStackFilled(inventoryStack));
        ItemStack droppedStack = inventoryStack.split(1);

        assertTrue(IsInventorySlotFilledRequirement.isStackFilled(droppedStack));
        assertNotSame(ItemStack.EMPTY, inventoryStack);
        assertFalse(IsInventorySlotFilledRequirement.isStackFilled(inventoryStack));
    }

    @Test
    void reportsNegativeCountStackAsNotFilled() {
        ItemStack stack = new ItemStack(Items.STONE);
        stack.setCount(-1);

        assertFalse(IsInventorySlotFilledRequirement.isStackFilled(stack));
    }

}
