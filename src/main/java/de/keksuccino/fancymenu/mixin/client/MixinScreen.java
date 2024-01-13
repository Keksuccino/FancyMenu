package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.events.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.client.GuiInitCompletedEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MixinScreen {

    @Inject(at = @At("TAIL"), method = "<init>")
    protected void onConstructInstance(Component title, CallbackInfo info) {
        MenuHandlerBase.cachedOriginalMenuTitles.put(this.getClass(), title);
    }

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("HEAD"))
    private void beforeInit(Minecraft minecraft, int width, int height, CallbackInfo info) {
        InitOrResizeScreenEvent.Pre e = new InitOrResizeScreenEvent.Pre(((Screen)(Object)this));
        Konkrete.getEventHandler().callEventsFor(e);
    }

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("TAIL"))
    private void afterInit(Minecraft minecraft, int width, int height, CallbackInfo info) {
        InitOrResizeScreenEvent.Post e = new InitOrResizeScreenEvent.Post(((Screen)(Object)this));
        Konkrete.getEventHandler().callEventsFor(e);
        GuiInitCompletedEvent e2 = new GuiInitCompletedEvent((Screen) ((Object)this));
        Konkrete.getEventHandler().callEventsFor(e2);
    }

}
