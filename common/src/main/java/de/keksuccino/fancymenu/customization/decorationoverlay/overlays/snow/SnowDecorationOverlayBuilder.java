package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.snow;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

public class SnowDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<SnowDecorationOverlay> {

    private static final String SNOW_COLOR_KEY = "snow_color_hex";
    private static final String SNOW_INTENSITY_KEY = "snow_intensity";
    private static final String SNOW_SCALE_KEY = "snow_scale";
    private static final String SNOW_SPEED_KEY = "snow_speed";
    private static final String SNOW_ACCUMULATION_KEY = "snow_accumulation";

    public SnowDecorationOverlayBuilder() {
        super("snowfall");
    }

    @Override
    public @NotNull SnowDecorationOverlay buildDefaultInstance() {
        return new SnowDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull SnowDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {

        instanceToWrite.snowColorHex = Objects.requireNonNullElse(deserializeFrom.getValue(SNOW_COLOR_KEY), instanceToWrite.snowColorHex);
        instanceToWrite.snowIntensity = Objects.requireNonNullElse(deserializeFrom.getValue(SNOW_INTENSITY_KEY), instanceToWrite.snowIntensity);
        instanceToWrite.snowScale = Objects.requireNonNullElse(deserializeFrom.getValue(SNOW_SCALE_KEY), instanceToWrite.snowScale);
        instanceToWrite.snowSpeed = Objects.requireNonNullElse(deserializeFrom.getValue(SNOW_SPEED_KEY), instanceToWrite.snowSpeed);
        instanceToWrite.snowAccumulation = deserializeBoolean(instanceToWrite.snowAccumulation, deserializeFrom.getValue(SNOW_ACCUMULATION_KEY));

    }

    @Override
    protected void serialize(@NotNull SnowDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {

        serializeTo.putProperty(SNOW_COLOR_KEY, instanceToSerialize.snowColorHex);
        serializeTo.putProperty(SNOW_INTENSITY_KEY, instanceToSerialize.snowIntensity);
        serializeTo.putProperty(SNOW_SCALE_KEY, instanceToSerialize.snowScale);
        serializeTo.putProperty(SNOW_SPEED_KEY, instanceToSerialize.snowSpeed);
        serializeTo.putProperty(SNOW_ACCUMULATION_KEY, instanceToSerialize.snowAccumulation);

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.snow");
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

}
