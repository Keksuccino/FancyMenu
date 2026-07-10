package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextEditorSelectionMathTest {

    @Test
    void ignoresFullyClippedLinesAndKeepsPartiallyVisibleLines() {
        List<Bounds> lines = List.of(new Bounds(70, 10), new Bounds(85, 10), new Bounds(105, 10), new Bounds(125, 10), new Bounds(140, 10));

        assertEquals(1, findClosestVisibleIndex(90, lines));
        assertEquals(2, findClosestVisibleIndex(109, lines));
        assertEquals(3, findClosestVisibleIndex(130, lines));
    }

    @Test
    void exactSharedBoundaryBelongsToFollowingLine() {
        List<Bounds> lines = List.of(new Bounds(100, 10), new Bounds(110, 10));

        assertEquals(1, findClosestVisibleIndex(110, lines));
    }

    @Test
    void resolvesNearestVisibleLineAcrossVerticalGaps() {
        List<Bounds> lines = List.of(new Bounds(100, 10), new Bounds(120, 10));

        assertEquals(0, findClosestVisibleIndex(114, lines));
        assertEquals(1, findClosestVisibleIndex(116, lines));
    }

    @Test
    void returnsNoIndexWhenEveryLineIsClipped() {
        List<Bounds> lines = List.of(new Bounds(70, 10), new Bounds(130, 10));

        assertEquals(-1, findClosestVisibleIndex(110, lines));
    }

    private int findClosestVisibleIndex(double mouseY, List<Bounds> lines) {
        return TextEditorSelectionMath.findClosestVisibleIndex(mouseY, 90, 130, lines, Bounds::y, Bounds::height);
    }

    private record Bounds(int y, int height) {
    }
}
