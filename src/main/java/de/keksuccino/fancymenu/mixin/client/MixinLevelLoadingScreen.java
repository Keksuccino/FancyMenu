package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.WorldLoadingScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//TODO übernehmen
@Mixin(LevelLoadingScreen.class)
public class MixinLevelLoadingScreen {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void beforeRenderLevelLoadingScreenFancyMenu(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo info) {
        Screen screen = (Screen)((Object)this);
        if (MenuCustomization.isMenuCustomizable(screen)) {
            if (MenuCustomization.isMenuCustomizable(screen)) {
                MenuHandlerBase handler = MenuHandlerRegistry.getHandlerFor(screen);
                if (handler instanceof WorldLoadingScreenHandler h) {
                    info.cancel();
                    screen.renderBackground(guiGraphics);
                    h.renderMenu(guiGraphics, screen);
                }
            }
        }
    }

}
