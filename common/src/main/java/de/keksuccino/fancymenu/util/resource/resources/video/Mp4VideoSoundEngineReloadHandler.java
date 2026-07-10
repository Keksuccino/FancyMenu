package de.keksuccino.fancymenu.util.resource.resources.video;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Coordinates Watermedia players with Minecraft's OpenAL context lifecycle.
 * Players must be released before {@code SoundEngine.reload()} destroys the old context; releasing them after the
 * reload can make Watermedia delete OpenAL objects through an unrelated replacement context.
 */
public final class Mp4VideoSoundEngineReloadHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final SoundEngineReloadLifecycle<Mp4Video> LIFECYCLE = new SoundEngineReloadLifecycle<>();

    private Mp4VideoSoundEngineReloadHandler() {
    }

    static void register(@NotNull Mp4Video video) {
        LIFECYCLE.register(video);
    }

    public static void beforeSoundEngineReload() {
        LIFECYCLE.beforeReload(Mp4Video::releasePlayerBeforeSoundEngineReload, (video, throwable) -> LOGGER.error("[FANCYMENU] Failed to release Watermedia video player before Minecraft sound-engine reload! source: {}", video.resolveVideoSourceForListener(), throwable));
    }

    public static void afterSoundEngineReload() {
        LIFECYCLE.afterReload(Mp4Video::retryPlayerAfterSoundEngineReload, (video, throwable) -> LOGGER.error("[FANCYMENU] Failed to retry Watermedia video player after Minecraft sound-engine reload! source: {}", video.resolveVideoSourceForListener(), throwable));
    }

    public static boolean isSoundEngineReloading() {
        return LIFECYCLE.isReloading();
    }

    public static boolean hasSoundEngineReloadCompleted() {
        return LIFECYCLE.hasReloadCompleted();
    }
}
