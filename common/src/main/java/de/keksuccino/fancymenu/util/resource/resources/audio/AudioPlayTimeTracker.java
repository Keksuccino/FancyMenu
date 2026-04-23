package de.keksuccino.fancymenu.util.resource.resources.audio;

public class AudioPlayTimeTracker {

    private long playStartTime = -1;
    private long totalPlayedTime = 0;
    private boolean isPaused = false;

    public void onPlay() {
        if (isPaused) {
            // Resume from pause
            isPaused = false;
            playStartTime = System.currentTimeMillis();
        } else {
            // Fresh start
            reset();
            playStartTime = System.currentTimeMillis();
        }
    }

    public void onPause() {
        if (!isPaused && playStartTime != -1) {
            totalPlayedTime += System.currentTimeMillis() - playStartTime;
            isPaused = true;
        }
    }

    public void onStop() {
        reset();
    }

    public void setPlayTime(float seconds, boolean paused) {
        if (!Float.isFinite(seconds) || seconds < 0.0F) {
            seconds = 0.0F;
        }
        this.totalPlayedTime = (long) (seconds * 1000.0F);
        this.playStartTime = System.currentTimeMillis();
        this.isPaused = paused;
    }

    public void reset() {
        playStartTime = -1;
        totalPlayedTime = 0;
        isPaused = false;
    }

    public float getCurrentPlayTime() {
        if (playStartTime == -1) return 0f;

        long currentTime = isPaused ? totalPlayedTime :
                totalPlayedTime + (System.currentTimeMillis() - playStartTime);

        return currentTime / 1000f; // Convert to seconds
    }

}
