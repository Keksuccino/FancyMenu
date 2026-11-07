package de.keksuccino.fancymenu.util.rendering.text.smooth;

import com.mojang.blaze3d.vertex.VertexConsumer;
import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.Minecraft;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;

/**
 * Draws and measures text with a {@link SmoothFont}.
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
        Matrix4f pose = graphics.pose().last().pose();
        float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        // Mojang's Matrix4f hides its fields, so the 2D affine part gets read by transforming the basis vectors and the origin.
        Vector4f axisX = transform(pose, 1.0F, 0.0F, 0.0F);
        Vector4f axisY = transform(pose, 0.0F, 1.0F, 0.0F);
        Vector4f origin = transform(pose, 0.0F, 0.0F, 1.0F);
        MESH.begin(font, axisX.x(), axisX.y(), axisY.x(), axisY.y(), origin.x(), origin.y(), guiScale);
        if (shadow) {
            MESH.addLayout(layout, x, y, size, color, true);
        }
        MESH.addLayout(layout, x, y, size, color, false);
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
        return dimensions;
    }

    @NotNull
    private static Vector4f transform(@NotNull Matrix4f matrix, float x, float y, float w) {
        Vector4f vector = new Vector4f(x, y, 0.0F, w);
        vector.transform(matrix);
        return vector;
    }

    private static boolean isMultilineEnabled() {
        return FancyMenu.getOptions().smoothFontMultilineRendering.getValue();
    }

}
