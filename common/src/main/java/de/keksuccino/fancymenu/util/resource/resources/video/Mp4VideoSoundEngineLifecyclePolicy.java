package de.keksuccino.fancymenu.util.resource.resources.video;

/**
 * Pure lifecycle decisions shared by MP4 startup and sound-engine reload recovery.
 */
final class Mp4VideoSoundEngineLifecyclePolicy {

    private Mp4VideoSoundEngineLifecyclePolicy() {
    }

    static boolean shouldCreatePlayer(boolean playRequested, boolean soundEngineReloading, boolean openAlReady, boolean soundEngineReloadCompleted) {
        return playRequested && !soundEngineReloading && (openAlReady || soundEngineReloadCompleted);
    }

    static boolean shouldCaptureResumePosition(boolean playRequested, long pendingSeekMs) {
        return playRequested && (pendingSeekMs < 0L);
    }

    static long resolveResumePosition(boolean playRequested, long pendingSeekMs, long reportedPlayTimeMs) {
        if (!shouldCaptureResumePosition(playRequested, pendingSeekMs)) return pendingSeekMs;
        return (reportedPlayTimeMs > 0L) ? reportedPlayTimeMs : pendingSeekMs;
    }

    static boolean shouldRetryPlayer(boolean closed, boolean playRequested, boolean hasPlayer) {
        return !closed && playRequested && !hasPlayer;
    }
}
