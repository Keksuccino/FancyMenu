package de.keksuccino.fancymenu.util.resource.resources.video;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Tracks resources affected by Minecraft's replace-in-place sound-engine lifecycle.
 * State changes happen before callbacks so callbacks and concurrent registrations always observe the active phase.
 */
final class SoundEngineReloadLifecycle<T> {

    private final Object lock = new Object();
    private final Set<T> instances = Collections.newSetFromMap(new WeakHashMap<>());
    private volatile boolean reloading = false;
    private volatile boolean reloadCompleted = false;

    void register(@NotNull T instance) {
        synchronized (this.lock) {
            this.instances.add(instance);
        }
    }

    void beforeReload(@NotNull Consumer<? super T> callback, @NotNull BiConsumer<? super T, ? super Throwable> failureHandler) {
        List<T> snapshot;
        synchronized (this.lock) {
            this.reloading = true;
            snapshot = new ArrayList<>(this.instances);
        }
        invokeEach(snapshot, callback, failureHandler);
    }

    void afterReload(@NotNull Consumer<? super T> callback, @NotNull BiConsumer<? super T, ? super Throwable> failureHandler) {
        List<T> snapshot;
        synchronized (this.lock) {
            this.reloading = false;
            this.reloadCompleted = true;
            snapshot = new ArrayList<>(this.instances);
        }
        invokeEach(snapshot, callback, failureHandler);
    }

    boolean isReloading() {
        return this.reloading;
    }

    boolean hasReloadCompleted() {
        return this.reloadCompleted;
    }

    private static <T> void invokeEach(@NotNull List<T> instances, @NotNull Consumer<? super T> callback, @NotNull BiConsumer<? super T, ? super Throwable> failureHandler) {
        for (T instance : instances) {
            try {
                callback.accept(instance);
            } catch (Throwable throwable) {
                // Third-party player cleanup must never prevent Minecraft from replacing its own sound engine.
                failureHandler.accept(instance, throwable);
            }
        }
    }
}
