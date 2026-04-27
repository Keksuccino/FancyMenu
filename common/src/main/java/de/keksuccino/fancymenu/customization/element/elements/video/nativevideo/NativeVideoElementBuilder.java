package de.keksuccino.fancymenu.customization.element.elements.video.nativevideo;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NativeVideoElementBuilder extends ElementBuilder<NativeVideoElement, NativeVideoEditorElement> {

    public NativeVideoElementBuilder() {
        super("video");
    }

    @Override
    public @NotNull NativeVideoElement buildDefaultInstance() {
        NativeVideoElement element = new NativeVideoElement(this);
        element.baseWidth = 400;
        element.baseHeight = 200;
        return element;
    }

    @Override
    public NativeVideoElement deserializeElement(@NotNull SerializedElement serialized) {
        return this.buildDefaultInstance();
    }

    @Override
    protected SerializedElement serializeElement(@NotNull NativeVideoElement element, @NotNull SerializedElement serializeTo) {
        return serializeTo;
    }

    @Override
    public @NotNull NativeVideoEditorElement wrapIntoEditorElement(@NotNull NativeVideoElement element, @NotNull LayoutEditorScreen editor) {
        return new NativeVideoEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.video");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.video.desc");
    }
}
