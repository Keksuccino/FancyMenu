package de.keksuccino.fancymenu.customization.element.elements.shape.rectangle;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RectangleShapeElementBuilder extends ElementBuilder<RectangleShapeElement, RectangleShapeEditorElement> {

    public RectangleShapeElementBuilder() {
        super("shape");
    }

    @Override
    public @NotNull RectangleShapeElement buildDefaultInstance() {
        RectangleShapeElement i = new RectangleShapeElement(this);
        i.baseWidth = 100;
        i.baseHeight = 100;
        return i;
    }

    @Override
    public RectangleShapeElement deserializeElement(@NotNull SerializedElement serialized) {

        return this.buildDefaultInstance();

    }

    @Override
    protected SerializedElement serializeElement(@NotNull RectangleShapeElement element, @NotNull SerializedElement serializeTo) {

        return serializeTo;
        
    }

    @Override
    public @NotNull RectangleShapeEditorElement wrapIntoEditorElement(@NotNull RectangleShapeElement element, @NotNull LayoutEditorScreen editor) {
        return new RectangleShapeEditorElement(element, editor);
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
