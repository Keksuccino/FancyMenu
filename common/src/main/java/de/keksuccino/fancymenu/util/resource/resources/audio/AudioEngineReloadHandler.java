package de.keksuccino.fancymenu.util.resource.resources.audio;

import de.keksuccino.fancymenu.customization.element.elements.audio.AudioElementBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.video.nativevideo.NativeVideoMenuBackground;
import de.keksuccino.fancymenu.customization.global.GlobalCustomizationHandler;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.melody.resources.audio.MinecraftSoundSettingsObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class AudioEngineReloadHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final long POST_RELOAD_AUDIO_COOLDOWN_MS = 500L;
    private static boolean registered = false;
    private static volatile long cooldownUntilMs = 0L;

    private AudioEngineReloadHandler() {
    }

    public static boolean isInPostReloadCooldown() {
        return System.currentTimeMillis() < cooldownUntilMs;
    }

    public static synchronized void extendPostReloadCooldown(long durationMs) {
        if (durationMs <= 0L) return;
        long newUntil = System.currentTimeMillis() + durationMs;
        if (newUntil > cooldownUntilMs) {
            cooldownUntilMs = newUntil;
        }
    }

    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftSoundSettingsObserver.registerSoundEngineReloadListener(() -> {
            extendPostReloadCooldown(POST_RELOAD_AUDIO_COOLDOWN_MS);
            LOGGER.info("[FANCYMENU] Sound engine reload detected. Releasing cached audio resources.");
            ResourceHandlers.getAudioHandler().releaseAll();
            AudioElementBuilder.stopAllActiveAudios();
            GlobalCustomizationHandler.resetMenuMusicAfterSoundEngineReload();
            MainThreadTaskExecutor.executeInMainThread(
                    NativeVideoMenuBackground::forceReloadAllAfterSoundEngineReload_FancyMenu,
                    MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK
            );
        });
    }
}
