package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.smooth.SmoothFont;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Function;

/**
 * The font a {@link MarkdownRenderer} measures and draws its text with, which is the font of Minecraft or, while the renderer
 * uses it, the font of the UI. Text gets measured and drawn at its normal size, and whoever draws it scales the render pose.<br>
 * The style can give code another font of Minecraft, which this class puts into the styles and components of code.
 */
final class MarkdownTextFont {

    private final MarkdownRenderer renderer;
    /** The metrics of the font the fragments got parsed with. NULL until they get asked for the first time after a parse. */
    @Nullable
    private MarkdownFontMetrics metrics = null;

    /** Looks up which characters a font has. Tests replace it, because fonts get read from the resources of the game. */
    @NotNull
    Function<Identifier, MarkdownFontCoverage> fontCoverage = MarkdownFontCoverage::of;

    MarkdownTextFont(@NotNull MarkdownRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Returns if text gets drawn with the font of Minecraft, which is made of pixels and can get replaced by other fonts of Minecraft.
     */
    boolean isMinecraftFont() {
        // The UI draws with the Minecraft font if the user prefers it or the smooth font failed to load
        return !this.renderer.uiFontRenderingEnabled || UIBase.shouldUseMinecraftFontForUIRendering();
    }

    /**
     * Returns the font the style gives code, or NULL if code has the font of the text.
     */
    @Nullable
    Identifier getCodeFont() {
        Identifier font = this.renderer.style.codeFont.get();
        return ((font != null) && this.isMinecraftFont()) ? font : null;
    }

    /**
     * Returns the given style with the font the style gives code.
     */
    @NotNull
    Style withCodeFont(@NotNull Style style) {
        Identifier font = this.getCodeFont();
        return (font != null) ? style.withFont(new FontDescription.Resource(font)) : style;
    }

    /**
     * Returns if the font of code has a glyph for the given character. Code without a font of its own has the font of the text, which draws every character.
     */
    boolean codeFontHas(int codePoint) {
        Identifier font = this.getCodeFont();
        return (font == null) || this.fontCoverage.apply(font).has(codePoint);
    }

    /**
     * Builds the component that draws the text of a fragment.<br>
     * Text in the font of code keeps the font of the text for every character the font of code has no glyph for.
     *
     * @param separateTrailingSpace Draws a space behind the text that has none of the lines the style puts below or through the text.
     */
    @NotNull
    Component component(@NotNull Style style, @NotNull String text, boolean separateTrailingSpace) {
        Identifier font = this.getCodeFont();
        if ((font == null) || !hasFont(style, font)) return MarkdownTextFragment.createRenderComponent(style, text, separateTrailingSpace);
        MarkdownFontCoverage coverage = this.fontCoverage.apply(font);
        if (coverage.hasAll(text)) return MarkdownTextFragment.createRenderComponent(style, text, separateTrailingSpace);
        // The parts of the text follow each other as children of an empty text, which gives them everything but the font
        Style fontStyle = Style.EMPTY.withFont(style.getFont());
        MutableComponent component = Component.literal("").setStyle(style.withFont(null));
        int runStart = 0;
        boolean runHasGlyphs = false;
        for (int index = 0; index < text.length(); ) {
            int codePoint = text.codePointAt(index);
            boolean hasGlyph = coverage.has(codePoint);
            if ((index > runStart) && (hasGlyph != runHasGlyphs)) {
                component.append(Component.literal(text.substring(runStart, index)).setStyle(runHasGlyphs ? fontStyle : Style.EMPTY));
                runStart = index;
            }
            runHasGlyphs = hasGlyph;
            index += Character.charCount(codePoint);
        }
        component.append(Component.literal(text.substring(runStart)).setStyle(runHasGlyphs ? fontStyle : Style.EMPTY));
        if (separateTrailingSpace) component.append(MarkdownTextFragment.createRenderComponent(Style.EMPTY, "", true));
        return component;
    }

    private static boolean hasFont(@NotNull Style style, @NotNull Identifier font) {
        return (style.getFont() instanceof FontDescription.Resource resource) && resource.id().equals(font);
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
