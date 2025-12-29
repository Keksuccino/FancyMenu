package de.keksuccino.fancymenu.customization.element.elements.checkbox;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CheckboxElementBuilder extends ElementBuilder<CheckboxElement, CheckboxEditorElement> {

    public CheckboxElementBuilder() {
        super("checkbox");
    }

    @Override
    public @NotNull CheckboxElement buildDefaultInstance() {
        CheckboxElement element = new CheckboxElement(this);
        element.shouldBeAffectedByDecorationOverlays.setDefault(true).set(true);
        element.baseWidth = 20;
        element.baseHeight = 20;
        return element;
    }

    @Override
    public CheckboxElement deserializeElement(@NotNull SerializedElement serialized) {
        return buildDefaultInstance();
    }

    @Override
    protected @NotNull SerializedElement serializeElement(@NotNull CheckboxElement element, @NotNull SerializedElement serializeTo) {
        return serializeTo;
    }

    @Override
    public @NotNull CheckboxEditorElement wrapIntoEditorElement(@NotNull CheckboxElement element, @NotNull LayoutEditorScreen editor) {
        return new CheckboxEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.checkbox");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.desc");
    }

}
