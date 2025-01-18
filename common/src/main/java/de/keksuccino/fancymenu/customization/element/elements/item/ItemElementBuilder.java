package de.keksuccino.fancymenu.customization.element.elements.item;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemElementBuilder extends ElementBuilder<ItemElement, ItemEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();

    public ItemElementBuilder() {
        super("item");
    }

    @Override
    public @NotNull ItemElement buildDefaultInstance() {
        ItemElement i = new ItemElement(this);
        i.baseWidth = 100;
        i.baseHeight = 100;
        return i;
    }

    @Override
    public ItemElement deserializeElement(@NotNull SerializedElement serialized) {

        ItemElement element = this.buildDefaultInstance();

        element.textureSupplier = deserializeImageResourceSupplier(serialized.getValue("source"));
        element.repeat = deserializeBoolean(element.repeat, serialized.getValue("repeat_texture"));
        element.nineSlice = deserializeBoolean(element.nineSlice, serialized.getValue("nine_slice_texture"));
        element.nineSliceBorderX = deserializeNumber(Integer.class, element.nineSliceBorderX, serialized.getValue("nine_slice_texture_border_x"));
        element.nineSliceBorderY = deserializeNumber(Integer.class, element.nineSliceBorderY, serialized.getValue("nine_slice_texture_border_y"));

        String tint = serialized.getValue("image_tint");
        if (tint != null) element.imageTint = DrawableColor.of(tint);

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull ItemElement element, @NotNull SerializedElement serializeTo) {

        if (element.textureSupplier != null) {
            serializeTo.putProperty("source", element.textureSupplier.getSourceWithPrefix());
        }
        serializeTo.putProperty("repeat_texture", "" + element.repeat);
        serializeTo.putProperty("nine_slice_texture", "" + element.nineSlice);
        serializeTo.putProperty("nine_slice_texture_border_x", "" + element.nineSliceBorderX);
        serializeTo.putProperty("nine_slice_texture_border_y", "" + element.nineSliceBorderY);
        serializeTo.putProperty("image_tint", element.imageTint.getHex());

        return serializeTo;

    }

    @Override
    public @NotNull ItemEditorElement wrapIntoEditorElement(@NotNull ItemElement element, @NotNull LayoutEditorScreen editor) {
        return new ItemEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.item");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.item.desc");
    }

}
