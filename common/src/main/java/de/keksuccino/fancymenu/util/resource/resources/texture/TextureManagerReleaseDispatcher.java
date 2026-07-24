package de.keksuccino.fancymenu.util.resource.resources.texture;

import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

/**
 * Owns deferred texture-manager mutations until the client thread executes them. Pending releases are retained separately
 * from the tick queue so shutdown can flush them before vanilla destroys the texture manager, even when a normal queued
 * task would no longer receive another client tick.
 */
public final class TextureManagerReleaseDispatcher {

    private static final Dispatcher DISPATCHER = new Dispatcher(() -> Minecraft.getInstance().isSameThread(), task -> MainThreadTaskExecutor.executeInMainThread(task, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK));

    private TextureManagerReleaseDispatcher() {}

    static void dispatch(@NotNull Runnable releaseTask) {
        DISPATCHER.dispatch(releaseTask);
    }

    /**
     * Must be called on the client thread before {@link MainThreadTaskExecutor#shutdown()}. Resource shutdown happens
     * immediately before this call, so no registered FancyMenu texture entry can be created after the pending set drains.
     */
    public static void flushPendingReleases() {
        DISPATCHER.flush();
    }

    static final class Dispatcher {

        private final BooleanSupplier mainThreadChecker;
        private final Consumer<Runnable> taskScheduler;
        private final Set<PendingRelease> pendingReleases = ConcurrentHashMap.newKeySet();
        private final AtomicBoolean drainScheduled = new AtomicBoolean();

        Dispatcher(@NotNull BooleanSupplier mainThreadChecker, @NotNull Consumer<Runnable> taskScheduler) {
            this.mainThreadChecker = mainThreadChecker;
            this.taskScheduler = taskScheduler;
        }

        void dispatch(@NotNull Runnable releaseTask) {
            PendingRelease pendingRelease = new PendingRelease(releaseTask, this.pendingReleases);
            this.pendingReleases.add(pendingRelease);
            if (this.mainThreadChecker.getAsBoolean()) {
                pendingRelease.run();
            } else {
                this.scheduleDrainIfNeeded();
            }
        }

        void flush() {
            while (true) {
                this.drainPendingReleases();
                this.drainScheduled.set(false);
                if (this.pendingReleases.isEmpty()) return;
            }
        }

        int pendingReleaseCount() {
            return this.pendingReleases.size();
        }

        private void scheduleDrainIfNeeded() {
            if (!this.drainScheduled.compareAndSet(false, true)) return;
            try {
                this.taskScheduler.accept(this::runScheduledDrain);
            } catch (RuntimeException | Error throwable) {
                this.drainScheduled.set(false);
                throw throwable;
            }
        }

        private void runScheduledDrain() {
            try {
                while (!this.pendingReleases.isEmpty()) this.drainPendingReleases();
            } finally {
                // Reset after draining, then recheck: a producer that observed the old scheduled state must not strand its release.
                this.drainScheduled.set(false);
                if (!this.pendingReleases.isEmpty()) this.scheduleDrainIfNeeded();
            }
        }

        private void drainPendingReleases() {
            for (PendingRelease pendingRelease : new ArrayList<>(this.pendingReleases)) pendingRelease.run();
        }

    }

    private static final class PendingRelease implements Runnable {

        private final Runnable releaseTask;
        private final Set<PendingRelease> pendingReleases;
        private final AtomicBoolean executed = new AtomicBoolean();

        private PendingRelease(@NotNull Runnable releaseTask, @NotNull Set<PendingRelease> pendingReleases) {
            this.releaseTask = releaseTask;
            this.pendingReleases = pendingReleases;
        }

        @Override
        public void run() {
            if (!this.executed.compareAndSet(false, true)) return;
            this.pendingReleases.remove(this);
            this.releaseTask.run();
        }

    }

}
