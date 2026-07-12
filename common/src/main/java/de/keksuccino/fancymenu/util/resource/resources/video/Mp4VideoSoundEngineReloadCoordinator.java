package de.keksuccino.fancymenu.util.resource.resources.video;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

final class Mp4VideoSoundEngineReloadCoordinator<T> {

    private final Object lock = new Object();
    private final Set<T> instances = Collections.newSetFromMap(new WeakHashMap<>());
    private volatile boolean soundEngineReloading = false;
    private volatile boolean soundEngineReloadCompleted = false;

    void register(@NotNull T instance) {
        synchronized (this.lock) {
            this.instances.add(instance);
        }
    }

    void beforeSoundEngineReload(@NotNull Consumer<T> releaseAction) {
        List<T> snapshot;
        synchronized (this.lock) {
            this.soundEngineReloading = true;
            snapshot = new ArrayList<>(this.instances);
        }
        snapshot.forEach(releaseAction);
    }

    void afterSoundEngineReload(@NotNull Consumer<T> retryAction) {
        List<T> snapshot;
        synchronized (this.lock) {
            this.soundEngineReloading = false;
            this.soundEngineReloadCompleted = true;
            snapshot = new ArrayList<>(this.instances);
        }
        snapshot.forEach(retryAction);
    }

    boolean isSoundEngineReloading() {
        return this.soundEngineReloading;
    }

    boolean hasSoundEngineReloadCompleted() {
        return this.soundEngineReloadCompleted;
    }
}
