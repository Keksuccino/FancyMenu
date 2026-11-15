package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.util.window.OffscreenTestClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InputConstants.class)
public class MixinInputConstants {

    /**
     * @reason Off-screen test clients must never move or lock the mouse cursor of the user (see OffscreenTestClient).
     * GLFW only moves the real cursor for a focused window, but the mode must not depend on the game never getting focus.
     */
    @Inject(method = "grabOrReleaseMouse", at = @At("HEAD"), cancellable = true)
    private static void cancel_grabOrReleaseMouse_FancyMenu(Window window, int cursorMode, double xpos, double ypos, CallbackInfo info) {
        if (OffscreenTestClient.isEnabled()) info.cancel();
    }

}
