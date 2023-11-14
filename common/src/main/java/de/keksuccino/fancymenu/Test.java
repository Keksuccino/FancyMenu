package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.resources.ResourceSupplier;
import de.keksuccino.fancymenu.util.resources.audio.IAudio;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    //private final ResourceSupplier<IAudio> audioSupplier = ResourceSupplier.audio(new File(LayoutHandler.ASSETS_DIR, "test.ogg").getPath());
    //private final ResourceSupplier<IAudio> audioSupplier = ResourceSupplier.audio("https://www2.cs.uic.edu/~i101/SoundFiles/ImperialMarch60.wav");
    private final ResourceSupplier<IAudio> audioSupplier = ResourceSupplier.audio("https://samples-files.com/samples/Audio/ogg/sample-file-4.ogg");

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

//                if ((wavTest != null) && !wavTest.isClipLoaded()) wavTest = null;
//                if (wavTest == null) wavTest = OggAudio.local(new File(LayoutHandler.ASSETS_DIR, "sound.wav"));
//                if (wavTest.isReady() && !wavTest.isPlaying()) {
//                    wavTest.play();
//                }

//                if (wav == null) wav = WavAudioClip.create();
//                if ((wav != null) && !opened) {
//                    opened = true;
//                    AudioInputStream stream = AudioSystem.getAudioInputStream(new File(LayoutHandler.ASSETS_DIR, "sound.wav"));
//                    WavAudioBuffer buffer = new WavAudioBuffer(ALUtils.readStreamIntoBuffer(stream), stream.getFormat());
//                    wav.setBuffer(buffer);
//                }
//                if ((wav != null) && !wav.isPlaying()) {
//                    LOGGER.info("################ PLAY AUDIO!");
//                    wav.setSoundChannel(SoundSource.MUSIC);
//                    wav.setVolume(1.0F);
//                    wav.play();
//                }

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
