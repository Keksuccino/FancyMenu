package de.keksuccino.fancymenu.customization.element.elements.shape.circle;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CircleShapeElementBuilder extends ElementBuilder<CircleShapeElement, CircleShapeEditorElement> {

    public CircleShapeElementBuilder() {
        super("shape_circle");
    }

    @Override
    public @NotNull CircleShapeElement buildDefaultInstance() {
        CircleShapeElement i = new CircleShapeElement(this);
        i.baseWidth = 100;
        i.baseHeight = 100;
        return i;
    }

    @Override
    public CircleShapeElement deserializeElement(@NotNull SerializedElement serialized) {

        return this.buildDefaultInstance();

    }

    @Override
    protected SerializedElement serializeElement(@NotNull CircleShapeElement element, @NotNull SerializedElement serializeTo) {

        return serializeTo;

    }

    @Override
    public @NotNull CircleShapeEditorElement wrapIntoEditorElement(@NotNull CircleShapeElement element, @NotNull LayoutEditorScreen editor) {
        return new CircleShapeEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.shape.circle");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.shape.circle.desc");
    }

}
