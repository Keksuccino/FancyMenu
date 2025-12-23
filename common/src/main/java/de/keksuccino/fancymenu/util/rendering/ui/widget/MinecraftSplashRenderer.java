package de.keksuccino.fancymenu.util.rendering.ui.widget;

import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinSplashRenderer;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.util.Mth;
import java.util.Objects;

/**
 * A custom splash text renderer that mimics the behavior of the vanilla splash,
 * but with added flexibility for positioning and color.
 * <p>
 * This class has been updated to use the modern {@link GuiGraphics} transformation system.
 */
public class MinecraftSplashRenderer {

    // This initialization logic remains correct, as it retrieves the splash string.
    public static final MinecraftSplashRenderer DEFAULT_INSTANCE = new MinecraftSplashRenderer(((IMixinSplashRenderer)Objects.requireNonNullElse(Minecraft.getInstance().getSplashManager().getSplash(), new SplashRenderer("ERROR"))).getSplashFancyMenu());

    public static final int WIDTH_OFFSET = 123;
    public static final int HEIGHT_OFFSET = 69;

    private final String splash;

    public MinecraftSplashRenderer(String splash) {
        this.splash = splash;
    }

    public int getDefaultPositionX(int screenWidth) {
        return (screenWidth / 2) + WIDTH_OFFSET;
    }

    public int getDefaultPositionY() {
        return HEIGHT_OFFSET;
    }

    /**
     * Renders the splash text at the default vanilla position.
     *
     * @param guiGraphics the graphics context to render with.
     * @param screenWidth the width of the screen.
     * @param font        the font to use for rendering.
     * @param color       an ARGB color value. The R and G channels of this color will be
     *                    overridden to create a yellow tint, while the A and B channels are preserved.
     */
    public void render(GuiGraphics guiGraphics, int screenWidth, Font font, int color) {
        guiGraphics.pose().pushMatrix();

        // Use 2D transformations from the new pose stack
        guiGraphics.pose().translate((float)screenWidth / 2.0F + WIDTH_OFFSET, (float)HEIGHT_OFFSET);
        guiGraphics.pose().rotate((float) Math.toRadians(-20.0F));

        float f = 1.8F - Mth.abs(Mth.sin((float)(Util.getMillis() % 1000L) / 1000.0F * (float) (Math.PI * 2)) * 0.1F);
        f = f * 100.0F / (float)(font.width(this.splash) + 32);

        guiGraphics.pose().scale(f, f);

        // The color logic `0xFFFF00 | color` forces the RGB parts towards yellow (0xFFFFxx)
        // while preserving the alpha and blue channels from the `color` parameter.
        guiGraphics.drawCenteredString(font, this.splash, 0, -8, 0xFFFF00 | color);

        guiGraphics.pose().popMatrix();
    }

    /**
     * Renders the splash text centered at a specified custom position.
     *
     * @param guiGraphics the graphics context to render with.
     * @param x           the x-coordinate to center the splash text at.
     * @param y           the y-coordinate to center the splash text at.
     * @param font        the font to use for rendering.
     * @param color       an ARGB color value. The R and G channels of this color will be
     *                    overridden to create a yellow tint, while the A and B channels are preserved.
     */
    public void renderAt(GuiGraphics guiGraphics, int x, int y, Font font, int color) {
        guiGraphics.pose().pushMatrix();

        // Translate to the specified custom position
        guiGraphics.pose().translate((float)x, (float)y);

        // Apply the same rotation as the original
        guiGraphics.pose().rotate((float) Math.toRadians(-20.0F));

        // Calculate the pulsing scale factor
        float f = 1.8F - Mth.abs(Mth.sin((float)(Util.getMillis() % 1000L) / 1000.0F * (float) (Math.PI * 2)) * 0.1F);
        f = f * 100.0F / (float)(font.width(this.splash) + 32);

        // Apply scaling
        guiGraphics.pose().scale(f, f);

        // Draw the centered text at the origin (which is now at x,y)
        guiGraphics.drawCenteredString(font, this.splash, 0, -8, 0xFFFF00 | color);

        guiGraphics.pose().popMatrix();
    }

    /**
     * Gets the splash text.
     *
     * @return the splash text
     */
    public String getSplash() {
        return this.splash;
    }

}