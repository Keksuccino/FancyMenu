package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.snow;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.rendering.ui.ContextMenuUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

public class SnowDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<SnowDecorationOverlay> {

    private static final String SNOW_COLOR_KEY = "snow_color_hex";
    private static final String SNOW_INTENSITY_KEY = "snow_intensity";
    private static final String SNOW_SCALE_KEY = "snow_scale";
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
        instanceToWrite.snowAccumulation = deserializeBoolean(instanceToWrite.snowAccumulation, deserializeFrom.getValue(SNOW_ACCUMULATION_KEY));

    }

    @Override
    protected void serialize(@NotNull SnowDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {

        serializeTo.putProperty(SNOW_COLOR_KEY, instanceToSerialize.snowColorHex);
        serializeTo.putProperty(SNOW_INTENSITY_KEY, instanceToSerialize.snowIntensity);
        serializeTo.putProperty(SNOW_SCALE_KEY, instanceToSerialize.snowScale);
        serializeTo.putProperty(SNOW_ACCUMULATION_KEY, instanceToSerialize.snowAccumulation);

    }

    @Override
    protected void buildConfigurationMenu(@NotNull SnowDecorationOverlay instance, @NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "accumulate_snow",
                        () -> instance.snowAccumulation,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            instance.snowAccumulation = aBoolean;
                        },
                        "fancymenu.decoration_overlays.snow.accumulate_snow")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.accumulate_snow.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "snow_color", Component.translatable("fancymenu.decoration_overlays.snow.color"),
                        () -> instance.snowColorHex,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.snowColorHex = s;
                        }, true,
                        "#FFFFFF", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.color.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "snow_intensity", Component.translatable("fancymenu.decoration_overlays.snow.intensity"),
                        () -> instance.snowIntensity,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.snowIntensity = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.intensity.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "snow_scale", Component.translatable("fancymenu.decoration_overlays.snow.scale"),
                        () -> instance.snowScale,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.snowScale = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.scale.desc")));

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
