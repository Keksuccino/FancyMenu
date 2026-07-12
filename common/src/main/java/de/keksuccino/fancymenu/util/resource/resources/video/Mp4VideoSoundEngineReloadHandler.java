package de.keksuccino.fancymenu.util.resource.resources.video;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Coordinates Watermedia players with Minecraft's OpenAL context lifecycle. Players must be released before
 * {@code SoundEngine.reload()} destroys the old context; releasing them afterward would delete OpenAL objects through
 * the unrelated replacement context.
 */
public final class Mp4VideoSoundEngineReloadHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Mp4VideoSoundEngineReloadCoordinator<Mp4Video> COORDINATOR = new Mp4VideoSoundEngineReloadCoordinator<>();

    private Mp4VideoSoundEngineReloadHandler() {
    }

    static void register(@NotNull Mp4Video video) {
        COORDINATOR.register(video);
    }

    public static void beforeSoundEngineReload() {
        COORDINATOR.beforeSoundEngineReload(video -> {
            try {
                video.releasePlayerBeforeSoundEngineReload();
            } catch (Throwable throwable) {
                // A third-party player cleanup failure must not abort Minecraft's own sound-engine reload.
                LOGGER.error("[FANCYMENU] Failed to release Watermedia video player before Minecraft sound-engine reload! source: {}", video.sourceName, throwable);
            }
        });
    }

    public static void afterSoundEngineReload() {
        COORDINATOR.afterSoundEngineReload(video -> {
            try {
                video.retryPlayerAfterSoundEngineReload();
            } catch (Throwable throwable) {
                LOGGER.error("[FANCYMENU] Failed to retry Watermedia video player after Minecraft sound-engine reload! source: {}", video.sourceName, throwable);
            }
        });
    }

    public static boolean isSoundEngineReloading() {
        return COORDINATOR.isSoundEngineReloading();
    }

    public static boolean hasSoundEngineReloadCompleted() {
        return COORDINATOR.hasSoundEngineReloadCompleted();
    }
}
