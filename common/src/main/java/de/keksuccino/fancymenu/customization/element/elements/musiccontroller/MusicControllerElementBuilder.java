package de.keksuccino.fancymenu.customization.element.elements.musiccontroller;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//TODO übernehmen
public class MusicControllerElementBuilder extends ElementBuilder<MusicControllerElement, MusicControllerEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();

    public MusicControllerElementBuilder() {
        super("music_controller");
        MusicControllerHandler.init();
    }

    @Override
    public @NotNull MusicControllerElement buildDefaultInstance() {
        MusicControllerElement i = new MusicControllerElement(this);
        i.baseWidth = 100;
        i.baseHeight = 100;
        return i;
    }

    @Override
    public MusicControllerElement deserializeElement(@NotNull SerializedElement serialized) {

        MusicControllerElement element = this.buildDefaultInstance();

        element.playMenuMusic = deserializeBoolean(element.playMenuMusic, serialized.getValue("play_menu_music"));
        element.playWorldMusic = deserializeBoolean(element.playWorldMusic, serialized.getValue("play_world_music"));

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull MusicControllerElement element, @NotNull SerializedElement serializeTo) {

        serializeTo.putProperty("play_menu_music", "" + element.playMenuMusic);
        serializeTo.putProperty("play_world_music", "" + element.playWorldMusic);

        return serializeTo;
        
    }

    @Override
    public @NotNull MusicControllerEditorElement wrapIntoEditorElement(@NotNull MusicControllerElement element, @NotNull LayoutEditorScreen editor) {
        return new MusicControllerEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Components.translatable("fancymenu.elements.music_controller");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.music_controller.desc");
    }

}
