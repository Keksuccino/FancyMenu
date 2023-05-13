package de.keksuccino.fancymenu.customization.background.backgrounds.animation;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.SerializedMenuBackground;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.elements.ChooseFilePopup;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class AnimationMenuBackgroundBuilder extends MenuBackgroundBuilder<AnimationMenuBackground> {

    public AnimationMenuBackgroundBuilder() {
        super("animation");
    }

    @Override
    public void buildNewOrEditInstance(@NotNull LayoutEditorScreen editor, @Nullable AnimationMenuBackground backgroundToEdit, @NotNull Consumer<AnimationMenuBackground> backgroundConsumer) {
        ChooseFilePopup p = new ChooseFilePopup((call) -> {
            if (call != null) {
                AnimationMenuBackground background = (backgroundToEdit != null) ? backgroundToEdit : new AnimationMenuBackground(this);
                background.imagePath = call;
                backgroundConsumer.accept(background);
            } else {
                backgroundConsumer.accept(backgroundToEdit);
            }
        }, "png", "jpg", "jpeg");
        if ((backgroundToEdit != null) && (backgroundToEdit.imagePath != null)) {
            p.setText(backgroundToEdit.imagePath);
        }
        PopupHandler.displayPopup(p);
    }

    @Override
    public AnimationMenuBackground deserializeBackground(SerializedMenuBackground serializedMenuBackground) {

        AnimationMenuBackground b = new AnimationMenuBackground(this);

        b.animation = serializedMenuBackground.getValue("animation_name");

        return b;

    }

    @Override
    public SerializedMenuBackground serializedBackground(AnimationMenuBackground background) {

        SerializedMenuBackground serialized = new SerializedMenuBackground();

        if (background.imagePath != null) {
            serialized.putProperty("image_path", background.imagePath);
        }

        return serialized;

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.background.image");
    }

    @Override
    public @Nullable Component[] getDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.background.image.desc");
    }

}
