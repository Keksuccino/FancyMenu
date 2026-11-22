package de.keksuccino.fancymenu.customization.placeholder;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Atomically claims bounded per-message log cooldowns without retaining every distinct parser failure forever.
 */
public final class LogCooldownTracker {

    private static final long ENTRY_OVERHEAD_WEIGHT = 64L;

    private final BoundedConcurrentCache<String, CooldownEntry> cooldowns;
    private final long cooldownMillis;

    /**
     * @param maximumWeight The maximum total length of all remembered keys, plus a small overhead per key. A key heavier than this never
     *                      gets remembered, so its message would get logged every time.
     */
    public LogCooldownTracker(long cooldownMillis, int maximumEntries, long maximumWeight) {
        if (cooldownMillis <= 0L) throw new IllegalArgumentException("cooldownMillis must be positive");
        this.cooldownMillis = cooldownMillis;
        this.cooldowns = new BoundedConcurrentCache<>(maximumEntries, maximumWeight, (error, ignored) -> ENTRY_OVERHEAD_WEIGHT + error.length());
    }

    /**
     * @return If the message with this key should get logged now, which claims its cooldown.
     */
    public boolean tryAcquire(@NotNull String error, long nowMillis) {
        Objects.requireNonNull(error);
        CooldownEntry candidate = new CooldownEntry(nowMillis);
        CooldownEntry result = this.cooldowns.compute(error, (ignored, current) -> ((current == null) || hasElapsed(current.claimedAtMillis(), nowMillis, this.cooldownMillis)) ? candidate : current);
        return result == candidate;
    }

    int size() {
        return this.cooldowns.size();
    }

    private static boolean hasElapsed(long previousMillis, long nowMillis, long cooldownMillis) {
        if (nowMillis < previousMillis) return true;
        long elapsed = nowMillis - previousMillis;
        return (elapsed < 0L) || (elapsed >= cooldownMillis);
    }

    private record CooldownEntry(long claimedAtMillis) {
    }

}
