package de.keksuccino.fancymenu.util.resource.resources.video;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Coordinates WaterMedia players with Minecraft's OpenAL context lifecycle. Players must be released before
 * {@code SoundEngine.reload()} destroys the old context; releasing afterward can target the replacement context.
 */
public final class Mp4VideoSoundEngineReloadHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final WeakSoundEngineReloadCoordinator<Mp4Video> COORDINATOR = new WeakSoundEngineReloadCoordinator<>(Mp4Video::releasePlayerBeforeSoundEngineReload, Mp4Video::retryPlayerAfterSoundEngineReload, Mp4VideoSoundEngineReloadHandler::logFailure);

    private Mp4VideoSoundEngineReloadHandler() {
    }

    static void register(@NotNull Mp4Video video) {
        COORDINATOR.register(video);
    }

    static void unregister(@NotNull Mp4Video video) {
        COORDINATOR.unregister(video);
    }

    public static void beforeSoundEngineReload() {
        COORDINATOR.beforeReload();
    }

    public static void afterSoundEngineReload() {
        COORDINATOR.afterReload();
    }

    public static boolean isSoundEngineReloading() {
        return COORDINATOR.isReloading();
    }

    public static boolean hasSoundEngineReloadCompleted() {
        return COORDINATOR.hasReloadCompleted();
    }

    private static void logFailure(@NotNull Mp4Video video, @NotNull WeakSoundEngineReloadCoordinator.ReloadPhase phase, @NotNull Throwable throwable) {
        String action = phase == WeakSoundEngineReloadCoordinator.ReloadPhase.BEFORE ? "release" : "retry";
        LOGGER.error("[FANCYMENU] Failed to {} WaterMedia video player {} Minecraft sound-engine reload! source: {}", action, phase == WeakSoundEngineReloadCoordinator.ReloadPhase.BEFORE ? "before" : "after", video.resolveVideoSourceForListener(), throwable);
    }
}
