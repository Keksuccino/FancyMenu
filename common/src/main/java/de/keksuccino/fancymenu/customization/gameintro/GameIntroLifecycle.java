package de.keksuccino.fancymenu.customization.gameintro;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class GameIntroLifecycle {

    private boolean closed;
    private boolean consumed;

    boolean complete() {
        return this.close(true);
    }

    boolean replace() {
        return this.close(false);
    }

    boolean isClosed() {
        return this.closed;
    }

    boolean isConsumed() {
        return this.consumed;
    }

    private boolean close(boolean consumed) {
        if (this.closed) return false;
        this.closed = true;
        this.consumed = consumed;
        return true;
    }

    public static boolean shouldLoadIntro(boolean introPlayed) {
        return !introPlayed;
    }

    static void stopSafely(@NotNull Runnable stopAction, @NotNull Consumer<Throwable> errorHandler) {
        try {
            stopAction.run();
        } catch (Throwable throwable) {
            errorHandler.accept(throwable);
        }
    }

}
