package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.events.ScreenBackgroundRenderedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.konkrete.Konkrete;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//TODO übernehmen
@Mixin(Screen.class)
public class MixinScreen {

    @Inject(at = @At("TAIL"), method = "<init>")
    protected void onConstructFancyMenu(Component title, CallbackInfo info) {
        MenuHandlerBase.cachedOriginalMenuTitles.put(this.getClass(), title);
    }

    @Inject(method = "renderTransparentBackground", at = @At("RETURN"))
    private void afterRenderTransparentBackgroundFancyMenu(GuiGraphics guiGraphics, CallbackInfo info) {
        Konkrete.getEventHandler().callEventsFor(new ScreenBackgroundRenderedEvent((Screen)((Object)this), guiGraphics));
    }

    @Inject(method = "renderDirtBackground", at = @At("RETURN"))
    private void afterRenderDirtBackgroundFancyMenu(GuiGraphics guiGraphics, CallbackInfo info) {
        Konkrete.getEventHandler().callEventsFor(new ScreenBackgroundRenderedEvent((Screen)((Object)this), guiGraphics));
    }

}
