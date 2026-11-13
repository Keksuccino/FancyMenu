package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownCodeStyleTest {

    private static final int CHAR_WIDTH = 6;
    private static final int LINE_HEIGHT = 9;

    @Test
    void classicLookKeepsItsPaddingsAndTheColorOfTheText() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("a `code` b\n```\nfirst\nlast\n```", 400);
        renderer.tick();
        MarkdownTextFragment code = renderer.fragmentContaining("code");
        MarkdownTextFragment first = renderer.fragmentContaining("first");

        assertEquals(code.x + 1, code.getTextX());
        assertEquals(code.getTextY() - 2, MarkdownCodePainter.getInlineTop(code));
        assertEquals(code.getTextY() + LINE_HEIGHT, MarkdownCodePainter.getInlineBottom(code));
        // The fragment ends with the space behind the code, which is no part of the background
        assertEquals(code.x + 1 + (4 * CHAR_WIDTH) + 1, MarkdownCodePainter.getInlineRight(code));
        assertEquals(first.x + 10, first.getTextX());
        assertEquals(first.y + 10, first.getTextY());
        assertEquals(10 + LINE_HEIGHT, first.parentLine.getLineHeight());
        assertNull(code.getRenderComponent().getStyle().getColor());
        assertNull(first.getRenderComponent().getStyle().getColor());
        assertFalse(first.hasFixedWidthCells());
    }

    @Test
    void pixelLookGivesInlineCodeTwoPixelsOfPaddingAroundItsLetters() {
        MarkdownTestRenderer renderer = pixelRenderer("a `code` b");
        renderer.tick();
        MarkdownTextFragment code = renderer.fragmentContaining("code");

        assertEquals(code.x + 2, code.getTextX());
        assertEquals(2 + (5 * CHAR_WIDTH) + 2, code.getRenderWidth());
        // The Minecraft font ends every letter with an empty column, so the background ends one pixel in front of the padding
        assertEquals(code.x + 2 + (4 * CHAR_WIDTH) + 2 - 1, MarkdownCodePainter.getInlineRight(code));
        assertEquals(code.getTextY() - 1, MarkdownCodePainter.getInlineTop(code));
        assertEquals(code.getTextY() + LINE_HEIGHT, MarkdownCodePainter.getInlineBottom(code));
        assertEquals(0xB4F0C6, code.getRenderComponent().getStyle().getColor().getValue() & 0xFFFFFF);
        assertFalse(code.hasFixedWidthCells());
    }

    @Test
    void pixelLookGivesCodeBlocksAPanelWithFiveAndSixPixelsOfPadding() {
        MarkdownTestRenderer renderer = pixelRenderer("```\nfirst\nmiddle\nlast\n```\n\n```\nsingle\n```");
        renderer.tick();
        MarkdownTextFragment first = renderer.fragmentContaining("first");
        MarkdownTextFragment middle = renderer.fragmentContaining("middle");
        MarkdownTextFragment last = renderer.fragmentContaining("last");
        MarkdownTextFragment single = renderer.fragmentContaining("single");

        assertEquals(5 + LINE_HEIGHT, first.parentLine.getLineHeight());
        assertEquals(LINE_HEIGHT, middle.parentLine.getLineHeight());
        assertEquals(LINE_HEIGHT + 5, last.parentLine.getLineHeight());
        assertEquals(5 + LINE_HEIGHT + 5, single.parentLine.getLineHeight());
        assertEquals(first.y + 5, first.getTextY());
        assertEquals(middle.y, middle.getTextY());
        assertEquals(last.y, last.getTextY());
        assertEquals(first.x + 6, first.getTextX());
        // Lines of the block are 11 pixels apart
        assertEquals(first.getTextY() + 11, middle.getTextY());
        assertEquals(middle.getTextY() + 11, last.getTextY());
        assertEquals(0xB4F0C6, first.getRenderComponent().getStyle().getColor().getValue() & 0xFFFFFF);
    }

    @Test
    void fixedWidthPutsEveryCharacterOfACodeBlockIntoACell() {
        MarkdownTestRenderer renderer = pixelRenderer("```\nab  cd e\n```\n`ab  cd`");
        renderer.tick();
        MarkdownTextFragment block = renderer.fragmentContaining("ab");
        MarkdownTextFragment inline = renderer.fragments.get(renderer.fragments.size() - 1);
        assertTrue(block.hasFixedWidthCells());
        assertEquals("ab  ", block.text);

        // Cells are as wide as the letter "m" unless the style says otherwise
        assertEquals(4 * CHAR_WIDTH, block.unscaledTextWidth);

        renderer.getStyle().codeBlockCellWidth.set(8.0F);
        renderer.tick();
        block = renderer.fragmentContaining("ab");

        assertEquals(4 * 8, block.unscaledTextWidth);
        assertEquals(2 * 8, renderer.fragmentContaining("cd").unscaledTextWidth - 8);
        MarkdownFixedWidthText text = block.getFixedWidthText();
        // Characters that are narrower than their cell get centered in it, each with a draw call of its own. Spaces need none.
        assertEquals(2, text.getRunCount());
        assertEquals("a", text.getRun(0).getString());
        assertEquals(1.0F, text.getRunOffset(0));
        assertEquals("b", text.getRun(1).getString());
        assertEquals(9.0F, text.getRunOffset(1));
        assertEquals(2, renderer.parses);
    }

    @Test
    void charactersThatFillTheirCellsShareOneDrawCall() {
        MarkdownTestRenderer renderer = pixelRenderer("```\nabcdef  gh\n```");
        renderer.tick();
        MarkdownTextFragment block = renderer.fragmentContaining("abcdef");

        MarkdownFixedWidthText text = block.getFixedWidthText();

        assertEquals(1, text.getRunCount());
        assertEquals("abcdef", text.getRun(0).getString());
        assertEquals(0.0F, text.getRunOffset(0));
        assertEquals(0xB4F0C6, text.getRun(0).getStyle().getColor().getValue() & 0xFFFFFF);
        // The text stays cached until something it gets built from changes
        assertTrue(text == block.getFixedWidthText());
        renderer.getStyle().codeBlockTextColor.set(null);
        assertFalse(text == block.getFixedWidthText());
        assertNull(block.getFixedWidthText().getRun(0).getStyle().getColor());
    }

    @Test
    void codeBlockWithoutFixedWidthMeasuresItsTextLikeAllOtherText() {
        MarkdownTestRenderer renderer = pixelRenderer("```\nabc\n```");
        renderer.getStyle().codeBlockCellWidth.set(8.0F);
        renderer.getStyle().codeBlockFixedWidth.set(false);
        renderer.tick();

        assertFalse(renderer.fragmentContaining("abc").hasFixedWidthCells());
        assertEquals(3 * CHAR_WIDTH, renderer.fragmentContaining("abc").unscaledTextWidth);
    }

    private static MarkdownTestRenderer pixelRenderer(String text) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, 400);
        renderer.setParentRenderScale(3.0F);
        renderer.getStyle().applyLook(MarkdownLook.PIXEL);
        return renderer;
    }

}
