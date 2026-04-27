package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.global.SeamlessWorldLoadingHandler;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Shadow @Final Minecraft minecraft;

    @Inject(method = "processBlurEffect", at = @At("HEAD"), cancellable = true)
    private void head_processBlurEffect_FancyMenu(CallbackInfo info) {
        if (RenderingUtils.isVanillaMenuBlurringBlocked()) info.cancel();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void before_render_FancyMenu(DeltaTracker $$0, boolean $$1, CallbackInfo info) {
        ScreenCustomization.onPreGameRenderTick();
    }

    @Inject(
        method = "renderLevel",
	        at = @At(
	            value = "INVOKE",
	            target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V",
	            shift = At.Shift.BEFORE
	        )
    )
    private void beforeRenderItemInHand_FancyMenu(DeltaTracker $$0, CallbackInfo info) {
        if (this.minecraft != null && this.minecraft.level != null) {
            SeamlessWorldLoadingHandler.captureFrameIfNeeded(this.minecraft.getMainRenderTarget());
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
    private void afterRenderLevel_FancyMenu(DeltaTracker $$0, boolean $$1, CallbackInfo info) {
        if (this.minecraft != null && this.minecraft.level != null) {
            SeamlessWorldLoadingHandler.captureFrameIfNeeded(this.minecraft.getMainRenderTarget());
        }
    }

}
