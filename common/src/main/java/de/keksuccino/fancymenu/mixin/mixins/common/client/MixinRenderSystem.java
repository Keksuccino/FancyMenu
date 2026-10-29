package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.window.OffscreenTestClient;
import net.minecraft.util.TimeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {

    /** @reason Prepares SDL for the dev-only off-screen test client mode (see OffscreenTestClient). */
    @Inject(method = "initBackendSystem", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLInit;SDL_Init(I)Z", remap = false))
    private static void before_SDL_Init_FancyMenu(CallbackInfoReturnable<TimeSource.NanoTimeSource> info) {
        OffscreenTestClient.prepareBackendInit();
    }

}
