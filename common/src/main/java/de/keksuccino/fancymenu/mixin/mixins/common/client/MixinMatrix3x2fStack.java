package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.RenderRotationUtil;
import de.keksuccino.fancymenu.util.rendering.RenderScaleUtil;
import de.keksuccino.fancymenu.util.rendering.RenderTranslationUtil;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(value = Matrix3x2fStack.class, remap = false)
public abstract class MixinMatrix3x2fStack extends Matrix3x2f {

    @Unique
    private final Deque<Float> renderScaleStack_FancyMenu = new ArrayDeque<>();
    @Unique
    private final Deque<RenderTranslationUtil.TranslationState> renderTranslationStack_FancyMenu = new ArrayDeque<>();
    @Unique
    private final Deque<RenderRotationUtil.RotationState> renderRotationStack_FancyMenu = new ArrayDeque<>();

    @Inject(method = "pushMatrix", at = @At("TAIL"), remap = false)
    private void after_pushMatrix_FancyMenu(CallbackInfoReturnable<Matrix3x2fStack> info) {
        ensureRenderScaleStackInitialized_FancyMenu();
        this.renderScaleStack_FancyMenu.addLast(this.renderScaleStack_FancyMenu.getLast());
        ensureRenderTranslationStackInitialized_FancyMenu();
        this.renderTranslationStack_FancyMenu.addLast(copyTranslation_FancyMenu(this.renderTranslationStack_FancyMenu.getLast()));
        ensureRenderRotationStackInitialized_FancyMenu();
        this.renderRotationStack_FancyMenu.addLast(new RenderRotationUtil.RotationState(this.renderRotationStack_FancyMenu.getLast()));
        updateActiveRenderScale_FancyMenu();
        updateActiveRenderTranslation_FancyMenu();
        updateActiveRenderRotation_FancyMenu();
    }

    @Inject(method = "popMatrix", at = @At("TAIL"), remap = false)
    private void after_popMatrix_FancyMenu(CallbackInfoReturnable<Matrix3x2fStack> info) {
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

    @Inject(method = "clear", at = @At("TAIL"), remap = false)
    private void after_clear_FancyMenu(CallbackInfoReturnable<Matrix3x2fStack> info) {
        this.renderScaleStack_FancyMenu.clear();
        this.renderTranslationStack_FancyMenu.clear();
        this.renderRotationStack_FancyMenu.clear();
        ensureRenderScaleStackInitialized_FancyMenu();
        ensureRenderTranslationStackInitialized_FancyMenu();
        ensureRenderRotationStackInitialized_FancyMenu();
        updateActiveRenderScale_FancyMenu();
        updateActiveRenderTranslation_FancyMenu();
        updateActiveRenderRotation_FancyMenu();
    }

    @Override
    public Matrix3x2f translate(float x, float y) {
        Matrix3x2f result = super.translate(x, y);
        after_translate_FancyMenu(x, y);
        return result;
    }

    @Override
    public Matrix3x2f scale(float x, float y) {
        Matrix3x2f result = super.scale(x, y);
        after_scale_FancyMenu(x, y);
        return result;
    }

    @Override
    public Matrix3x2f scale(float xy) {
        Matrix3x2f result = super.scale(xy);
        after_scale_FancyMenu(xy, xy);
        return result;
    }

    @Override
    public Matrix3x2f rotate(float ang) {
        Matrix3x2f result = super.rotate(ang);
        after_rotate_FancyMenu(ang);
        return result;
    }

    @Unique
    private void after_translate_FancyMenu(float x, float y) {
        ensureRenderScaleStackInitialized_FancyMenu();
        ensureRenderTranslationStackInitialized_FancyMenu();
        RenderTranslationUtil.TranslationState currentTranslation = this.renderTranslationStack_FancyMenu.removeLast();
        float scaleFactor = this.renderScaleStack_FancyMenu.getLast();
        currentTranslation.x += x * scaleFactor;
        currentTranslation.y += y * scaleFactor;
        this.renderTranslationStack_FancyMenu.addLast(currentTranslation);
        updateActiveRenderTranslation_FancyMenu();
    }

    @Unique
    private void after_scale_FancyMenu(float x, float y) {
        ensureRenderScaleStackInitialized_FancyMenu();
        float currentScale = this.renderScaleStack_FancyMenu.removeLast();
        float scaleFactor = getAbsoluteScaleFactor_FancyMenu(x, y);
        this.renderScaleStack_FancyMenu.addLast(currentScale * scaleFactor);
        updateActiveRenderScale_FancyMenu();
    }

    @Unique
    private void after_rotate_FancyMenu(float angleRadians) {
        ensureRenderRotationStackInitialized_FancyMenu();
        RenderRotationUtil.RotationState currentRotation = this.renderRotationStack_FancyMenu.removeLast();
        multiplyZRotation_FancyMenu(currentRotation, angleRadians);
        this.renderRotationStack_FancyMenu.addLast(currentRotation);
        updateActiveRenderRotation_FancyMenu();
    }

    @Unique
    private float getAbsoluteScaleFactor_FancyMenu(float x, float y) {
        float absX = Math.abs(x);
        float absY = Math.abs(y);
        if (absX == 0.0F && absY == 0.0F) {
            return 1.0F;
        }
        if (Math.abs(absX - absY) < 1.0E-4F) {
            return absX;
        }
        return (absX + absY) * 0.5F;
    }

    @Unique
    private static void multiplyZRotation_FancyMenu(RenderRotationUtil.RotationState state, float angleRadians) {
        float halfAngle = angleRadians * 0.5F;
        float qz = (float)Math.sin(halfAngle);
        float qw = (float)Math.cos(halfAngle);

        float nx = state.x * qw + state.y * qz;
        float ny = -state.x * qz + state.y * qw;
        float nz = state.w * qz + state.z * qw;
        float nw = state.w * qw - state.z * qz;

        state.x = nx;
        state.y = ny;
        state.z = nz;
        state.w = nw;

        float len = (float)Math.sqrt(state.x * state.x + state.y * state.y + state.z * state.z + state.w * state.w);
        if (len > 0.0F && Float.isFinite(len)) {
            float invLen = 1.0F / len;
            state.x *= invLen;
            state.y *= invLen;
            state.z *= invLen;
            state.w *= invLen;
        }
    }

    @Unique
    private RenderTranslationUtil.TranslationState copyTranslation_FancyMenu(RenderTranslationUtil.TranslationState translation) {
        RenderTranslationUtil.TranslationState copy = new RenderTranslationUtil.TranslationState();
        copy.x = translation.x;
        copy.y = translation.y;
        copy.z = translation.z;
        return copy;
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
