package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.confetti;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.LegacyOverlayOptions;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfettiDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<ConfettiDecorationOverlay> {

    /** Saved by older versions: how often bursts spawn. Together with the burst density, it is now the Amount option. */
    static final String LEGACY_BURST_AMOUNT_KEY = "confetti_burst_amount";
    /** Saved by older versions: how many pieces a burst has. Together with the burst amount, it is now the Amount option. */
    static final String LEGACY_BURST_DENSITY_KEY = "confetti_burst_density";
    /** Saved by older versions: a free piece scale, now the Size option */
    static final String LEGACY_SCALE_KEY = "confetti_scale";
    /** Saved by older versions: true made confetti only burst from clicks, which is now the poppers switched off */
    static final String LEGACY_CLICK_MODE_KEY = "confetti_mouse_click_mode";

    public ConfettiDecorationOverlayBuilder() {
        super("confetti");
    }

    @Override
    public @NotNull ConfettiDecorationOverlay buildDefaultInstance() {
        return new ConfettiDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull ConfettiDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
        if (LegacyOverlayOptions.isMissing(deserializeFrom, instanceToWrite.amount.property.getKey())) {
            Float burstAmount = LegacyOverlayOptions.readFloat(deserializeFrom, LEGACY_BURST_AMOUNT_KEY);
            Float burstDensity = LegacyOverlayOptions.readFloat(deserializeFrom, LEGACY_BURST_DENSITY_KEY);
            if ((burstAmount != null) || (burstDensity != null)) {
                float total = ((burstAmount != null) ? burstAmount : 1.0F) * ((burstDensity != null) ? burstDensity : 1.0F);
                instanceToWrite.amount.property.set(LegacyOverlayOptions.toAmountPercent(total));
            }
        }
        LegacyOverlayOptions.migrateSize(deserializeFrom, instanceToWrite.size, LEGACY_SCALE_KEY);
        if (LegacyOverlayOptions.isMissing(deserializeFrom, instanceToWrite.confettiPoppers.getKey()) && "true".equalsIgnoreCase(deserializeFrom.getValue(LEGACY_CLICK_MODE_KEY))) {
            instanceToWrite.confettiPoppers.set(false);
        }
    }

    @Override
    protected void serialize(@NotNull ConfettiDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.confetti");
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

}
