package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {

    @Inject(method = "enableDepthTest", at = @At("HEAD"), remap = false, cancellable = true)
    private static void head_enableDepthTest_FancyMenu(CallbackInfo info) {
        if (RenderingUtils.isDepthTestLocked()) info.cancel();
    }

    @Inject(method = "disableDepthTest", at = @At("HEAD"), remap = false, cancellable = true)
    private static void head_disableDepthTest_FancyMenu(CallbackInfo info) {
        if (RenderingUtils.isDepthTestLocked()) info.cancel();
    }

}
