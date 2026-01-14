package de.keksuccino.fancymenu.util.rendering.text.smooth;

import javax.annotation.Nonnull;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.util.Objects;

public final class SmoothFont implements AutoCloseable {

    private final String debugName;
    private final float baseSize;
    private final float sdfRange;
    private final FontRenderContext fontRenderContext;
    private final Font plainFont;
    private final Font boldFont;
    private final Font italicFont;
    private final Font boldItalicFont;
    private final float ascent;
    private final float descent;
    private final float lineHeight;
    private final SmoothFontAtlas plainAtlas;
    private final SmoothFontAtlas boldAtlas;
    private final SmoothFontAtlas italicAtlas;
    private final SmoothFontAtlas boldItalicAtlas;

    SmoothFont(@Nonnull String debugName, @Nonnull Font baseFont, float baseSize, float sdfRange) {
        this.debugName = Objects.requireNonNull(debugName);
        this.baseSize = Math.max(1.0F, baseSize);
        this.sdfRange = Math.max(1.0F, sdfRange);
        this.fontRenderContext = new FontRenderContext(null, true, true);
        this.plainFont = Objects.requireNonNull(baseFont).deriveFont(Font.PLAIN, this.baseSize);
        this.boldFont = baseFont.deriveFont(Font.BOLD, this.baseSize);
        this.italicFont = baseFont.deriveFont(Font.ITALIC, this.baseSize);
        this.boldItalicFont = baseFont.deriveFont(Font.BOLD | Font.ITALIC, this.baseSize);

        LineMetrics metrics = this.plainFont.getLineMetrics("Hg", this.fontRenderContext);
        this.ascent = metrics.getAscent();
        this.descent = metrics.getDescent();
        this.lineHeight = metrics.getHeight();

        this.plainAtlas = new SmoothFontAtlas(this, this.plainFont, this.fontRenderContext, this.sdfRange, this.debugName + "_plain");
        this.boldAtlas = new SmoothFontAtlas(this, this.boldFont, this.fontRenderContext, this.sdfRange, this.debugName + "_bold");
        this.italicAtlas = new SmoothFontAtlas(this, this.italicFont, this.fontRenderContext, this.sdfRange, this.debugName + "_italic");
        this.boldItalicAtlas = new SmoothFontAtlas(this, this.boldItalicFont, this.fontRenderContext, this.sdfRange, this.debugName + "_bold_italic");
    }

    public float getBaseSize() {
        return baseSize;
    }

    public float getLineHeight(float size) {
        return lineHeight * scaleForSize(size);
    }

    public float getAscent(float size) {
        return ascent * scaleForSize(size);
    }

    public float getDescent(float size) {
        return descent * scaleForSize(size);
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

    float scaleForSize(float size) {
        return size / baseSize;
    }

    @Override
    public void close() {
        plainAtlas.close();
        boldAtlas.close();
        italicAtlas.close();
        boldItalicAtlas.close();
    }

}
