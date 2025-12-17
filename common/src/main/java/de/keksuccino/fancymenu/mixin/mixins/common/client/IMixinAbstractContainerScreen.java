package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import javax.annotation.Nullable;

@Mixin(AbstractContainerScreen.class)
public interface IMixinAbstractContainerScreen {

    @Accessor("hoveredSlot") @Nullable Slot get_hoveredSlot_FancyMenu();

}
