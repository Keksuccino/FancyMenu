package de.keksuccino.fancymenu.customization.element.elements.shape;

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

public class ShapeElementBuilder extends ElementBuilder<ShapeElement, ShapeEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();

    public ShapeElementBuilder() {
        super("shape");
    }

    @Override
    public @NotNull ShapeElement buildDefaultInstance() {
        ShapeElement i = new ShapeElement(this);
        i.baseWidth = 100;
        i.baseHeight = 100;
        return i;
    }

    @Override
    public ShapeElement deserializeElement(@NotNull SerializedElement serialized) {

        ShapeElement element = this.buildDefaultInstance();

        String shape = serialized.getValue("shape");
        if (shape != null) {
            element.shape = ShapeElement.Shape.getByName(shape);
        }

        String colorHex = serialized.getValue("color");
        if (colorHex != null) {
            element.colorRaw = colorHex;
        }

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull ShapeElement element, @NotNull SerializedElement serializeTo) {

        if (element.shape != null) {
            serializeTo.putProperty("shape", element.shape.name);
        }

        if (element.color != null) {
            serializeTo.putProperty("color", element.colorRaw);
        }

        return serializeTo;
        
    }

    @Override
    public @NotNull ShapeEditorElement wrapIntoEditorElement(@NotNull ShapeElement element, @NotNull LayoutEditorScreen editor) {
        return new ShapeEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.shape.rectangle");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.shape.rectangle.desc");
    }

}
