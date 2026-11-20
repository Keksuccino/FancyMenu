package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * A small seeded random number generator (mulberry32, the mockup's {@code mulberry}) for pixel-art overlays.<br>
 * The same seed always gives the same numbers, which keeps procedurally built art (clouds, vines, frost) the same every time it gets built.
 * It also has the mockup's shortcuts: {@link #range(float, float)} is {@code rnd}, {@link #rangeInt(int, int)} is {@code rint}
 * and {@link #pick(List)} is {@code pick}.<br>
 * Not thread-safe. Every overlay uses its own instances on the render thread.
 */
public final class PixelRandom {

    @Nullable
    private static Integer seedOverride = null;
    private static int overrideCounter = 0;

    private int state;

    public PixelRandom(int seed) {
        this.state = seed;
    }

    /**
     * Creates a generator with a random seed. While the overlay preview of test clients sets a seed with {@link #setSeedOverride(Integer)},
     * generators get seeds derived from it instead, so test runs build the same overlays every time.
     */
    @NotNull
    public static PixelRandom create() {
        Integer override = seedOverride;
        if (override != null) return new PixelRandom(override + 0x9E3779B9 * (overrideCounter++));
        long nanos = System.nanoTime();
        return new PixelRandom((int)(nanos ^ (nanos >>> 32)) ^ System.identityHashCode(new Object()));
    }

    /**
     * Only for the dev-only overlay preview of off-screen test clients.
     */
    @ApiStatus.Internal
    public static void setSeedOverride(@Nullable Integer seed) {
        seedOverride = seed;
        overrideCounter = 0;
    }

    public void setSeed(int seed) {
        this.state = seed;
    }

    /**
     * Returns the next raw 32-bit value of the sequence.
     */
    public int nextInt() {
        this.state = this.state + 0x6D2B79F5;
        int t = this.state;
        t = (t ^ (t >>> 15)) * (1 | t);
        t = (t + (t ^ (t >>> 7)) * (61 | t)) ^ t;
        return t ^ (t >>> 14);
    }

    /**
     * Returns a value from 0 (inclusive) to 1 (exclusive).
     */
    public float next() {
        // The top 24 bits fit a float exactly, so the result never rounds up to 1.0
        return (this.nextInt() >>> 8) / 16777216.0F;
    }

    /**
     * Returns a value from {@code min} (inclusive) to {@code max} (exclusive), the mockup's {@code rnd(min, max)}.
     */
    public float range(float min, float max) {
        return min + this.next() * (max - min);
    }

    /**
     * Returns a whole number from {@code min} to {@code max}, both inclusive, the mockup's {@code rint(min, max)}.
     */
    public int rangeInt(int min, int max) {
        if (max <= min) return min;
        return min + (int)(this.next() * (max - min + 1));
    }

    /**
     * Returns true with the given probability (0 never, 1 always).
     */
    public boolean chance(float probability) {
        return this.next() < probability;
    }

    /**
     * Returns -1 or 1 with the same probability.
     */
    public int sign() {
        return (this.next() < 0.5F) ? -1 : 1;
    }

    /**
     * Returns a random index for a list or array of the given size. The size must be at least 1.
     */
    public int index(int size) {
        return (int)(this.next() * size);
    }

    @NotNull
    public <T> T pick(@NotNull List<T> values) {
        return values.get(this.index(values.size()));
    }

    @NotNull
    public <T> T pick(@NotNull T[] values) {
        return values[this.index(values.length)];
    }

}
