package de.keksuccino.fancymenu.customization.element.elements.image;

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

public class ImageElementBuilder extends ElementBuilder<ImageElement, ImageEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();

    public ImageElementBuilder() {
        super("image");
    }

    @Override
    public @NotNull ImageElement buildDefaultInstance() {
        ImageElement i = new ImageElement(this);
        i.baseWidth = 100;
        i.baseHeight = 100;
        return i;
    }

    @Override
    public ImageElement deserializeElement(@NotNull SerializedElement serialized) {

        ImageElement element = this.buildDefaultInstance();

        element.textureSupplier = deserializeImageResourceSupplier(serialized.getValue("source"));
        element.repeat = deserializeBoolean(element.repeat, serialized.getValue("repeat_texture"));
        element.nineSlice = deserializeBoolean(element.nineSlice, serialized.getValue("nine_slice_texture"));
        element.nineSliceBorderX = deserializeNumber(Integer.class, element.nineSliceBorderX, serialized.getValue("nine_slice_texture_border_x"));
        element.nineSliceBorderY = deserializeNumber(Integer.class, element.nineSliceBorderY, serialized.getValue("nine_slice_texture_border_y"));

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull ImageElement element, @NotNull SerializedElement serializeTo) {

        if (element.textureSupplier != null) {
            serializeTo.putProperty("source", element.textureSupplier.getSourceWithPrefix());
        }
        serializeTo.putProperty("repeat_texture", "" + element.repeat);
        serializeTo.putProperty("nine_slice_texture", "" + element.nineSlice);
        serializeTo.putProperty("nine_slice_texture_border_x", "" + element.nineSliceBorderX);
        serializeTo.putProperty("nine_slice_texture_border_y", "" + element.nineSliceBorderY);

        return serializeTo;
        
    }

    @Override
    public @NotNull ImageEditorElement wrapIntoEditorElement(@NotNull ImageElement element, @NotNull LayoutEditorScreen editor) {
        return new ImageEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.editor.add.image");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.editor.add.image.desc");
    }

}
