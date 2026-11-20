package de.keksuccino.fancymenu.customization.decorationoverlay;

import de.keksuccino.fancymenu.util.rendering.RenderFrameCounter;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;
import java.util.function.LongSupplier;

/**
 * Keeps a decoration overlay to one simulation update per frame and one run per render pass per frame.<br>
 * Whichever pass runs first in a frame does the update, so it still happens exactly once when a screen never runs the background pass.
 * A pass that runs a second time in the same frame gets skipped (the loading overlay, for example, fires the screen's render
 * events twice per frame), so nothing draws twice.<br>
 * Frames come from {@link RenderFrameCounter}, which counts up once per frame between two screen renders.
 */
final class DecorationOverlayFrameTracker {

    /** Longer frames (lag, a paused or hidden overlay) advance the simulation by this much at most. */
    static final float MAX_DELTA_SECONDS = 0.1F;
    private static final long NEVER = Long.MIN_VALUE;

    @NotNull
    private final LongSupplier frameCounter;
    @NotNull
    private final LongSupplier nanoClock;
    private final long[] passFrames = new long[DecorationOverlayPass.values().length];
    private final long[] passCounts = new long[DecorationOverlayPass.values().length];
    private long updateFrame = NEVER;
    private long updateNanos = NEVER;
    private long updateCount = 0L;

    DecorationOverlayFrameTracker() {
        this(RenderFrameCounter::getFrame, System::nanoTime);
    }

    DecorationOverlayFrameTracker(@NotNull LongSupplier frameCounter, @NotNull LongSupplier nanoClock) {
        this.frameCounter = frameCounter;
        this.nanoClock = nanoClock;
        Arrays.fill(this.passFrames, NEVER);
    }

    /**
     * Starts a pass. Returns false if the pass already ran in this frame; it must not draw again then.
     */
    boolean beginPass(@NotNull DecorationOverlayPass pass) {
        long frame = this.frameCounter.getAsLong();
        int index = pass.ordinal();
        if (this.passFrames[index] == frame) return false;
        this.passFrames[index] = frame;
        this.passCounts[index]++;
        return true;
    }

    /**
     * Whether the simulation didn't advance in this frame yet.
     */
    boolean isUpdatePending() {
        return this.updateFrame != this.frameCounter.getAsLong();
    }

    /**
     * Marks this frame as updated and returns the seconds since the last update, from 0 to {@link #MAX_DELTA_SECONDS}.
     * The very first update gets 0.
     */
    float consumeUpdate() {
        this.updateFrame = this.frameCounter.getAsLong();
        long now = this.nanoClock.getAsLong();
        long last = this.updateNanos;
        this.updateNanos = now;
        this.updateCount++;
        if (last == NEVER) return 0.0F;
        float seconds = (now - last) / 1_000_000_000.0F;
        return (seconds < 0.0F) ? 0.0F : Math.min(seconds, MAX_DELTA_SECONDS);
    }

    long getUpdateCount() {
        return this.updateCount;
    }

    long getPassCount(@NotNull DecorationOverlayPass pass) {
        return this.passCounts[pass.ordinal()];
    }

}
