//---
package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PauseScreen.class)
public interface IMixinPauseScreen {

    @Accessor("showPauseMenu") public boolean getShowPauseMenuFancyMenu();

}
