package de.keksuccino.fancymenu.customization.element.elements.image;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.event.events.ModReloadEvent;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImageElementBuilder extends ElementBuilder<ImageElement, ImageEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();

    public ImageElementBuilder() {
        super("image");
        EventHandler.INSTANCE.registerListenersOf(this);
    }

    @EventListener
    public void onModReload(ModReloadEvent e) {
        LOGGER.info("[FANCYMENU] Clearing Image element cache..");
        ImageElement.CACHED_WEB_IMAGES.clear();
    }

    @Override
    public @NotNull ImageElement buildDefaultInstance() {
        ImageElement i = new ImageElement(this);
        i.width = 200;
        i.height = 200;
        return i;
    }

    @Override
    public ImageElement deserializeElement(@NotNull SerializedElement serialized) {

        ImageElement element = this.buildDefaultInstance();

        element.source = serialized.getValue("source");

        String sourceMode = serialized.getValue("source_mode");
        if (sourceMode != null) {
            element.sourceMode = ImageElement.SourceMode.getByName(sourceMode);
            if (element.sourceMode == null) {
                element.sourceMode = ImageElement.SourceMode.LOCAL;
            }
        }

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull ImageElement element, @NotNull SerializedElement serializeTo) {

        if (element.source != null) {
            serializeTo.putProperty("source", element.source);
        }

        if (element.sourceMode != null) {
            serializeTo.putProperty("source_mode", element.sourceMode.name);
        }

        return serializeTo;
        
    }

    @Override
    public @NotNull ImageEditorElement wrapIntoEditorElement(@NotNull ImageElement element, @NotNull LayoutEditorScreen editor) {
        return new ImageEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.customization.items.slider");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.slider.desc");
    }

}
