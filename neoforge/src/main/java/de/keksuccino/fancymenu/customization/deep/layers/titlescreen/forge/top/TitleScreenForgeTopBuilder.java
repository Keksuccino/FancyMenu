package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.forge.top;

import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.TitleScreenLayer;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TitleScreenForgeTopBuilder extends DeepElementBuilder<TitleScreenLayer, TitleScreenForgeTopDeepElement, TitleScreenForgeTopDeepEditorElement> {

    public TitleScreenForgeTopBuilder(TitleScreenLayer layer) {
        super("title_screen_forge_top", layer);
    }

    @Override
    public @NotNull TitleScreenForgeTopDeepElement buildDefaultInstance() {
        return new TitleScreenForgeTopDeepElement(this);
    }

    @Override
    public TitleScreenForgeTopDeepElement deserializeElement(@NotNull SerializedElement serialized) {
        return this.buildDefaultInstance();
    }

    @Override
    protected SerializedElement serializeElement(@NotNull TitleScreenForgeTopDeepElement element, @NotNull SerializedElement serializeTo) {
        return serializeTo;
    }

    @Override
    public void stackElements(@NotNull TitleScreenForgeTopDeepElement element, @NotNull TitleScreenForgeTopDeepElement stack) {
    }

    @Override
    public @NotNull TitleScreenForgeTopDeepEditorElement wrapIntoEditorElement(@NotNull TitleScreenForgeTopDeepElement element, @NotNull LayoutEditorScreen editor) {
        return new TitleScreenForgeTopDeepEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.top");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return null;
    }

}