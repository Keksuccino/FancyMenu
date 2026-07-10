package de.keksuccino.fancymenu.util.resource.resources.video;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Coordinates Watermedia players with Minecraft's OpenAL context lifecycle.
 * Players must be released before {@code SoundEngine.reload()} destroys the old context; releasing them after the
 * reload can make Watermedia delete OpenAL objects through an unrelated replacement context.
 */
public final class Mp4VideoSoundEngineReloadHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Object LOCK = new Object();
    private static final Set<Mp4Video> INSTANCES = Collections.newSetFromMap(new WeakHashMap<>());
    private static volatile boolean soundEngineReloading = false;
    private static volatile boolean soundEngineReloadCompleted = false;

    private Mp4VideoSoundEngineReloadHandler() {
    }

    static void register(@NotNull Mp4Video video) {
        synchronized (LOCK) {
            INSTANCES.add(video);
        }
    }

    public static void beforeSoundEngineReload() {
        List<Mp4Video> videos;
        synchronized (LOCK) {
            soundEngineReloading = true;
            videos = new ArrayList<>(INSTANCES);
        }
        for (Mp4Video video : videos) {
            try {
                video.releasePlayerBeforeSoundEngineReload();
            } catch (Throwable throwable) {
                // A third-party player cleanup failure must not abort Minecraft's own sound-engine reload.
                LOGGER.error("[FANCYMENU] Failed to release Watermedia video player before Minecraft sound-engine reload! source: {}", video.sourceName, throwable);
            }
        }
    }

    public static void afterSoundEngineReload() {
        List<Mp4Video> videos;
        synchronized (LOCK) {
            soundEngineReloading = false;
            soundEngineReloadCompleted = true;
            videos = new ArrayList<>(INSTANCES);
        }
        for (Mp4Video video : videos) {
            try {
                video.retryPlayerAfterSoundEngineReload();
            } catch (Throwable throwable) {
                LOGGER.error("[FANCYMENU] Failed to retry Watermedia video player after Minecraft sound-engine reload! source: {}", video.sourceName, throwable);
            }
        }
    }

    public static boolean isSoundEngineReloading() {
        return soundEngineReloading;
    }

    public static boolean hasSoundEngineReloadCompleted() {
        return soundEngineReloadCompleted;
    }
}
