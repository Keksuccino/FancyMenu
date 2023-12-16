package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.forge.copyright;

import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.TitleScreenLayer;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TitleScreenForgeCopyrightBuilder extends DeepElementBuilder<TitleScreenLayer, TitleScreenForgeCopyrightDeepElement, TitleScreenForgeCopyrightDeepEditorElement> {

    public TitleScreenForgeCopyrightBuilder(TitleScreenLayer layer) {
        super("title_screen_forge_copyright", layer);
    }

    @Override
    public @NotNull TitleScreenForgeCopyrightDeepElement buildDefaultInstance() {
        return new TitleScreenForgeCopyrightDeepElement(this);
    }

    @Override
    public TitleScreenForgeCopyrightDeepElement deserializeElement(@NotNull SerializedElement serialized) {
        return this.buildDefaultInstance();
    }

    @Override
    protected SerializedElement serializeElement(@NotNull TitleScreenForgeCopyrightDeepElement element, @NotNull SerializedElement serializeTo) {
        return serializeTo;
    }

    @Override
    public void stackElements(@NotNull TitleScreenForgeCopyrightDeepElement element, @NotNull TitleScreenForgeCopyrightDeepElement stack) {
    }

    @Override
    public @NotNull TitleScreenForgeCopyrightDeepEditorElement wrapIntoEditorElement(@NotNull TitleScreenForgeCopyrightDeepElement element, @NotNull LayoutEditorScreen editor) {
        return new TitleScreenForgeCopyrightDeepEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Components.translatable("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.copyright");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return null;
    }

}