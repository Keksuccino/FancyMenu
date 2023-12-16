package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.realmsnotification;

import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.TitleScreenLayer;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TitleScreenRealmsNotificationBuilder extends DeepElementBuilder<TitleScreenLayer, TitleScreenRealmsNotificationDeepElement, TitleScreenRealmsNotificationDeepEditorElement> {

    public TitleScreenRealmsNotificationBuilder(TitleScreenLayer layer) {
        super("title_screen_realms_notification", layer);
    }

    @Override
    public void stackElements(@NotNull TitleScreenRealmsNotificationDeepElement element, @NotNull TitleScreenRealmsNotificationDeepElement stack) {
    }

    @Override
    public @NotNull TitleScreenRealmsNotificationDeepElement buildDefaultInstance() {
        return new TitleScreenRealmsNotificationDeepElement(this);
    }

    @Override
    public TitleScreenRealmsNotificationDeepElement deserializeElement(@NotNull SerializedElement serialized) {
        return this.buildDefaultInstance();
    }

    @Override
    protected SerializedElement serializeElement(@NotNull TitleScreenRealmsNotificationDeepElement element, @NotNull SerializedElement serializeTo) {
        return serializeTo;
    }

    @Override
    public @NotNull TitleScreenRealmsNotificationDeepEditorElement wrapIntoEditorElement(@NotNull TitleScreenRealmsNotificationDeepElement element, @NotNull LayoutEditorScreen editor) {
        return new TitleScreenRealmsNotificationDeepEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Components.translatable("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.realmsnotification");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return null;
    }

}