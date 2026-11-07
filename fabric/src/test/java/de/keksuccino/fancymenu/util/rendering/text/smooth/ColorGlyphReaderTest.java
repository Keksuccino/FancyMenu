package de.keksuccino.fancymenu.util.rendering.text.smooth;

import org.junit.jupiter.api.Test;
import java.awt.Font;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorGlyphReaderTest {

    @Test
    void notoColorEmojiProvidesPngBitmaps() throws Exception {
        byte[] bytes = TestFonts.bytes(TestFonts.COLOR_EMOJI);
        OpenTypeTables tables = OpenTypeTables.parse(bytes);
        assertNotNull(tables);
        ColorGlyphReader reader = ColorGlyphReader.create(tables);
        assertNotNull(reader);
        Font font = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(bytes)).deriveFont(32.0F);
        int smiley = font.createGlyphVector(SmoothFontSource.RENDER_CONTEXT, "😀").getGlyphCode(0);
        assertTrue(reader.hasColorGlyph(smiley));
        ColorGlyphReader.EmbeddedBitmap bitmap = reader.findBitmap(smiley, 20.0F);
        assertNotNull(bitmap);
        assertEquals(109, bitmap.ppem());
        assertFalse(bitmap.offsetFromBottom());
        byte[] png = bitmap.imageData();
        assertEquals((byte) 0x89, png[0]);
        assertEquals('P', png[1]);
        assertEquals('N', png[2]);
        assertEquals('G', png[3]);
        assertNull(reader.findLayers(smiley));
        // Emoji are wider than they are tall in this font: 2550 units at 2048 units per em.
        assertEquals(2048, tables.unitsPerEm());
        assertEquals(2550, tables.advanceWidth(smiley));
    }

    @Test
    void outlineFontsHaveNoColorGlyphs() {
        OpenTypeTables tables = OpenTypeTables.parse(TestFonts.bytes(TestFonts.BASE));
        assertNotNull(tables);
        assertTrue(tables.has("glyf"));
        assertNull(ColorGlyphReader.create(tables));
    }

    @Test
    void invalidDataIsNoFont() {
        assertNull(OpenTypeTables.parse(new byte[4]));
    }

}
