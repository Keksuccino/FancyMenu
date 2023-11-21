package de.keksuccino.fancymenu.customization.element.elements.audio;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

public class AudioElementBuilder extends ElementBuilder<AudioElement, AudioEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();

    public AudioElementBuilder() {
        super("audio_v2");
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
