package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.resources.ResourceSupplier;
import de.keksuccino.fancymenu.util.resources.audio.IAudio;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    //private final ResourceSupplier<IAudio> audioSupplier = ResourceSupplier.audio(new File(LayoutHandler.ASSETS_DIR, "test.ogg").getPath());
    private final ResourceSupplier<IAudio> audioSupplier = ResourceSupplier.audio(new File(LayoutHandler.ASSETS_DIR, "sound.wav").getPath());

    @EventListener
    public void onRenderPost(RenderScreenEvent.Post e) {
        try {
            if (e.getScreen() instanceof OptionsScreen) {
                IAudio audio = audioSupplier.get();
                if ((audio != null) && audio.isReady() && !audio.isPlaying()) {
                    LOGGER.info("############# STARTING AUDIO");
                    audio.setSoundChannel(SoundSource.MUSIC);
                    audio.play();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
