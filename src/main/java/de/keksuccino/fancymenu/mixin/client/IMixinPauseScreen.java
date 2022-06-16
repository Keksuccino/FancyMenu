package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.screen.IngameMenuScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IngameMenuScreen.class)
public interface IMixinPauseScreen {

    @Accessor("isFullMenu") public boolean getShowPauseMenuFancyMenu();

}
