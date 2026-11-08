package de.keksuccino.fancymenu.util.rendering.text.smooth;

import com.mojang.blaze3d.vertex.VertexConsumer;
import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
 * Draws and measures text with a {@link SmoothFont}, and draws single glyphs of icon fonts.
 * <p>
 * Text gets shaped once into a cached, size-independent layout, so measuring and drawing the same text again is cheap.
 * Drawing turns the layout into one batch per atlas page, with glyphs rasterized at the device pixel size derived from the
 * current pose and GUI scale. Must only be used on the render thread.
 * </p>
 */
public final class SmoothTextRenderer {

    private static final TextDimensions EMPTY_DIMENSIONS = new TextDimensions(0.0F, 0.0F);
    private static final SmoothStyledText STYLED_SCRATCH = new SmoothStyledText(128);
    private static final SmoothTextMesh MESH = new SmoothTextMesh();

    private SmoothTextRenderer() {
    }

    /**
     * Drops all cached text layouts of all smooth fonts.
     */
    public static void clearCaches() {
        SmoothFontManager.clearLayoutCaches();
    }

    public static TextDimensions renderText(@NotNull GuiGraphics graphics, @NotNull SmoothFont font, @NotNull String text, float x, float y, int color, float size, boolean shadow) {
        if (text.isEmpty() || size <= 0.0F) return EMPTY_DIMENSIONS;
        return render(graphics, font, font.layoutLegacy(text, isMultilineEnabled()), x, y, color, size, shadow);
    }

    public static TextDimensions renderText(@NotNull GuiGraphics graphics, @NotNull SmoothFont font, @NotNull Component text, float x, float y, int color, float size, boolean shadow) {
        return renderText(graphics, font, text.getVisualOrderText(), x, y, color, size, shadow);
    }

    public static TextDimensions renderText(@NotNull GuiGraphics graphics, @NotNull SmoothFont font, @NotNull FormattedCharSequence text, float x, float y, int color, float size, boolean shadow) {
        if (size <= 0.0F || text == FormattedCharSequence.EMPTY) return EMPTY_DIMENSIONS;
        return render(graphics, font, layout(font, text), x, y, color, size, shadow);
    }

    public static float getTextWidth(@NotNull SmoothFont font, @NotNull String text, float size) {
        if (text.isEmpty() || size <= 0.0F) return 0.0F;
        return font.layoutLegacy(text, isMultilineEnabled()).width * size;
    }

    public static float getTextWidth(@NotNull SmoothFont font, @NotNull Component text, float size) {
        return getTextWidth(font, text.getVisualOrderText(), size);
    }

    public static float getTextWidth(@NotNull SmoothFont font, @NotNull FormattedCharSequence text, float size) {
        if (size <= 0.0F || text == FormattedCharSequence.EMPTY) return 0.0F;
        return layout(font, text).width * size;
    }

    /**
     * Returns the horizontal caret position before every codepoint of a single-line text, plus the end position, in GUI units.
     * Positions inside clusters (emoji sequences, combining marks) are {@code NaN}, because the text must not get split there.
     * This is what line wrapping measures text with.
     */
    @NotNull
    public static float[] getCaretPositions(@NotNull SmoothFont font, @NotNull FormattedCharSequence text, float size) {
        float[] carets = font.layout(STYLED_SCRATCH.reset(false).appendSequence(text)).carets.clone();
        for (int i = 0; i < carets.length; i++) {
            carets[i] *= size;
        }
        return carets;
    }

    /**
     * Draws one character of an icon font, scaled to fit its ink into the given area and centered in it. Like text, the icon gets
     * rasterized at the device pixel size it is shown at, so it stays sharp at every GUI scale and pose scale.
     *
     * @param color ARGB color the icon gets drawn in
     */
    public static void renderIcon(@NotNull GuiGraphics graphics, @NotNull SmoothFont font, int codepoint, float x, float y, float width, float height, int color) {
        if (!(width > 0.0F) || !(height > 0.0F)) return;
        SmoothFont.IconGlyph icon = font.iconGlyph(codepoint);
        if (icon == null) return;
        Matrix4f pose = beginMesh(graphics, font);
        MESH.addIcon(icon, x, y, width, height, color);
        submitMesh(graphics, pose);
    }

    @NotNull
    private static SmoothTextLayout layout(@NotNull SmoothFont font, @NotNull FormattedCharSequence text) {
        return font.layout(STYLED_SCRATCH.reset(isMultilineEnabled()).appendSequence(text));
    }

    @NotNull
    private static TextDimensions render(@NotNull GuiGraphics graphics, @NotNull SmoothFont font, @NotNull SmoothTextLayout layout, float x, float y, int color, float size, boolean shadow) {
        if (layout.lineCount == 0) {
            return EMPTY_DIMENSIONS;
        }
        TextDimensions dimensions = new TextDimensions(layout.width * size, layout.lineCount * font.getLineHeight(size));
        if (layout.glyphCount == 0 && layout.decorationCount == 0) {
            return dimensions;
        }
        Matrix4f pose = beginMesh(graphics, font);
        if (shadow) {
            MESH.addLayout(layout, x, y, size, color, true);
        }
        MESH.addLayout(layout, x, y, size, color, false);
        submitMesh(graphics, pose);
        return dimensions;
    }

    /**
     * Starts the shared mesh for the current pose and returns that pose for the vertices.
     */
    @NotNull
    private static Matrix4f beginMesh(@NotNull GuiGraphics graphics, @NotNull SmoothFont font) {
        Matrix4f pose = graphics.pose().last().pose();
        float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        MESH.begin(font, pose.m00(), pose.m01(), pose.m10(), pose.m11(), pose.m30(), pose.m31(), guiScale);
        return pose;
    }

    /**
     * Draws every batch of the shared mesh right away, so it stays in order with the other immediate GUI draws.
     */
    private static void submitMesh(@NotNull GuiGraphics graphics, @NotNull Matrix4f pose) {
        MultiBufferSource.BufferSource buffers = graphics.bufferSource();
        for (int i = 0; i < MESH.batchCount(); i++) {
            SmoothTextMesh.Batch batch = MESH.batchAt(i);
            SmoothGlyphAtlas.Page page = batch.page;
            if (page == null || batch.quadCount == 0) {
                continue;
            }
            VertexConsumer consumer = buffers.getBuffer(page.texture.renderType());
            for (int quad = 0; quad < batch.quadCount; quad++) {
                int offset = quad * SmoothTextMesh.FLOATS_PER_QUAD;
                float x0 = batch.quads[offset];
                float y0 = batch.quads[offset + 1];
                float x1 = batch.quads[offset + 2];
                float y1 = batch.quads[offset + 3];
                float u0 = batch.quads[offset + 4];
                float v0 = batch.quads[offset + 5];
                float u1 = batch.quads[offset + 6];
                float v1 = batch.quads[offset + 7];
                int quadColor = batch.colors[quad];
                consumer.vertex(pose, x0, y0, 0.0F).uv(u0, v0).color(quadColor).endVertex();
                consumer.vertex(pose, x0, y1, 0.0F).uv(u0, v1).color(quadColor).endVertex();
                consumer.vertex(pose, x1, y1, 0.0F).uv(u1, v1).color(quadColor).endVertex();
                consumer.vertex(pose, x1, y0, 0.0F).uv(u1, v0).color(quadColor).endVertex();
            }
        }
        graphics.flush();
    }

    private static boolean isMultilineEnabled() {
        return FancyMenu.getOptions().smoothFontMultilineRendering.getValue();
    }

}
