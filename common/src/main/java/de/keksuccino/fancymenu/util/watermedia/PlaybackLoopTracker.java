package de.keksuccino.fancymenu.util.watermedia;

/**
 * Counts loop restarts of a repeating media player by noticing its play time jumping back to the start.<br>
 * The play time needs to get sampled at least once per loop, otherwise restarts can get missed.
 */
public final class PlaybackLoopTracker {

    /** Play time can jitter a bit backwards without an actual restart, so only bigger jumps count as restarts. **/
    static final long MAX_RESTART_TOLERANCE_MS = 100L;
    /** How much of the duration a pass can miss and still count as played through, which covers the time between two samples. **/
    static final long MAX_UNPLAYED_MS = 1000L;

    private final boolean requirePlayedThrough;
    private volatile long lastTimeMs = -1L;
    private volatile long playedMs = 0L;
    private volatile int completedLoops = 0;

    /**
     * @param requirePlayedThrough if jumps back only count as restart after the current pass got played through, which ignores jumps caused by seeking, or by play times that are still stale right after seeking
     */
    public PlaybackLoopTracker(boolean requirePlayedThrough) {
        this.requirePlayedThrough = requirePlayedThrough;
    }

    /**
     * Samples the current play time and returns if the player restarted its loop since the last sample.<br>
     * The first sample after creating the tracker or restarting its pass only sets the starting point.
     *
     * @param durationMs the duration of one loop or a value of 0 or lower if it is not known yet
     */
    public boolean update(long timeMs, long durationMs) {
        long currentTimeMs = Math.max(0L, timeMs);
        long previousTimeMs = this.lastTimeMs;
        this.lastTimeMs = currentTimeMs;
        if (previousTimeMs < 0L) return false;
        // Short loops get a smaller tolerance, so their restarts don't get mistaken for jitter
        long toleranceMs = (durationMs > 0L) ? Math.min(MAX_RESTART_TOLERANCE_MS, durationMs / 2L) : MAX_RESTART_TOLERANCE_MS;
        if (previousTimeMs <= (currentTimeMs + toleranceMs)) {
            this.playedMs += Math.max(0L, currentTimeMs - previousTimeMs);
            return false;
        }
        boolean restarted = !this.requirePlayedThrough || this.isPlayedThrough(durationMs);
        this.playedMs = 0L;
        if (restarted) this.completedLoops++;
        return restarted;
    }

    /**
     * If the current pass got played through from its starting point to (nearly) the end of the given duration.
     */
    public boolean isPlayedThrough(long durationMs) {
        if (durationMs <= 0L) return false;
        return this.playedMs >= (durationMs - Math.min(MAX_UNPLAYED_MS, durationMs / 2L));
    }

    public int getCompletedLoops() {
        return this.completedLoops;
    }

    /**
     * Starts a new pass, like after seeking, keeping the completed loops. The next sample only sets the starting point.
     */
    public void restartPass() {
        this.lastTimeMs = -1L;
        this.playedMs = 0L;
    }

    public void reset() {
        this.restartPass();
        this.completedLoops = 0;
    }

}
