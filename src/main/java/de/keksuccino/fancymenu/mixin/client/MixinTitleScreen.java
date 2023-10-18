package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.events.ScreenBackgroundRenderedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.MainMenuHandler;
import de.keksuccino.konkrete.Konkrete;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//TODO übernehmen
@Mixin(TitleScreen.class)
public class MixinTitleScreen {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cancelTitleScreenRenderingInLoadingScreenFancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
//        if (MenuCustomization.isMenuCustomizable((Screen)((Object)this)) && (Minecraft.getInstance().getOverlay() != null) && (Minecraft.getInstance().getOverlay() instanceof LoadingOverlay)) {
//            info.cancel();
//        }

        //Cancel rendering and fire BackgroundRenderEvent to make MainMenuHandler work like supposed (that's just a dirty FMv2 hack)
        if (MenuCustomization.isMenuCustomizable((Screen)((Object)this))) {
            info.cancel();
            Konkrete.getEventHandler().callEventsFor(new ScreenBackgroundRenderedEvent((Screen)((Object)this), graphics));
        }

    }

//    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PanoramaRenderer;render(FF)V", shift = At.Shift.AFTER))
//    private void afterRenderBackgroundFancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
//        Konkrete.getEventHandler().callEventsFor(new ScreenBackgroundRenderedEvent((Screen)((Object)this), graphics));
//    }

}
