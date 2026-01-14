package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.RenderScaleUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(PoseStack.class)
public class MixinPoseStack {

    @Unique
    private final Deque<Float> renderScaleStack_FancyMenu = new ArrayDeque<>();

    @Inject(method = "pushPose", at = @At("TAIL"))
    private void after_pushPose_FancyMenu(CallbackInfo info) {
        ensureRenderScaleStackInitialized_FancyMenu();
        this.renderScaleStack_FancyMenu.addLast(this.renderScaleStack_FancyMenu.getLast());
        updateActiveRenderScale_FancyMenu();
    }

    @Inject(method = "popPose", at = @At("TAIL"))
    private void after_popPose_FancyMenu(CallbackInfo info) {
        ensureRenderScaleStackInitialized_FancyMenu();
        if (this.renderScaleStack_FancyMenu.size() > 1) {
            this.renderScaleStack_FancyMenu.removeLast();
        }
        updateActiveRenderScale_FancyMenu();
    }

    @Inject(method = "scale", at = @At("TAIL"))
    private void after_scale_FancyMenu(float x, float y, float z, CallbackInfo info) {
        ensureRenderScaleStackInitialized_FancyMenu();
        float currentScale = this.renderScaleStack_FancyMenu.removeLast();
        float scaleFactor = RenderScaleUtil.getAbsoluteScaleFactor_FancyMenu(x, y, z);
        this.renderScaleStack_FancyMenu.addLast(currentScale * scaleFactor);
        updateActiveRenderScale_FancyMenu();
    }

    @Unique
    private void ensureRenderScaleStackInitialized_FancyMenu() {
        if (this.renderScaleStack_FancyMenu.isEmpty()) {
            this.renderScaleStack_FancyMenu.addLast(1.0F);
        }
    }

    @Unique
    private void updateActiveRenderScale_FancyMenu() {
        RenderScaleUtil.setActiveRenderScale_FancyMenu(this.renderScaleStack_FancyMenu.getLast());
    }

}
