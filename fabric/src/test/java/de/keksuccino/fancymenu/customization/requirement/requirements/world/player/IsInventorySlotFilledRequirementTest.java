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
    static void bootstrapVanillaRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void canonicalEmptyStackIsNotFilled() {
        assertFalse(IsInventorySlotFilledRequirement.isFilled(ItemStack.EMPTY));
    }

    @Test
    void positiveCountStackIsFilled() {
        assertTrue(IsInventorySlotFilledRequirement.isFilled(new ItemStack(Items.STONE)));
    }

    @Test
    void airStackIsNotFilled() {
        assertFalse(IsInventorySlotFilledRequirement.isFilled(new ItemStack(Items.AIR)));
    }

    @Test
    void stackDepletedInPlaceBySplitIsNotFilled() {
        ItemStack selectedStack = new ItemStack(Items.STONE);

        ItemStack removedStack = selectedStack.split(1);

        assertFalse(removedStack.isEmpty());
        assertNotSame(ItemStack.EMPTY, selectedStack);
        assertFalse(IsInventorySlotFilledRequirement.isFilled(selectedStack));
    }

    @Test
    void nonPositiveCountStacksAreNotFilled() {
        ItemStack zeroCountStack = new ItemStack(Items.STONE);
        ItemStack negativeCountStack = new ItemStack(Items.STONE);
        zeroCountStack.setCount(0);
        negativeCountStack.setCount(-1);

        assertFalse(IsInventorySlotFilledRequirement.isFilled(zeroCountStack));
        assertFalse(IsInventorySlotFilledRequirement.isFilled(negativeCountStack));
    }

}
