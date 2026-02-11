package de.keksuccino.fancymenu.util.resource.resources.audio;

import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public final class AudioResourceReloadTracker {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Object AUDIO_INSTANCE_LOCK_FANCYMENU = new Object();
    private static final Set<IAudio> AUDIO_INSTANCES_FANCYMENU = Collections.newSetFromMap(new WeakHashMap<>());

    private AudioResourceReloadTracker() {
    }

    public static void registerAudioInstance_FancyMenu(@NotNull IAudio audio) {
        synchronized (AUDIO_INSTANCE_LOCK_FANCYMENU) {
            AUDIO_INSTANCES_FANCYMENU.add(audio);
        }
    }

    public static int forceReloadAllAfterSoundEngineReload_FancyMenu() {
        List<IAudio> audios;
        synchronized (AUDIO_INSTANCE_LOCK_FANCYMENU) {
            audios = new ArrayList<>(AUDIO_INSTANCES_FANCYMENU);
        }

        if (audios.isEmpty()) return 0;

        int releasedCount = 0;
        for (IAudio audio : audios) {
            if (audio == null) continue;
            try {
                ResourceHandlers.getAudioHandler().release(audio);
                releasedCount++;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to release cached audio resource after sound engine reload!", ex);
            }
        }

        LOGGER.info("[FANCYMENU] Forced audio resource reload after sound engine reload. audioResourcesReleased: {}", releasedCount);
        return releasedCount;
    }
}
