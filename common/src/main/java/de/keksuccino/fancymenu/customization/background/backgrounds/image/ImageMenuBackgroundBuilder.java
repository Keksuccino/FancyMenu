package de.keksuccino.fancymenu.customization.background.backgrounds.image;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

public class ImageMenuBackgroundBuilder extends MenuBackgroundBuilder<ImageMenuBackground> {

    public ImageMenuBackgroundBuilder() {
        super("image");
    }

    @Override
    public @NotNull ImageMenuBackground buildDefaultInstance() {
        return new ImageMenuBackground(this);
    }

    @Override
    public void deserializeBackground(@NotNull PropertyContainer serializedBackground, @NotNull ImageMenuBackground deserializeTo) {

        // Legacy support for old single-value parallax intensity
        String parallaxIntensityX = serializedBackground.getValue("parallax_intensity_x");
        String parallaxIntensityY = serializedBackground.getValue("parallax_intensity_y");
        if ((parallaxIntensityX == null) || (parallaxIntensityY == null)) {
            String legacyParallaxIntensity = Objects.requireNonNullElse(serializedBackground.getValue("parallax_intensity"), "0.02");
            deserializeTo.parallaxIntensityXString.setManualInput(legacyParallaxIntensity);
            deserializeTo.parallaxIntensityYString.setManualInput(legacyParallaxIntensity);
        }

    }

    @Override
    public void serializeBackground(@NotNull ImageMenuBackground background, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.backgrounds.image");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.backgrounds.image.desc");
    }

}
