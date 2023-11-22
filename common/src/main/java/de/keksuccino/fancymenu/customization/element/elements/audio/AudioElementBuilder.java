package de.keksuccino.fancymenu.customization.element.elements.audio;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.Trio;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.resources.ResourceSupplier;
import de.keksuccino.fancymenu.util.resources.audio.IAudio;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class AudioElementBuilder extends ElementBuilder<AudioElement, AudioEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();
    protected static final Map<String, Trio<ResourceSupplier<IAudio>, IAudio, Integer>> CURRENT_AUDIO_CACHE = new HashMap<>();

    public AudioElementBuilder() {
        super("audio_v2");
        EventHandler.INSTANCE.registerListenersOf(this);
    }

    @EventListener
    public void onInitOrResizeScreenCompleted(InitOrResizeScreenCompletedEvent ignored) {
        ScreenCustomizationLayer activeLayer = ScreenCustomizationLayerHandler.getActiveLayer();
        if (activeLayer != null) {
            List<String> removeFromCache = new ArrayList<>();
            CURRENT_AUDIO_CACHE.forEach((s, resourceSupplierIAudioPair) -> {
                AbstractElement element = activeLayer.getElementByInstanceIdentifier(s);
                if (element == null) {
                    resourceSupplierIAudioPair.getSecond().stop();
                    //TODO remove debug
                    LOGGER.info("########### BUILDER: STOPPED CACHED AUDIO (audio element not found in new layer)");
                    removeFromCache.add(s);
                } else if (element instanceof AudioElement a) {
                    boolean audioFound = false;
                    for (AudioElement.AudioInstance instance : a.audios) {
                        if (Objects.equals(instance.supplier.get(), resourceSupplierIAudioPair.getSecond())) {
                            audioFound = true;
                            break;
                        }
                    }
                    if (!audioFound) {
                        resourceSupplierIAudioPair.getSecond().stop();
                        removeFromCache.add(s);
                        //TODO remove debug
                        LOGGER.info("########### BUILDER: STOPPED CACHED AUDIO (IAudio not found in element)");
                    }
                }
            });
            for (String s : removeFromCache) {
                CURRENT_AUDIO_CACHE.remove(s);
            }
        } else {
            CURRENT_AUDIO_CACHE.forEach((s, resourceSupplierIAudioPair) -> resourceSupplierIAudioPair.getSecond().stop());
            CURRENT_AUDIO_CACHE.clear();
            //TODO remove debug
            LOGGER.info("########### BUILDER: STOPPED ALL CACHED AUDIOS (layer was NULL)");
        }
    }

    @Override
    public @NotNull AudioElement buildDefaultInstance() {
        AudioElement i = new AudioElement(this);
        i.baseWidth = 100;
        i.baseHeight = 100;
        return i;
    }

    @Override
    public AudioElement deserializeElement(@NotNull SerializedElement serialized) {

        AudioElement element = this.buildDefaultInstance();

        element.audios.addAll(AudioElement.AudioInstance.deserializeAllOfContainer(serialized));

        String playMode = serialized.getValue("play_mode");
        if (playMode != null) element.setPlayMode(Objects.requireNonNullElse(AudioElement.PlayMode.getByName(playMode), AudioElement.PlayMode.NORMAL));

        element.setLooping(deserializeBoolean(element.loop, serialized.getValue("looping")));

        element.setVolume(deserializeNumber(Float.class, element.volume, serialized.getValue("volume")));

        String soundSource = serialized.getValue("sound_source");
        if (soundSource != null) element.setSoundSource(Objects.requireNonNullElse(getSoundSourceByName(soundSource), SoundSource.MASTER));

        return element;

    }

    @Nullable
    protected static SoundSource getSoundSourceByName(@NotNull String name) {
        for (SoundSource source : SoundSource.values()) {
            if (source.getName().equals(name)) return source;
        }
        return null;
    }

    @Override
    protected SerializedElement serializeElement(@NotNull AudioElement element, @NotNull SerializedElement serializeTo) {

        AudioElement.AudioInstance.serializeAllToExistingContainer(element.audios, serializeTo);

        serializeTo.putProperty("play_mode", element.playMode.getName());

        serializeTo.putProperty("looping", "" + element.loop);

        serializeTo.putProperty("volume", "" + element.volume);

        serializeTo.putProperty("sound_source", element.soundSource.getName());

        return serializeTo;
        
    }

    @Override
    public @NotNull AudioEditorElement wrapIntoEditorElement(@NotNull AudioElement element, @NotNull LayoutEditorScreen editor) {
        return new AudioEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.audio");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.audio.desc");
    }

}
