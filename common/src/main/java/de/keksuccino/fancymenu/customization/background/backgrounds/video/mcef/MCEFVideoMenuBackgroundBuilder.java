package de.keksuccino.fancymenu.customization.background.backgrounds.video.mcef;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.SerializedMenuBackground;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.SerializationHelper;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

public class MCEFVideoMenuBackgroundBuilder extends MenuBackgroundBuilder<MCEFVideoMenuBackground> {

    public MCEFVideoMenuBackgroundBuilder() {
        super("video_mcef");
    }

    @Override
    public void buildNewOrEditInstance(Screen currentScreen, @Nullable MCEFVideoMenuBackground backgroundToEdit, @NotNull Consumer<MCEFVideoMenuBackground> backgroundConsumer) {
        MCEFVideoMenuBackground back = (backgroundToEdit != null) ? (MCEFVideoMenuBackground) backgroundToEdit.copy() : null;
        if (back == null) {
            back = new MCEFVideoMenuBackground(this);
        }
        MCEFVideoMenuBackgroundConfigScreen s = new MCEFVideoMenuBackgroundConfigScreen(back, background -> {
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
    public MCEFVideoMenuBackground deserializeBackground(SerializedMenuBackground serialized) {

        MCEFVideoMenuBackground background = new MCEFVideoMenuBackground(this);

        String source = serialized.getValue("source");
        background.rawVideoUrlSource = (source != null) ? ResourceSource.of(source) : null;
        background.loop = SerializationHelper.INSTANCE.deserializeBoolean(background.loop, serialized.getValue("loop"));
        background.volume = SerializationHelper.INSTANCE.deserializeNumber(Float.class, background.volume, serialized.getValue("volume"));
        String soundSource = serialized.getValue("sound_source");
        if (soundSource != null) background.soundSource = Objects.requireNonNullElse(getSoundSourceByName(soundSource), SoundSource.MASTER);

        background.parallaxEnabled = SerializationHelper.INSTANCE.deserializeBoolean(background.parallaxEnabled, serialized.getValue("parallax"));
        String parallaxIntensityX = serialized.getValue("parallax_intensity_x");
        String parallaxIntensityY = serialized.getValue("parallax_intensity_y");

        String legacyParallaxIntensity = null;
        if (parallaxIntensityX == null || parallaxIntensityY == null) {
            legacyParallaxIntensity = Objects.requireNonNullElse(serialized.getValue("parallax_intensity"), "0.02");
        }

        if (parallaxIntensityX == null && parallaxIntensityY == null) {
            parallaxIntensityX = legacyParallaxIntensity;
            parallaxIntensityY = legacyParallaxIntensity;
        } else {
            if (parallaxIntensityX == null) parallaxIntensityX = (parallaxIntensityY != null) ? parallaxIntensityY : legacyParallaxIntensity;
            if (parallaxIntensityY == null) parallaxIntensityY = (parallaxIntensityX != null) ? parallaxIntensityX : legacyParallaxIntensity;
        }

        background.parallaxIntensityXString = parallaxIntensityX;
        background.parallaxIntensityYString = parallaxIntensityY;
        background.invertParallax = SerializationHelper.INSTANCE.deserializeBoolean(background.invertParallax, serialized.getValue("invert_parallax"));

        return background;

    }

    @Override
    public SerializedMenuBackground serializeBackground(MCEFVideoMenuBackground background) {

        SerializedMenuBackground serialized = new SerializedMenuBackground();

        if (background.rawVideoUrlSource != null) {
            serialized.putProperty("source", background.rawVideoUrlSource.getSerializationSource());
        }
        serialized.putProperty("loop", "" + background.loop);
        serialized.putProperty("volume", "" + background.volume);
        serialized.putProperty("sound_source", background.soundSource.getName());

        serialized.putProperty("parallax", "" + background.parallaxEnabled);
        serialized.putProperty("parallax_intensity_x", background.parallaxIntensityXString);
        serialized.putProperty("parallax_intensity_y", background.parallaxIntensityYString);
        serialized.putProperty("invert_parallax", "" + background.invertParallax);

        return serialized;

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.backgrounds.video_mcef");
    }

    @Override
    public @Nullable Component[] getDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.backgrounds.video_mcef.desc");
    }

    @Nullable
    protected static SoundSource getSoundSourceByName(@NotNull String name) {
        for (SoundSource source : SoundSource.values()) {
            if (source.getName().equals(name)) return source;
        }
        return null;
    }

}
