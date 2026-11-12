package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownPartlyVisibleBlockTest {

    private static final String QUOTE = "before\n> quote one\nquote two\nquote three\n\nafter";
    private static final String CODE_BLOCK = "before\n```\ncode one\ncode two\ncode three\ncode four\n```\nafter";

    @Test
    void codeBlockBackgroundGetsDrawnByItsFirstFragmentWhileThatOneIsVisible() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(CODE_BLOCK, 400);
        renderer.tick();

        assertEquals(List.of(firstFragmentOfLine(renderer, "one")), codeBlockBackgroundDrawers(renderer));

        // The first line of the block is only partly inside the visible range
        MarkdownTextLine firstLine = renderer.fragmentContaining("one").parentLine;
        renderer.setVisibleVerticalRange(firstLine.offsetY + firstLine.getLineHeight(), 1000);

        assertEquals(List.of(firstFragmentOfLine(renderer, "one")), codeBlockBackgroundDrawers(renderer));
    }

    @Test
    void codeBlockBackgroundGetsDrawnByTheFirstVisibleLineOfTheBlock() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(CODE_BLOCK, 400);
        renderer.tick();

        renderer.setVisibleVerticalRange(renderer.fragmentContaining("two").parentLine.offsetY, 1000);
        assertEquals(List.of(firstFragmentOfLine(renderer, "two")), codeBlockBackgroundDrawers(renderer));

        renderer.setVisibleVerticalRange(renderer.fragmentContaining("four").parentLine.offsetY, 1000);
        assertEquals(List.of(firstFragmentOfLine(renderer, "four")), codeBlockBackgroundDrawers(renderer));

        renderer.setVisibleVerticalRange(renderer.fragmentContaining("after").parentLine.offsetY, 1000);
        assertEquals(List.of(), codeBlockBackgroundDrawers(renderer));
    }

    @Test
    void codeBlockBackgroundIsAsBigAsTheWholeBlockNoMatterWhichLinesAreVisible() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(CODE_BLOCK, 400);
        renderer.tick();
        MarkdownTextFragment first = renderer.fragmentContaining("one");
        MarkdownTextFragment last = renderer.fragmentContaining("four");

        // Scrolling moves the renderer, and lines outside the visible range don't render, so their fragments don't get moved
        renderer.setY(-500);
        renderer.setVisibleVerticalRange(-500 + renderer.fragmentContaining("two").parentLine.offsetY, -500 + renderer.fragmentContaining("three").parentLine.offsetY);

        assertEquals(-500 + first.parentLine.offsetY, first.getRenderLineY());
        assertEquals(-500 + last.parentLine.offsetY, last.getRenderLineY());
    }

    @Test
    void quoteLineGetsDrawnByTheLastFragmentOfTheQuoteWhileThatOneIsVisible() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(QUOTE, 400);
        renderer.tick();

        assertEquals(List.of(lastFragmentOfLine(renderer, "three")), quoteLineDrawers(renderer));

        // The last line of the quote is only partly inside the visible range
        renderer.setVisibleVerticalRange(0, renderer.fragmentContaining("three").parentLine.offsetY);

        assertEquals(List.of(lastFragmentOfLine(renderer, "three")), quoteLineDrawers(renderer));
    }

    @Test
    void quoteLineGetsDrawnByTheLastVisibleLineOfTheQuote() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(QUOTE, 400);
        renderer.tick();

        renderer.setVisibleVerticalRange(0, renderer.fragmentContaining("three").parentLine.offsetY - 1);
        assertEquals(List.of(lastFragmentOfLine(renderer, "two")), quoteLineDrawers(renderer));

        renderer.setVisibleVerticalRange(0, renderer.fragmentContaining("two").parentLine.offsetY - 1);
        assertEquals(List.of(lastFragmentOfLine(renderer, "one")), quoteLineDrawers(renderer));

        renderer.setVisibleVerticalRange(0, renderer.fragmentContaining("one").parentLine.offsetY - 1);
        assertEquals(List.of(), quoteLineDrawers(renderer));
    }

    @Test
    void quoteLineStartsAtTheFirstLineOfTheQuoteNoMatterWhichLinesAreVisible() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(QUOTE, 400);
        renderer.tick();
        MarkdownTextFragment first = renderer.fragmentContaining("one").parentLine.fragments.get(0);

        // Scrolling moves the renderer, and lines outside the visible range don't render, so their fragments don't get moved
        renderer.setY(-500);
        renderer.setVisibleVerticalRange(-500 + renderer.fragmentContaining("two").parentLine.offsetY, 1000);

        assertEquals(-500 + first.parentLine.offsetY, first.quoteContext.getQuoteStart().getRenderLineY());
    }

    private static MarkdownTextFragment lastFragmentOfLine(MarkdownTestRenderer renderer, String textInLine) {
        List<MarkdownTextFragment> fragments = renderer.fragmentContaining(textInLine).parentLine.fragments;
        return fragments.get(fragments.size() - 1);
    }

    private static List<MarkdownTextFragment> quoteLineDrawers(MarkdownTestRenderer renderer) {
        List<MarkdownTextFragment> drawers = new ArrayList<>();
        for (MarkdownTextLine line : renderer.lines) {
            if (!renderer.layout.isLineVisible(line)) continue;
            for (MarkdownTextFragment fragment : line.fragments) {
                if ((fragment.quoteContext != null) && fragment.drawsQuoteLine()) drawers.add(fragment);
            }
        }
        return drawers;
    }

    private static MarkdownTextFragment firstFragmentOfLine(MarkdownTestRenderer renderer, String textInLine) {
        return renderer.fragmentContaining(textInLine).parentLine.fragments.get(0);
    }

    private static List<MarkdownTextFragment> codeBlockBackgroundDrawers(MarkdownTestRenderer renderer) {
        List<MarkdownTextFragment> drawers = new ArrayList<>();
        for (MarkdownTextLine line : renderer.lines) {
            if (!renderer.layout.isLineVisible(line)) continue;
            for (MarkdownTextFragment fragment : line.fragments) {
                if ((fragment.codeBlockContext != null) && !fragment.codeBlockContext.singleLine && fragment.drawsCodeBlockBackground()) drawers.add(fragment);
            }
        }
        return drawers;
    }

}
