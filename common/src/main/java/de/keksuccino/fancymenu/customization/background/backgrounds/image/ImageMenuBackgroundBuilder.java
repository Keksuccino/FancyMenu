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
        ImageMenuBackgroundConfigScreen s = new ImageMenuBackgroundConfigScreen(back, background -> {
           if (background != null) {
               backgroundConsumer.accept(background);
           } else {
               backgroundConsumer.accept(backgroundToEdit);
           }
           Minecraft.getInstance().setScreen(currentScreen);
        });
        Minecraft.getInstance().setScreen(s);
    }

    @Override
    public ImageMenuBackground deserializeBackground(SerializedMenuBackground serializedMenuBackground) {

        ImageMenuBackground b = new ImageMenuBackground(this);

        b.imagePathOrUrl = serializedMenuBackground.getValue("image_path");

        String slide = serializedMenuBackground.getValue("slide");
        if ((slide != null) && slide.equals("true")) {
            b.slideLeftRight = true;
        }

        String type = serializedMenuBackground.getValue("background_image_type");
        if (type != null) {
            ImageMenuBackground.BackgroundImageType imgType = ImageMenuBackground.BackgroundImageType.getByName(type);
            if (imgType != null) b.type = imgType;
        }

        b.webImageFallbackPath = serializedMenuBackground.getValue("web_image_fallback_path");

        return b;

    }

    @Override
    public SerializedMenuBackground serializedBackground(ImageMenuBackground background) {

        SerializedMenuBackground serialized = new SerializedMenuBackground();

        serialized.putProperty("image_path", background.imagePathOrUrl);

        serialized.putProperty("background_image_type", background.type.getName());

        serialized.putProperty("slide", "" + background.slideLeftRight);

        serialized.putProperty("web_image_fallback_path", background.webImageFallbackPath);

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
