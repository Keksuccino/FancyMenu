package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.runtime.AnimationControllerRuntime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Public integration facade for the element animation runtime. */
public final class AnimationControllerHandler {

    private AnimationControllerHandler() {
    }

    public static boolean applyAnimation(@NotNull AnimationControllerElement controller, @NotNull AnimationControllerElement.TargetElement targetConfig, @Nullable AbstractElement targetElement) {
        return AnimationControllerRuntime.applyAnimation(controller, targetConfig, targetElement);
    }

    public static void tick() {
        AnimationControllerRuntime.tick();
    }

    public static void resetAnimationState(@NotNull String targetElementId) {
        AnimationControllerRuntime.resetAnimationState(targetElementId);
    }

    public static void resetController(@NotNull AnimationControllerElement controller) {
        AnimationControllerRuntime.resetController(controller);
    }

    public static void stopAnimation(@NotNull String targetElementId) {
        AnimationControllerRuntime.stopAnimation(targetElementId);
    }

    public static void stopAllAnimations() {
        AnimationControllerRuntime.stopAllAnimations();
    }

    public static void clearMemory() {
        AnimationControllerRuntime.clearMemory();
    }

    public static boolean wasAnimatedInThePast(@NotNull String targetElementId) {
        return AnimationControllerRuntime.wasAnimatedInThePast(targetElementId);
    }

    public static boolean isAnimating(@NotNull String targetElementId) {
        return AnimationControllerRuntime.isAnimating(targetElementId);
    }

    public static boolean isFinished(@NotNull String targetElementId) {
        return AnimationControllerRuntime.isFinished(targetElementId);
    }

}
