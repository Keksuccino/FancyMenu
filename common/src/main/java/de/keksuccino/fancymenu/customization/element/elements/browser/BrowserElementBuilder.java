package de.keksuccino.fancymenu.customization.element.elements.browser;

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
import java.util.Objects;

public class BrowserElementBuilder extends ElementBuilder<BrowserElement, BrowserEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();

    public BrowserElementBuilder() {
        super("browser");
    }

    @Override
    public @NotNull BrowserElement buildDefaultInstance() {
        BrowserElement i = new BrowserElement(this);
        i.baseWidth = 250;
        i.baseHeight = 250;
        return i;
    }

    @Override
    public BrowserElement deserializeElement(@NotNull SerializedElement serialized) {

        BrowserElement element = this.buildDefaultInstance();

        element.url = Objects.requireNonNullElse(serialized.getValue("url"), element.url);
        element.interactable = deserializeBoolean(element.interactable, serialized.getValue("interactable"));
        element.hideVideoControls = deserializeBoolean(element.hideVideoControls, serialized.getValue("hide_video_controls"));
        element.loopVideos = deserializeBoolean(element.loopVideos, serialized.getValue("loop_videos"));
        element.muteMedia = deserializeBoolean(element.muteMedia, serialized.getValue("mute_media"));

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull BrowserElement element, @NotNull SerializedElement serializeTo) {

        serializeTo.putProperty("url", element.url);
        serializeTo.putProperty("interactable", "" + element.interactable);
        serializeTo.putProperty("hide_video_controls", "" + element.hideVideoControls);
        serializeTo.putProperty("loop_videos", "" + element.loopVideos);
        serializeTo.putProperty("mute_media", "" + element.muteMedia);

        return serializeTo;

    }

    @Override
    public @NotNull BrowserEditorElement wrapIntoEditorElement(@NotNull BrowserElement element, @NotNull LayoutEditorScreen editor) {
        return new BrowserEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.browser");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.browser.desc");
    }

}
