package de.keksuccino.fancymenu.customization.background.backgrounds.video.mcef;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.SerializedMenuBackground;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.SerializationUtils;
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
        background.loop = SerializationUtils.deserializeBoolean(background.loop, serialized.getValue("loop"));
        background.volume = SerializationUtils.deserializeNumber(Float.class, background.volume, serialized.getValue("volume"));
        String soundSource = serialized.getValue("sound_source");
        if (soundSource != null) background.soundSource = Objects.requireNonNullElse(getSoundSourceByName(soundSource), SoundSource.MASTER);

        background.parallaxEnabled = SerializationUtils.deserializeBoolean(background.parallaxEnabled, serialized.getValue("parallax"));
        background.parallaxIntensity = SerializationUtils.deserializeNumber(Float.class, background.parallaxIntensity, serialized.getValue("parallax_intensity"));
        background.invertParallax = SerializationUtils.deserializeBoolean(background.invertParallax, serialized.getValue("invert_parallax"));

        return background;

    }

    @Override
    public SerializedMenuBackground serializedBackground(MCEFVideoMenuBackground background) {

        SerializedMenuBackground serialized = new SerializedMenuBackground();

        if (background.rawVideoUrlSource != null) {
            serialized.putProperty("source", background.rawVideoUrlSource.getSerializationSource());
        }
        serialized.putProperty("loop", "" + background.loop);
        serialized.putProperty("volume", "" + background.volume);
        serialized.putProperty("sound_source", background.soundSource.getName());

        serialized.putProperty("parallax", "" + background.parallaxEnabled);
        serialized.putProperty("parallax_intensity", "" + background.parallaxIntensity);
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
