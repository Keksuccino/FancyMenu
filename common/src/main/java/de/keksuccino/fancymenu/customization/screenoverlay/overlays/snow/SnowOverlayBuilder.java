package de.keksuccino.fancymenu.customization.screenoverlay.overlays.snow;

import de.keksuccino.fancymenu.customization.screenoverlay.AbstractOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;

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
    public @NotNull SnowOverlay deserializeFromProperties(@NotNull PropertyContainer container) {

        var instance = buildDefaultInstance();

        instance;

        return instance;

    }

    @Override
    public void serializeToProperties(@NotNull SnowOverlay instance, @NotNull PropertyContainer container) {

    }

}
