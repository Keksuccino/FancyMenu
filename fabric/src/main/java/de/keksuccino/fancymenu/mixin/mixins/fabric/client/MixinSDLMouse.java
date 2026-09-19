package de.keksuccino.fancymenu.mixin.mixins.fabric.client;

import de.keksuccino.fancymenu.util.rendering.ui.cursor.SdlCursorTracker;
import org.lwjgl.sdl.SDLMouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SDLMouse.class, remap = false)
public class MixinSDLMouse {

    @Inject(method = "SDL_CreateSystemCursor", at = @At("RETURN"))
    private static void after_SDL_CreateSystemCursor_FancyMenu(int shape, CallbackInfoReturnable<Long> cir) {
        SdlCursorTracker.onCreateSystemCursor(shape, cir.getReturnValue());
    }

    @Inject(method = "SDL_DestroyCursor", at = @At("RETURN"))
    private static void after_SDL_DestroyCursor_FancyMenu(long cursor, CallbackInfo ci) {
        SdlCursorTracker.onDestroyCursor(cursor);
    }

}
