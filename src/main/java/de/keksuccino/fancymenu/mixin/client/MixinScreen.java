package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
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

}
