package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.screens.ProgressScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ProgressScreen.class)
public interface IMixinProgressScreen {

    @Accessor("progress") int getProgressFancyMenu();

}
