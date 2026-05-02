package de.keksuccino.fancymenu.mixin.mixins.forge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {

    /**
     * @reason Prevent depth test state changes while FancyMenu's depth test lock is active.
     */
    @Inject(method = "enableDepthTest", at = @At("HEAD"), cancellable = true)
    private static void cancel_enableDepthTest_FancyMenu(CallbackInfo info) {
        if (RenderingUtils.isDepthTestLocked()) info.cancel();
    }

    /**
     * @reason Prevent depth test state changes while FancyMenu's depth test lock is active.
     */
    @Inject(method = "disableDepthTest", at = @At("HEAD"), cancellable = true)
    private static void cancel_disableDepthTest_FancyMenu(CallbackInfo info) {
        if (RenderingUtils.isDepthTestLocked()) info.cancel();
    }

}
