package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ReceivingLevelScreen.class)
public interface IMixinReceivingLevelScreen {

    @Mutable
    @Accessor("createdAt") void setCreatedAtFancyMenu(long createdAt);

}
