package de.keksuccino.fancymenu.customization.gameintro;

import java.util.function.Consumer;

/**
 * Owns the terminal state and media cleanup of one game intro overlay.
 *
 * <p>The terminal reason must be recorded before invoking media cleanup because native or third-party playback
 * implementations can fail or re-enter overlay code while stopping.</p>
 */
final class GameIntroLifecycle {

    private final Runnable stopAction;
    private final Consumer<Throwable> cleanupErrorHandler;
    private State state = State.OPEN;

    GameIntroLifecycle(Runnable stopAction, Consumer<Throwable> cleanupErrorHandler) {
        this.stopAction = stopAction;
        this.cleanupErrorHandler = cleanupErrorHandler;
    }

    boolean markFinished() {
        if (this.state != State.OPEN) return false;
        this.state = State.FINISHED;
        return true;
    }

    boolean closeFinished() {
        if (this.state != State.FINISHED) return false;
        this.state = State.CLOSED_FINISHED;
        this.stopSafely();
        return true;
    }

    boolean replace() {
        if (this.state == State.FINISHED) return this.closeFinished();
        if (this.state != State.OPEN) return false;
        this.state = State.REPLACED;
        this.stopSafely();
        return true;
    }

    boolean isClosed() {
        return this.state == State.CLOSED_FINISHED || this.state == State.REPLACED;
    }

    boolean isConsumed() {
        return this.state == State.FINISHED || this.state == State.CLOSED_FINISHED;
    }

    private void stopSafely() {
        try {
            this.stopAction.run();
        } catch (Throwable throwable) {
            this.cleanupErrorHandler.accept(throwable);
        }
    }

    private enum State {
        OPEN,
        FINISHED,
        CLOSED_FINISHED,
        REPLACED
    }

}
