package de.keksuccino.fancymenu.customization.gameintro;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class GameIntroAttemptController {

    private final AtomicReference<State> state = new AtomicReference<>(State.ACTIVE);

    public static <T> @Nullable T resolveRetryableIntro(boolean consumed, Supplier<? extends T> resolver) {
        Objects.requireNonNull(resolver);
        return consumed ? null : resolver.get();
    }

    boolean completeAttempt(Runnable stopAction, Runnable initializeTargetAction, Runnable consumeAction) {
        Objects.requireNonNull(stopAction);
        Objects.requireNonNull(initializeTargetAction);
        Objects.requireNonNull(consumeAction);
        if (!this.state.compareAndSet(State.ACTIVE, State.COMPLETING)) return false;
        try {
            stopAction.run();
            if (this.state.get() != State.COMPLETING) return false;
            initializeTargetAction.run();
            if (!this.state.compareAndSet(State.COMPLETING, State.COMPLETED)) return false;
            consumeAction.run();
            return true;
        } finally {
            this.state.compareAndSet(State.COMPLETING, State.ACTIVE);
        }
    }

    boolean replaceAttempt(Runnable stopAction) {
        Objects.requireNonNull(stopAction);
        State current;
        do {
            current = this.state.get();
            if (current == State.COMPLETED || current == State.REPLACED) return false;
        } while (!this.state.compareAndSet(current, State.REPLACED));
        stopAction.run();
        return true;
    }

    boolean isActive() {
        return this.state.get() == State.ACTIVE;
    }

    boolean isCompleted() {
        return this.state.get() == State.COMPLETED;
    }

    boolean isReplaced() {
        return this.state.get() == State.REPLACED;
    }

    private enum State {
        ACTIVE,
        COMPLETING,
        COMPLETED,
        REPLACED
    }

}
