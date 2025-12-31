package de.keksuccino.fancymenu.customization.element.elements.progressbar;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.SerializationUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

public class ProgressBarElementBuilder extends ElementBuilder<ProgressBarElement, ProgressBarEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();

    public ProgressBarElementBuilder() {
        super("progress_bar");
    }

    @Override
    public @NotNull ProgressBarElement buildDefaultInstance() {
        ProgressBarElement element = new ProgressBarElement(this);
        element.baseWidth = 200;
        element.baseHeight = 20;
        element.progressSource = "50";
        return element;
    }

    @Override
    public ProgressBarElement deserializeElement(@NotNull SerializedElement serialized) {

        ProgressBarElement element = this.buildDefaultInstance();

        element.useProgressForElementAnchor = deserializeBoolean(element.useProgressForElementAnchor, serialized.getValue("progress_for_element_anchor"));

        element.barTextureSupplier = deserializeImageResourceSupplier(serialized.getValue("bar_texture"));
        element.barNineSlice = deserializeBoolean(element.barNineSlice, serialized.getValue("bar_nine_slice"));
        element.barNineSliceBorderTop = deserializeNumber(Integer.class, element.barNineSliceBorderTop, serialized.getValue("bar_nine_slice_border_top"));
        element.barNineSliceBorderRight = deserializeNumber(Integer.class, element.barNineSliceBorderRight, serialized.getValue("bar_nine_slice_border_right"));
        element.barNineSliceBorderBottom = deserializeNumber(Integer.class, element.barNineSliceBorderBottom, serialized.getValue("bar_nine_slice_border_bottom"));
        element.barNineSliceBorderLeft = deserializeNumber(Integer.class, element.barNineSliceBorderLeft, serialized.getValue("bar_nine_slice_border_left"));

        element.backgroundTextureSupplier = deserializeImageResourceSupplier(serialized.getValue("background_texture"));
        element.backgroundNineSlice = deserializeBoolean(element.backgroundNineSlice, serialized.getValue("background_nine_slice"));
        element.backgroundNineSliceBorderTop = deserializeNumber(Integer.class, element.backgroundNineSliceBorderTop, serialized.getValue("background_nine_slice_border_top"));
        element.backgroundNineSliceBorderRight = deserializeNumber(Integer.class, element.backgroundNineSliceBorderRight, serialized.getValue("background_nine_slice_border_right"));
        element.backgroundNineSliceBorderBottom = deserializeNumber(Integer.class, element.backgroundNineSliceBorderBottom, serialized.getValue("background_nine_slice_border_bottom"));
        element.backgroundNineSliceBorderLeft = deserializeNumber(Integer.class, element.backgroundNineSliceBorderLeft, serialized.getValue("background_nine_slice_border_left"));

        String barDirection = serialized.getValue("direction");
        if (barDirection != null) {
            element.direction = Objects.requireNonNullElse(ProgressBarElement.BarDirection.getByName(barDirection), ProgressBarElement.BarDirection.LEFT);
        }

        String valueMode = serialized.getValue("value_mode");
        if (valueMode != null) {
            element.progressValueMode = Objects.requireNonNullElse(ProgressBarElement.ProgressValueMode.getByName(valueMode), ProgressBarElement.ProgressValueMode.PERCENTAGE);
        }

        element.progressSource = serialized.getValue("progress_source");

        element.smoothFillingAnimation = SerializationUtils.deserializeBoolean(element.smoothFillingAnimation, serialized.getValue("smooth_filling_animation"));

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull ProgressBarElement element, @NotNull SerializedElement serializeTo) {

        if (element.barTextureSupplier != null) {
            serializeTo.putProperty("bar_texture", element.barTextureSupplier.getSourceWithPrefix());
        }
        serializeTo.putProperty("bar_nine_slice", "" + element.barNineSlice);
        serializeTo.putProperty("bar_nine_slice_border_top", "" + element.barNineSliceBorderTop);
        serializeTo.putProperty("bar_nine_slice_border_right", "" + element.barNineSliceBorderRight);
        serializeTo.putProperty("bar_nine_slice_border_bottom", "" + element.barNineSliceBorderBottom);
        serializeTo.putProperty("bar_nine_slice_border_left", "" + element.barNineSliceBorderLeft);
        if (element.backgroundTextureSupplier != null) {
            serializeTo.putProperty("background_texture", element.backgroundTextureSupplier.getSourceWithPrefix());
        }
        serializeTo.putProperty("background_nine_slice", "" + element.backgroundNineSlice);
        serializeTo.putProperty("background_nine_slice_border_top", "" + element.backgroundNineSliceBorderTop);
        serializeTo.putProperty("background_nine_slice_border_right", "" + element.backgroundNineSliceBorderRight);
        serializeTo.putProperty("background_nine_slice_border_bottom", "" + element.backgroundNineSliceBorderBottom);
        serializeTo.putProperty("background_nine_slice_border_left", "" + element.backgroundNineSliceBorderLeft);
        serializeTo.putProperty("direction", element.direction.getName());
        serializeTo.putProperty("progress_for_element_anchor", "" + element.useProgressForElementAnchor);
        serializeTo.putProperty("progress_source", element.progressSource);
        serializeTo.putProperty("value_mode", element.progressValueMode.getName());

        serializeTo.putProperty("smooth_filling_animation", "" + element.smoothFillingAnimation);

        return serializeTo;
        
    }

    @Override
    public @NotNull ProgressBarEditorElement wrapIntoEditorElement(@NotNull ProgressBarElement element, @NotNull LayoutEditorScreen editor) {
        return new ProgressBarEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.progress_bar");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.progress_bar.desc");
    }

}
