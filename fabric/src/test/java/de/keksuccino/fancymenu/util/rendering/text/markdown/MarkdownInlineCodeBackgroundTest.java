package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownInlineCodeBackgroundTest {

    @ParameterizedTest
    @ValueSource(strings = {"text with `code` in it", "- item with `code`", "- item\n  - nested item with `code`", "> quote with `code`", "# headline with `code`"})
    void backgroundSurroundsTheTextOfItsCodeSpan(String text) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, 2000);
        renderer.setBulletListSpacing(7);
        renderer.setX(30).setY(50);
        renderer.tick();
        MarkdownTextFragment code = renderer.fragmentContaining("code");

        float top = MarkdownCodePainter.getInlineTop(code);
        float bottom = MarkdownCodePainter.getInlineBottom(code);

        assertTrue(top <= code.getTextY(), "the background starts below the top of its text");
        assertTrue((code.getTextY() - top) <= 2.0F, "the background starts too far above its text");
        assertEquals(code.getTextY() + code.getTextHeight(), bottom);
    }

}
