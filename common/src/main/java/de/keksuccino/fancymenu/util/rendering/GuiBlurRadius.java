package de.keksuccino.fancymenu.util.rendering;

/**
 * Central work limit for FancyMenu's six-pass GUI box blur.
 *
 * <p>The box-blur shader samples proportionally to its radius for every affected pixel. The
 * renderer's existing recommended upper bound of 16 pixels is therefore also the hard limit at
 * property, transformed-GUI, framebuffer, and shader boundaries. Applying the same limit after
 * coordinate scaling is intentional: transforms and forced GUI scales must not expand an otherwise
 * safe value back into an unbounded shader loop.</p>
 */
public final class GuiBlurRadius {

    public static final float MAX_RADIUS = 16.0F;

    private GuiBlurRadius() {
    }

    /**
     * Maps invalid radii to zero and clamps finite positive radii to {@link #MAX_RADIUS}.
     */
    public static float sanitize(float radius) {
        if (!Float.isFinite(radius) || radius <= 0.0F) {
            return 0.0F;
        }
        return Math.min(radius, MAX_RADIUS);
    }

    /**
     * Converts a GUI-space radius into the final shader radius without allowing scaling to exceed
     * the shared work limit. Invalid radius or scale inputs map to zero, while a product that
     * overflows from otherwise finite positive inputs saturates at {@link #MAX_RADIUS}.
     */
    public static float resolveShaderRadius(float guiRadius, double guiScale) {
        if (!Float.isFinite(guiRadius) || !Double.isFinite(guiScale) || guiScale <= 0.0D) {
            return 0.0F;
        }
        float sanitizedGuiRadius = sanitize(guiRadius);
        if (sanitizedGuiRadius == 0.0F) {
            return 0.0F;
        }
        double scaledRadius = sanitizedGuiRadius * guiScale;
        if (!Double.isFinite(scaledRadius) || scaledRadius >= MAX_RADIUS) {
            return MAX_RADIUS;
        }
        return sanitize((float)scaledRadius);
    }

}
