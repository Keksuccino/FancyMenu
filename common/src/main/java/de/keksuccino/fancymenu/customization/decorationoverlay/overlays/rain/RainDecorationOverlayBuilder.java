package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.LegacyOverlayOptions;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RainDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<RainDecorationOverlay> {

    /** Saved by older versions: a multiplier for the number of drops, now the Amount option */
    static final String LEGACY_INTENSITY_KEY = "rain_intensity";
    /** Saved by older versions: a free drop scale, now the Size option */
    static final String LEGACY_SCALE_KEY = "rain_scale";

    public RainDecorationOverlayBuilder() {
        super("rainfall");
    }

    @Override
    public @NotNull RainDecorationOverlay buildDefaultInstance() {
        return new RainDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull RainDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
        LegacyOverlayOptions.migrateAmount(deserializeFrom, instanceToWrite.amount, LEGACY_INTENSITY_KEY);
        LegacyOverlayOptions.migrateSize(deserializeFrom, instanceToWrite.size, LEGACY_SCALE_KEY);
    }

    @Override
    protected void serialize(@NotNull RainDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
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
