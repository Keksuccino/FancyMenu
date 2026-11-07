package de.keksuccino.fancymenu.util.rendering;

/**
 * Radius limits of FancyMenu's GUI blur.
 *
 * <p>Blur radii are GUI-space values, so a blur keeps the same look relative to the GUI at every GUI scale and resolution.
 * {@link #MAX_RADIUS} is the limit in GUI pixels for user-configured radii, like the blur radius property of elements. The renderer
 * itself doesn't cap GUI radii, because scaling (e.g. FancyMenu's UI scale and blur intensity) must not flatten strong blurs at small
 * GUI scales. {@link #MAX_FRAMEBUFFER_RADIUS} bounds the GPU work after the GUI-to-framebuffer conversion instead, so transforms and
 * forced GUI scales can't create unbounded blur work.</p>
 */
public final class GuiBlurRadius {

    public static final float MAX_RADIUS = 16.0F;
    public static final float MAX_FRAMEBUFFER_RADIUS = 256.0F;

    private GuiBlurRadius() {
    }

    /**
     * Maps invalid radii to zero and clamps finite positive radii to {@link #MAX_RADIUS}. Use this for user-configured radii.
     */
    public static float sanitize(float radius) {
        if (!Float.isFinite(radius) || radius <= 0.0F) {
            return 0.0F;
        }
        return Math.min(radius, MAX_RADIUS);
    }

    /**
     * Converts a GUI-space radius into framebuffer pixels. Invalid radius or scale inputs map to zero, while a product
     * above {@link #MAX_FRAMEBUFFER_RADIUS} (including overflows from finite inputs) saturates at that limit.
     */
    public static float resolveFramebufferRadius(float guiRadius, double guiScale) {
        if (!Float.isFinite(guiRadius) || guiRadius <= 0.0F || !Double.isFinite(guiScale) || guiScale <= 0.0D) {
            return 0.0F;
        }
        double framebufferRadius = guiRadius * guiScale;
        if (!Double.isFinite(framebufferRadius) || framebufferRadius >= MAX_FRAMEBUFFER_RADIUS) {
            return MAX_FRAMEBUFFER_RADIUS;
        }
        return (float) framebufferRadius;
    }

}
