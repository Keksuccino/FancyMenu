package de.keksuccino.fancymenu.util.rendering.text.smooth;

import org.junit.jupiter.api.Test;
import java.awt.Font;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothGlyphRasterizerTest {

    @Test
    void outlineGlyphsHaveCoverageInsideATransparentBorder() {
        SmoothFontSource.Loaded source = TestFonts.font(TestFonts.BASE).source(0).get();
        assertNotNull(source);
        SmoothGlyphRasterizer.GlyphBitmap bitmap = SmoothGlyphRasterizer.rasterizeOutline(source.rasterFont(0, 32), glyph(source.font, "H"), 0.0F);
        assertFalse(bitmap.isEmpty());
        assertFalse(bitmap.color());
        byte[] coverage = bitmap.coverage();
        assertNotNull(coverage);
        assertEquals(bitmap.width() * bitmap.height(), coverage.length);
        for (int x = 0; x < bitmap.width(); x++) {
            assertEquals(0, coverage[x]);
            assertEquals(0, coverage[((bitmap.height() - 1) * bitmap.width()) + x]);
        }
        int full = 0;
        for (byte value : coverage) {
            if ((value & 0xFF) == 255) full++;
        }
        assertTrue(full > 20, "stems should be fully covered");
        // The cap height sits above the baseline, so the bitmap starts above the pen position.
        assertTrue(bitmap.top() < -15);
    }

    @Test
    void subpixelOffsetsShiftTheInk() {
        SmoothFontSource.Loaded source = TestFonts.font(TestFonts.BASE).source(0).get();
        assertNotNull(source);
        Font font = source.rasterFont(0, 32);
        int glyph = glyph(source.font, "l");
        float shift = inkCenter(SmoothGlyphRasterizer.rasterizeOutline(font, glyph, 0.5F)) - inkCenter(SmoothGlyphRasterizer.rasterizeOutline(font, glyph, 0.0F));
        assertEquals(0.5F, shift, 0.08F);
    }

    @Test
    void whitespaceHasNoBitmap() {
        SmoothFontSource.Loaded source = TestFonts.font(TestFonts.BASE).source(0).get();
        assertNotNull(source);
        assertTrue(SmoothGlyphRasterizer.rasterizeOutline(source.rasterFont(0, 32), glyph(source.font, " "), 0.0F).isEmpty());
    }

    @Test
    void colorBitmapGlyphsKeepTheirColorsAtTheRequestedSize() {
        SmoothFontSource.Loaded source = TestFonts.font(TestFonts.BASE, TestFonts.COLOR_EMOJI).source(1).get();
        assertNotNull(source);
        SmoothGlyphRasterizer.GlyphBitmap bitmap = SmoothGlyphRasterizer.rasterizeColor(source, glyph(source.font, "😀"), 24.0F, 9.0F);
        assertTrue(bitmap.color());
        int[] argb = bitmap.argb();
        assertNotNull(argb);
        assertNull(bitmap.coverage());
        // Noto Color Emoji bitmaps are 136x128 at 109 ppem, so 24 px per em gives about 30x28 plus the border.
        assertEquals(30 + (SmoothGlyphRasterizer.PADDING * 2), bitmap.width(), 1);
        assertEquals(28 + (SmoothGlyphRasterizer.PADDING * 2), bitmap.height(), 1);
        int colored = 0;
        for (int pixel : argb) {
            int red = (pixel >> 16) & 0xFF;
            int green = (pixel >> 8) & 0xFF;
            int blue = pixel & 0xFF;
            if ((pixel >>> 24) > 200 && Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue)) > 60) colored++;
        }
        assertTrue(colored > 100, "colored pixels: " + colored);
        // Vertically centered around 9 px above the baseline.
        float center = bitmap.top() + (bitmap.height() / 2.0F);
        assertEquals(-9.0F, center, 1.0F);
    }

    @Test
    void bleedingFillsTransparentNeighborsWithEdgeColors() {
        SmoothGlyphRasterizer.GlyphBitmap bitmap = SmoothGlyphRasterizer.padAndBleed(new int[]{0xFFFF0000}, 1, 1, 0, 0);
        int[] argb = bitmap.argb();
        assertNotNull(argb);
        assertEquals(9, argb.length);
        for (int i = 0; i < argb.length; i++) {
            if (i == 4) {
                assertEquals(0xFFFF0000, argb[i]);
            } else {
                assertEquals(0x00FF0000, argb[i]);
            }
        }
    }

    private static int glyph(Font font, String text) {
        return font.deriveFont(32.0F).createGlyphVector(SmoothFontSource.RENDER_CONTEXT, text).getGlyphCode(0);
    }

    private static float inkCenter(SmoothGlyphRasterizer.GlyphBitmap bitmap) {
        byte[] coverage = bitmap.coverage();
        assertNotNull(coverage);
        double weighted = 0.0;
        double total = 0.0;
        for (int y = 0; y < bitmap.height(); y++) {
            for (int x = 0; x < bitmap.width(); x++) {
                int value = coverage[(y * bitmap.width()) + x] & 0xFF;
                weighted += value * (x + bitmap.left() + 0.5);
                total += value;
            }
        }
        return (float) (weighted / total);
    }

}
