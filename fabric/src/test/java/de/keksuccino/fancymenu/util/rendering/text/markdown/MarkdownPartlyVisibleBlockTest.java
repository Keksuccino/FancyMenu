package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownPartlyVisibleBlockTest {

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
