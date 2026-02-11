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
    private static boolean registered = false;

    private AudioEngineReloadHandler() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftSoundSettingsObserver.registerSoundEngineReloadListener(() -> {
            MainThreadTaskExecutor.executeInMainThread(
                    () -> {
                        LOGGER.info("[FANCYMENU] Sound engine reload detected. Releasing cached audio resources.");
                        AudioElementBuilder.stopAllActiveAudios();
                        GlobalCustomizationHandler.resetMenuMusicAfterSoundEngineReload();
                        AudioResourceReloadTracker.forceReloadAllAfterSoundEngineReload_FancyMenu();
                        ResourceHandlers.getAudioHandler().releaseAll();
                        NativeVideoMenuBackground.forceReloadAllAfterSoundEngineReload_FancyMenu();
                    },
                    MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK
            );
        });
    }
}
