package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.smooth.SmoothFont;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The font a {@link MarkdownRenderer} measures and draws its text with, which is the font of Minecraft or, while the renderer
 * uses it, the font of the UI. Text gets measured and drawn at its normal size, and whoever draws it scales the render pose.
 */
final class MarkdownTextFont {

    private final MarkdownRenderer renderer;
    /** The metrics of the font the fragments got parsed with. NULL until they get asked for the first time after a parse. */
    @Nullable
    private MarkdownFontMetrics metrics = null;

    MarkdownTextFont(@NotNull MarkdownRenderer renderer) {
        this.renderer = renderer;
    }

    float width(@NotNull Component text) {
        if (this.renderer.uiFontRenderingEnabled) {
            return UIBase.getUITextWidth(text, UIBase.getUITextSizeNormal());
        }
        return this.renderer.font.width(text);
    }

    float width(@NotNull String text) {
        if (this.renderer.uiFontRenderingEnabled) {
            return UIBase.getUITextWidth(text, UIBase.getUITextSizeNormal());
        }
        return this.renderer.font.width(text);
    }

    float lineHeight() {
        if (this.renderer.uiFontRenderingEnabled) {
            float height = UIBase.getUITextHeight(UIBase.getUITextSizeNormal());
            if (this.renderer.style.fullLineHeight.get() && !UIBase.shouldUseMinecraftFontForUIRendering()) {
                // The line height of the UI font ends at its baseline, so its descenders hang out of the line
                SmoothFont uiFont = UIBase.getUIFont();
                float size = UIBase.getUITextSizeNormal();
                height = Math.max(height, uiFont.getAscent(size) + uiFont.getYOffset(size) + uiFont.getDescent(size));
            }
            return height;
        }
        return this.renderer.font.lineHeight;
    }

    /**
     * Returns the metrics of the font.<br>
     * Whatever changes the font makes the text get parsed again, so the metrics only get looked up once per parse.
     */
    @NotNull
    MarkdownFontMetrics metrics() {
        if (this.metrics == null) this.metrics = MarkdownFontMetrics.of(this.renderer);
        return this.metrics;
    }

    /**
     * Makes the metrics get looked up again. Has to get called before every parse.
     */
    void forgetMetrics() {
        this.metrics = null;
    }

    /**
     * Returns if text and the pixel art that goes with it get drawn with a shadow. The UI font has none, whatever the style says.
     */
    boolean hasShadow() {
        return this.renderer.style.textShadow.get() && !this.renderer.uiFontRenderingEnabled;
    }

    void draw(@NotNull GuiGraphicsExtractor graphics, @NotNull Component text, float x, float y, int color, boolean shadow) {
        if (this.renderer.uiFontRenderingEnabled) {
            UIBase.renderText(graphics, text, x, y, color, UIBase.getUITextSizeNormal());
            return;
        }
        graphics.text(this.renderer.font, text, (int)x, (int)y, color, shadow);
    }

}
