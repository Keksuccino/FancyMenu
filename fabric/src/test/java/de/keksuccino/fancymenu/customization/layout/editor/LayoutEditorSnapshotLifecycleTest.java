package de.keksuccino.fancymenu.customization.layout.editor;

import de.keksuccino.fancymenu.customization.layout.Layout;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutEditorSnapshotLifecycleTest {

    @Test
    void discardedUnsavedPreDragSnapshotIsDestroyed() {
        TrackingLayout currentLayout = new TrackingLayout();
        TrackingLayout preDragLayout = new TrackingLayout();
        LayoutEditorHistory.Snapshot preDragSnapshot = new LayoutEditorHistory.Snapshot(preDragLayout);

        LayoutEditorSnapshotLifecycle.destroySnapshotsNoLongerReferenced(currentLayout, List.of(), List.of(preDragSnapshot));

        assertTrue(preDragLayout.destroyed);
        assertFalse(currentLayout.destroyed);
    }

    @Test
    void savedPreDragSnapshotRemainsAliveUntilRemovedFromHistory() {
        TrackingLayout currentLayout = new TrackingLayout();
        TrackingLayout preDragLayout = new TrackingLayout();
        LayoutEditorHistory.Snapshot preDragSnapshot = new LayoutEditorHistory.Snapshot(preDragLayout);

        LayoutEditorSnapshotLifecycle.destroySnapshotsNoLongerReferenced(currentLayout, List.of(preDragSnapshot), List.of(preDragSnapshot));
        assertFalse(preDragLayout.destroyed);

        LayoutEditorSnapshotLifecycle.destroySnapshotsNoLongerReferenced(currentLayout, List.of(), List.of(preDragSnapshot));
        assertTrue(preDragLayout.destroyed);
    }

    @Test
    void nestedRedoStateIsRetainedAndThenDestroyedExactlyOnceWithItsParent() {
        TrackingLayout currentLayout = new TrackingLayout();
        TrackingLayout snapshotLayout = new TrackingLayout();
        TrackingLayout redoLayout = new TrackingLayout();
        LayoutEditorHistory.Snapshot snapshot = new LayoutEditorHistory.Snapshot(snapshotLayout);
        snapshot.preSnapshotState = new LayoutEditorHistory.Snapshot(redoLayout);

        LayoutEditorSnapshotLifecycle.destroySnapshotsNoLongerReferenced(currentLayout, List.of(snapshot), List.of(snapshot));
        assertFalse(snapshotLayout.destroyed);
        assertFalse(redoLayout.destroyed);

        LayoutEditorSnapshotLifecycle.destroySnapshotsNoLongerReferenced(currentLayout, List.of(), List.of(snapshot, snapshot));
        assertTrue(snapshotLayout.destroyed);
        assertTrue(redoLayout.destroyed);
        assertEquals(1, snapshotLayout.destroyCalls);
        assertEquals(1, redoLayout.destroyCalls);
    }

    private static final class TrackingLayout extends Layout {

        private boolean destroyed;
        private int destroyCalls;

        private TrackingLayout() {
            super("test-screen");
        }

        @Override
        public void destroy() {
            if (this.destroyed) return;
            this.destroyed = true;
            this.destroyCalls++;
            super.destroy();
        }
    }
}
