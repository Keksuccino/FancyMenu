package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.firefly;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class FireflyDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<FireflyDecorationOverlay> {

    private static final String FIREFLY_INTENSITY_KEY = "firefly_intensity";
    private static final String FIREFLY_GROUP_DENSITY_KEY = "firefly_group_density";
    private static final String FIREFLY_GROUP_AMOUNT_KEY = "firefly_group_amount";
    private static final String FIREFLY_GROUP_SIZE_KEY = "firefly_group_size";
    private static final String FIREFLY_SCALE_KEY = "firefly_scale";
    private static final String FIREFLY_FOLLOW_MOUSE_KEY = "firefly_follow_mouse";
    private static final String FIREFLY_LANDING_KEY = "firefly_landing";

    public FireflyDecorationOverlayBuilder() {
        super("fireflies");
    }

    @Override
    public @NotNull FireflyDecorationOverlay buildDefaultInstance() {
        return new FireflyDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull FireflyDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {

        String legacyIntensity = deserializeFrom.getValue(FIREFLY_INTENSITY_KEY);
        String groupDensity = deserializeFrom.getValue(FIREFLY_GROUP_DENSITY_KEY);
        String groupAmount = deserializeFrom.getValue(FIREFLY_GROUP_AMOUNT_KEY);
        instanceToWrite.fireflyGroupDensity = Objects.requireNonNullElse(groupDensity, Objects.requireNonNullElse(legacyIntensity, instanceToWrite.fireflyGroupDensity));
        instanceToWrite.fireflyGroupAmount = Objects.requireNonNullElse(groupAmount, Objects.requireNonNullElse(legacyIntensity, instanceToWrite.fireflyGroupAmount));
        instanceToWrite.fireflyGroupSize = Objects.requireNonNullElse(deserializeFrom.getValue(FIREFLY_GROUP_SIZE_KEY), instanceToWrite.fireflyGroupSize);
        instanceToWrite.fireflyScale = Objects.requireNonNullElse(deserializeFrom.getValue(FIREFLY_SCALE_KEY), instanceToWrite.fireflyScale);
        instanceToWrite.fireflyFollowMouse = deserializeBoolean(instanceToWrite.fireflyFollowMouse, deserializeFrom.getValue(FIREFLY_FOLLOW_MOUSE_KEY));
        instanceToWrite.fireflyLanding = deserializeBoolean(instanceToWrite.fireflyLanding, deserializeFrom.getValue(FIREFLY_LANDING_KEY));

    }

    @Override
    protected void serialize(@NotNull FireflyDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {

        serializeTo.putProperty(FIREFLY_GROUP_DENSITY_KEY, instanceToSerialize.fireflyGroupDensity);
        serializeTo.putProperty(FIREFLY_GROUP_AMOUNT_KEY, instanceToSerialize.fireflyGroupAmount);
        serializeTo.putProperty(FIREFLY_GROUP_SIZE_KEY, instanceToSerialize.fireflyGroupSize);
        serializeTo.putProperty(FIREFLY_SCALE_KEY, instanceToSerialize.fireflyScale);
        serializeTo.putProperty(FIREFLY_FOLLOW_MOUSE_KEY, instanceToSerialize.fireflyFollowMouse);
        serializeTo.putProperty(FIREFLY_LANDING_KEY, instanceToSerialize.fireflyLanding);

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.fireflies");
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

}
