package de.keksuccino.fancymenu.mixin.mixins.common.client;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Tracks a polled value only while its listener source is active.
 * Reactivation establishes a silent baseline so state changes that happened while dormant cannot produce synthetic transition events.
 */
final class DormantTransitionTracker<T> {

    private boolean initialized;
    private boolean dormant;
    @Nullable private T previousValue;
    @Nullable private T value;

    @NotNull
    Phase observe(boolean tracking, @Nullable T currentValue) {
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
    T previousValue() {
        return this.previousValue;
    }

    @Nullable
    T currentValue() {
        return this.value;
    }

    void resetForDormancy() {
        this.initialized = false;
        this.dormant = true;
        this.previousValue = null;
        this.value = null;
    }

    void resetBaseline() {
        this.initialized = false;
        this.dormant = false;
        this.previousValue = null;
        this.value = null;
    }

    enum Phase {
        DISABLED,
        INITIAL,
        REACTIVATED,
        UNCHANGED,
        CHANGED
    }

}
