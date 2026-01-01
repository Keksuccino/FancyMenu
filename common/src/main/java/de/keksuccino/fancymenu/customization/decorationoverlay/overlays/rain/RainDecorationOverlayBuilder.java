package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class RainDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<RainDecorationOverlay> {

    private static final String RAIN_COLOR_KEY = "rain_color_hex";
    private static final String RAIN_INTENSITY_KEY = "rain_intensity";
    private static final String RAIN_SCALE_KEY = "rain_scale";
    private static final String RAIN_PUDDLES_KEY = "rain_puddles";
    private static final String RAIN_DRIPS_KEY = "rain_drips";
    private static final String RAIN_THUNDER_KEY = "rain_thunder";
    private static final String RAIN_THUNDER_BRIGHTNESS_KEY = "rain_thunder_brightness";

    public RainDecorationOverlayBuilder() {
        super("rainfall");
    }

    @Override
    public @NotNull RainDecorationOverlay buildDefaultInstance() {
        return new RainDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull RainDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {

        instanceToWrite.rainColorHex = Objects.requireNonNullElse(deserializeFrom.getValue(RAIN_COLOR_KEY), instanceToWrite.rainColorHex);
        instanceToWrite.rainIntensity = Objects.requireNonNullElse(deserializeFrom.getValue(RAIN_INTENSITY_KEY), instanceToWrite.rainIntensity);
        instanceToWrite.rainScale = Objects.requireNonNullElse(deserializeFrom.getValue(RAIN_SCALE_KEY), instanceToWrite.rainScale);
        instanceToWrite.rainThunderBrightness = Objects.requireNonNullElse(deserializeFrom.getValue(RAIN_THUNDER_BRIGHTNESS_KEY), instanceToWrite.rainThunderBrightness);
        instanceToWrite.rainPuddles = deserializeBoolean(instanceToWrite.rainPuddles, deserializeFrom.getValue(RAIN_PUDDLES_KEY));
        instanceToWrite.rainDrips = deserializeBoolean(instanceToWrite.rainDrips, deserializeFrom.getValue(RAIN_DRIPS_KEY));
        instanceToWrite.rainThunder = deserializeBoolean(instanceToWrite.rainThunder, deserializeFrom.getValue(RAIN_THUNDER_KEY));

    }

    @Override
    protected void serialize(@NotNull RainDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {

        serializeTo.putProperty(RAIN_COLOR_KEY, instanceToSerialize.rainColorHex);
        serializeTo.putProperty(RAIN_INTENSITY_KEY, instanceToSerialize.rainIntensity);
        serializeTo.putProperty(RAIN_SCALE_KEY, instanceToSerialize.rainScale);
        serializeTo.putProperty(RAIN_THUNDER_BRIGHTNESS_KEY, instanceToSerialize.rainThunderBrightness);
        serializeTo.putProperty(RAIN_PUDDLES_KEY, instanceToSerialize.rainPuddles);
        serializeTo.putProperty(RAIN_DRIPS_KEY, instanceToSerialize.rainDrips);
        serializeTo.putProperty(RAIN_THUNDER_KEY, instanceToSerialize.rainThunder);

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.rain");
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

}
