package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class MarkdownPartlyVisibleBlockTest {

    private static final String QUOTE = "before\n> quote one\nquote two\nquote three\n\nafter";
    private static final String CODE_BLOCK = "before\n```\ncode one\ncode two\ncode three\ncode four\n```\nafter";

    @Test
    void codeBlockBackgroundGetsDrawnByItsFirstLineWhileThatOneIsVisible() {
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
    void quotePanelGetsDrawnByTheFirstLineOfTheQuoteWhileThatOneIsVisible() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(QUOTE, 400);
        renderer.tick();

        assertEquals(List.of(firstFragmentOfLine(renderer, "one")), quotePanelDrawers(renderer));

        // The first line of the quote is only partly inside the visible range
        MarkdownTextLine firstLine = renderer.fragmentContaining("one").parentLine;
        renderer.setVisibleVerticalRange(firstLine.offsetY + firstLine.getLineHeight(), 1000);

        assertEquals(List.of(firstFragmentOfLine(renderer, "one")), quotePanelDrawers(renderer));
    }

    @Test
    void quotePanelGetsDrawnByTheFirstVisibleLineOfTheQuote() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(QUOTE, 400);
        renderer.tick();

        renderer.setVisibleVerticalRange(renderer.fragmentContaining("two").parentLine.offsetY, 1000);
        assertEquals(List.of(firstFragmentOfLine(renderer, "two")), quotePanelDrawers(renderer));

        renderer.setVisibleVerticalRange(renderer.fragmentContaining("three").parentLine.offsetY, 1000);
        assertEquals(List.of(firstFragmentOfLine(renderer, "three")), quotePanelDrawers(renderer));

        renderer.setVisibleVerticalRange(renderer.fragmentContaining("after").parentLine.offsetY, 1000);
        assertEquals(List.of(), quotePanelDrawers(renderer));
    }

    @Test
    void quotePanelGetsDrawnOnceWhenATableSplitsTheLinesOfTheQuote() {
        // A table inside a quote is no part of the quote, which goes on behind it
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("> one\n| a | b |\n| --- | --- |\n| c | d |\ntwo\n\nafter", 400);
        renderer.tick();
        assertSame(renderer.fragmentContaining("one").quoteContext, renderer.fragmentContaining("two").quoteContext);

        assertEquals(List.of(firstFragmentOfLine(renderer, "one")), quotePanelDrawers(renderer));

        renderer.setVisibleVerticalRange(renderer.tableFragment(0).parentLine.offsetY, 1000);
        assertEquals(List.of(firstFragmentOfLine(renderer, "two")), quotePanelDrawers(renderer));
    }

    @Test
    void quotePanelCoversTheWholeQuoteNoMatterWhichLinesAreVisible() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(QUOTE, 400);
        renderer.tick();
        MarkdownTextFragment first = renderer.fragmentContaining("one").parentLine.fragments.get(0);
        MarkdownTextLine lastLine = renderer.fragmentContaining("three").parentLine;

        // Scrolling moves the renderer, and lines outside the visible range don't render, so their fragments don't get moved
        renderer.setY(-500);
        renderer.setVisibleVerticalRange(-500 + renderer.fragmentContaining("two").parentLine.offsetY, -500 + renderer.fragmentContaining("two").parentLine.offsetY + 1);

        assertEquals(-500 + first.parentLine.offsetY, MarkdownQuotePainter.getTop(first.quoteContext));
        assertEquals(-500 + lastLine.offsetY + lastLine.getLineHeight(), MarkdownQuotePainter.getBottom(first.quoteContext));
    }

    /**
     * Returns the first fragments of the lines that draw the panel of their quote.
     */
    private static List<MarkdownTextFragment> quotePanelDrawers(MarkdownTestRenderer renderer) {
        List<MarkdownTextFragment> drawers = new ArrayList<>();
        for (MarkdownTextLine line : renderer.lines) {
            if (!renderer.layout.isLineVisible(line)) continue;
            if ((line.fragments.get(0).quoteContext != null) && line.drawsQuotePanel()) drawers.add(line.fragments.get(0));
        }
        return drawers;
    }

    private static MarkdownTextFragment firstFragmentOfLine(MarkdownTestRenderer renderer, String textInLine) {
        return renderer.fragmentContaining(textInLine).parentLine.fragments.get(0);
    }

    /**
     * Returns the first fragments of the lines that draw the panel of their code block.
     */
    private static List<MarkdownTextFragment> codeBlockBackgroundDrawers(MarkdownTestRenderer renderer) {
        List<MarkdownTextFragment> drawers = new ArrayList<>();
        for (MarkdownTextLine line : renderer.lines) {
            if (!renderer.layout.isLineVisible(line)) continue;
            if ((line.blockType == MarkdownBlockType.CODE_BLOCK) && line.drawsCodeBlockPanel()) drawers.add(line.fragments.get(0));
        }
        return drawers;
    }

}
