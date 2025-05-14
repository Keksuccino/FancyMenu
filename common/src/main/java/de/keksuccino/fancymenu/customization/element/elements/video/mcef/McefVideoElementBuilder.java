package de.keksuccino.fancymenu.customization.element.elements.video.mcef;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class McefVideoElementBuilder extends ElementBuilder<McefVideoElement, McefVideoEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();

    public McefVideoElementBuilder() {
        super("video_mcef");
    }

    @Override
    public @NotNull McefVideoElement buildDefaultInstance() {
        McefVideoElement i = new McefVideoElement(this);
        i.baseWidth = 400;
        i.baseHeight = 200;
        return i;
    }

    @Override
    public McefVideoElement deserializeElement(@NotNull SerializedElement serialized) {

        McefVideoElement element = this.buildDefaultInstance();

        String source = serialized.getValue("source");
        element.rawVideoUrlSource = (source != null) ? ResourceSource.of(source) : null;
        element.loop = deserializeBoolean(element.loop, serialized.getValue("loop"));
        element.volume = deserializeNumber(Float.class, element.volume, serialized.getValue("volume"));
        String soundSource = serialized.getValue("sound_source");
        if (soundSource != null) element.soundSource = Objects.requireNonNullElse(getSoundSourceByName(soundSource), SoundSource.MASTER);

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull McefVideoElement element, @NotNull SerializedElement serializeTo) {

        if (element.rawVideoUrlSource != null) {
            serializeTo.putProperty("source", element.rawVideoUrlSource.getSerializationSource());
        }
        serializeTo.putProperty("loop", "" + element.loop);
        serializeTo.putProperty("volume", "" + element.volume);
        serializeTo.putProperty("sound_source", element.soundSource.getName());

        return serializeTo;

    }

    @Override
    public @NotNull McefVideoEditorElement wrapIntoEditorElement(@NotNull McefVideoElement element, @NotNull LayoutEditorScreen editor) {
        return new McefVideoEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.video_mcef");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.video_mcef.desc");
    }

    @Nullable
    protected static SoundSource getSoundSourceByName(@NotNull String name) {
        for (SoundSource source : SoundSource.values()) {
            if (source.getName().equals(name)) return source;
        }
        return null;
    }

}
