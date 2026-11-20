package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fireworks;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.LegacyOverlayOptions;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FireworksDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<FireworksDecorationOverlay> {

    /** Saved by older versions: a multiplier for how many rockets launch, now the Amount option */
    static final String LEGACY_AMOUNT_KEY = "fireworks_amount";
    /** Saved by older versions: a free scale of the whole show, now the Size option */
    static final String LEGACY_SCALE_KEY = "fireworks_scale";

    public FireworksDecorationOverlayBuilder() {
        super("fireworks");
    }

    @Override
    public @NotNull FireworksDecorationOverlay buildDefaultInstance() {
        return new FireworksDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull FireworksDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
        LegacyOverlayOptions.migrateAmount(deserializeFrom, instanceToWrite.amount, LEGACY_AMOUNT_KEY);
        LegacyOverlayOptions.migrateSize(deserializeFrom, instanceToWrite.size, LEGACY_SCALE_KEY);
    }

    @Override
    protected void serialize(@NotNull FireworksDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.fireworks");
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

}
