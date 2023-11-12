package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.resources.audio.ogg.OggAudio;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    OggAudio audio;

    @EventListener
    public void onInit(InitOrResizeScreenEvent.Pre e) {
        try {
            if (e.getScreen() instanceof OptionsScreen) {
                if ((audio != null) && !audio.isClipLoaded()) {
                    LOGGER.info("############ REBUILDING AUDIO BECAUSE OLD ONE IS NOT LOADED ANYMORE!");
                    try {
                        audio.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    audio = null;
                }
                if (audio == null) audio = OggAudio.local(new File(LayoutHandler.ASSETS_DIR, "test.ogg"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @EventListener
    public void onRenderPost(RenderScreenEvent.Post e) {
        try {
            if (e.getScreen() instanceof OptionsScreen) {
                if (audio.isReady() && !audio.isPlaying()) {
                    LOGGER.info("############# STARTING AUDIO");
                    audio.setVolume(1.0F);
                    audio.play();
                }
            }
            if (e.getScreen() instanceof SoundOptionsScreen) {
                if (audio != null) LOGGER.info("###### AUDIO VOLUME: " + audio.getVolume());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
