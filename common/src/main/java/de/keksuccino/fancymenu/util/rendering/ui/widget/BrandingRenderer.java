package de.keksuccino.fancymenu.util.rendering.ui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * A renderer for the branding text shown on the title screen.
 * This class allows for customizable positioning of the branding text.
 */
public class BrandingRenderer {

    private final Font font;
    private float opacity = 1.0F; // Default opacity
    private final int screenHeight;
    private final List<Component> lines = Services.COMPAT.getTitleScreenBrandingLines();

    public BrandingRenderer(int screenHeight) {
        this.font = Minecraft.getInstance().font;
        this.screenHeight = screenHeight;
    }

    /**
     * Sets the opacity for rendering the branding text.
     * @param opacity The opacity value (0.0F to 1.0F)
     * @return This renderer instance for method chaining
     */
    public BrandingRenderer setOpacity(float opacity) {
        this.opacity = opacity;
        return this;
    }

    /**
     * Gets the default X position for rendering branding (2 pixels from left).
     * @return The default X position
     */
    public int getDefaultPositionX() {
        return 2;
    }

    /**
     * Gets the default Y position for rendering branding.
     * This is the Y position of the top-most line of text.
     * @return The default Y position
     */
    public int getDefaultPositionY() {
        int totalHeight = getTotalHeight();
        return this.screenHeight - 2 - totalHeight;
    }

    /**
     * Calculates the total width of the branding text (width of the widest line).
     * @return The total width in pixels
     */
    public int getTotalWidth() {
        int width = 0;

        for (Component line : lines) {
            int lineWidth = font.width(line);
            if (lineWidth > width) {
                width = lineWidth;
            }
        }

        return width;
    }

    /**
     * Calculates the total height of the branding text.
     * @return The total height in pixels
     */
    public int getTotalHeight() {
        int totalHeight = (font.lineHeight + 1) * lines.size();
        if (totalHeight > 0) {
            totalHeight--;
        }
        return totalHeight;
    }

    /**
     * Renders the branding text at the default position.
     * @param graphics The GuiGraphics to render with
     */
    public void render(@NotNull GuiGraphics graphics) {
        render(graphics, getDefaultPositionX(), getDefaultPositionY());
    }

    /**
     * Renders the branding text at the specified position.
     * @param graphics The GuiGraphics to render with
     * @param x The x-coordinate to render at
     * @param y The y-coordinate of the top-most line of text
     */
    public void render(@NotNull GuiGraphics graphics, int x, int y) {

         

        if (lines.isEmpty()) {
            return;
        }

        int currentY = y;
        for (Component line : lines) {
            graphics.drawString(font, line, x, currentY, DrawableColor.WHITE.getColorIntWithAlpha(this.opacity));
            currentY += font.lineHeight + 1;
        }

    }

}