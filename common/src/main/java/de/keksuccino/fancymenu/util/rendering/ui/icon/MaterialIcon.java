package de.keksuccino.fancymenu.util.rendering.ui.icon;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.smooth.SmoothFont;
import de.keksuccino.fancymenu.util.rendering.text.smooth.SmoothTextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/**
 * One icon of the Material icon font. Icons get drawn like smooth text: rasterized at the device pixel size they are shown at
 * and batched through the icon font's glyph atlas, so they stay sharp at every GUI scale and zoom level.
 */
public final class MaterialIcon {

    private final String name;
    private final int codepoint;

    MaterialIcon(@NotNull String name, int codepoint) {
        this.name = Objects.requireNonNull(name);
        this.codepoint = codepoint;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public int getCodepoint() {
        return this.codepoint;
    }

    /**
     * Draws the icon, scaled to fit into the given area and centered in it.
     * Like textures drawn with {@code blit}, the color gets multiplied with the active shader color.
     *
     * @param color ARGB color of the icon
     */
    public void render(@NotNull GuiGraphicsExtractor graphics, float x, float y, float width, float height, int color) {
        SmoothFont font = MaterialIcons.font();
        if (font != null) {
            SmoothTextRenderer.renderIcon(graphics, font, this.codepoint, x, y, width, height, RenderingUtils.applyShaderColor(color));
        }
    }

    /**
     * Draws the icon like {@link #render(GuiGraphicsExtractor, float, float, float, float, int)}, rotated around the center of the area.
     */
    public void render(@NotNull GuiGraphicsExtractor graphics, float x, float y, float width, float height, int color, float rotationDegrees) {
        if (rotationDegrees == 0.0F) {
            this.render(graphics, x, y, width, height, color);
            return;
        }
        float halfWidth = width * 0.5F;
        float halfHeight = height * 0.5F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + halfWidth, y + halfHeight);
        graphics.pose().rotate((float) Math.toRadians(rotationDegrees));
        this.render(graphics, -halfWidth, -halfHeight, width, height, color);
        graphics.pose().popMatrix();
    }

}
