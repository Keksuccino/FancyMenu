package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.RenderScaleUtil;
import de.keksuccino.fancymenu.util.rendering.RenderTranslationUtil;
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

    @Inject(method = "pushPose", at = @At("TAIL"))
    private void after_pushPose_FancyMenu(CallbackInfo info) {
        ensureRenderScaleStackInitialized_FancyMenu();
        this.renderScaleStack_FancyMenu.addLast(this.renderScaleStack_FancyMenu.getLast());
        ensureRenderTranslationStackInitialized_FancyMenu();
        RenderTranslationUtil.TranslationState lastTranslation = this.renderTranslationStack_FancyMenu.getLast();
        RenderTranslationUtil.TranslationState nextTranslation = new RenderTranslationUtil.TranslationState();
        nextTranslation.x = lastTranslation.x;
        nextTranslation.y = lastTranslation.y;
        nextTranslation.z = lastTranslation.z;
        this.renderTranslationStack_FancyMenu.addLast(nextTranslation);
        updateActiveRenderScale_FancyMenu();
        updateActiveRenderTranslation_FancyMenu();
    }

    @Inject(method = "popPose", at = @At("TAIL"))
    private void after_popPose_FancyMenu(CallbackInfo info) {
        ensureRenderScaleStackInitialized_FancyMenu();
        if (this.renderScaleStack_FancyMenu.size() > 1) {
            this.renderScaleStack_FancyMenu.removeLast();
        }
        ensureRenderTranslationStackInitialized_FancyMenu();
        if (this.renderTranslationStack_FancyMenu.size() > 1) {
            this.renderTranslationStack_FancyMenu.removeLast();
        }
        updateActiveRenderScale_FancyMenu();
        updateActiveRenderTranslation_FancyMenu();
    }

    @Inject(method = "scale", at = @At("TAIL"))
    private void after_scale_FancyMenu(float x, float y, float z, CallbackInfo info) {
        ensureRenderScaleStackInitialized_FancyMenu();
        float currentScale = this.renderScaleStack_FancyMenu.removeLast();
        float scaleFactor = RenderScaleUtil.getAbsoluteScaleFactor_FancyMenu(x, y, z);
        this.renderScaleStack_FancyMenu.addLast(currentScale * scaleFactor);
        updateActiveRenderScale_FancyMenu();
    }

    @Inject(method = "translate(FFF)V", at = @At("TAIL"))
    private void after_translate_FancyMenu(float x, float y, float z, CallbackInfo info) {
        ensureRenderTranslationStackInitialized_FancyMenu();
        RenderTranslationUtil.TranslationState currentTranslation = this.renderTranslationStack_FancyMenu.removeLast();
        float scaleFactor = RenderScaleUtil.getCurrentAdditionalRenderScale();
        currentTranslation.x += x * scaleFactor;
        currentTranslation.y += y * scaleFactor;
        currentTranslation.z += z * scaleFactor;
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
    private void ensureRenderTranslationStackInitialized_FancyMenu() {
        if (this.renderTranslationStack_FancyMenu.isEmpty()) {
            this.renderTranslationStack_FancyMenu.addLast(new RenderTranslationUtil.TranslationState());
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

}
