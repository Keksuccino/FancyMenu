package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownHeadlineDecoration;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownHeadlineStyleTest {

    private static final int CHAR_WIDTH = 6;
    private static final int LINE_HEIGHT = 9;
    private static final String TEXT = "# first\n## second\n### third\ntext";

    @Test
    void classicLookScalesEveryLevelAndKeepsRoomForAnUnderlineBelowEveryLine() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 400);
        renderer.tick();

        assertHeadline(renderer, "first", 2.0F, (LINE_HEIGHT * 2.0F) + 8);
        assertHeadline(renderer, "second", 1.6F, (LINE_HEIGHT * 1.6F) + 8);
        assertHeadline(renderer, "third", 1.2F, (LINE_HEIGHT * 1.2F) + 6);
        Style style = renderer.fragmentContaining("second").getRenderComponent().getStyle();
        assertFalse(style.isBold());
        assertNull(style.getColor());
    }

    @Test
    void classicLookKeepsThatRoomInEveryLineAHeadlineWrapsInto() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("# one two three four five six", 100);
        renderer.tick();

        assertTrue(renderer.lines.size() > 2);
        for (MarkdownTextLine line : renderer.lines) {
            assertEquals(8, line.paddingBottom);
            assertEquals((LINE_HEIGHT * 2) + 8, line.getLineHeight());
        }
    }

    @Test
    void pixelLookKeepsTheSmallerLevelsAtTextSizeAndMakesThemBold() {
        MarkdownTestRenderer renderer = pixelRenderer(TEXT, 400);
        renderer.tick();

        // The rule of the biggest level stands 3 pixels below it and is 1 pixel thick, with the row of its shadow below it
        assertHeadline(renderer, "first", 2.0F, (LINE_HEIGHT * 2) + 3 + 1 + 1);
        assertHeadline(renderer, "second", 1.0F, LINE_HEIGHT);
        assertHeadline(renderer, "third", 1.0F, LINE_HEIGHT);
        assertFalse(renderer.fragmentContaining("first").getRenderComponent().getStyle().isBold());
        assertTrue(renderer.fragmentContaining("second").getRenderComponent().getStyle().isBold());
        assertTrue(renderer.fragmentContaining("third").getRenderComponent().getStyle().isBold());
        assertEquals(0xFFC94D, renderer.fragmentContaining("first").getRenderComponent().getStyle().getColor().getValue() & 0xFFFFFF);
        assertEquals(0xFFC94D, renderer.fragmentContaining("second").getRenderComponent().getStyle().getColor().getValue() & 0xFFFFFF);
        assertEquals(0xFFFFFF, renderer.fragmentContaining("third").getRenderComponent().getStyle().getColor().getValue() & 0xFFFFFF);
    }

    @Test
    void ruleOnlyTakesSpaceBelowTheLastLineOfItsHeadline() {
        MarkdownTestRenderer renderer = pixelRenderer("# one two three four five six", 100);
        renderer.tick();

        assertTrue(renderer.lines.size() > 2);
        for (int i = 0; i < renderer.lines.size(); i++) {
            boolean last = (i == (renderer.lines.size() - 1));
            assertEquals(last ? 5 : 0, renderer.lines.get(i).paddingBottom, "padding below line " + i);
            // Lines of the same headline keep the line spacing between them
            if (i > 0) assertEquals(renderer.lines.get(i - 1).offsetY + (LINE_HEIGHT * 2) + 2, renderer.lines.get(i).offsetY);
        }
    }

    @Test
    void boldTextInsideAHeadlineKeepsTheColorOfTheHeadline() {
        MarkdownTestRenderer renderer = pixelRenderer("## headline **strong**\ntext **bold**", 400);
        renderer.tick();

        assertEquals(0xFFC94D, renderer.fragmentContaining("strong").getRenderComponent().getStyle().getColor().getValue() & 0xFFFFFF);
        assertEquals(0xFFFFFF, renderer.fragmentContaining("bold").getRenderComponent().getStyle().getColor().getValue() & 0xFFFFFF);
    }

    @Test
    void optionsOfOneLevelLeaveTheOtherLevelsAlone() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 400);
        renderer.getStyle().headline(2).scale.set(3.0F);
        renderer.getStyle().headline(2).color.set(DrawableColor.of(1, 2, 3));
        renderer.getStyle().headline(2).decoration.set(MarkdownHeadlineDecoration.NONE);
        renderer.getStyle().headline(2).linePadding.set(0.0F);
        renderer.tick();

        assertHeadline(renderer, "first", 2.0F, (LINE_HEIGHT * 2.0F) + 8);
        assertHeadline(renderer, "second", 3.0F, LINE_HEIGHT * 3.0F);
        assertHeadline(renderer, "third", 1.2F, (LINE_HEIGHT * 1.2F) + 6);
        assertEquals(0x010203, renderer.fragmentContaining("second").getRenderComponent().getStyle().getColor().getValue() & 0xFFFFFF);
        assertNull(renderer.fragmentContaining("first").getRenderComponent().getStyle().getColor());
    }

    @Test
    void headlinePaddingsGrowWithTheTextIfTheStyleScalesItsSizes() {
        MarkdownTestRenderer renderer = pixelRenderer("# first", 400);
        renderer.setTextBaseScale(2.0F);
        renderer.tick();

        assertHeadline(renderer, "first", 4.0F, (LINE_HEIGHT * 4) + 10);
    }

    private static void assertHeadline(MarkdownTestRenderer renderer, String text, float scale, float lineHeight) {
        MarkdownTextFragment fragment = renderer.fragmentContaining(text);
        assertEquals(scale, fragment.getScale());
        assertEquals(text.length() * CHAR_WIDTH * scale, fragment.getTextRenderWidth(), 0.001F);
        assertEquals(lineHeight, fragment.parentLine.getLineHeight(), 0.001F);
        // The text stays at the top of its line, above the room for the decoration
        assertEquals(fragment.y, fragment.getTextY());
    }

    private static MarkdownTestRenderer pixelRenderer(String text, float width) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, width);
        renderer.setParentRenderScale(3.0F);
        renderer.getStyle().applyLook(MarkdownLook.PIXEL);
        return renderer;
    }

}
