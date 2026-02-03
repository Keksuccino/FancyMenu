package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonModelElementBuilder extends ElementBuilder<JsonModelElement, JsonModelEditorElement> {

    public JsonModelElementBuilder() {
        super("json_model");
    }

    @Override
    public @NotNull JsonModelElement buildDefaultInstance() {
        JsonModelElement element = new JsonModelElement(this);
        element.baseWidth = 100;
        element.baseHeight = 100;
        return element;
    }

    @Override
    public JsonModelElement deserializeElement(@NotNull SerializedElement serialized) {
        return this.buildDefaultInstance();
    }

    @Override
    protected SerializedElement serializeElement(@NotNull JsonModelElement element, @NotNull SerializedElement serializeTo) {
        return serializeTo;
    }

    @Override
    public @NotNull JsonModelEditorElement wrapIntoEditorElement(@NotNull JsonModelElement element, @NotNull LayoutEditorScreen editor) {
        return new JsonModelEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.json_model");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.json_model.desc");
    }
}
