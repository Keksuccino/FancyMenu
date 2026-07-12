package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TextEditorSelectionLineResolverTest {

    @Test
    void resolvesLineContainingPointer() {
        List<LineBounds> lines = List.of(new LineBounds("first", 10, 10), new LineBounds("second", 20, 10));

        LineBounds resolved = resolve(lines, 14.0D, 10, 30);

        assertEquals("first", resolved.name());
    }

    @Test
    void sharedBoundaryBelongsToFollowingLine() {
        List<LineBounds> lines = List.of(new LineBounds("first", 10, 10), new LineBounds("second", 20, 10));

        LineBounds resolved = resolve(lines, 20.0D, 10, 30);

        assertEquals("second", resolved.name());
    }

    @Test
    void fullyClippedLineCannotBecomeSelectionEndpoint() {
        List<LineBounds> lines = List.of(new LineBounds("visible", 20, 10), new LineBounds("clipped", 30, 10));

        LineBounds resolved = resolve(lines, 30.0D, 10, 30);

        assertEquals("visible", resolved.name());
    }

    @Test
    void resolvesNearestVisibleLineAcrossEmptyVerticalSpace() {
        List<LineBounds> lines = List.of(new LineBounds("first", 10, 5), new LineBounds("second", 25, 5));

        LineBounds resolved = resolve(lines, 23.0D, 10, 30);

        assertEquals("second", resolved.name());
    }

    @Test
    void returnsNullWhenEveryLineIsClipped() {
        List<LineBounds> lines = List.of(new LineBounds("above", 0, 10), new LineBounds("below", 30, 10));

        LineBounds resolved = resolve(lines, 20.0D, 10, 30);

        assertNull(resolved);
    }

    private static LineBounds resolve(List<LineBounds> lines, double mouseY, int editorTop, int editorBottom) {
        return TextEditorSelectionLineResolver.findClosestVisibleLineAtY(lines, mouseY, editorTop, editorBottom, LineBounds::y, LineBounds::height);
    }

    private record LineBounds(String name, int y, int height) {}

}
