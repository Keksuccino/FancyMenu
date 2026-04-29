package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.RenderScaleUtil;
import de.keksuccino.fancymenu.util.rendering.RenderRotationUtil;
import de.keksuccino.fancymenu.util.rendering.RenderTranslationUtil;
import com.mojang.math.Quaternion;
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
    @Unique
    private final Deque<RenderTranslationUtil.TranslationState> renderTranslationStack_FancyMenu = new ArrayDeque<>();
    @Unique
    private final Deque<RenderRotationUtil.RotationState> renderRotationStack_FancyMenu = new ArrayDeque<>();

    @Inject(method = "pushPose", at = @At("TAIL"))
    private void after_pushPose_FancyMenu(CallbackInfo info) {
        if (this.isRenderSystemModelViewStack_FancyMenu()) {
            return;
        }
        ensureRenderScaleStackInitialized_FancyMenu();
        this.renderScaleStack_FancyMenu.addLast(this.renderScaleStack_FancyMenu.getLast());
        ensureRenderTranslationStackInitialized_FancyMenu();
        RenderTranslationUtil.TranslationState lastTranslation = this.renderTranslationStack_FancyMenu.getLast();
        RenderTranslationUtil.TranslationState nextTranslation = new RenderTranslationUtil.TranslationState();
        nextTranslation.x = lastTranslation.x;
        nextTranslation.y = lastTranslation.y;
        nextTranslation.z = lastTranslation.z;
        this.renderTranslationStack_FancyMenu.addLast(nextTranslation);
        ensureRenderRotationStackInitialized_FancyMenu();
        RenderRotationUtil.RotationState lastRotation = this.renderRotationStack_FancyMenu.getLast();
        this.renderRotationStack_FancyMenu.addLast(new RenderRotationUtil.RotationState(lastRotation));
        updateActiveRenderScale_FancyMenu();
        updateActiveRenderTranslation_FancyMenu();
        updateActiveRenderRotation_FancyMenu();
    }

    @Inject(method = "popPose", at = @At("TAIL"))
    private void after_popPose_FancyMenu(CallbackInfo info) {
        if (this.isRenderSystemModelViewStack_FancyMenu()) {
            return;
        }
        ensureRenderScaleStackInitialized_FancyMenu();
        if (this.renderScaleStack_FancyMenu.size() > 1) {
            this.renderScaleStack_FancyMenu.removeLast();
        }
        ensureRenderTranslationStackInitialized_FancyMenu();
        if (this.renderTranslationStack_FancyMenu.size() > 1) {
            this.renderTranslationStack_FancyMenu.removeLast();
        }
        ensureRenderRotationStackInitialized_FancyMenu();
        if (this.renderRotationStack_FancyMenu.size() > 1) {
            this.renderRotationStack_FancyMenu.removeLast();
        }
        updateActiveRenderScale_FancyMenu();
        updateActiveRenderTranslation_FancyMenu();
        updateActiveRenderRotation_FancyMenu();
    }

    @Inject(method = "scale", at = @At("TAIL"))
    private void after_scale_FancyMenu(float x, float y, float z, CallbackInfo info) {
        if (this.isRenderSystemModelViewStack_FancyMenu()) {
            return;
        }
        ensureRenderScaleStackInitialized_FancyMenu();
        float currentScale = this.renderScaleStack_FancyMenu.removeLast();
        float scaleFactor = RenderScaleUtil.getAbsoluteScaleFactor_FancyMenu(x, y, z);
        this.renderScaleStack_FancyMenu.addLast(currentScale * scaleFactor);
        updateActiveRenderScale_FancyMenu();
    }

    @Inject(method = "mulPose", at = @At("TAIL"))
    private void after_mulPose_FancyMenu(Quaternion quaternion, CallbackInfo info) {
        if (this.isRenderSystemModelViewStack_FancyMenu()) {
            return;
        }
        ensureRenderRotationStackInitialized_FancyMenu();
        RenderRotationUtil.RotationState currentRotation = this.renderRotationStack_FancyMenu.removeLast();
        currentRotation.mul(quaternion);
        this.renderRotationStack_FancyMenu.addLast(currentRotation);
        updateActiveRenderRotation_FancyMenu();
    }

    @Inject(method = "translate(DDD)V", at = @At("TAIL"))
    private void after_translate_FancyMenu(double x, double y, double z, CallbackInfo info) {
        if (this.isRenderSystemModelViewStack_FancyMenu()) {
            return;
        }
        ensureRenderTranslationStackInitialized_FancyMenu();
        RenderTranslationUtil.TranslationState currentTranslation = this.renderTranslationStack_FancyMenu.removeLast();
        float scaleFactor = RenderScaleUtil.getCurrentAdditionalRenderScale();
        currentTranslation.x += (float)(x * scaleFactor);
        currentTranslation.y += (float)(y * scaleFactor);
        currentTranslation.z += (float)(z * scaleFactor);
        this.renderTranslationStack_FancyMenu.addLast(currentTranslation);
        updateActiveRenderTranslation_FancyMenu();
    }

    @Unique
    private void ensureRenderScaleStackInitialized_FancyMenu() {
        if (this.renderScaleStack_FancyMenu.isEmpty()) {
            this.renderScaleStack_FancyMenu.addLast(1.0F);
        }
    }

    @Unique
    private boolean isRenderSystemModelViewStack_FancyMenu() {
        return RenderSystem.getModelViewStack() == (PoseStack)(Object)this;
    }

    @Unique
    private void ensureRenderTranslationStackInitialized_FancyMenu() {
        if (this.renderTranslationStack_FancyMenu.isEmpty()) {
            this.renderTranslationStack_FancyMenu.addLast(new RenderTranslationUtil.TranslationState());
        }
    }

    @Unique
    private void ensureRenderRotationStackInitialized_FancyMenu() {
        if (this.renderRotationStack_FancyMenu.isEmpty()) {
            this.renderRotationStack_FancyMenu.addLast(new RenderRotationUtil.RotationState());
        }
    }

    @Unique
    private void updateActiveRenderScale_FancyMenu() {
        RenderScaleUtil.setActiveRenderScale_FancyMenu(this.renderScaleStack_FancyMenu.getLast());
    }

    @Unique
    private void updateActiveRenderTranslation_FancyMenu() {
        RenderTranslationUtil.TranslationState translation = this.renderTranslationStack_FancyMenu.getLast();
        RenderTranslationUtil.setActiveRenderTranslation_FancyMenu(translation.x, translation.y, translation.z);
    }

    @Unique
    private void updateActiveRenderRotation_FancyMenu() {
        RenderRotationUtil.setActiveRenderRotation_FancyMenu(this.renderRotationStack_FancyMenu.getLast());
    }

}
