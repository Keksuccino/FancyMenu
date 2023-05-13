package de.keksuccino.fancymenu.customization.background.backgrounds.image;

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

public class ImageMenuBackgroundBuilder extends MenuBackgroundBuilder<ImageMenuBackground> {

    public ImageMenuBackgroundBuilder() {
        super("image");
    }

    @Override
    public void buildNewOrEditInstance(@NotNull LayoutEditorScreen editor, @Nullable ImageMenuBackground backgroundToEdit, @NotNull Consumer<ImageMenuBackground> backgroundConsumer) {
        ChooseFilePopup p = new ChooseFilePopup((call) -> {
            if (call != null) {
                ImageMenuBackground background = (backgroundToEdit != null) ? backgroundToEdit : new ImageMenuBackground(this);
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
    public ImageMenuBackground deserializeBackground(SerializedMenuBackground serializedMenuBackground) {

        ImageMenuBackground b = new ImageMenuBackground(this);

        b.imagePath = serializedMenuBackground.getValue("image_path");

        return b;

    }

    @Override
    public SerializedMenuBackground serializedBackground(ImageMenuBackground background) {

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
