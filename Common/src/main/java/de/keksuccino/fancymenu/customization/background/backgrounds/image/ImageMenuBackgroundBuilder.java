package de.keksuccino.fancymenu.customization.background.backgrounds.image;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.SerializedMenuBackground;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ImageMenuBackgroundBuilder extends MenuBackgroundBuilder<ImageMenuBackground> {

    public ImageMenuBackgroundBuilder() {
        super("image");
    }

    @Override
    public void buildNewOrEditInstance(Screen currentScreen, @Nullable ImageMenuBackground backgroundToEdit, @NotNull Consumer<ImageMenuBackground> backgroundConsumer) {
        ImageMenuBackground back = (backgroundToEdit != null) ? (ImageMenuBackground) backgroundToEdit.copy() : null;
        if (back == null) {
            back = new ImageMenuBackground(this);
        }
        ImageMenuBackgroundConfigScreen s = new ImageMenuBackgroundConfigScreen(currentScreen, back, (call) -> {
            if (call != null) {
                backgroundConsumer.accept(call);
            } else {
                backgroundConsumer.accept(backgroundToEdit);
            }
        });
        Minecraft.getInstance().setScreen(s);
    }

    @Override
    public ImageMenuBackground deserializeBackground(SerializedMenuBackground serializedMenuBackground) {

        ImageMenuBackground b = new ImageMenuBackground(this);

        b.imagePath = serializedMenuBackground.getValue("image_path");

        String slide = serializedMenuBackground.getValue("slide");
        if ((slide != null) && slide.equals("true")) {
            b.slideLeftRight = true;
        }

        return b;

    }

    @Override
    public SerializedMenuBackground serializedBackground(ImageMenuBackground background) {

        SerializedMenuBackground serialized = new SerializedMenuBackground();

        if (background.imagePath != null) {
            serialized.putProperty("image_path", background.imagePath);
        }

        serialized.putProperty("slide", "" + background.slideLeftRight);

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
