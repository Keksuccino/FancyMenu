package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

/** Calculates parent-editor GUI scale and viewport dimensions without depending on Minecraft runtime state. */
final class AnimationPreviewScalePolicy {

    private AnimationPreviewScalePolicy() {
    }

    static double resolveParentGuiScale(int framebufferWidth, int framebufferHeight, double defaultScale, double forcedScale, int autoScalingWidth, int autoScalingHeight) {
        double resolvedScale = normalizeScale(defaultScale);

        if (forcedScale != 0.0D) resolvedScale = normalizeScale(forcedScale);

        if ((autoScalingWidth != 0) && (autoScalingHeight != 0)) {
            int viewportWidth = calculateViewportDimension(framebufferWidth, resolvedScale);
            int viewportHeight = calculateViewportDimension(framebufferHeight, resolvedScale);
            double guiWidth = (double)viewportWidth * resolvedScale;
            double guiHeight = (double)viewportHeight * resolvedScale;
            double newScaleX = (guiWidth / (double)autoScalingWidth) * resolvedScale;
            double newScaleY = (guiHeight / (double)autoScalingHeight) * resolvedScale;
            resolvedScale = normalizeScale(Math.min(newScaleX, newScaleY));
        }

        return resolvedScale;
    }

    static int calculateViewportDimension(int framebufferDimension, double guiScale) {
        return Math.max(1, (int)Math.ceil((double)Math.max(1, framebufferDimension) / normalizeScale(guiScale)));
    }

    static boolean shouldCorrectManagerScale(double managerScale, double parentScale) {
        return normalizeScale(managerScale) > normalizeScale(parentScale);
    }

    private static double normalizeScale(double scale) {
        return Double.isFinite(scale) && (scale > 0.0D) ? scale : 1.0D;
    }

}
