package de.keksuccino.fancymenu.customization.element.elements.glsl;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GlslElementBuilder extends ElementBuilder<GlslElement, GlslEditorElement> {

    public GlslElementBuilder() {
        super("glsl_shader");
    }

    @Override
    public @NotNull GlslElement buildDefaultInstance() {
        GlslElement element = new GlslElement(this);
        element.baseWidth = 320;
        element.baseHeight = 180;
        return element;
    }

    @Override
    public GlslElement deserializeElement(@NotNull SerializedElement serialized) {
        return this.buildDefaultInstance();
    }

    @Override
    protected SerializedElement serializeElement(@NotNull GlslElement element, @NotNull SerializedElement serializeTo) {
        return serializeTo;
    }

    @Override
    public @NotNull GlslEditorElement wrapIntoEditorElement(@NotNull GlslElement element, @NotNull LayoutEditorScreen editor) {
        return new GlslEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.glsl");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.glsl.desc");
    }

}
