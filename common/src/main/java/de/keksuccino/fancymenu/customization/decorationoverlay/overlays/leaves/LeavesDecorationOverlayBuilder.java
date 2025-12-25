package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.leaves;

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

public class LeavesDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<LeavesDecorationOverlay> {

    private static final String LEAVES_COLOR_START_KEY = "leaves_color_start_hex";
    private static final String LEAVES_COLOR_END_KEY = "leaves_color_end_hex";
    private static final String LEAVES_DENSITY_KEY = "leaves_density";
    private static final String LEAVES_WIND_INTENSITY_KEY = "leaves_wind_intensity";
    private static final String LEAVES_WIND_BLOWS_KEY = "leaves_wind_blows";
    private static final String LEAVES_FALL_SPEED_KEY = "leaves_fall_speed";
    private static final String LEAVES_SCALE_KEY = "leaves_scale";

    public LeavesDecorationOverlayBuilder() {
        super("leaves");
    }

    @Override
    public @NotNull LeavesDecorationOverlay buildDefaultInstance() {
        return new LeavesDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull LeavesDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {

        instanceToWrite.leavesColorStartHex = Objects.requireNonNullElse(deserializeFrom.getValue(LEAVES_COLOR_START_KEY), instanceToWrite.leavesColorStartHex);
        instanceToWrite.leavesColorEndHex = Objects.requireNonNullElse(deserializeFrom.getValue(LEAVES_COLOR_END_KEY), instanceToWrite.leavesColorEndHex);
        instanceToWrite.leavesDensity = Objects.requireNonNullElse(deserializeFrom.getValue(LEAVES_DENSITY_KEY), instanceToWrite.leavesDensity);
        instanceToWrite.leavesWindIntensity = Objects.requireNonNullElse(deserializeFrom.getValue(LEAVES_WIND_INTENSITY_KEY), instanceToWrite.leavesWindIntensity);
        instanceToWrite.leavesWindBlows = deserializeBoolean(instanceToWrite.leavesWindBlows, deserializeFrom.getValue(LEAVES_WIND_BLOWS_KEY));
        instanceToWrite.leavesFallSpeed = Objects.requireNonNullElse(deserializeFrom.getValue(LEAVES_FALL_SPEED_KEY), instanceToWrite.leavesFallSpeed);
        instanceToWrite.leavesScale = Objects.requireNonNullElse(deserializeFrom.getValue(LEAVES_SCALE_KEY), instanceToWrite.leavesScale);

    }

    @Override
    protected void serialize(@NotNull LeavesDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {

        serializeTo.putProperty(LEAVES_COLOR_START_KEY, instanceToSerialize.leavesColorStartHex);
        serializeTo.putProperty(LEAVES_COLOR_END_KEY, instanceToSerialize.leavesColorEndHex);
        serializeTo.putProperty(LEAVES_DENSITY_KEY, instanceToSerialize.leavesDensity);
        serializeTo.putProperty(LEAVES_WIND_INTENSITY_KEY, instanceToSerialize.leavesWindIntensity);
        serializeTo.putProperty(LEAVES_WIND_BLOWS_KEY, instanceToSerialize.leavesWindBlows);
        serializeTo.putProperty(LEAVES_FALL_SPEED_KEY, instanceToSerialize.leavesFallSpeed);
        serializeTo.putProperty(LEAVES_SCALE_KEY, instanceToSerialize.leavesScale);

    }

    @Override
    protected void buildConfigurationMenu(@NotNull LeavesDecorationOverlay instance, @NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "leaves_color_start", Component.translatable("fancymenu.decoration_overlays.leaves.color_start"),
                        () -> instance.leavesColorStartHex,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.leavesColorStartHex = s;
                        }, true,
                        "#7BA84F", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.color_start.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "leaves_color_end", Component.translatable("fancymenu.decoration_overlays.leaves.color_end"),
                        () -> instance.leavesColorEndHex,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.leavesColorEndHex = s;
                        }, true,
                        "#D58A3B", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.color_end.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "leaves_density", Component.translatable("fancymenu.decoration_overlays.leaves.density"),
                        () -> instance.leavesDensity,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.leavesDensity = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.density.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "leaves_wind_intensity", Component.translatable("fancymenu.decoration_overlays.leaves.wind"),
                        () -> instance.leavesWindIntensity,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.leavesWindIntensity = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.wind.desc")));

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "leaves_wind_blows",
                        () -> instance.leavesWindBlows,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            instance.leavesWindBlows = aBoolean;
                        },
                        "fancymenu.decoration_overlays.leaves.wind_blows")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.wind_blows.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "leaves_fall_speed", Component.translatable("fancymenu.decoration_overlays.leaves.fall_speed"),
                        () -> instance.leavesFallSpeed,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.leavesFallSpeed = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.fall_speed.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "leaves_scale", Component.translatable("fancymenu.decoration_overlays.leaves.scale"),
                        () -> instance.leavesScale,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.leavesScale = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.scale.desc")));

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.leaves");
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

}
