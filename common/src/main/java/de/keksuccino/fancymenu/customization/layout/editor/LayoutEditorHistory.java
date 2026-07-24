package de.keksuccino.fancymenu.customization.layout.editor;

import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidget;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LayoutEditorHistory {

    protected LayoutEditorScreen editor;
    private List<Snapshot> history = new ArrayList<>();
    private int current = -1;
    private boolean preventSnapshotSaving = false;

    public LayoutEditorHistory(LayoutEditorScreen editor) {
        this.editor = editor;
    }

    public void saveSnapshot() {
        this.saveSnapshot(this.createSnapshot());
    }

    public void saveSnapshot(Snapshot snap) {
        List<Snapshot> discardedSnapshots = new ArrayList<>();
        if (!this.preventSnapshotSaving) {
            this.editor.unsavedChanges = true;
            if (this.current < 0) {
                discardedSnapshots.addAll(this.history);
                this.history.clear();
                this.history.add(snap);
                this.current = 0;
            } else if (this.current <= this.history.size() - 1) {
                List<Snapshot> previousHistory = this.history;
                List<Snapshot> retainedHistory = new ArrayList<>();
                int i = 0;
                while (i <= this.current) {
                    retainedHistory.add(this.history.get(i));
                    i++;
                }
                retainedHistory.add(snap);
                this.history = retainedHistory;
                this.current = this.history.size() - 1;
                discardedSnapshots.addAll(previousHistory);
            } else {
                this.current = this.history.size() - 1;
                this.saveSnapshot(snap);
                return;
            }
        } else {
            discardedSnapshots.add(snap);
        }
        this.destroySnapshotsNoLongerReferenced(discardedSnapshots);
    }

    public Snapshot createSnapshot() {
        return new Snapshot(this.editor);
    }

    public void setPreventSnapshotSaving(boolean preventSaving) {
        this.preventSnapshotSaving = preventSaving;
    }

    public void stepBack() {
        if ((this.current < 0) || (this.current > this.history.size() - 1)) return;

        Snapshot snapshot = this.history.get(this.current);
        Snapshot replacedPreSnapshotState = snapshot.preSnapshotState;
        Layout replacedLayout = this.editor.layout;
        this.current--;
        snapshot.preSnapshotState = this.createSnapshot();
        this.editor.layout = snapshot.snapshot;
        this.resetEditorAfterHistoryStep();
        if (replacedPreSnapshotState != null) this.destroySnapshotsNoLongerReferenced(List.of(replacedPreSnapshotState));
        this.destroyLayoutsNoLongerReferenced(List.of(replacedLayout));
    }

    public void stepForward() {
        if ((this.current < -1) || (this.current >= this.history.size() - 1)) return;

        this.current++;
        Snapshot snapshot = this.history.get(this.current).preSnapshotState;
        if (snapshot == null) return;
        Layout replacedLayout = this.editor.layout;
        this.editor.layout = snapshot.snapshot;
        this.resetEditorAfterHistoryStep();
        this.destroyLayoutsNoLongerReferenced(List.of(replacedLayout));
    }

    public void destroy() {
        List<Snapshot> discardedSnapshots = new ArrayList<>(this.history);
        if (this.editor.preDragElementSnapshot != null) discardedSnapshots.add(this.editor.preDragElementSnapshot);
        this.history.clear();
        this.current = -1;
        this.editor.preDragElementSnapshot = null;
        this.destroySnapshotsNoLongerReferenced(discardedSnapshots);
    }

    /** Permanently releases a snapshot unless its exact layout remains reachable from the current editor history. */
    public void discardSnapshot(@Nullable Snapshot snapshot) {
        if (snapshot != null) this.destroySnapshotsNoLongerReferenced(List.of(snapshot));
    }

    private void resetEditorAfterHistoryStep() {
        Snapshot discardedPreDragSnapshot = this.editor.preDragElementSnapshot;
        this.editor.isMouseSelection = false;
        this.editor.preDragElementSnapshot = null;
        this.editor.rightClickMenu.closeMenu();
        if (this.editor.activeElementContextMenu != null) {
            this.editor.activeElementContextMenu.closeMenu();
            this.editor.activeElementContextMenu = null;
        }
        this.editor.constructElementInstances();
        for (AbstractLayoutEditorWidget widget : this.editor.layoutEditorWidgets) widget.refresh();
        this.discardSnapshot(discardedPreDragSnapshot);
    }

    private void destroySnapshotsNoLongerReferenced(Collection<Snapshot> snapshots) {
        LayoutEditorSnapshotLifecycle.destroySnapshotsNoLongerReferenced(this.editor.layout, this.history, snapshots);
    }

    private void destroyLayoutsNoLongerReferenced(Collection<Layout> layouts) {
        LayoutEditorSnapshotLifecycle.destroyLayoutsNoLongerReferenced(this.editor.layout, this.history, layouts);
    }

    public static class Snapshot {

        public Layout snapshot;
        public Snapshot preSnapshotState = null;

        public Snapshot(LayoutEditorScreen editor) {
            editor.serializeElementInstancesToLayoutInstance();
            this.snapshot = editor.layout.copy();
        }

        Snapshot(Layout snapshot) {
            this.snapshot = snapshot;
        }
    }
}
