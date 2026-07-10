package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextEditorMouseSelectionPolicyTest {

    private static final ToIntFunction<LineBounds> LINE_TOP = LineBounds::top;
    private static final ToIntFunction<LineBounds> LINE_HEIGHT = LineBounds::height;

    @Test
    void captureRequiresBothWindowOwnershipAndPressedLeftButton() {
        assertAll(() -> assertTrue(TextEditorMouseSelectionPolicy.isCaptureValid(true, true)), () -> assertFalse(TextEditorMouseSelectionPolicy.isCaptureValid(false, true)), () -> assertFalse(TextEditorMouseSelectionPolicy.isCaptureValid(true, false)), () -> assertFalse(TextEditorMouseSelectionPolicy.isCaptureValid(false, false)));
    }

    @Test
    void fullyClippedLinesAreExcludedFromEndpointResolution() {
        List<LineBounds> lines = List.of(new LineBounds(70, 10), new LineBounds(80, 10), new LineBounds(90, 10), new LineBounds(100, 10));

        assertAll(() -> assertEquals(1, resolve(80.0D, 80, 100, lines)), () -> assertEquals(2, resolve(99.0D, 80, 100, lines)), () -> assertEquals(2, resolve(100.0D, 80, 100, lines)));
    }

    @Test
    void sharedBoundaryBelongsToFollowingLine() {
        List<LineBounds> lines = List.of(new LineBounds(80, 10), new LineBounds(90, 10));

        assertAll(() -> assertEquals(0, resolve(89.999D, 80, 100, lines)), () -> assertEquals(1, resolve(90.0D, 80, 100, lines)));
    }

    @Test
    void coordinatesOutsideVisibleContentResolveToNearestVisibleLine() {
        List<LineBounds> lines = List.of(new LineBounds(85, 5), new LineBounds(95, 5));

        assertAll(() -> assertEquals(0, resolve(80.0D, 80, 100, lines)), () -> assertEquals(0, resolve(92.0D, 80, 100, lines)), () -> assertEquals(1, resolve(93.0D, 80, 100, lines)), () -> assertEquals(1, resolve(100.0D, 80, 100, lines)));
    }

    @Test
    void noVisibleLinesProduceNoEndpoint() {
        List<LineBounds> lines = List.of(new LineBounds(60, 10), new LineBounds(100, 10));

        assertEquals(-1, resolve(90.0D, 80, 100, lines));
    }

    private static int resolve(double mouseY, int editorTop, int editorBottom, List<LineBounds> lines) {
        return TextEditorMouseSelectionPolicy.findClosestVisibleLineIndex(mouseY, editorTop, editorBottom, lines, LINE_TOP, LINE_HEIGHT);
    }

    private record LineBounds(int top, int height) {
    }

}
