package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownQuoteBarPosition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownQuoteLineTest {

    private static final int LINE_HEIGHT = 9;

    @ParameterizedTest
    @ValueSource(floats = {2.0F, 6.0F, 15.0F})
    void barOfALeftAlignedQuoteStandsBetweenTheBorderAndTheQuoteText(float border) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("before\n> quote one\nquote two\n\nafter", 400);
        renderer.setBorder(border);
        renderer.setX(30).setY(50);
        renderer.tick();
        MarkdownTextFragment quote = renderer.fragmentContaining("one");

        float barX = MarkdownQuotePainter.getBarX(quote.quoteContext);

        // Containers like scroll areas clip the border in front of the text area, so nothing may get drawn in there
        assertTrue(barX >= (30 + border), "the bar starts inside the border");
        assertTrue((barX + MarkdownQuotePainter.getBarWidth(renderer)) <= quote.parentLine.fragments.get(0).getTextX(), "the bar reaches into the quote text");
    }

    @ParameterizedTest
    @EnumSource(MarkdownLook.class)
    void barIsInsideTheTextAreaInEveryLook(MarkdownLook look) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("> left quote\n\n|||\n> right quote\n|||", 400);
        renderer.setParentRenderScale(2.0F);
        renderer.getStyle().applyLook(look);
        renderer.setX(30).setY(50);
        renderer.tick();
        float padding = renderer.getStyle().padding.get();
        float barWidth = MarkdownQuotePainter.getBarWidth(renderer);

        float leftBar = MarkdownQuotePainter.getBarX(renderer.fragmentContaining("left").quoteContext);
        float rightBar = MarkdownQuotePainter.getBarX(renderer.fragmentContaining("right").quoteContext);

        assertTrue(barWidth > 0);
        assertEquals(30 + padding, leftBar);
        assertEquals(30 + renderer.getRealWidth() - padding - barWidth, rightBar);
    }

    @ParameterizedTest
    @ValueSource(strings = {"> quote\nlast line\n\nafter", "> quote\n- last line\n\nafter", "> quote\n# last line\n\nafter"})
    void barOfAQuoteReachesBelowTheTextOfItsLastLine(String text) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, 400);
        renderer.setBulletListSpacing(7);
        renderer.setX(30).setY(50);
        renderer.tick();
        MarkdownTextFragment quote = renderer.fragmentContaining("quote");
        MarkdownTextFragment last = renderer.fragmentContaining("last");

        float bottom = MarkdownQuotePainter.getBottom(quote.quoteContext);

        // A quote goes on over the lines behind it until a blank line ends it, so its last line can be a list item or a headline
        assertTrue(bottom >= (last.getTextY() + last.getTextHeight()), "the bar ends above the bottom of the last line of the quote");
        assertEquals(quote.getTextY(), MarkdownQuotePainter.getTop(quote.quoteContext));
    }

    @ParameterizedTest
    @ValueSource(floats = {2.0F, 6.0F, 15.0F})
    void barOfARightAlignedQuoteMirrorsTheBarOfALeftAlignedQuote(float border) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("> left quote\n\n|||\n> right quote\n|||", 400);
        renderer.setBorder(border);
        renderer.setX(30).setY(50);
        renderer.tick();
        MarkdownTextFragment right = renderer.fragmentContaining("right");
        float textAreaLeft = 30 + border;
        float textAreaRight = 30 + renderer.getRealWidth() - border;

        float leftBarStart = MarkdownQuotePainter.getBarX(renderer.fragmentContaining("left").quoteContext);
        float rightBarStart = MarkdownQuotePainter.getBarX(right.quoteContext);
        float rightBarEnd = rightBarStart + MarkdownQuotePainter.getBarWidth(renderer);

        assertEquals(leftBarStart - textAreaLeft, textAreaRight - rightBarEnd);
        MarkdownTextFragment lastOfLine = right.parentLine.fragments.get(right.parentLine.fragments.size() - 1);
        assertTrue(rightBarStart >= (lastOfLine.getTextX() + lastOfLine.getTextWidth()), "the bar reaches into the quote text");
    }

    @Test
    void barInTheBorderStandsNextToTheTextArea() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("> left quote\n\n|||\n> right quote\n|||", 400);
        renderer.setBorder(10);
        renderer.getStyle().quoteBarPosition.set(MarkdownQuoteBarPosition.BORDER);
        renderer.getStyle().quoteBarWidth.set(3);
        renderer.setX(30).setY(50);
        renderer.tick();

        assertEquals(30 + 10 - 3, MarkdownQuotePainter.getBarX(renderer.fragmentContaining("left").quoteContext));
        assertEquals(30 + renderer.getRealWidth() - 10, MarkdownQuotePainter.getBarX(renderer.fragmentContaining("right").quoteContext));
    }

    @Test
    void centeredQuoteHasNoBar() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("> left\n\n^^^\n> centered\n^^^", 400);
        renderer.tick();

        assertTrue(MarkdownQuotePainter.hasBar(renderer.fragmentContaining("left").quoteContext));
        assertFalse(MarkdownQuotePainter.hasBar(renderer.fragmentContaining("centered").quoteContext));
    }

    @Test
    void pixelLookPutsQuotesIntoAPanelWithPaddings() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("before\n\n> one\ntwo\nthree\n\nafter", 400);
        renderer.setParentRenderScale(3.0F);
        renderer.getStyle().applyLook(MarkdownLook.PIXEL);
        renderer.setX(30).setY(50);
        renderer.tick();
        MarkdownTextFragment one = renderer.fragmentContaining("one");
        MarkdownTextFragment two = renderer.fragmentContaining("two");
        MarkdownTextFragment three = renderer.fragmentContaining("three");

        // 4 pixels of padding above the first and below the last line, and lines that are 11 pixels apart
        assertEquals(4 + LINE_HEIGHT, one.parentLine.getLineHeight());
        assertEquals(LINE_HEIGHT, two.parentLine.getLineHeight());
        assertEquals(LINE_HEIGHT + 4, three.parentLine.getLineHeight());
        assertEquals(one.y + 4, one.getTextY());
        assertEquals(one.getTextY() + 11, two.getTextY());
        assertEquals(two.getTextY() + 11, three.getTextY());
        assertEquals(one.y, MarkdownQuotePainter.getTop(one.quoteContext));
        assertEquals(three.getTextY() + LINE_HEIGHT + 4, MarkdownQuotePainter.getBottom(one.quoteContext));
        // The text starts 8 pixels behind the edge of the text area, where the bar of 2 pixels stands
        assertEquals(30 + 4, MarkdownQuotePainter.getBarX(one.quoteContext));
        assertEquals(30 + 4 + 8, one.getTextX());
        assertEquals(2, MarkdownQuotePainter.getBarWidth(renderer));
    }

    @Test
    void panelPaddingOfAQuoteKeepsItsTextAwayFromTheOtherEdgeOfTheTextArea() {
        String words = "aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa";
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("> " + words, 120);
        renderer.setParentRenderScale(3.0F);
        renderer.getStyle().applyLook(MarkdownLook.PIXEL);
        renderer.tick();

        assertTrue(renderer.lines.size() > 1);
        for (MarkdownTextLine line : renderer.lines) {
            MarkdownTextFragment last = line.fragments.get(line.fragments.size() - 1);
            // The style has a padding of 4, and the panel has one of 4 behind the text
            assertTrue((last.getTextX() + last.getTextWidth() - (last.text.endsWith(" ") ? 6 : 0)) <= (120 - 4 - 4), "text reaches into the padding of the panel");
            assertTrue(line.getLineWidth() <= (120 - 8));
        }
    }

}
