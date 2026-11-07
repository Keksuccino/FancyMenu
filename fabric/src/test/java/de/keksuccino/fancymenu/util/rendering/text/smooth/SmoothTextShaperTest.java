package de.keksuccino.fancymenu.util.rendering.text.smooth;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicIntegerArray;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothTextShaperTest {

    private static final String FAMILY = "👨‍👩‍👧";
    private static final String GERMAN_FLAG = "🇩🇪";

    @Test
    void kerningMakesPairsNarrowerThanTheirSingleGlyphs() {
        SmoothFont font = TestFonts.font(TestFonts.BASE);
        float pair = font.layoutLegacy("AV", false).width;
        float singles = font.layoutLegacy("A", false).width + font.layoutLegacy("V", false).width;
        assertTrue(pair < singles - 0.01F, "kerned " + pair + " vs " + singles);
    }

    @Test
    void emojiSequencesBecomeSingleColorGlyphs() {
        SmoothFont font = TestFonts.font(TestFonts.BASE, TestFonts.COLOR_EMOJI);
        SmoothTextLayout family = font.layoutLegacy(FAMILY, false);
        assertEquals(1, family.glyphCount);
        assertEquals(1, family.glyphSources[0]);
        assertTrue((family.glyphFlags[0] & SmoothTextLayout.GLYPH_COLOR) != 0);
        assertTrue(family.width > 0.5F);
        int codepoints = (int) FAMILY.codePoints().count();
        assertEquals(0.0F, family.carets[0]);
        for (int i = 1; i < codepoints; i++) {
            assertTrue(Float.isNaN(family.carets[i]), "caret inside the cluster at " + i);
        }
        assertEquals(family.width, family.carets[codepoints]);

        SmoothTextLayout flag = font.layoutLegacy(GERMAN_FLAG, false);
        assertEquals(1, flag.glyphCount);
        assertTrue((flag.glyphFlags[0] & SmoothTextLayout.GLYPH_COLOR) != 0);
    }

    @Test
    void fallbackFontsOnlyLoadWhenTheirScriptShowsUp() {
        AtomicIntegerArray loads = new AtomicIntegerArray(3);
        SmoothFont font = TestFonts.font("en_us", null, loads, TestFonts.BASE, TestFonts.JAPANESE, TestFonts.COLOR_EMOJI);
        font.layoutLegacy("Hello world", false);
        assertEquals(1, loads.get(0));
        assertEquals(0, loads.get(1));
        assertEquals(0, loads.get(2));
        font.layoutLegacy("日本", false);
        assertEquals(1, loads.get(1));
        assertEquals(0, loads.get(2));
        font.layoutLegacy("😀", false);
        assertEquals(1, loads.get(2));
        font.layoutLegacy("😀 日", false);
        assertEquals(1, loads.get(1));
        assertEquals(1, loads.get(2));
    }

    @Test
    void textPresentationSelectorAvoidsColorEmoji() {
        SmoothFont font = TestFonts.font(TestFonts.BASE, TestFonts.COLOR_EMOJI, TestFonts.MONO_EMOJI);
        SmoothTextLayout text = font.layoutLegacy("❤︎", false);
        assertTrue(text.glyphCount >= 1);
        assertEquals(0, text.glyphFlags[0] & SmoothTextLayout.GLYPH_COLOR);
        SmoothTextLayout emoji = font.layoutLegacy("❤️", false);
        assertTrue((emoji.glyphFlags[0] & SmoothTextLayout.GLYPH_COLOR) != 0);
    }

    @Test
    void charactersNoFontCanDrawBecomeTheReplacementGlyph() {
        SmoothFont font = TestFonts.font(TestFonts.BASE);
        SmoothTextLayout missing = font.layoutLegacy(new String(Character.toChars(0x10FFFD)), false);
        SmoothTextLayout replacement = font.layoutLegacy("?", false);
        assertEquals(1, missing.glyphCount);
        assertEquals(replacement.glyphIds[0], missing.glyphIds[0]);
    }

    @Test
    void multilineTextStartsNewLinesAndRestartsCarets() {
        SmoothFont font = TestFonts.font(TestFonts.BASE);
        SmoothTextLayout multiline = font.layoutLegacy("ab\ncd", true);
        assertEquals(2, multiline.lineCount);
        assertEquals(0.0F, multiline.carets[3]);
        assertEquals(1, multiline.glyphLines[multiline.glyphCount - 1]);
        assertEquals(font.layoutLegacy("cd", false).width, multiline.carets[5], 1.0E-4F);

        SmoothTextLayout singleLine = font.layoutLegacy("ab\ncd", false);
        assertEquals(1, singleLine.lineCount);
        assertEquals(font.layoutLegacy("ab cd", false).width, singleLine.width, 1.0E-4F);
    }

    @Test
    void legacyColorsAreKeptPerGlyph() {
        SmoothFont font = TestFonts.font(TestFonts.BASE);
        SmoothTextLayout layout = font.layoutLegacy("§ca§9b", false);
        assertEquals(2, layout.glyphCount);
        assertEquals(0xFFFF5555, SmoothStyledText.resolveColor(layout.glyphStyles[0], 0xFFFFFFFF));
        assertEquals(0xFF5555FF, SmoothStyledText.resolveColor(layout.glyphStyles[1], 0xFFFFFFFF));
    }

    @Test
    void underlinesSplitWhereTheirColorChanges() {
        SmoothFont font = TestFonts.font(TestFonts.BASE);
        SmoothTextLayout layout = font.layoutLegacy("§nab§9§ncd§rx", false);
        assertEquals(2, layout.decorationCount);
        assertEquals(SmoothTextLayout.DECORATION_UNDERLINE, layout.decorationKinds[0]);
        assertEquals(0.0F, layout.decorationStarts[0]);
        assertEquals(layout.carets[2], layout.decorationEnds[0]);
        assertEquals(layout.carets[2], layout.decorationStarts[1]);
        assertEquals(layout.carets[4], layout.decorationEnds[1]);
        assertNotEquals(layout.decorationStyles[0], layout.decorationStyles[1]);
    }

    @Test
    void boldAndItalicChangeTheGlyphFlags() {
        SmoothFont font = TestFonts.font(TestFonts.BASE);
        SmoothTextLayout layout = font.layoutLegacy("a§lb§r§oc", false);
        assertEquals(0, layout.glyphFlags[0] & (SmoothTextLayout.GLYPH_BOLD | SmoothTextLayout.GLYPH_ITALIC));
        assertEquals(SmoothTextLayout.GLYPH_BOLD, layout.glyphFlags[1] & SmoothTextLayout.GLYPH_BOLD);
        assertEquals(SmoothTextLayout.GLYPH_ITALIC, layout.glyphFlags[2] & SmoothTextLayout.GLYPH_ITALIC);
        assertTrue(font.layoutLegacy("§lmmmm", false).width > font.layoutLegacy("mmmm", false).width);
    }

    @Test
    void obfuscationSkipsSpaces() {
        SmoothFont font = TestFonts.font(TestFonts.BASE);
        SmoothTextLayout layout = font.layoutLegacy("§ka b", false);
        assertEquals(3, layout.glyphCount);
        assertTrue((layout.glyphFlags[0] & SmoothTextLayout.GLYPH_OBFUSCATED) != 0);
        assertFalse((layout.glyphFlags[1] & SmoothTextLayout.GLYPH_OBFUSCATED) != 0);
        assertTrue((layout.glyphFlags[2] & SmoothTextLayout.GLYPH_OBFUSCATED) != 0);
    }

    @Test
    void layoutsAreCachedAndCacheKeysMatchByContent() {
        SmoothFont font = TestFonts.font(TestFonts.BASE);
        assertSame(font.layoutLegacy("cached", false), font.layoutLegacy("cached", false));
        SmoothStyledText first = new SmoothStyledText(4).reset(false).appendLegacy("§aKey");
        SmoothStyledText second = new SmoothStyledText(4).reset(false).appendLegacy("§aKey");
        assertSame(font.layout(first), font.layout(second));
        font.clearLayouts();
        SmoothTextLayout fresh = font.layout(second);
        assertArrayEquals(fresh.glyphIds, font.layout(first).glyphIds);
    }

    @Test
    void legacyStringsWithOnlyFormattingCodesTakeOneEmptyLine() {
        SmoothFont font = TestFonts.font(TestFonts.BASE);
        SmoothTextLayout codesOnly = font.layoutLegacy("§a§l", false);
        assertEquals(1, codesOnly.lineCount);
        assertEquals(0.0F, codesOnly.width);
        assertEquals(0, codesOnly.glyphCount);
        assertEquals(2, font.layoutLegacy("§a\n§b", true).lineCount);
    }

    @Test
    void emptyTextHasNoLines() {
        SmoothFont font = TestFonts.font(TestFonts.BASE);
        SmoothTextLayout empty = font.layout(new SmoothStyledText(1).reset(false));
        assertEquals(0, empty.lineCount);
        assertEquals(0.0F, empty.width);
    }

}
