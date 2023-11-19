package de.keksuccino.fancymenu.customization.element.elements.audio;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

        element.textureSupplier = deserializeImageResourceSupplier(serialized.getValue("source"));

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull AudioElement element, @NotNull SerializedElement serializeTo) {

        if (element.textureSupplier != null) {
            serializeTo.putProperty("source", element.textureSupplier.getSourceWithPrefix());
        }

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
