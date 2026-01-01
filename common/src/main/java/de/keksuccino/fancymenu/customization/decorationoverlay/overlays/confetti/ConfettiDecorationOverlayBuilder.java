package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.confetti;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

public class ConfettiDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<ConfettiDecorationOverlay> {

    private static final String CONFETTI_SCALE_KEY = "confetti_scale";
    private static final String CONFETTI_FALL_SPEED_KEY = "confetti_fall_speed";
    private static final String CONFETTI_DENSITY_KEY = "confetti_burst_density";
    private static final String CONFETTI_AMOUNT_KEY = "confetti_burst_amount";
    private static final String CONFETTI_PARTICLE_CAP_KEY = "confetti_particle_cap";
    private static final String CONFETTI_COLOR_MIX_KEY = "confetti_color_mix_mode";
    private static final String CONFETTI_MOUSE_CLICK_KEY = "confetti_mouse_click_mode";

    public ConfettiDecorationOverlayBuilder() {
        super("confetti");
    }

    @Override
    public @NotNull ConfettiDecorationOverlay buildDefaultInstance() {
        return new ConfettiDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull ConfettiDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {

        instanceToWrite.confettiScale = Objects.requireNonNullElse(deserializeFrom.getValue(CONFETTI_SCALE_KEY), instanceToWrite.confettiScale);
        instanceToWrite.confettiFallSpeed = Objects.requireNonNullElse(deserializeFrom.getValue(CONFETTI_FALL_SPEED_KEY), instanceToWrite.confettiFallSpeed);
        instanceToWrite.confettiBurstDensity = Objects.requireNonNullElse(deserializeFrom.getValue(CONFETTI_DENSITY_KEY), instanceToWrite.confettiBurstDensity);
        instanceToWrite.confettiBurstAmount = Objects.requireNonNullElse(deserializeFrom.getValue(CONFETTI_AMOUNT_KEY), instanceToWrite.confettiBurstAmount);
        instanceToWrite.confettiParticleCap = Objects.requireNonNullElse(deserializeFrom.getValue(CONFETTI_PARTICLE_CAP_KEY), instanceToWrite.confettiParticleCap);
        instanceToWrite.confettiColorMixMode = deserializeBoolean(instanceToWrite.confettiColorMixMode, deserializeFrom.getValue(CONFETTI_COLOR_MIX_KEY));
        instanceToWrite.confettiMouseClickMode = deserializeBoolean(instanceToWrite.confettiMouseClickMode, deserializeFrom.getValue(CONFETTI_MOUSE_CLICK_KEY));

    }

    @Override
    protected void serialize(@NotNull ConfettiDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {

        serializeTo.putProperty(CONFETTI_SCALE_KEY, instanceToSerialize.confettiScale);
        serializeTo.putProperty(CONFETTI_FALL_SPEED_KEY, instanceToSerialize.confettiFallSpeed);
        serializeTo.putProperty(CONFETTI_DENSITY_KEY, instanceToSerialize.confettiBurstDensity);
        serializeTo.putProperty(CONFETTI_AMOUNT_KEY, instanceToSerialize.confettiBurstAmount);
        serializeTo.putProperty(CONFETTI_PARTICLE_CAP_KEY, instanceToSerialize.confettiParticleCap);
        serializeTo.putProperty(CONFETTI_COLOR_MIX_KEY, instanceToSerialize.confettiColorMixMode);
        serializeTo.putProperty(CONFETTI_MOUSE_CLICK_KEY, instanceToSerialize.confettiMouseClickMode);

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
