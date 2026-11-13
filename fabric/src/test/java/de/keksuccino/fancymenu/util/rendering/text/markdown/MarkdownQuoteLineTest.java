package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownQuoteLineTest {

    @ParameterizedTest
    @ValueSource(floats = {2.0F, 6.0F, 15.0F})
    void lineOfALeftAlignedQuoteStandsBetweenTheBorderAndTheQuoteText(float border) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("before\n> quote one\nquote two\n\nafter", 400);
        renderer.setBorder(border);
        renderer.setX(30).setY(50);
        renderer.tick();
        MarkdownTextFragment quote = renderer.fragmentContaining("one");

        float lineX = quote.getLeftQuoteLineX();

        // Containers like scroll areas clip the border in front of the text area, so nothing may get drawn in there
        assertTrue(lineX >= (30 + border), "the line starts inside the border");
        assertTrue((lineX + MarkdownTextFragment.QUOTE_LINE_WIDTH) <= quote.parentLine.fragments.get(0).getTextX(), "the line reaches into the quote text");
    }

}
