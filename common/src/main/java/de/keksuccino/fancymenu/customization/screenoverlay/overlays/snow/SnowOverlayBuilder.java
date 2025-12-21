package de.keksuccino.fancymenu.customization.screenoverlay.overlays.snow;

import de.keksuccino.fancymenu.customization.screenoverlay.AbstractOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class SnowOverlayBuilder extends AbstractOverlayBuilder<SnowOverlay> {

    private static final String SNOW_COLOR_KEY = "snow_color_hex";
    private static final String SNOW_INTENSITY_KEY = "snow_intensity";
    private static final String SNOW_ACCUMULATION_KEY = "snow_accumulation";

    public SnowOverlayBuilder() {
        super("snowfall");
    }

    @Override
    public @NotNull SnowOverlay buildDefaultInstance() {
        return new SnowOverlay();
    }

    @Override
    protected void deserialize(@NotNull SnowOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {

        instanceToWrite.snowColorHex = Objects.requireNonNullElse(deserializeFrom.getValue(SNOW_COLOR_KEY), instanceToWrite.snowColorHex);
        instanceToWrite.snowIntensity = Objects.requireNonNullElse(deserializeFrom.getValue(SNOW_INTENSITY_KEY), instanceToWrite.snowIntensity);
        instanceToWrite.snowAccumulation = deserializeBoolean(instanceToWrite.snowAccumulation, deserializeFrom.getValue(SNOW_ACCUMULATION_KEY));

    }

    @Override
    protected void serialize(@NotNull SnowOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {

        serializeTo.putProperty(SNOW_COLOR_KEY, instanceToSerialize.snowColorHex);
        serializeTo.putProperty(SNOW_INTENSITY_KEY, instanceToSerialize.snowIntensity);
        serializeTo.putProperty(SNOW_ACCUMULATION_KEY, instanceToSerialize.snowAccumulation);

    }

}
