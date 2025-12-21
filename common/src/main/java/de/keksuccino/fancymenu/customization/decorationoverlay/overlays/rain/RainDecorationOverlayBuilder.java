package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain;

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

public class RainDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<RainDecorationOverlay> {

    private static final String RAIN_COLOR_KEY = "rain_color_hex";
    private static final String RAIN_INTENSITY_KEY = "rain_intensity";
    private static final String RAIN_PUDDLES_KEY = "rain_puddles";
    private static final String RAIN_DRIPS_KEY = "rain_drips";

    public RainDecorationOverlayBuilder() {
        super("rainfall");
    }

    @Override
    public @NotNull RainDecorationOverlay buildDefaultInstance() {
        return new RainDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull RainDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {

        instanceToWrite.rainColorHex = Objects.requireNonNullElse(deserializeFrom.getValue(RAIN_COLOR_KEY), instanceToWrite.rainColorHex);
        instanceToWrite.rainIntensity = Objects.requireNonNullElse(deserializeFrom.getValue(RAIN_INTENSITY_KEY), instanceToWrite.rainIntensity);
        instanceToWrite.rainPuddles = deserializeBoolean(instanceToWrite.rainPuddles, deserializeFrom.getValue(RAIN_PUDDLES_KEY));
        instanceToWrite.rainDrips = deserializeBoolean(instanceToWrite.rainDrips, deserializeFrom.getValue(RAIN_DRIPS_KEY));

    }

    @Override
    protected void serialize(@NotNull RainDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {

        serializeTo.putProperty(RAIN_COLOR_KEY, instanceToSerialize.rainColorHex);
        serializeTo.putProperty(RAIN_INTENSITY_KEY, instanceToSerialize.rainIntensity);
        serializeTo.putProperty(RAIN_PUDDLES_KEY, instanceToSerialize.rainPuddles);
        serializeTo.putProperty(RAIN_DRIPS_KEY, instanceToSerialize.rainDrips);

    }

    @Override
    protected void buildConfigurationMenu(@NotNull RainDecorationOverlay instance, @NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "rain_puddles",
                        () -> instance.rainPuddles,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            instance.rainPuddles = aBoolean;
                        },
                        "fancymenu.decoration_overlays.rain.puddles")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.puddles.desc")));

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "rain_drips",
                        () -> instance.rainDrips,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            instance.rainDrips = aBoolean;
                        },
                        "fancymenu.decoration_overlays.rain.drips")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.drips.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "rain_color", Component.translatable("fancymenu.decoration_overlays.rain.color"),
                        () -> instance.rainColorHex,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.rainColorHex = s;
                        }, true,
                        "#CFE7FF", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.color.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "rain_intensity", Component.translatable("fancymenu.decoration_overlays.rain.intensity"),
                        () -> instance.rainIntensity,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.rainIntensity = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.intensity.desc")));

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
