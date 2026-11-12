package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownTextScaleTest {

    private static final int CHAR_WIDTH = 6;
    private static final int LINE_HEIGHT = 9;
    private static final String TEXT = "# headline\nbefore\n```\nfirst\nmiddle\nlast\n```\n- bullet\n  - nested bullet\n- second bullet\n\n> quote\n\nafter `inline code` end";

    @ParameterizedTest
    @ValueSource(floats = {0.5F, 1.0F, 1.5F, 2.0F, 3.0F})
    void textOfEveryFragmentStaysInsideItsLineAndItsOwnWidth(float scale) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 2000);
        renderer.setTextBaseScale(scale);
        renderer.setX(10).setY(20);
        renderer.tick();

        for (MarkdownTextLine line : renderer.lines) {
            float lineTop = 20 + line.offsetY;
            float lineBottom = lineTop + line.getLineHeight();
            for (MarkdownTextFragment fragment : line.fragments) {
                assertTrue(fragment.getTextY() >= lineTop, () -> "text of '" + fragment.text + "' starts above its line");
                assertTrue((fragment.getTextY() + fragment.getTextHeight()) <= (lineBottom + 0.001F), () -> "text of '" + fragment.text + "' ends below its line");
                assertTrue(fragment.getTextX() >= fragment.x, () -> "text of '" + fragment.text + "' starts left of the fragment");
                assertTrue((fragment.getTextX() + fragment.getTextWidth()) <= (fragment.x + fragment.getRenderWidth() + 0.001F), () -> "text of '" + fragment.text + "' ends right of the fragment");
            }
        }
    }

    @Test
    void codeBlockPaddingScalesWithTheText() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("```\nfirst\nmiddle\nlast\n```", 2000);
        renderer.setTextBaseScale(2.0F);
        renderer.tick();
        MarkdownTextFragment first = renderer.fragmentContaining("first");
        MarkdownTextFragment middle = renderer.fragmentContaining("middle");
        MarkdownTextFragment last = renderer.fragmentContaining("last");

        assertEquals((LINE_HEIGHT * 2) + 20, first.parentLine.getLineHeight());
        assertEquals(LINE_HEIGHT * 2, middle.parentLine.getLineHeight());
        assertEquals((LINE_HEIGHT * 2) + 20, last.parentLine.getLineHeight());
        assertEquals(first.y + 20, first.getTextY());
        assertEquals(("middle".length() * CHAR_WIDTH * 2) + 20 + 20, middle.getRenderWidth());
        assertEquals(middle.x + 20, middle.getTextX());
    }

    @Test
    void bulletSpacingScalesWithTheText() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("- bullet\n- second", 2000);
        renderer.setBulletListSpacing(4);
        renderer.setTextBaseScale(2.0F);
        renderer.tick();
        MarkdownTextFragment bullet = renderer.fragmentContaining("bullet");

        assertEquals((LINE_HEIGHT * 2) + 8, bullet.parentLine.getLineHeight());
        assertEquals(bullet.y + 8, bullet.getTextY());
    }

    @Test
    void inlineCodePaddingScalesWithTheText() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("a `code` b", 2000);
        renderer.setTextBaseScale(2.0F);
        renderer.tick();
        MarkdownTextFragment code = renderer.fragmentContaining("code");

        assertEquals(code.x + 2, code.getTextX());
        assertEquals((code.text.length() * CHAR_WIDTH * 2) + 2 + 2, code.getRenderWidth());
    }

    @Test
    void paddingsStayTheSameAtNormalScale() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 2000);
        renderer.tick();

        assertEquals(LINE_HEIGHT + 10, renderer.fragmentContaining("first").parentLine.getLineHeight());
        assertEquals(("middle".length() * CHAR_WIDTH) + 10 + 10, renderer.fragmentContaining("middle").getRenderWidth());
        assertEquals(LINE_HEIGHT + 3, renderer.fragmentContaining("bullet").parentLine.getLineHeight());
        MarkdownTextFragment codeEnd = renderer.fragmentContaining("code");
        assertEquals((codeEnd.text.length() * CHAR_WIDTH) + 1, codeEnd.getRenderWidth());
    }

}
