package de.keksuccino.fancymenu.util.resource.resources.video;

/**
 * Pure decisions for creating and restoring WaterMedia players around Minecraft's sound-engine lifecycle.
 */
final class Mp4VideoSoundLifecyclePolicy {

    enum PlayerCreationMode {
        NONE(false),
        VIDEO_ONLY(false),
        VIDEO_WITH_AUDIO(true);

        private final boolean audioEnabled;

        PlayerCreationMode(boolean audioEnabled) {
            this.audioEnabled = audioEnabled;
        }

        boolean isAudioEnabled() {
            return this.audioEnabled;
        }
    }

    private Mp4VideoSoundLifecyclePolicy() {
    }

    static PlayerCreationMode determinePlayerCreationMode(boolean closed, boolean playRequested, boolean playerPresent, boolean soundEngineReloading, boolean openAlReady, boolean soundEngineReloadCompleted) {
        if (closed || !playRequested || playerPresent || soundEngineReloading) return PlayerCreationMode.NONE;
        if (openAlReady) return PlayerCreationMode.VIDEO_WITH_AUDIO;
        if (soundEngineReloadCompleted) return PlayerCreationMode.VIDEO_ONLY;
        return PlayerCreationMode.NONE;
    }

    static boolean shouldRetryAfterReload(boolean closed, boolean playRequested, boolean playerPresent) {
        return !closed && playRequested && !playerPresent;
    }

    static long selectSeekForReload(boolean playRequested, long pendingSeekMs, long currentPlayerTimeMs) {
        if (!playRequested) return -1L;
        if (pendingSeekMs >= 0L) return pendingSeekMs;
        return currentPlayerTimeMs > 0L ? currentPlayerTimeMs : -1L;
    }
}
