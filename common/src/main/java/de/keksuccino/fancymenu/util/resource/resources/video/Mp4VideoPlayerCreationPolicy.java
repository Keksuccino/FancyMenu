package de.keksuccino.fancymenu.util.resource.resources.video;

final class Mp4VideoPlayerCreationPolicy {

    enum Decision {
        WAIT,
        CREATE_WITH_AUDIO,
        CREATE_VIDEO_ONLY
    }

    private Mp4VideoPlayerCreationPolicy() {
    }

    static boolean shouldCheckOpenAl(boolean playRequested, boolean soundEngineReloading) {
        return playRequested && !soundEngineReloading;
    }

    static Decision decide(boolean playRequested, boolean soundEngineReloading, boolean soundEngineReloadCompleted, boolean openAlReady) {
        if (!shouldCheckOpenAl(playRequested, soundEngineReloading)) return Decision.WAIT;
        if (openAlReady) return Decision.CREATE_WITH_AUDIO;
        // SoundEngine catches OpenAL startup failures internally. Once reload() has returned, video playback can still
        // proceed without an audio engine instead of leaving every MP4 permanently waiting for unavailable audio.
        if (soundEngineReloadCompleted) return Decision.CREATE_VIDEO_ONLY;
        return Decision.WAIT;
    }
}
