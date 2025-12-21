package de.keksuccino.fancymenu.customization.screenoverlay.overlays.snow;

import de.keksuccino.fancymenu.customization.screenoverlay.AbstractOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.rendering.ui.ContextMenuUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @Override
    protected void buildConfigurationMenu(@NotNull SnowOverlay instance, @NotNull ContextMenu menu) {

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "accumulate_snow",
                        () -> instance.snowAccumulation,
                        aBoolean -> instance.snowAccumulation = aBoolean,
                        "fancymenu.screen_overlays.snow.accumulate_snow")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.screen_overlays.snow.accumulate_snow.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "snow_color", Component.translatable("fancymenu.screen_overlays.snow.color"),
                        () -> instance.snowColorHex,
                        s -> instance.snowColorHex = s, true,
                        "#FFFFFF", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.screen_overlays.snow.color.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "snow_intensity", Component.translatable("fancymenu.screen_overlays.snow.intensity"),
                        () -> instance.snowIntensity,
                        s -> instance.snowIntensity = s, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.screen_overlays.snow.intensity.desc")));

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.screen_overlays.snow");
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

}
