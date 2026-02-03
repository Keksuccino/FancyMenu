package de.keksuccino.fancymenu.customization.element.elements.progressbar;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.SerializationHelper;
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
        element.progressSource.set(50.0F);
        return element;
    }

    @Override
    public ProgressBarElement deserializeElement(@NotNull SerializedElement serialized) {

        ProgressBarElement element = this.buildDefaultInstance();

        element.useProgressForElementAnchor = deserializeBoolean(element.useProgressForElementAnchor, serialized.getValue("progress_for_element_anchor"));

        element.barTextureSupplier = deserializeImageResourceSupplier(serialized.getValue("bar_texture"));
        element.barNineSlice = deserializeBoolean(element.barNineSlice, serialized.getValue("bar_nine_slice"));

        element.backgroundTextureSupplier = deserializeImageResourceSupplier(serialized.getValue("background_texture"));
        element.backgroundNineSlice = deserializeBoolean(element.backgroundNineSlice, serialized.getValue("background_nine_slice"));

        String barDirection = serialized.getValue("direction");
        if (barDirection != null) {
            element.direction = Objects.requireNonNullElse(ProgressBarElement.BarDirection.getByName(barDirection), ProgressBarElement.BarDirection.LEFT);
        }

        String valueMode = serialized.getValue("value_mode");
        if (valueMode != null) {
            element.progressValueMode = Objects.requireNonNullElse(ProgressBarElement.ProgressValueMode.getByName(valueMode), ProgressBarElement.ProgressValueMode.PERCENTAGE);
        }

        if (serialized.getValue("progress_source") == null) {
            serialized.putProperty("progress_source", "0");
        }

        element.smoothFillingAnimation = SerializationHelper.INSTANCE.deserializeBoolean(element.smoothFillingAnimation, serialized.getValue("smooth_filling_animation"));

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull ProgressBarElement element, @NotNull SerializedElement serializeTo) {

        if (element.barTextureSupplier != null) {
            serializeTo.putProperty("bar_texture", element.barTextureSupplier.getSourceWithPrefix());
        }
        serializeTo.putProperty("bar_nine_slice", "" + element.barNineSlice);
        if (element.backgroundTextureSupplier != null) {
            serializeTo.putProperty("background_texture", element.backgroundTextureSupplier.getSourceWithPrefix());
        }
        serializeTo.putProperty("background_nine_slice", "" + element.backgroundNineSlice);
        serializeTo.putProperty("direction", element.direction.getName());
        serializeTo.putProperty("progress_for_element_anchor", "" + element.useProgressForElementAnchor);
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
