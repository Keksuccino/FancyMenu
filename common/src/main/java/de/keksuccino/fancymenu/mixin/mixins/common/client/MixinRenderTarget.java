package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderTarget.class)
public class MixinRenderTarget {

    /**
     * @reason Preserve caller-controlled blending for RenderTarget blits that explicitly request it.
     */
    @Inject(method = "_blitToScreen", at = @At("HEAD"))
    private void before_blitToScreen_FancyMenu(int width, int height, boolean disableBlend, CallbackInfo info) {
        if (!disableBlend) {
            RenderingUtils.assumeOpaqueShaderBlendMode();
        }
    }

}
