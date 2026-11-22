package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotItemDisplayNameFmPlaceholderTest {

    @BeforeAll
    static void bootStrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void filledSlotReturnsTheItemName() {
        String displayName = SlotItemDisplayNameFmPlaceholder.getSlotItemDisplayName(inventoryWithStoneInFirstSlot(), 0);

        assertTrue(displayName.contains(Items.STONE.getDescriptionId()), displayName);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -9, Integer.MIN_VALUE})
    void negativeSlotsReturnAnEmptyString(int slot) {
        assertEquals("", SlotItemDisplayNameFmPlaceholder.getSlotItemDisplayName(inventoryWithStoneInFirstSlot(), slot));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 1000, Integer.MAX_VALUE})
    void emptyAndMissingSlotsReturnAnEmptyString(int slot) {
        assertEquals("", SlotItemDisplayNameFmPlaceholder.getSlotItemDisplayName(inventoryWithStoneInFirstSlot(), slot));
    }

    @NotNull
    private static Inventory inventoryWithStoneInFirstSlot() {
        // Getting and setting items never uses the player of the inventory, so it doesn't need one
        Inventory inventory = new Inventory(null);
        inventory.setItem(0, new ItemStack(Items.STONE));
        return inventory;
    }

}
