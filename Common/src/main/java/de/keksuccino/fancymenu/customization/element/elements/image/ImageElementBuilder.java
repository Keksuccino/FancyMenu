package de.keksuccino.fancymenu.customization.element.elements.image;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImageElementBuilder extends ElementBuilder<ImageElement, ImageEditorElement> {

    public ImageElementBuilder() {
        super("image");
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

        element.path = serialized.getValue("path");

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull ImageElement element, @NotNull SerializedElement serializeTo) {

        if (element.path != null) {
            serializeTo.putProperty("path", element.path);
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
