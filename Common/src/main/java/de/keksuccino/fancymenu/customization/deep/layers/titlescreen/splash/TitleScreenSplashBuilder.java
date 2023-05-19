package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.splash;

import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.TitleScreenLayer;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TitleScreenSplashBuilder extends DeepElementBuilder<TitleScreenLayer, TitleScreenSplashDeepElement, TitleScreenSplashDeepEditorElement> {

    public TitleScreenSplashBuilder(TitleScreenLayer layer) {
        super("title_screen_splash", layer);
    }

    @Override
    public @NotNull TitleScreenSplashDeepElement buildDefaultInstance() {
        TitleScreenSplashDeepElement i = new TitleScreenSplashDeepElement(this);
        i.anchorPoint = ElementAnchorPoints.VANILLA;
        return i;
    }

    @Override
    public TitleScreenSplashDeepElement deserializeElement(@NotNull SerializedElement serialized) {
        return this.buildDefaultInstance();
    }

    @Override
    protected SerializedElement serializeElement(@NotNull TitleScreenSplashDeepElement element, @NotNull SerializedElement serializeTo) {
        return serializeTo;
    }

    @Override
    public void stackElements(@NotNull TitleScreenSplashDeepElement element, @NotNull TitleScreenSplashDeepElement stack) {
    }

    @Override
    public @NotNull TitleScreenSplashDeepEditorElement wrapIntoEditorElement(@NotNull TitleScreenSplashDeepElement element, @NotNull LayoutEditorScreen editor) {
        return new TitleScreenSplashDeepEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return null;
    }

}