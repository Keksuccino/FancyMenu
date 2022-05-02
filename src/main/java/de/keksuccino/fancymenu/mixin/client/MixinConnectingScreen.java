package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.screen.ConnectingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectingScreen.class)
public class MixinConnectingScreen {

    @Inject(at = @At("HEAD"), method = "connect", cancellable = true)
    private void onConnect(String ip, int port, CallbackInfo info) {
        if (ip.equals("%fancymenu_dummy_address%")) {
            info.cancel();
        }
    }

}
