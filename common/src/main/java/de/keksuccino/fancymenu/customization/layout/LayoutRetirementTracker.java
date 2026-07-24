package de.keksuccino.fancymenu.customization.layout;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Keeps replaced layout instances alive only while an active screen layer still renders them. */
final class LayoutRetirementTracker {

    private final Object lock = new Object();
    private final Set<Layout> retiredLayouts = newIdentityLayoutSet();

    void retire(@NotNull Collection<Layout> layouts, @NotNull Collection<Layout> activeLayouts) {
        Set<Layout> active = newIdentityLayoutSet(activeLayouts);
        List<Layout> layoutsToDestroy = new ArrayList<>();
        synchronized (this.lock) {
            for (Layout layout : layouts) {
                if (active.contains(layout)) {
                    this.retiredLayouts.add(layout);
                } else {
                    this.retiredLayouts.remove(layout);
                    layoutsToDestroy.add(layout);
                }
            }
        }
        layoutsToDestroy.forEach(Layout::destroy);
    }

    void releaseNotIn(@NotNull Collection<Layout> retainedLayouts) {
        Set<Layout> retained = newIdentityLayoutSet(retainedLayouts);
        List<Layout> layoutsToDestroy = new ArrayList<>();
        synchronized (this.lock) {
            Iterator<Layout> iterator = this.retiredLayouts.iterator();
            while (iterator.hasNext()) {
                Layout retired = iterator.next();
                if (retained.contains(retired)) continue;
                iterator.remove();
                layoutsToDestroy.add(retired);
            }
        }
        layoutsToDestroy.forEach(Layout::destroy);
    }

    void destroyAll(@NotNull Collection<Layout> loadedLayouts) {
        Set<Layout> layoutsToDestroy = newIdentityLayoutSet(loadedLayouts);
        synchronized (this.lock) {
            layoutsToDestroy.addAll(this.retiredLayouts);
            this.retiredLayouts.clear();
        }
        layoutsToDestroy.forEach(Layout::destroy);
    }

    @NotNull
    private static Set<Layout> newIdentityLayoutSet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    @NotNull
    private static Set<Layout> newIdentityLayoutSet(@NotNull Collection<Layout> layouts) {
        Set<Layout> identities = newIdentityLayoutSet();
        identities.addAll(layouts);
        return identities;
    }
}
