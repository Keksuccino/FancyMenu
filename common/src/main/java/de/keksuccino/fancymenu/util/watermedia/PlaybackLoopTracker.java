package de.keksuccino.fancymenu.util.watermedia;

/**
 * Counts loop restarts of a repeating media player by noticing its play time jumping back to the start.<br>
 * The play time needs to get sampled at least once per loop, otherwise restarts can get missed.
 */
public final class PlaybackLoopTracker {

    /** Play time can jitter a bit backwards without an actual restart, so only bigger jumps count as restarts. **/
    static final long MAX_RESTART_TOLERANCE_MS = 100L;

    private volatile long lastTimeMs = 0L;
    private volatile int completedLoops = 0;

    /**
     * Samples the current play time and returns if the player restarted its loop since the last sample.
     *
     * @param durationMs the duration of one loop or a value of 0 or lower if it is not known yet
     */
    public boolean update(long timeMs, long durationMs) {
        long currentTimeMs = Math.max(0L, timeMs);
        long previousTimeMs = this.lastTimeMs;
        this.lastTimeMs = currentTimeMs;
        // Short loops get a smaller tolerance, so their restarts don't get mistaken for jitter
        long toleranceMs = (durationMs > 0L) ? Math.min(MAX_RESTART_TOLERANCE_MS, durationMs / 2L) : MAX_RESTART_TOLERANCE_MS;
        if (previousTimeMs <= (currentTimeMs + toleranceMs)) return false;
        this.completedLoops++;
        return true;
    }

    public int getCompletedLoops() {
        return this.completedLoops;
    }

    public void reset() {
        this.lastTimeMs = 0L;
        this.completedLoops = 0;
    }

}
