package de.keksuccino.fancymenu.customization.element.elements.video.mcef;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import net.minecraft.network.chat.Component;
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

//        element.repeat = deserializeBoolean(element.repeat, serialized.getValue("repeat_texture"));
//        element.nineSlice = deserializeBoolean(element.nineSlice, serialized.getValue("nine_slice_texture"));
//        element.nineSliceBorderX = deserializeNumber(Integer.class, element.nineSliceBorderX, serialized.getValue("nine_slice_texture_border_x"));
//        element.nineSliceBorderY = deserializeNumber(Integer.class, element.nineSliceBorderY, serialized.getValue("nine_slice_texture_border_y"));
//        String tint = serialized.getValue("image_tint");
//        if (tint != null) element.imageTint = DrawableColor.of(tint);

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull McefVideoElement element, @NotNull SerializedElement serializeTo) {

        if (element.rawVideoUrlSource != null) {
            serializeTo.putProperty("source", element.rawVideoUrlSource.getSerializationSource());
        }

//        serializeTo.putProperty("repeat_texture", "" + element.repeat);
//        serializeTo.putProperty("nine_slice_texture", "" + element.nineSlice);
//        serializeTo.putProperty("nine_slice_texture_border_x", "" + element.nineSliceBorderX);
//        serializeTo.putProperty("nine_slice_texture_border_y", "" + element.nineSliceBorderY);
//        serializeTo.putProperty("image_tint", element.imageTint.getHex());

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

}
