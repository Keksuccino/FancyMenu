package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.branding;

import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.TitleScreenLayer;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TitleScreenBrandingBuilder extends DeepElementBuilder<TitleScreenLayer, TitleScreenBrandingDeepElement, TitleScreenBrandingDeepEditorElement> {

    public TitleScreenBrandingBuilder(TitleScreenLayer layer) {
        super("title_screen_branding", layer);
    }

    @Override
    public @NotNull TitleScreenBrandingDeepElement buildDefaultInstance() {
        return new TitleScreenBrandingDeepElement(this);
    }

    @Override
    public TitleScreenBrandingDeepElement deserializeElement(@NotNull SerializedElement serialized) {
        return this.buildDefaultInstance();
    }

    @Override
    protected SerializedElement serializeElement(@NotNull TitleScreenBrandingDeepElement element, @NotNull SerializedElement serializeTo) {
        return serializeTo;
    }

    @Override
    public void stackElements(@NotNull TitleScreenBrandingDeepElement element, @NotNull TitleScreenBrandingDeepElement stack) {
    }

    @Override
    public @NotNull TitleScreenBrandingDeepEditorElement wrapIntoEditorElement(@NotNull TitleScreenBrandingDeepElement element, @NotNull LayoutEditorScreen editor) {
        return new TitleScreenBrandingDeepEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.branding");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return null;
    }

}