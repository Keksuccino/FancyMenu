package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.resources.audio.OggAudio;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    OggAudio audio;
    boolean started = false;

    @EventListener
    public void onInit(InitOrResizeScreenEvent.Pre e) {
        try {
            if (e.getScreen() instanceof OptionsScreen) {
                if (audio == null) audio = OggAudio.of(FileUtils.openInputStream(new File(LayoutHandler.ASSETS_DIR, "test.ogg")));
                if (audio.isReady()) {
                    LOGGER.info("############# STARTING AUDIO");
                    audio.setVolume(1.0F);
                    audio.stop();
                    audio.play();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
