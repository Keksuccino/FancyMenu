package de.keksuccino.fancymenu.util.rendering.ui.blur;

import net.minecraft.client.gui.GuiGraphics;

public final class GuiBlurUtils {

    private static final float DEFAULT_CORNER_RATIO = 0.25F;

    private GuiBlurUtils() {
    }

    public static void applyBlurArea(GuiGraphics graphics, float x, float y, float width, float height, int tintArgb, float intensity, boolean rounded) {
        float cornerRadius = rounded ? Math.min(width, height) * DEFAULT_CORNER_RATIO : 0.0F;
        applyBlurArea(graphics, x, y, width, height, tintArgb, intensity, cornerRadius, rounded);
    }

    public static void applyBlurArea(GuiGraphics graphics, float x, float y, float width, float height, int tintArgb, float intensity, float cornerRadius, boolean rounded) {
        GuiBlurRenderer.getInstance().apply(graphics, x, y, width, height, tintArgb, intensity, cornerRadius, rounded);
    }
}
