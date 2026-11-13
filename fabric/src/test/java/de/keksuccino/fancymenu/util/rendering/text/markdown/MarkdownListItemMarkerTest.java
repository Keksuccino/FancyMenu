package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownMarkerShape;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownRemovedMarkerMode;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownListItemMarkerTest {

    private static final String PATCH_NOTES = "+ added\n- removed\n  - nested\n~ changed\n* highlight";

    @ParameterizedTest
    @EnumSource(MarkdownLook.class)
    void markerFollowsTheCharacterAnItemStartsWithInEveryLook(MarkdownLook look) {
        MarkdownTestRenderer renderer = renderer(PATCH_NOTES, look);
        MarkdownStyle style = renderer.getStyle();

        assertMarker(renderer, "added", style.listPlusMarkerShape.get(), style.listPlusMarkerColor);
        assertMarker(renderer, "removed", style.listMinusMarkerShape.get(), style.listMinusMarkerColor);
        assertMarker(renderer, "changed", style.listTildeMarkerShape.get(), style.listTildeMarkerColor);
        assertMarker(renderer, "highlight", style.listAsteriskMarkerShape.get(), style.listAsteriskMarkerColor);
        // Items of a list inside of a list item always have the marker of their level
        assertMarker(renderer, "nested", style.listLevel(2).markerShape.get(), style.listLevel(2).markerColor);
        // The neutral look has the icons of the UI where the other looks have pixel art
        boolean neutral = (look == MarkdownLook.NEUTRAL);
        assertSame(neutral ? MarkdownMarkerShape.ICON_ADD : MarkdownMarkerShape.PLUS, style.listPlusMarkerShape.get());
        assertSame(neutral ? MarkdownMarkerShape.ICON_REMOVE : MarkdownMarkerShape.MINUS, style.listMinusMarkerShape.get());
        assertSame(neutral ? MarkdownMarkerShape.ICON_EDIT : MarkdownMarkerShape.WAVE, style.listTildeMarkerShape.get());
        assertSame(neutral ? MarkdownMarkerShape.ICON_STAR : MarkdownMarkerShape.STAR, style.listAsteriskMarkerShape.get());
    }

    @Test
    void plainListOfDashItemsStaysANormalList() {
        MarkdownTestRenderer renderer = renderer("- one\n- two\n  - nested\n~ changed\n* highlight", MarkdownLook.PIXEL);
        MarkdownStyle style = renderer.getStyle();

        assertFalse(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("one")));
        assertMarker(renderer, "one", MarkdownMarkerShape.CURSOR, style.listLevel(1).markerColor);
        assertMarker(renderer, "two", MarkdownMarkerShape.CURSOR, style.listLevel(1).markerColor);
        assertMarker(renderer, "nested", MarkdownMarkerShape.CURSOR_SMALL, style.listLevel(2).markerColor);
    }

    @Test
    void automaticModeOnlyLooksAtTheListOfAnItem() {
        MarkdownTestRenderer renderer = renderer("## Added\n- plain\n- list\n\n### Patch notes\n+ added\n- removed\n\ntext\n\n- last", MarkdownLook.PIXEL);
        MarkdownStyle style = renderer.getStyle();

        // The first and the last list have no "+" items of their own, whatever the list between them has
        assertFalse(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("plain")));
        assertFalse(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("list")));
        assertTrue(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("removed")));
        assertFalse(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("last")));
        assertMarker(renderer, "plain", style.listLevel(1).markerShape.get(), style.listLevel(1).markerColor);
        assertMarker(renderer, "removed", style.listMinusMarkerShape.get(), style.listMinusMarkerColor);
        assertMarker(renderer, "last", style.listLevel(1).markerShape.get(), style.listLevel(1).markerColor);
    }

    @Test
    void automaticModeFindsPlusItemsBeforeAndBehindAnItemOfTheirList() {
        MarkdownTestRenderer renderer = renderer("- first\n+ added\n- last", MarkdownLook.PIXEL);

        assertTrue(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("first")));
        assertTrue(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("last")));

        renderer.setText("- first\n- text with a + in it\n- last");
        renderer.tick();

        assertFalse(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("first")));
        assertFalse(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("last")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "- first\ntext\n+ added",
            "- first\n\ntext\n\n+ added",
            "- first\n# Headline\n+ added",
            "- first\n\n## Headline\n\n+ added",
            "- first\n---\n+ added",
            "- first\n> quote\n+ added",
            "- first\n| a | b |\n| --- | --- |\n| 1 | 2 |\n+ added",
            "- first\n```\ncode\n```\n+ added",
            "+ added\ntext\n- first"
    })
    void anythingButBlankLinesBetweenTwoItemsMakesThemTwoLists(String text) {
        MarkdownTestRenderer renderer = renderer(text, MarkdownLook.PIXEL);

        assertFalse(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("first")), text);
    }

    @Test
    void blankLinesStayInsideOfAList() {
        MarkdownTestRenderer renderer = renderer("- first\n\n\n+ added\n   \n- last\n\n", MarkdownLook.PIXEL);

        assertTrue(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("first")));
        assertTrue(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("last")));
    }

    @Test
    void itemsWithNumbersAndOtherMarkersArePartOfTheList() {
        MarkdownTestRenderer renderer = renderer("- first\n1. numbered\n~ changed\n* highlight\n  - nested\n+ added\n2) numbered too\n- last", MarkdownLook.PIXEL);
        renderer.setNumberedListsEnabled(true);
        renderer.tick();

        assertTrue(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("first")));
        assertTrue(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("last")));
        assertFalse(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("nested")));
        assertFalse(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("numbered")));

        // Without numbered lists, a line with a number is text, which ends the list in front of it
        renderer.setNumberedListsEnabled(false);
        renderer.tick();

        assertFalse(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("first")));
        assertFalse(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("last")));
    }

    @Test
    void plusItemsOfEveryLevelCountForTheirListAndNestedItemsStayNeutral() {
        MarkdownTestRenderer renderer = renderer("- first\n  + deeper plus\n\ntext\n\n+ added\n  - nested\n- removed", MarkdownLook.PIXEL);
        MarkdownStyle style = renderer.getStyle();

        assertTrue(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("first")));
        assertTrue(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("removed")));
        assertFalse(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("nested").parentLine.fragments.get(0)));
        assertMarker(renderer, "nested", style.listLevel(2).markerShape.get(), style.listLevel(2).markerColor);
    }

    @Test
    void itemsThatContinueAQuoteAreNoPartOfTheListBehindTheQuote() {
        MarkdownTestRenderer renderer = renderer("> quote\n- inside\n\n+ added\n- removed", MarkdownLook.PIXEL);
        MarkdownTextFragment inside = renderer.fragmentContaining("inside");

        // The line behind a quote line continues the quote, and the blank line ends it
        assertSame(renderer.fragmentContaining("quote").quoteContext, inside.quoteContext);
        assertNull(renderer.fragmentContaining("added").quoteContext);
        assertFalse(MarkdownListMarkerPainter.isRemovedItem(inside));
        assertTrue(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("removed")));
    }

    @Test
    void itemWithFormattedTextKnowsItsList() {
        MarkdownTestRenderer renderer = renderer("- **bold** first [link](https://example.com) item\n+ *added*", MarkdownLook.PIXEL);

        assertTrue(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("first").parentLine.fragments.get(0)));
        assertMarker(renderer, "first", renderer.getStyle().listMinusMarkerShape.get(), renderer.getStyle().listMinusMarkerColor);
    }

    @Test
    void modeCanSwitchTheRemovedMarkerOffOrKeepItOn() {
        MarkdownTestRenderer renderer = renderer("- one\n  - nested", MarkdownLook.PIXEL);
        MarkdownTextFragment one = renderer.fragmentContaining("one");
        MarkdownTextFragment nested = renderer.fragmentContaining("nested");

        renderer.getStyle().listMinusMarkerMode.set(MarkdownRemovedMarkerMode.ALWAYS);
        assertTrue(MarkdownListMarkerPainter.isRemovedItem(one));
        assertFalse(MarkdownListMarkerPainter.isRemovedItem(nested));

        renderer.setText(PATCH_NOTES);
        renderer.getStyle().listMinusMarkerMode.set(MarkdownRemovedMarkerMode.NEVER);
        renderer.tick();
        assertFalse(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("removed")));
        // The mode is no part of the layout or the text
        assertEquals(2, renderer.parses);
        assertEquals(2, renderer.layouts);
    }

    @Test
    void plusItemsNeedTheExtendedMarkersOfTheRenderer() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("+ added\n- removed", 400);
        renderer.tick();

        assertFalse(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("removed")));
    }

    private static void assertMarker(MarkdownTestRenderer renderer, String text, MarkdownMarkerShape shape, Object color) {
        MarkdownTextFragment item = renderer.fragmentContaining(text).parentLine.fragments.get(0);
        assertSame(shape, MarkdownListMarkerPainter.getMarkerShape(item));
        assertSame(color, MarkdownListMarkerPainter.getMarkerColor(item));
    }

    private static MarkdownTestRenderer renderer(String text, MarkdownLook look) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, 400);
        renderer.setParentRenderScale(3.0F);
        renderer.setExtendedBulletListMarkersEnabled(true);
        renderer.getStyle().applyLook(look);
        renderer.tick();
        return renderer;
    }

}
