package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownBlankLineMode;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownBlockGapMode;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownBlockSpacingTest {

    private static final int LINE_HEIGHT = 9;

    @Test
    void classicLookStacksAllLinesWithTheLineSpacingAndKeepsBlankLines() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("one\ntwo\n\nthree\n> quote\n\n```\ncode\n```\n---\nlast", 400);
        renderer.tick();

        assertEquals(2, line(renderer, "one").offsetY);
        for (int i = 1; i < renderer.lines.size(); i++) {
            assertEquals(2, spaceAbove(renderer, renderer.lines.get(i)), "space above line " + i);
        }
        MarkdownTextLine blank = renderer.lines.get(2);
        assertEquals(MarkdownBlockType.BLANK, blank.blockType);
        assertFalse(blank.collapsed);
        assertEquals(LINE_HEIGHT, blank.getLineHeight());
        MarkdownTextLine last = line(renderer, "last");
        assertEquals(last.offsetY + LINE_HEIGHT + 2 + 2, renderer.getRealHeight());
    }

    @Test
    void classicLookKeepsItsSpacesAboveListItemsAndAroundTables() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("- first\n- second\n  - nested\ntext\n| a | b |\n| --- | --- |\n| c | d |\nlast", 400);
        renderer.setBulletListSpacing(3);
        renderer.setTableMargin(4);
        renderer.tick();

        // The first block of the text has its space above it as well
        assertEquals(2 + 3, line(renderer, "first").offsetY);
        assertEquals(2 + 3, spaceAbove(renderer, line(renderer, "second")));
        assertEquals(2 + 3, spaceAbove(renderer, line(renderer, "nested")));
        assertEquals(2, spaceAbove(renderer, line(renderer, "text")));
        MarkdownTextLine table = renderer.tableFragment(0).parentLine;
        assertEquals(2 + 4, spaceAbove(renderer, table));
        assertEquals(2 + 4, spaceAbove(renderer, line(renderer, "last")));
    }

    @Test
    void listItemInsideAQuoteKeepsItsItemSpacing() {
        // A quote goes on over the lines behind it until a blank line ends it
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("> quote\n- item\nmore", 400);
        renderer.setBulletListSpacing(3);
        renderer.tick();
        MarkdownTextLine item = line(renderer, "item");

        assertEquals(MarkdownBlockType.QUOTE, item.blockType);
        assertEquals(2 + 3, spaceAbove(renderer, item));
        assertEquals(2, spaceAbove(renderer, line(renderer, "more")));

        renderer.setParentRenderScale(3.0F);
        renderer.getStyle().applyLook(MarkdownLook.PIXEL);
        renderer.tick();

        assertEquals(3, spaceAbove(renderer, line(renderer, "item")));
        assertEquals(2, spaceAbove(renderer, line(renderer, "more")));
    }

    @Test
    void pixelLookGivesEveryBlockTheSpaceOfTheMockup() {
        MarkdownTestRenderer renderer = pixelRenderer("# Headline\nIntro text\nsecond line of it\n\n## Added\n- item one\n  - nested item\n- item two\n\n## Fixed\n- item three\n\n> quote\n\n### Balance\n| a | b |\n| --- | --- |\n| c | d |\n\n### Modders\n```\ncode\nmore\n```\n\n---\n\n^^^\nbye\n^^^");
        renderer.tick();

        // The style has a padding of 4, and the first block has no space above it
        assertEquals(4, line(renderer, "Headline").offsetY);
        assertEquals(6, spaceAbove(renderer, line(renderer, "Intro")));
        assertEquals(2, spaceAbove(renderer, line(renderer, "second")));
        assertEquals(9, spaceAbove(renderer, line(renderer, "Added")));
        assertEquals(4, spaceAbove(renderer, line(renderer, "one")));
        assertEquals(3, spaceAbove(renderer, line(renderer, "nested")));
        assertEquals(3, spaceAbove(renderer, line(renderer, "two")));
        assertEquals(9, spaceAbove(renderer, line(renderer, "Fixed")));
        assertEquals(4, spaceAbove(renderer, line(renderer, "three")));
        assertEquals(6, spaceAbove(renderer, line(renderer, "quote")));
        assertEquals(7, spaceAbove(renderer, line(renderer, "Balance")));
        assertEquals(6, spaceAbove(renderer, renderer.tableFragment(0).parentLine));
        assertEquals(7, spaceAbove(renderer, line(renderer, "Modders")));
        assertEquals(6, spaceAbove(renderer, line(renderer, "code")));
        assertEquals(2, spaceAbove(renderer, line(renderer, "more")));
        MarkdownTextLine separator = renderer.fragmentContaining("---").parentLine;
        assertEquals(MarkdownBlockType.SEPARATOR, separator.blockType);
        assertEquals(7, spaceAbove(renderer, separator));
        MarkdownTextLine last = line(renderer, "bye");
        assertEquals(7, spaceAbove(renderer, last));
        // Nothing but the padding follows the last block
        assertEquals(last.offsetY + last.getLineHeight() + 4, renderer.getRealHeight());
    }

    @Test
    void blankLinesOfThePixelLookTakeNoSpaceAndOnlySeparateBlocks() {
        MarkdownTestRenderer renderer = pixelRenderer("\n\none\n\n\n\ntwo\nthree\n\n- item\n\n- other item");
        renderer.tick();

        int collapsed = 0;
        for (MarkdownTextLine line : renderer.lines) {
            if (line.blockType != MarkdownBlockType.BLANK) continue;
            assertTrue(line.collapsed);
            assertEquals(0, line.getLineHeight());
            collapsed++;
        }
        assertEquals(7, collapsed);
        assertEquals(4, line(renderer, "one").offsetY);
        assertEquals(5, spaceAbove(renderer, line(renderer, "two")));
        assertEquals(2, spaceAbove(renderer, line(renderer, "three")));
        assertEquals(5, spaceAbove(renderer, line(renderer, "item")));
        // A blank line ends the list, so this is the space between two lists and not the one between two items
        assertEquals(5, spaceAbove(renderer, line(renderer, "other")));
    }

    @Test
    void blankLinesBehindTheFirstOneAreEmptyLinesWhileTheyDoNotCollapse() {
        MarkdownTestRenderer renderer = pixelRenderer("one\n\n\n\ntwo");
        renderer.getStyle().collapseBlankLines.set(false);
        renderer.tick();

        assertTrue(renderer.lines.get(1).collapsed);
        assertFalse(renderer.lines.get(2).collapsed);
        assertFalse(renderer.lines.get(3).collapsed);
        assertEquals(LINE_HEIGHT, renderer.lines.get(2).getLineHeight());
        MarkdownTextLine one = line(renderer, "one");
        // Two empty lines between the two paragraphs, with the paragraph space on both sides of them
        assertEquals(one.offsetY + LINE_HEIGHT + 5 + LINE_HEIGHT + LINE_HEIGHT + 5, line(renderer, "two").offsetY);
    }

    @Test
    void emptyLineModeCollapsesRowsOfBlankLinesIntoOneLine() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("one\n\n\n\ntwo", 400);
        renderer.getStyle().collapseBlankLines.set(true);
        renderer.tick();

        MarkdownTextLine one = line(renderer, "one");
        assertEquals(one.offsetY + LINE_HEIGHT + 2 + LINE_HEIGHT + 2, line(renderer, "two").offsetY);
    }

    @Test
    void paragraphThatChangesItsAlignmentIsANewBlock() {
        MarkdownTestRenderer renderer = pixelRenderer("left\n^^^\ncentered\n^^^");
        renderer.tick();

        assertEquals(5, spaceAbove(renderer, line(renderer, "centered")));
    }

    @Test
    void gapModesBuildTheSpaceBetweenBlocksDifferently() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("# headline\ntext", 400);
        renderer.getStyle().blankLineMode.set(MarkdownBlankLineMode.PARAGRAPH_GAP);
        renderer.getStyle().headline(1).spaceBelow.set(7);
        renderer.getStyle().paragraphSpaceAbove.set(4);
        renderer.setLineSpacing(3);
        renderer.tick();

        assertEquals(3 + 7 + 4, spaceAbove(renderer, line(renderer, "text")));

        renderer.getStyle().blockGapMode.set(MarkdownBlockGapMode.LARGEST);
        renderer.tick();

        assertEquals(7, spaceAbove(renderer, line(renderer, "text")));
    }

    @Test
    void spacesOfTheRendererSettersStayInSync() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer();

        renderer.setBulletListSpacing(7);
        renderer.setTableMargin(9);

        assertEquals(7, renderer.getStyle().listItemSpacing.get());
        assertEquals(7, renderer.getStyle().listSpaceAbove.get());
        assertEquals(9, renderer.getStyle().tableSpaceAbove.get());
        assertEquals(9, renderer.getStyle().tableSpaceBelow.get());
        assertEquals(7, renderer.getBulletListSpacing());
        assertEquals(9, renderer.getTableMargin());
    }

    private static MarkdownTestRenderer pixelRenderer(String text) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, 400);
        renderer.setParentRenderScale(3.0F);
        renderer.getStyle().applyLook(MarkdownLook.PIXEL);
        return renderer;
    }

    private static MarkdownTextLine line(MarkdownTestRenderer renderer, String text) {
        return renderer.fragmentContaining(text).parentLine;
    }

    /**
     * Returns the space between the given line and the last line in front of it that takes space.
     */
    private static float spaceAbove(MarkdownTestRenderer renderer, MarkdownTextLine line) {
        int index = renderer.lines.indexOf(line) - 1;
        while (renderer.lines.get(index).collapsed) index--;
        MarkdownTextLine previous = renderer.lines.get(index);
        return line.offsetY - (previous.offsetY + previous.getLineHeight());
    }

}
