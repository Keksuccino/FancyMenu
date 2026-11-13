package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownRightAlignedQuoteTest {

    private static final int CHAR_WIDTH = 6;
    private static final String WORD = "aaaaaaaaaa";

    @Test
    void wrappedLinesKeepTheirIndentInsideTheOptimalWidth() {
        // Three words and the spaces behind them are exactly as wide as the text area, so the third one only fits without the quote indent behind it
        float optimalWidth = (3 * (WORD.length() + 1) * CHAR_WIDTH) + 4;
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("|||\n> " + WORD + " " + WORD + " " + WORD + " " + WORD + "\n|||", optimalWidth);
        renderer.tick();

        assertEquals(optimalWidth, renderer.getRealWidth());
        assertTrue(renderer.lines.size() > 1);
        for (MarkdownTextLine line : renderer.lines) {
            assertTrue(line.getLineWidth() <= (optimalWidth - 4), "line is wider than the text area");
        }
    }

}
