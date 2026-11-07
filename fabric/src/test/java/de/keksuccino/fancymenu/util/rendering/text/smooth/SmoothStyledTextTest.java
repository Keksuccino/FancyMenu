package de.keksuccino.fancymenu.util.rendering.text.smooth;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothStyledTextTest {

    @Test
    void colorCodesSetTheColorAndResetFormatFlags() {
        SmoothStyledText text = new SmoothStyledText(4).reset(false).appendLegacy("§la§cb§rc");
        assertEquals(3, text.length());
        assertEquals(SmoothStyledText.BOLD, text.styleAt(0));
        assertEquals(SmoothStyledText.HAS_COLOR | 0xFF5555, text.styleAt(1));
        assertEquals(0, text.styleAt(2));
    }

    @Test
    void formatFlagsAccumulate() {
        SmoothStyledText text = new SmoothStyledText(4).reset(false).appendLegacy("§c§l§nx");
        assertEquals(SmoothStyledText.HAS_COLOR | 0xFF5555 | SmoothStyledText.BOLD | SmoothStyledText.UNDERLINE, text.styleAt(0));
    }

    @Test
    void hexColorCodesNeedSixSeparatedDigits() {
        SmoothStyledText valid = new SmoothStyledText(4).reset(false).appendLegacy("§x§1§2§a§B§c§Dz");
        assertEquals(1, valid.length());
        assertEquals(SmoothStyledText.HAS_COLOR | 0x12ABCD, valid.styleAt(0));
        // A malformed hex code is no formatting code, so its characters stay visible.
        SmoothStyledText malformed = new SmoothStyledText(4).reset(false).appendLegacy("§x§1§2");
        assertTrue(malformed.length() > 1);
    }

    @Test
    void danglingFormatPrefixIsKeptAsText() {
        SmoothStyledText text = new SmoothStyledText(4).reset(false).appendLegacy("a§");
        assertEquals(2, text.length());
        assertEquals('§', text.codepointAt(1));
    }

    @Test
    void surrogatePairsBecomeSingleCodepoints() {
        SmoothStyledText text = new SmoothStyledText(1).reset(false).appendLegacy("😀!");
        assertEquals(2, text.length());
        assertEquals(0x1F600, text.codepointAt(0));
    }

    @Test
    void equalContentIsAnEqualCacheKey() {
        SmoothStyledText scratch = new SmoothStyledText(2).reset(false).appendLegacy("§aHello");
        SmoothStyledText copy = scratch.copy();
        assertEquals(scratch, copy);
        assertEquals(scratch.hashCode(), copy.hashCode());
        assertNotEquals(copy, new SmoothStyledText(2).reset(true).appendLegacy("§aHello"));
        assertNotEquals(copy, new SmoothStyledText(2).reset(false).appendLegacy("§bHello"));
        // Refilling the scratch instance must not change stored copies.
        scratch.reset(false).appendLegacy("other");
        assertEquals(new SmoothStyledText(2).reset(false).appendLegacy("§aHello"), copy);
    }

    @Test
    void packedComponentStylesKeepColorAndFlags() {
        Style style = Style.EMPTY.withColor(TextColor.fromRgb(0x123456)).withItalic(true).withStrikethrough(true);
        assertEquals(SmoothStyledText.HAS_COLOR | 0x123456 | SmoothStyledText.ITALIC | SmoothStyledText.STRIKETHROUGH, SmoothStyledText.packStyle(style));
        assertEquals(0, SmoothStyledText.packStyle(Style.EMPTY));
    }

    @Test
    void resolvedColorsKeepTheBaseAlpha() {
        assertEquals(0x80FF5555, SmoothStyledText.resolveColor(SmoothStyledText.HAS_COLOR | 0xFF5555, 0x80FFFFFF));
        assertEquals(0x80123456, SmoothStyledText.resolveColor(SmoothStyledText.BOLD, 0x80123456));
    }

}
