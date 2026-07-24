package de.keksuccino.fancymenu.customization.layout.editor;

import de.keksuccino.fancymenu.customization.layout.Layout;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Identity-based reachability handling for layout copies owned by editor history snapshots. */
final class LayoutEditorSnapshotLifecycle {

    private LayoutEditorSnapshotLifecycle() {
    }

    static void destroySnapshotsNoLongerReferenced(@NotNull Layout currentLayout, @NotNull Collection<LayoutEditorHistory.Snapshot> retainedSnapshots, @NotNull Collection<LayoutEditorHistory.Snapshot> discardedSnapshots) {
        Set<Layout> candidates = newIdentityLayoutSet();
        Set<LayoutEditorHistory.Snapshot> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (LayoutEditorHistory.Snapshot snapshot : discardedSnapshots) collectSnapshotLayouts(snapshot, candidates, visited);
        destroyLayoutsNoLongerReferenced(currentLayout, retainedSnapshots, candidates);
    }

    static void destroyLayoutsNoLongerReferenced(@NotNull Layout currentLayout, @NotNull Collection<LayoutEditorHistory.Snapshot> retainedSnapshots, @NotNull Collection<Layout> candidateLayouts) {
        Set<Layout> retained = newIdentityLayoutSet();
        retained.add(currentLayout);
        Set<LayoutEditorHistory.Snapshot> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (LayoutEditorHistory.Snapshot snapshot : retainedSnapshots) collectSnapshotLayouts(snapshot, retained, visited);
        Set<Layout> candidates = newIdentityLayoutSet();
        candidates.addAll(candidateLayouts);
        candidates.removeAll(retained);
        candidates.forEach(Layout::destroy);
    }

    private static void collectSnapshotLayouts(LayoutEditorHistory.Snapshot snapshot, @NotNull Set<Layout> layouts, @NotNull Set<LayoutEditorHistory.Snapshot> visited) {
        if ((snapshot == null) || !visited.add(snapshot)) return;
        if (snapshot.snapshot != null) layouts.add(snapshot.snapshot);
        collectSnapshotLayouts(snapshot.preSnapshotState, layouts, visited);
    }

    @NotNull
    private static Set<Layout> newIdentityLayoutSet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }
}
