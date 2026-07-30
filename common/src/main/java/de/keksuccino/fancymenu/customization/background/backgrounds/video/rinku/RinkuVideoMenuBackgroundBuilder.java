package de.keksuccino.fancymenu.customization.background.backgrounds.video.rinku;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

public class RinkuVideoMenuBackgroundBuilder extends MenuBackgroundBuilder<RinkuVideoMenuBackground> {

    public RinkuVideoMenuBackgroundBuilder() {
        super("video_rinku");
    }

    @Override
    public boolean isDeprecated() {
        return true;
    }

    @Override
    public @NotNull RinkuVideoMenuBackground buildDefaultInstance() {
        return new RinkuVideoMenuBackground(this);
    }

    @Override
    public void deserializeBackground(@NotNull PropertyContainer serializedBackground, @NotNull RinkuVideoMenuBackground deserializeTo) {

        // Legacy support for old single-value parallax intensity
        String parallaxIntensityX = serializedBackground.getValue("parallax_intensity_x");
        String parallaxIntensityY = serializedBackground.getValue("parallax_intensity_y");
        if ((parallaxIntensityX == null) || (parallaxIntensityY == null)) {
            String legacyParallaxIntensity = Objects.requireNonNullElse(serializedBackground.getValue("parallax_intensity"), "0.02");
            deserializeTo.parallaxIntensityXString.setManualInput(legacyParallaxIntensity);
            deserializeTo.parallaxIntensityYString.setManualInput(legacyParallaxIntensity);
        }

        // Fix for unknown sound channels
        String soundSource = deserializeTo.soundSource.getString();
        if ((soundSource != null) && (RinkuVideoMenuBackground.getSoundSourceByName(soundSource) == null)) {
            deserializeTo.soundSource.set(SoundSource.MASTER.getName());
        }

    }

    @Override
    public void serializeBackground(@NotNull RinkuVideoMenuBackground background, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.backgrounds.video_rinku");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.backgrounds.video_rinku.desc");
    }

}
