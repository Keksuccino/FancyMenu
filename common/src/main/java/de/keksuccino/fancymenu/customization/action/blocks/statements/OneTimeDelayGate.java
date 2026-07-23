package de.keksuccino.fancymenu.customization.action.blocks.statements;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Runtime state for Delay blocks. This is deliberately a polling gate, not a scheduler: the first positive-delay
 * reach records a deadline, and only a later reach can open the gate. Once open, the gate stays open for its lifetime.
 */
final class OneTimeDelayGate {

    @NotNull
    private final LongSupplier currentTimeMillisSupplier;

    private boolean timerStarted;
    private boolean open;
    private long deadlineMillis;

    OneTimeDelayGate(@NotNull LongSupplier currentTimeMillisSupplier) {
        this.currentTimeMillisSupplier = Objects.requireNonNull(currentTimeMillisSupplier);
    }

    /**
     * Only the gate-state transition is serialized. The caller executes contained actions after this method releases
     * the monitor, so user-configured code never runs under this lock and every concurrent reach of an open gate runs.
     */
    synchronized boolean shouldExecute(long delayMillis) {
        if (delayMillis <= 0L) {
            this.open = true;
            return true;
        }
        if (this.open) {
            return true;
        }
        long now = this.currentTimeMillisSupplier.getAsLong();
        if (!this.timerStarted) {
            this.timerStarted = true;
            this.deadlineMillis = calculateDeadline(now, delayMillis);
            return false;
        }
        if (now < this.deadlineMillis) {
            return false;
        }
        this.open = true;
        return true;
    }

    private static long calculateDeadline(long nowMillis, long delayMillis) {
        try {
            return Math.addExact(nowMillis, delayMillis);
        } catch (ArithmeticException ignored) {
            // A wrapped negative deadline would make every later reach restart the timer. Saturation preserves the
            // intended inclusive boundary and keeps even Long.MAX_VALUE delays capable of opening eventually.
            return Long.MAX_VALUE;
        }
    }

}
