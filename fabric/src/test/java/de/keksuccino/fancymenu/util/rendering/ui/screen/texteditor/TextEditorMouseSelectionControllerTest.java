package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextEditorMouseSelectionControllerTest {

    @Test
    void clampsSelectionEndpointsToEditorBounds() {
        assertEquals(10.0D, TextEditorMouseSelectionController.clampEndpoint(-5.0D, 10.0D, 30.0D));
        assertEquals(18.5D, TextEditorMouseSelectionController.clampEndpoint(18.5D, 10.0D, 30.0D));
        assertEquals(30.0D, TextEditorMouseSelectionController.clampEndpoint(45.0D, 10.0D, 30.0D));
    }

    @Test
    void resolvesOnlyVisiblePortionsOfClippedLines() {
        List<LineBounds> lines = List.of(new LineBounds(0, 10), new LineBounds(10, 10), new LineBounds(20, 10));

        assertEquals(0, resolve(lines, 5.0D, 5, 18));
        assertEquals(1, resolve(lines, 17.9D, 5, 18));
        assertEquals(1, resolve(lines, 18.0D, 5, 18));
    }

    @Test
    void sharedBoundaryBelongsToFollowingLine() {
        List<LineBounds> lines = List.of(new LineBounds(0, 10), new LineBounds(10, 10));

        assertEquals(1, resolve(lines, 10.0D, 0, 20));
    }

    @Test
    void resolvesClosestVisibleLineAcrossEmptyVerticalSpace() {
        List<LineBounds> lines = List.of(new LineBounds(0, 5), new LineBounds(10, 5));

        assertEquals(0, resolve(lines, 7.0D, 0, 15));
        assertEquals(1, resolve(lines, 8.0D, 0, 15));
    }

    @Test
    void emptyEditorHasNoSelectionLine() {
        assertEquals(-1, resolve(List.of(), 5.0D, 0, 10));
    }

    @Test
    void captureRemainsValidWhilePointerAndWindowStillOwnIt() {
        assertTrue(TextEditorMouseSelectionController.isCaptureValid(true, true, false, true, true));
        assertTrue(TextEditorMouseSelectionController.isCaptureValid(false, false, true, false, true));
    }

    @Test
    void releasedMouseInvalidatesCapture() {
        assertFalse(TextEditorMouseSelectionController.isCaptureValid(true, true, false, true, false));
    }

    @Test
    void unavailableWindowInvalidatesCapture() {
        assertFalse(TextEditorMouseSelectionController.isCaptureValid(true, false, false, true, true));
        assertFalse(TextEditorMouseSelectionController.isCaptureValid(true, true, true, true, true));
        assertFalse(TextEditorMouseSelectionController.isCaptureValid(true, true, false, false, true));
    }

    private static int resolve(List<LineBounds> lines, double mouseY, int editorTop, int editorBottom) {
        return TextEditorMouseSelectionController.findClosestVisibleLineIndex(lines, mouseY, editorTop, editorBottom, LineBounds::y, LineBounds::height);
    }

    private record LineBounds(int y, int height) {}
}
