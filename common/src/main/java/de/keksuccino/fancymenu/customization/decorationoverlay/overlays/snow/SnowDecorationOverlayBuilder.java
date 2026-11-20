package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.snow;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.LegacyOverlayOptions;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SnowDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<SnowDecorationOverlay> {

    /** Saved by older versions: a multiplier for the number of flakes, now the Amount option */
    static final String LEGACY_INTENSITY_KEY = "snow_intensity";
    /** Saved by older versions: a free flake scale, now the Size option */
    static final String LEGACY_SCALE_KEY = "snow_scale";

    public SnowDecorationOverlayBuilder() {
        super("snowfall");
    }

    @Override
    public @NotNull SnowDecorationOverlay buildDefaultInstance() {
        return new SnowDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull SnowDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
        LegacyOverlayOptions.migrateAmount(deserializeFrom, instanceToWrite.amount, LEGACY_INTENSITY_KEY);
        LegacyOverlayOptions.migrateSize(deserializeFrom, instanceToWrite.size, LEGACY_SCALE_KEY);
    }

    @Override
    protected void serialize(@NotNull SnowDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
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
