package de.keksuccino.fancymenu.util.rendering.text.smooth;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGuiGraphics;
import de.keksuccino.fancymenu.util.rendering.GuiScissorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2f;

/**
 * Draws and measures text with a {@link SmoothFont}.
 * <p>
 * Text gets shaped once into a cached, size-independent layout, so measuring and drawing the same text again is cheap.
 * Drawing turns the layout into one batched GUI element per atlas page, with glyphs rasterized at the device pixel size
 * derived from the current pose and GUI scale. Must only be used on the render thread.
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
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        MESH.begin(font, pose.m00, pose.m01, pose.m10, pose.m11, pose.m20, pose.m21, guiScale);
        if (shadow) {
            MESH.addLayout(layout, x, y, size, color, true);
        }
        MESH.addLayout(layout, x, y, size, color, false);
        ScreenRectangle scissor = GuiScissorUtil.getActiveScissor(graphics);
        GuiRenderState renderState = ((IMixinGuiGraphics) graphics).get_guiRenderState_FancyMenu();
        for (int i = 0; i < MESH.batchCount(); i++) {
            SmoothTextMesh.Batch batch = MESH.batchAt(i);
            SmoothGlyphAtlas.Page page = batch.page;
            if (page != null && batch.quadCount > 0) {
                renderState.submitGuiElement(new SmoothTextRenderState(pose, batch, page.texture.textureSetup(), scissor));
            }
        }
        return dimensions;
    }

    private static boolean isMultilineEnabled() {
        return FancyMenu.getOptions().smoothFontMultilineRendering.getValue();
    }

}
