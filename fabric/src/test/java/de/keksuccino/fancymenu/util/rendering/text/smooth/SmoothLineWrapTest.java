package de.keksuccino.fancymenu.util.rendering.text.smooth;

import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothLineWrapTest {

    private static final float SIZE = 10.0F;
    private static final String FAMILY = "👨‍👩‍👧";

    private static SmoothFont font;

    @BeforeAll
    static void setUp() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        font = TestFonts.font(TestFonts.BASE, TestFonts.COLOR_EMOJI);
    }

    @Test
    void linesBreakAtWhitespace() {
        List<String> lines = wrap("alpha beta gamma", width("alpha beta") + 0.1F);
        assertEquals(List.of("alpha beta", "gamma"), lines);
    }

    @Test
    void longWordsBreakBetweenCharacters() {
        List<String> lines = wrap("abcdefghij", width("abcde") + 0.1F);
        assertEquals("abcde", lines.get(0));
        assertEquals("abcdefghij", String.join("", lines));
    }

    @Test
    void emojiSequencesNeverGetSplit() {
        List<String> lines = wrap("aa" + FAMILY, width("aa") + 0.1F);
        assertEquals(List.of("aa", FAMILY), lines);
    }

    @Test
    void lineBreaksAndBlankLinesAreKept() {
        assertEquals(List.of("a", " ", "b"), wrap("a\n\nb", 1000.0F));
    }

    @Test
    void wrappedLinesFitTheWidth() {
        float maxWidth = width("The quick brown");
        for (String line : wrap("The quick brown fox jumps over the lazy dog, again and again.", maxWidth)) {
            assertTrue(width(line) <= maxWidth + 0.01F, line);
        }
    }

    private static List<String> wrap(String text, float maxWidth) {
        List<MutableComponent> lines = TextFormattingUtils.lineWrapComponentsSmooth(Component.literal(text), font, SIZE, maxWidth);
        return lines.stream().map(Component::getString).toList();
    }

    private static float width(String text) {
        float[] carets = SmoothTextRenderer.getCaretPositions(font, Component.literal(text).getVisualOrderText(), SIZE);
        return carets[carets.length - 1];
    }

}
