package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RealmsNotificationsScreen.class)
public class MixinRealmsNotificationsScreen {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstructFancyMenu(CallbackInfo info) {
        //This is to avoid crashes in the TitleScreen
        this.getRealmsScreenFancyMenu().init(0, 0);
    }

    @SuppressWarnings("all")
    private RealmsNotificationsScreen getRealmsScreenFancyMenu() {
        return (RealmsNotificationsScreen) ((Object)this);
    }

}
