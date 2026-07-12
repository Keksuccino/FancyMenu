package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.editor;

import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframe;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyframeEditorStateTest {

    @Test
    void undoAndRedoUseDeepSnapshotsAndRestoreSelectionByIdentifier() {
        KeyframeEditorState state = new KeyframeEditorState(List.of(keyframe(0L), keyframe(100L)));
        AnimationKeyframe selected = state.getKeyframes().getLast();
        state.select(selected, false);
        state.saveSnapshot();
        selected.timestamp = 250L;

        assertTrue(state.undo());
        assertEquals(100L, state.getKeyframes().getLast().timestamp);
        assertSame(state.getKeyframes().getLast(), state.getSelectedKeyframes().getFirst());

        assertTrue(state.redo());
        assertEquals(250L, state.getKeyframes().getLast().timestamp);
        state.getKeyframes().getLast().timestamp = 400L;
        assertTrue(state.undo());
        assertEquals(100L, state.getKeyframes().getLast().timestamp);
    }

    @Test
    void deletingSelectionCreatesOneReversibleEdit() {
        KeyframeEditorState state = new KeyframeEditorState(List.of(keyframe(0L), keyframe(100L), keyframe(200L)));
        state.select(state.getKeyframes().get(1), false);
        state.select(state.getKeyframes().get(2), true);

        assertTrue(state.deleteSelected());
        assertEquals(1, state.getKeyframes().size());
        assertTrue(state.getSelectedKeyframes().isEmpty());
        assertTrue(state.undo());
        assertEquals(3, state.getKeyframes().size());
        assertFalse(state.canUndo());
    }

    @Test
    void smoothingKeepsFirstTimestampAndSortsTheDocument() {
        KeyframeEditorState state = new KeyframeEditorState(List.of(keyframe(100L), keyframe(300L), keyframe(500L)));
        state.select(state.getKeyframes().get(2), false);
        state.select(state.getKeyframes().get(0), true);
        state.select(state.getKeyframes().get(1), true);

        assertTrue(state.smoothSelected(50L));
        assertEquals(List.of(100L, 150L, 200L), state.getKeyframes().stream().map(keyframe -> keyframe.timestamp).toList());
    }

    @Test
    void exposesLiveViewsWithoutAllowingStructuralMutationOutsideTheDocument() {
        KeyframeEditorState state = new KeyframeEditorState(List.of(keyframe(0L)));

        assertThrows(UnsupportedOperationException.class, () -> state.getKeyframes().clear());
        assertThrows(UnsupportedOperationException.class, () -> state.getSelectedKeyframes().add(state.getKeyframes().getFirst()));
    }

    private static AnimationKeyframe keyframe(long timestamp) {
        return new AnimationKeyframe(timestamp, 0, 0, 10, 10, ElementAnchorPoints.TOP_LEFT, false);
    }

}
