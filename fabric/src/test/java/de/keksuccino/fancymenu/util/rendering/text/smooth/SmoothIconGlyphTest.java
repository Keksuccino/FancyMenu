package de.keksuccino.fancymenu.util.rendering.text.smooth;

import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import org.junit.jupiter.api.Test;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothIconGlyphTest {

    @Test
    void everyMaterialIconHasAGlyphInTheIconFont() {
        SmoothFont font = TestFonts.font(TestFonts.MATERIAL_ICONS);
        List<String> missing = new ArrayList<>();
        for (MaterialIcon icon : MaterialIcons.values()) {
            if (font.iconGlyph(icon.getCodepoint()) == null) {
                missing.add(icon.getName());
            }
        }
        assertTrue(missing.isEmpty(), "icons without glyph: " + missing);
    }

    @Test
    void iconGlyphsGetCachedAndUnknownCharactersHaveNone() {
        SmoothFont font = TestFonts.font(TestFonts.MATERIAL_ICONS);
        assertSame(font.iconGlyph(MaterialIcons.CLOSE.getCodepoint()), font.iconGlyph(MaterialIcons.CLOSE.getCodepoint()));
        assertNull(font.iconGlyph(0x10FFFD));
        assertNull(font.iconGlyph(0x10FFFD));
    }

    @Test
    void inkBoundsIgnoreCurveControlPoints() {
        Path2D.Float arch = new Path2D.Float();
        arch.moveTo(0.0F, 0.0F);
        arch.quadTo(5.0F, 10.0F, 10.0F, 0.0F);
        arch.closePath();
        float[] bounds = SmoothFont.inkBounds(arch);
        assertNotNull(bounds);
        assertEquals(0.0F, bounds[0], 1.0E-3F);
        assertEquals(0.0F, bounds[1], 1.0E-3F);
        assertEquals(10.0F, bounds[2], 1.0E-3F);
        // The curve peaks halfway to its control point
        assertEquals(5.0F, bounds[3], 0.02F);
        assertNull(SmoothFont.inkBounds(new Path2D.Float()));
    }

    @Test
    void iconInkSpansWholePixelsWithSharpOuterEdges() {
        SmoothFont font = TestFonts.font(TestFonts.MATERIAL_ICONS);
        SmoothFont.IconGlyph stop = font.iconGlyph(MaterialIcons.STOP.getCodepoint());
        assertNotNull(stop);
        assertEquals(stop.inkWidth(), stop.inkHeight(), 1.0E-4F);
        for (int inkPixels : new int[]{9, 14, 20, 31}) {
            SmoothGlyphRasterizer.GlyphBitmap bitmap = SmoothGlyphRasterizer.rasterizeIcon(stop, inkPixels / stop.inkWidth());
            int padding = SmoothGlyphRasterizer.PADDING;
            assertEquals(inkPixels + (padding * 2), bitmap.width());
            assertEquals(inkPixels + (padding * 2), bitmap.height());
            assertEquals(-padding, bitmap.left());
            assertEquals(-padding, bitmap.top());
            int middle = bitmap.height() / 2;
            assertEquals(0, coverage(bitmap, padding - 1, middle), "left padding");
            assertEquals(255, coverage(bitmap, padding, middle), "first ink column at " + inkPixels + "px");
            assertEquals(255, coverage(bitmap, padding + inkPixels - 1, middle), "last ink column at " + inkPixels + "px");
            assertEquals(0, coverage(bitmap, padding + inkPixels, middle), "right padding");
            assertEquals(255, coverage(bitmap, middle, padding), "first ink row at " + inkPixels + "px");
            assertEquals(255, coverage(bitmap, middle, padding + inkPixels - 1), "last ink row at " + inkPixels + "px");
        }
    }

    @Test
    void iconBitmapsGetTheirOwnAtlasKeys() {
        assertNotEquals(SmoothGlyphAtlas.key(0, 42, 0, 20, 0), SmoothGlyphAtlas.iconKey(0, 42, 20));
        assertNotEquals(SmoothGlyphAtlas.iconKey(0, 42, 20), SmoothGlyphAtlas.iconKey(0, 42, 21));
        assertNotEquals(SmoothGlyphAtlas.iconKey(0, 42, 20), SmoothGlyphAtlas.iconKey(0, 43, 20));
    }

    private static int coverage(SmoothGlyphRasterizer.GlyphBitmap bitmap, int x, int y) {
        byte[] coverage = bitmap.coverage();
        assertNotNull(coverage);
        return coverage[(y * bitmap.width()) + x] & 0xFF;
    }

}
