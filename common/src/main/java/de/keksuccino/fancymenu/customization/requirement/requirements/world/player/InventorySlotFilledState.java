package de.keksuccino.fancymenu.customization.requirement.requirements.world.player;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

final class InventorySlotFilledState {

    private InventorySlotFilledState() {
    }

    static boolean isFilled(@Nullable ItemStack stack) {
        return stack != null && !stack.isEmpty();
    }

}
