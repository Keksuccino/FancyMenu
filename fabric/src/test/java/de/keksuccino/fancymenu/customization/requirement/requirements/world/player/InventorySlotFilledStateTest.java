package de.keksuccino.fancymenu.customization.requirement.requirements.world.player;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventorySlotFilledStateTest {

    @BeforeAll
    static void bootStrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void canonicalEmptyStackIsNotFilled() {
        assertFalse(InventorySlotFilledState.isFilled(ItemStack.EMPTY));
    }

    @Test
    void missingStackIsNotFilled() {
        assertFalse(InventorySlotFilledState.isFilled(null));
    }

    @Test
    void positiveStoneStackIsFilled() {
        assertTrue(InventorySlotFilledState.isFilled(new ItemStack(Items.STONE, 1)));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void nonPositiveStoneStackIsNotFilled(int count) {
        assertFalse(InventorySlotFilledState.isFilled(new ItemStack(Items.STONE, count)));
    }

    @Test
    void airStackIsNotFilled() {
        assertFalse(InventorySlotFilledState.isFilled(new ItemStack(Items.AIR, 1)));
    }

    @Test
    void partialSplitLeavesOriginalStackFilled() {
        ItemStack original = new ItemStack(Items.STONE, 2);
        ItemStack split = original.split(1);

        assertTrue(InventorySlotFilledState.isFilled(original));
        assertTrue(InventorySlotFilledState.isFilled(split));
    }

    @Test
    void fullSplitLeavesNoncanonicalOriginalStackEmpty() {
        ItemStack original = new ItemStack(Items.STONE, 1);
        ItemStack split = original.split(1);

        assertNotSame(ItemStack.EMPTY, original);
        assertFalse(InventorySlotFilledState.isFilled(original));
        assertTrue(InventorySlotFilledState.isFilled(split));
    }

}
