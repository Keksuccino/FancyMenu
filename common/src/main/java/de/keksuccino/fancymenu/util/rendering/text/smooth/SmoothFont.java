package de.keksuccino.fancymenu.util.rendering.text.smooth;

import javax.annotation.Nonnull;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.util.Objects;

public final class SmoothFont implements AutoCloseable {

    private final String debugName;
    private final float baseSize;
    private final float generationSize;
    private final float renderScale;
    private final float sdfRange;
    private final FontRenderContext fontRenderContext;
    private final Font plainFont;
    private final Font boldFont;
    private final Font italicFont;
    private final Font boldItalicFont;

    // Metrics are stored relative to baseSize (logical size)
    private final float ascent;
    private final float descent;
    private final float lineHeight;
    private final float underlineOffset;
    private final float underlineThickness;
    private final float strikethroughOffset;
    private final float strikethroughThickness;

    private final SmoothFontAtlas plainAtlas;
    private final SmoothFontAtlas boldAtlas;
    private final SmoothFontAtlas italicAtlas;
    private final SmoothFontAtlas boldItalicAtlas;

    SmoothFont(@Nonnull String debugName, @Nonnull Font rawFont, float baseSize, float generationSize, float sdfRange) {
        this.debugName = Objects.requireNonNull(debugName);
        this.baseSize = Math.max(1.0F, baseSize);
        this.generationSize = Math.max(1.0F, generationSize);
        this.sdfRange = Math.max(1.0F, sdfRange);

        // The scale factor to bring generated glyphs back to logical size
        this.renderScale = baseSize / generationSize;

        this.fontRenderContext = new FontRenderContext(null, true, true);

        // Create AWT fonts at the high-res generation size
        this.plainFont = Objects.requireNonNull(rawFont).deriveFont(Font.PLAIN, this.generationSize);
        this.boldFont = rawFont.deriveFont(Font.BOLD, this.generationSize);
        this.italicFont = rawFont.deriveFont(Font.ITALIC, this.generationSize);
        this.boldItalicFont = rawFont.deriveFont(Font.BOLD | Font.ITALIC, this.generationSize);

        // Calculate metrics based on the logical size for consistent layout
        Font logicalFont = rawFont.deriveFont(Font.PLAIN, this.baseSize);
        LineMetrics metrics = logicalFont.getLineMetrics("Hg", this.fontRenderContext);

        this.ascent = metrics.getAscent();
        this.descent = metrics.getDescent();
        this.lineHeight = metrics.getHeight();
        this.underlineOffset = metrics.getUnderlineOffset();
        this.underlineThickness = metrics.getUnderlineThickness();
        this.strikethroughOffset = metrics.getStrikethroughOffset();
        this.strikethroughThickness = metrics.getStrikethroughThickness();

        this.plainAtlas = new SmoothFontAtlas(this, this.plainFont, this.fontRenderContext, this.sdfRange, this.debugName + "_plain");
        this.boldAtlas = new SmoothFontAtlas(this, this.boldFont, this.fontRenderContext, this.sdfRange, this.debugName + "_bold");
        this.italicAtlas = new SmoothFontAtlas(this, this.italicFont, this.fontRenderContext, this.sdfRange, this.debugName + "_italic");
        this.boldItalicAtlas = new SmoothFontAtlas(this, this.boldItalicFont, this.fontRenderContext, this.sdfRange, this.debugName + "_bold_italic");
    }

    public float getBaseSize() {
        return baseSize;
    }

    // Scale calculation includes the internal downscaling from generation size
    float scaleForSize(float size) {
        return (size / baseSize) * renderScale;
    }

    public float getLineHeight(float size) {
        return lineHeight * (size / baseSize);
    }

    public float getAscent(float size) {
        return ascent * (size / baseSize);
    }

    public float getDescent(float size) {
        return descent * (size / baseSize);
    }

    public float getUnderlineOffset(float size) {
        return underlineOffset * (size / baseSize);
    }

    public float getUnderlineThickness(float size) {
        return Math.max(1.0F, underlineThickness * (size / baseSize));
    }

    public float getStrikethroughOffset(float size) {
        return strikethroughOffset * (size / baseSize);
    }

    public float getStrikethroughThickness(float size) {
        return Math.max(1.0F, strikethroughThickness * (size / baseSize));
    }

    SmoothFontGlyph getGlyph(int codepoint, boolean bold, boolean italic) {
        return getAtlas(bold, italic).getGlyph(codepoint);
    }

    SmoothFontAtlas getAtlas(boolean bold, boolean italic) {
        if (bold && italic) {
            return boldItalicAtlas;
        }
        if (bold) {
            return boldAtlas;
        }
        if (italic) {
            return italicAtlas;
        }
        return plainAtlas;
    }

    float getSdfRange() {
        return sdfRange;
    }

    @Override
    public void close() {
        plainAtlas.close();
        boldAtlas.close();
        italicAtlas.close();
        boldItalicAtlas.close();
    }
}
