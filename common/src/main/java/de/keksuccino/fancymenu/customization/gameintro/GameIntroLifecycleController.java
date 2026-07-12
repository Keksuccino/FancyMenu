package de.keksuccino.fancymenu.customization.gameintro;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Owns the terminal state and ordering of a single game intro overlay without depending on Minecraft runtime objects.
 */
final class GameIntroLifecycleController {

    private final Runnable stopAction;
    private final Runnable initializeTargetAction;
    private final Runnable markPlayedAction;
    private final Runnable clearOverlayAction;
    private final Consumer<Throwable> stopFailureHandler;
    private boolean closed;
    private boolean stopAttempted;

    GameIntroLifecycleController(@NotNull Runnable stopAction, @NotNull Runnable initializeTargetAction, @NotNull Runnable markPlayedAction, @NotNull Runnable clearOverlayAction, @NotNull Consumer<Throwable> stopFailureHandler) {
        this.stopAction = Objects.requireNonNull(stopAction);
        this.initializeTargetAction = Objects.requireNonNull(initializeTargetAction);
        this.markPlayedAction = Objects.requireNonNull(markPlayedAction);
        this.clearOverlayAction = Objects.requireNonNull(clearOverlayAction);
        this.stopFailureHandler = Objects.requireNonNull(stopFailureHandler);
    }

    void complete() {
        if (this.closed) return;
        this.stopSafely();
        if (this.closed) return;
        // Initialization must succeed before the intro is consumed. Otherwise the same overlay remains retryable.
        this.initializeTargetAction.run();
        if (this.closed) return;
        this.markPlayedAction.run();
        this.closed = true;
        this.clearOverlayAction.run();
    }

    void replaceIfDisplaced(@NotNull Object currentOverlay, @Nullable Object replacementOverlay) {
        if (currentOverlay == replacementOverlay || this.closed) return;
        // Close before native or third-party cleanup so reentrant replacement cannot stop the same resource twice.
        this.closed = true;
        this.stopSafely();
    }

    boolean isClosed() {
        return this.closed;
    }

    private void stopSafely() {
        if (this.stopAttempted) return;
        // Native and third-party resources are not guaranteed to tolerate multiple terminal cleanup calls.
        this.stopAttempted = true;
        try {
            this.stopAction.run();
        } catch (Throwable throwable) {
            try {
                this.stopFailureHandler.accept(throwable);
            } catch (Throwable ignored) {
                // Cleanup and its reporting are both best-effort; neither may strand an overlay transition.
            }
        }
    }

}
