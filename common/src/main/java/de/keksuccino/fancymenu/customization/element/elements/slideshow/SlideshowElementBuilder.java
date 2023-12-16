package de.keksuccino.fancymenu.customization.element.elements.slideshow;

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

public class SlideshowElementBuilder extends ElementBuilder<SlideshowElement, SlideshowEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();

    public SlideshowElementBuilder() {
        super("slideshow");
    }

    @Override
    public @NotNull SlideshowElement buildDefaultInstance() {
        SlideshowElement i = new SlideshowElement(this);
        i.baseWidth = 200;
        i.baseHeight = 200;
        return i;
    }

    @Override
    public SlideshowElement deserializeElement(@NotNull SerializedElement serialized) {

        SlideshowElement element = this.buildDefaultInstance();

        element.slideshowName = serialized.getValue("slideshow_name");

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull SlideshowElement element, @NotNull SerializedElement serializeTo) {

        if (element.slideshowName != null) {
            serializeTo.putProperty("slideshow_name", element.slideshowName);
        }

        return serializeTo;
        
    }

    @Override
    public @NotNull SlideshowEditorElement wrapIntoEditorElement(@NotNull SlideshowElement element, @NotNull LayoutEditorScreen editor) {
        return new SlideshowEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Components.translatable("fancymenu.editor.add.slideshow");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.editor.add.slideshow.desc");
    }

}
