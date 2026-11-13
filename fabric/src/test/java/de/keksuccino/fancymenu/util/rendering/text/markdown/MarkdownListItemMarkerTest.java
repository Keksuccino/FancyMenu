package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownMarkerShape;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownRemovedMarkerMode;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        assertFalse(renderer.textHasPlusListItems);
        assertMarker(renderer, "one", MarkdownMarkerShape.CURSOR, style.listLevel(1).markerColor);
        assertMarker(renderer, "two", MarkdownMarkerShape.CURSOR, style.listLevel(1).markerColor);
        assertMarker(renderer, "nested", MarkdownMarkerShape.CURSOR_SMALL, style.listLevel(2).markerColor);
    }

    @Test
    void automaticModeNoticesPlusItemsAnywhereInTheText() {
        MarkdownTestRenderer renderer = renderer("- first\n\ntext\n\n  + nested plus", MarkdownLook.PIXEL);

        assertTrue(renderer.textHasPlusListItems);
        assertTrue(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("first")));

        renderer.setText("- first\n\ntext with a + in it");
        renderer.tick();

        assertFalse(renderer.textHasPlusListItems);
        assertFalse(MarkdownListMarkerPainter.isRemovedItem(renderer.fragmentContaining("first")));
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

        assertFalse(renderer.textHasPlusListItems);
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
