package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.logo;

import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.TitleScreenLayer;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TitleScreenLogoBuilder extends DeepElementBuilder<TitleScreenLayer, TitleScreenLogoDeepElement, TitleScreenLogoDeepEditorElement> {

    public TitleScreenLogoBuilder(TitleScreenLayer layer) {
        super("title_screen_logo", layer);
    }

    @Override
    public @NotNull TitleScreenLogoDeepElement buildDefaultInstance() {
        return new TitleScreenLogoDeepElement(this);
    }

    @Override
    public TitleScreenLogoDeepElement deserializeElement(@NotNull SerializedElement serialized) {
        return this.buildDefaultInstance();
    }

    @Override
    protected SerializedElement serializeElement(@NotNull TitleScreenLogoDeepElement element, @NotNull SerializedElement serializeTo) {
        return serializeTo;
    }

    @Override
    public void stackElements(@NotNull TitleScreenLogoDeepElement element, @NotNull TitleScreenLogoDeepElement stack) {
    }

    @Override
    public @NotNull TitleScreenLogoDeepEditorElement wrapIntoEditorElement(@NotNull TitleScreenLogoDeepElement element, @NotNull LayoutEditorScreen editor) {
        return new TitleScreenLogoDeepEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.logo");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return null;
    }

}