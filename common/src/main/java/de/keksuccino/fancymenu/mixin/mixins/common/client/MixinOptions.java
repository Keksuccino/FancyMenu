package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public class MixinOptions {

    @Inject(method = "getMenuBackgroundBlurriness", at = @At("RETURN"), cancellable = true)
    private void after_getMenuBackgroundBlurriness_FancyMenu(CallbackInfoReturnable<Integer> info) {
        if (RenderingUtils.shouldOverrideBackgroundBlurRadius()) {
            info.setReturnValue(RenderingUtils.getOverrideBackgroundBlurRadius());
        }
    }

}
