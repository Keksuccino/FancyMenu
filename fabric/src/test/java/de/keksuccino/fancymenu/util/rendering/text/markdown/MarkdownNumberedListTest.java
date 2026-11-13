package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownNumberAlignment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownNumberedListTest {

    private static final int CHAR_WIDTH = 6;
    private static final String LIST = "1. one\n2. two\n10. ten\n  7) nested\n- bullet\ntext";

    @ParameterizedTest
    @EnumSource(MarkdownLook.class)
    void numberStandsRightAlignedInFrontOfItsItemInEveryLook(MarkdownLook look) {
        MarkdownTestRenderer renderer = renderer(LIST, look);
        float gap = renderer.getStyle().listNumberGap.get();

        float textX = line(renderer, "one").fragments.get(0).getTextX();
        for (String item : new String[]{"one", "two", "ten"}) {
            MarkdownTextLine line = line(renderer, item);
            MarkdownTextFragment first = line.fragments.get(0);
            assertNotNull(line.listNumber);
            assertEquals(first.listItemNumber, line.listNumber.getString());
            assertEquals(first.listItemNumber.length() * CHAR_WIDTH, line.listNumberWidth);
            // The texts of all items of a level start below each other, and their numbers end below each other
            assertEquals(textX, first.getTextX());
            assertEquals(textX - gap, MarkdownListMarkerPainter.getNumberX(line) + line.listNumberWidth);
            // Short numbers have room in front of their item
            if (first.listItemNumber.length() == 2) assertTrue(MarkdownListMarkerPainter.getNumberX(line) >= renderer.getStyle().padding.get(), "number starts in front of the text area");
        }
        assertEquals("7)", line(renderer, "nested").listNumber.getString());
        assertNull(line(renderer, "bullet").listNumber);
        assertNull(line(renderer, "text").listNumber);
    }

    @Test
    void pixelLookPutsNumbersWhereTheMockupHasThem() {
        MarkdownTestRenderer renderer = renderer(LIST, MarkdownLook.PIXEL);
        MarkdownTextLine one = line(renderer, "one");
        MarkdownTextLine nested = line(renderer, "nested");
        MarkdownTextFragment bullet = line(renderer, "bullet").fragments.get(0);

        // Text 15 pixels behind the start of the level, and numbers that end 3 pixels in front of it
        assertEquals(one.fragments.get(0).x + 15, one.fragments.get(0).getTextX());
        assertEquals(one.fragments.get(0).x + 12 - (2 * CHAR_WIDTH), MarkdownListMarkerPainter.getNumberX(one));
        assertEquals(nested.fragments.get(0).x + 9 + 15, nested.fragments.get(0).getTextX());
        assertEquals(bullet.x + 9, bullet.getTextX());
    }

    @Test
    void leftAlignedNumbersStartWhereTheMarkersOfTheirLevelStart() {
        MarkdownTestRenderer renderer = renderer(LIST, MarkdownLook.PIXEL);
        renderer.getStyle().listNumberAlignment.set(MarkdownNumberAlignment.LEFT);
        MarkdownTextLine one = line(renderer, "one");
        MarkdownTextLine ten = line(renderer, "ten");
        MarkdownTextFragment bullet = line(renderer, "bullet").fragments.get(0);

        assertEquals(MarkdownListMarkerPainter.getMarkerX(bullet), MarkdownListMarkerPainter.getNumberX(one));
        assertEquals(MarkdownListMarkerPainter.getNumberX(one), MarkdownListMarkerPainter.getNumberX(ten));
    }

    @Test
    void wrappedLinesOfANumberedItemKeepItsIndentButGetNoNumber() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("1. one two three four five six seven", 100);
        renderer.setNumberedListsEnabled(true);
        renderer.tick();

        assertTrue(renderer.lines.size() > 1);
        float textX = renderer.lines.get(0).fragments.get(0).getTextX();
        assertNotNull(renderer.lines.get(0).listNumber);
        for (int i = 1; i < renderer.lines.size(); i++) {
            assertNull(renderer.lines.get(i).listNumber);
            assertEquals(textX, renderer.lines.get(i).fragments.get(0).getTextX());
        }
    }

    @Test
    void numbersNeedTheNumberedListsOfTheRenderer() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("1. one", 400);
        renderer.tick();

        assertNull(renderer.lines.get(0).listNumber);
        assertEquals(renderer.lines.get(0).fragments.get(0).x, renderer.lines.get(0).fragments.get(0).getTextX());
    }

    private static MarkdownTextLine line(MarkdownTestRenderer renderer, String text) {
        return renderer.fragmentContaining(text).parentLine;
    }

    private static MarkdownTestRenderer renderer(String text, MarkdownLook look) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, 400);
        renderer.setParentRenderScale(3.0F);
        renderer.setNumberedListsEnabled(true);
        renderer.getStyle().applyLook(look);
        renderer.tick();
        return renderer;
    }

}
