package de.keksuccino.fancymenu.customization.background.backgrounds.image;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.SerializedMenuBackground;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.SerializationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
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

        b.textureSupplier = SerializationHelper.INSTANCE.deserializeImageResourceSupplier(serializedMenuBackground.getValue("image_path"));

        String slide = serializedMenuBackground.getValue("slide");
        if ((slide != null) && slide.equals("true")) {
            b.slideLeftRight = true;
        }

        b.fallbackTextureSupplier = SerializationHelper.INSTANCE.deserializeImageResourceSupplier(serializedMenuBackground.getValue("fallback_path"));
        if (b.fallbackTextureSupplier == null) {
            b.fallbackTextureSupplier = SerializationHelper.INSTANCE.deserializeImageResourceSupplier(serializedMenuBackground.getValue("web_image_fallback_path"));
        }

        b.repeat = SerializationHelper.INSTANCE.deserializeBoolean(b.repeat, serializedMenuBackground.getValue("repeat_texture"));

        b.parallaxEnabled = SerializationHelper.INSTANCE.deserializeBoolean(b.parallaxEnabled, serializedMenuBackground.getValue("parallax"));
        String parallaxIntensityX = serializedMenuBackground.getValue("parallax_intensity_x");
        String parallaxIntensityY = serializedMenuBackground.getValue("parallax_intensity_y");

        String legacyParallaxIntensity = null;
        if (parallaxIntensityX == null || parallaxIntensityY == null) {
            legacyParallaxIntensity = Objects.requireNonNullElse(serializedMenuBackground.getValue("parallax_intensity"), "0.02");
        }

        if (parallaxIntensityX == null && parallaxIntensityY == null) {
            parallaxIntensityX = legacyParallaxIntensity;
            parallaxIntensityY = legacyParallaxIntensity;
        } else {
            if (parallaxIntensityX == null) parallaxIntensityX = (parallaxIntensityY != null) ? parallaxIntensityY : legacyParallaxIntensity;
            if (parallaxIntensityY == null) parallaxIntensityY = (parallaxIntensityX != null) ? parallaxIntensityX : legacyParallaxIntensity;
        }

        b.parallaxIntensityXString = parallaxIntensityX;
        b.parallaxIntensityYString = parallaxIntensityY;
        b.invertParallax = SerializationHelper.INSTANCE.deserializeBoolean(b.invertParallax, serializedMenuBackground.getValue("invert_parallax"));

        b.restartAnimatedOnMenuLoad = SerializationHelper.INSTANCE.deserializeBoolean(false, serializedMenuBackground.getValue("restart_animated_on_menu_load"));

        return b;

    }

    @Override
    public SerializedMenuBackground serializeBackground(ImageMenuBackground background) {

        SerializedMenuBackground serialized = new SerializedMenuBackground();

        if (background.textureSupplier != null) {
            serialized.putProperty("image_path", background.textureSupplier.getSourceWithPrefix());
        }

        serialized.putProperty("slide", "" + background.slideLeftRight);

        serialized.putProperty("repeat_texture", "" + background.repeat);

        if (background.fallbackTextureSupplier != null) {
            serialized.putProperty("fallback_path", background.fallbackTextureSupplier.getSourceWithPrefix());
        }

        serialized.putProperty("parallax", "" + background.parallaxEnabled);
        serialized.putProperty("parallax_intensity_x", background.parallaxIntensityXString);
        serialized.putProperty("parallax_intensity_y", background.parallaxIntensityYString);
        serialized.putProperty("invert_parallax", "" + background.invertParallax);

        serialized.putProperty("restart_animated_on_menu_load", "" + background.restartAnimatedOnMenuLoad);

        return serialized;

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.backgrounds.image");
    }

    @Override
    public @Nullable Component[] getDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.backgrounds.image.desc");
    }

}
