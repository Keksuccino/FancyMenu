package de.keksuccino.fancymenu.mixin.support.client;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Tracks a polled value only while its listener source is active.
 * Reactivation establishes a silent baseline so state changes that happened while dormant cannot produce synthetic transition events.
 */
public final class DormantTransitionTracker<T> {

    private boolean initialized;
    private boolean dormant;
    @Nullable private T previousValue;
    @Nullable private T value;

    public DormantTransitionTracker() {
    }

    @NotNull
    public Phase observe(boolean tracking, @Nullable T currentValue) {
        if (!tracking) {
            this.resetForDormancy();
            return Phase.DISABLED;
        }
        if (!this.initialized) {
            Phase phase = this.dormant ? Phase.REACTIVATED : Phase.INITIAL;
            this.initialized = true;
            this.dormant = false;
            this.previousValue = null;
            this.value = currentValue;
            return phase;
        }
        this.previousValue = this.value;
        this.value = currentValue;
        return Objects.equals(this.previousValue, currentValue) ? Phase.UNCHANGED : Phase.CHANGED;
    }

    @Nullable
    public T previousValue() {
        return this.previousValue;
    }

    @Nullable
    public T currentValue() {
        return this.value;
    }

    public void resetForDormancy() {
        this.initialized = false;
        this.dormant = true;
        this.previousValue = null;
        this.value = null;
    }

    public void resetBaseline() {
        this.initialized = false;
        this.dormant = false;
        this.previousValue = null;
        this.value = null;
    }

    public enum Phase {
        DISABLED,
        INITIAL,
        REACTIVATED,
        UNCHANGED,
        CHANGED
    }

}
