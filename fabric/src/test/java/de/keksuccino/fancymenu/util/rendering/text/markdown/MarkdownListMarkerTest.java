package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownMarkerShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class MarkdownListMarkerTest {

    private static final int LINE_HEIGHT = 9;
    private static final String LIST = "- one\n  - two\n    - three\n      - four";

    @Test
    void classicLookPutsASmallSquareInFrontOfTheIndentedText() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(LIST, 400);
        renderer.setX(30).setY(50);
        renderer.tick();

        for (int level = 1; level <= 4; level++) {
            MarkdownTextFragment item = renderer.lines.get(level - 1).fragments.get(0);
            assertEquals(level, item.bulletListLevel);
            assertEquals(item.x + (8 * level) + 5, item.getTextX());
            // The square is 3 pixels big, and 7 pixels are between it and the text
            assertEquals(item.getTextX() - 7 - 3, MarkdownListMarkerPainter.getMarkerX(item));
            assertEquals(item.getTextY() + ((LINE_HEIGHT - 3) / 2.0F), MarkdownListMarkerPainter.getMarkerY(item));
            assertSame(MarkdownMarkerShape.SQUARE, renderer.getStyle().listLevel(level).markerShape.get());
        }
    }

    @Test
    void pixelLookIndentsBy9PixelsPerLevelAndPutsItsMarkersOnTheLowercaseLetters() {
        MarkdownTestRenderer renderer = pixelRenderer(LIST);
        renderer.setX(30).setY(50);
        renderer.tick();

        for (int level = 1; level <= 4; level++) {
            MarkdownTextFragment item = renderer.lines.get(level - 1).fragments.get(0);
            assertEquals(item.x + (9 * level), item.getTextX());
            // The marker area is 5 pixels big and starts where the text of the level above starts
            assertEquals(item.x + (9 * (level - 1)), MarkdownListMarkerPainter.getMarkerX(item));
            // Lowercase letters fill the rows 2 to 6 of a line of the Minecraft font
            assertEquals(item.getTextY() + 2, MarkdownListMarkerPainter.getMarkerY(item));
        }
        assertSame(MarkdownMarkerShape.CURSOR, renderer.getStyle().listLevel(1).markerShape.get());
        assertSame(MarkdownMarkerShape.CURSOR_SMALL, renderer.getStyle().listLevel(2).markerShape.get());
        assertSame(MarkdownMarkerShape.DOT, renderer.getStyle().listLevel(3).markerShape.get());
        // Deeper levels share the options of the third one
        assertSame(renderer.getStyle().listLevel(3), renderer.getStyle().listLevel(4));
        assertSame(renderer.getStyle().listLevel(3), renderer.getStyle().listLevel(10));
    }

    @Test
    void markerScalesWithTheTextOfItsItem() {
        MarkdownTestRenderer renderer = pixelRenderer("- item");
        renderer.setTextBaseScale(2.0F);
        renderer.setX(30).setY(50);
        renderer.tick();
        MarkdownTextFragment item = renderer.fragmentContaining("item");

        assertEquals(item.x + 18, item.getTextX());
        assertEquals(item.x, MarkdownListMarkerPainter.getMarkerX(item));
        assertEquals(item.getTextY() + 4, MarkdownListMarkerPainter.getMarkerY(item));
    }

    @Test
    void indentOfTheRendererSetterKeepsTheClassicOffsetOfTheText() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(LIST, 400);
        renderer.setBulletListIndent(12);
        renderer.tick();

        for (int level = 1; level <= 4; level++) {
            MarkdownTextFragment item = renderer.lines.get(level - 1).fragments.get(0);
            assertEquals(item.x + (12 * level) + 5, item.getTextX());
            assertEquals(item.x + (12 * level) - 5, MarkdownListMarkerPainter.getMarkerX(item));
        }
    }

    private static MarkdownTestRenderer pixelRenderer(String text) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, 400);
        renderer.setParentRenderScale(3.0F);
        renderer.getStyle().applyLook(MarkdownLook.PIXEL);
        return renderer;
    }

}
