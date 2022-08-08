package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {

    private static final Logger MIXIN_LOGGER = LogManager.getLogger("fm/MixinLocalPlayer");

    @Inject(at = @At("HEAD"), method = "chat")
    private void onChat(String s, CallbackInfo info) {

        MIXIN_LOGGER.info("################## onChat:_" + s);

        for (StackTraceElement e : new Throwable().getStackTrace()) {
            MIXIN_LOGGER.info(e.toString());
        }

    }

}
