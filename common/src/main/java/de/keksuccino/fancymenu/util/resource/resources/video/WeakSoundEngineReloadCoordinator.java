package de.keksuccino.fancymenu.util.resource.resources.video;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Dispatches sound-engine lifecycle callbacks without retaining the registered resources. Callback snapshots are
 * built under the coordinator lock and executed outside it so registrations and callback reentrancy cannot deadlock.
 */
final class WeakSoundEngineReloadCoordinator<T> {

    enum ReloadPhase {
        BEFORE,
        AFTER
    }

    @FunctionalInterface
    interface FailureHandler<T> {
        void onFailure(@NotNull T participant, @NotNull ReloadPhase phase, @NotNull Throwable throwable);
    }

    private final Object lock = new Object();
    private final List<WeakReference<T>> participantReferences = new ArrayList<>();
    private final Consumer<T> beforeReloadAction;
    private final Consumer<T> afterReloadAction;
    private final FailureHandler<T> failureHandler;
    private final Function<T, WeakReference<T>> referenceFactory;
    private volatile boolean reloading = false;
    private volatile boolean reloadCompleted = false;
    private boolean beforeDispatching = false;
    private boolean afterDispatching = false;
    private boolean afterPending = false;

    WeakSoundEngineReloadCoordinator(@NotNull Consumer<T> beforeReloadAction, @NotNull Consumer<T> afterReloadAction, @NotNull FailureHandler<T> failureHandler) {
        this(beforeReloadAction, afterReloadAction, failureHandler, WeakReference::new);
    }

    WeakSoundEngineReloadCoordinator(@NotNull Consumer<T> beforeReloadAction, @NotNull Consumer<T> afterReloadAction, @NotNull FailureHandler<T> failureHandler, @NotNull Function<T, WeakReference<T>> referenceFactory) {
        this.beforeReloadAction = Objects.requireNonNull(beforeReloadAction);
        this.afterReloadAction = Objects.requireNonNull(afterReloadAction);
        this.failureHandler = Objects.requireNonNull(failureHandler);
        this.referenceFactory = Objects.requireNonNull(referenceFactory);
    }

    void register(@NotNull T participant) {
        Objects.requireNonNull(participant);
        synchronized (this.lock) {
            Iterator<WeakReference<T>> iterator = this.participantReferences.iterator();
            while (iterator.hasNext()) {
                T registeredParticipant = iterator.next().get();
                if (registeredParticipant == null) iterator.remove();
                else if (registeredParticipant == participant) return;
            }
            this.participantReferences.add(Objects.requireNonNull(this.referenceFactory.apply(participant)));
        }
    }

    void unregister(@NotNull T participant) {
        Objects.requireNonNull(participant);
        synchronized (this.lock) {
            this.participantReferences.removeIf(reference -> {
                T registeredParticipant = reference.get();
                return registeredParticipant == null || registeredParticipant == participant;
            });
        }
    }

    void beforeReload() {
        List<T> participants;
        synchronized (this.lock) {
            // Overlapping before calls describe the same Vanilla reload and must not release a player twice.
            if (this.reloading || this.beforeDispatching || this.afterDispatching) return;
            this.reloading = true;
            this.beforeDispatching = true;
            participants = this.snapshotParticipants_FancyMenu();
        }

        this.dispatch_FancyMenu(participants, this.beforeReloadAction, ReloadPhase.BEFORE);

        boolean runPendingAfter;
        synchronized (this.lock) {
            this.beforeDispatching = false;
            runPendingAfter = this.afterPending;
            this.afterPending = false;
        }
        // A reentrant or concurrent RETURN callback cannot overtake the remaining HEAD cleanup callbacks.
        if (runPendingAfter) this.afterReload();
    }

    void afterReload() {
        List<T> participants;
        synchronized (this.lock) {
            if (this.beforeDispatching) {
                this.afterPending = true;
                return;
            }
            if (!this.reloading || this.afterDispatching) return;
            this.reloading = false;
            this.reloadCompleted = true;
            this.afterDispatching = true;
            participants = this.snapshotParticipants_FancyMenu();
        }

        this.dispatch_FancyMenu(participants, this.afterReloadAction, ReloadPhase.AFTER);

        synchronized (this.lock) {
            this.afterDispatching = false;
        }
    }

    boolean isReloading() {
        return this.reloading;
    }

    boolean hasReloadCompleted() {
        return this.reloadCompleted;
    }

    @NotNull
    private List<T> snapshotParticipants_FancyMenu() {
        List<T> participants = new ArrayList<>(this.participantReferences.size());
        Iterator<WeakReference<T>> iterator = this.participantReferences.iterator();
        while (iterator.hasNext()) {
            T participant = iterator.next().get();
            if (participant == null) iterator.remove();
            else participants.add(participant);
        }
        return participants;
    }

    private void dispatch_FancyMenu(@NotNull List<T> participants, @NotNull Consumer<T> action, @NotNull ReloadPhase phase) {
        for (T participant : participants) {
            try {
                action.accept(participant);
            } catch (Throwable throwable) {
                try {
                    this.failureHandler.onFailure(participant, phase, throwable);
                } catch (Throwable ignored) {
                    // Failure reporting is isolated too; one broken logger must not skip another player's cleanup.
                }
            }
        }
    }
}
