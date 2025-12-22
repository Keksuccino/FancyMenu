package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights;

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

public class StringLightsDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<StringLightsDecorationOverlay> {

    private static final String LIGHTS_COLOR_KEY = "string_lights_color_hex";
    private static final String LIGHTS_SCALE_KEY = "string_lights_scale";
    private static final String LIGHTS_WIND_STRENGTH_KEY = "string_lights_wind_strength";
    private static final String LIGHTS_FLICKER_SPEED_KEY = "string_lights_flicker_speed";
    private static final String LIGHTS_CHRISTMAS_MODE_KEY = "string_lights_christmas_mode";
    private static final String LIGHTS_LEFT_CENTER_TO_TOP_CENTER_KEY = "string_lights_left_center_to_top_center";
    private static final String LIGHTS_RIGHT_CENTER_TO_TOP_CENTER_KEY = "string_lights_right_center_to_top_center";
    private static final String LIGHTS_BOTTOM_LEFT_TO_TOP_CENTER_KEY = "string_lights_bottom_left_to_top_center";
    private static final String LIGHTS_TOP_LEFT_TO_TOP_RIGHT_KEY = "string_lights_top_left_to_top_right";
    private static final String LIGHTS_BOTTOM_LEFT_TO_BOTTOM_RIGHT_KEY = "string_lights_bottom_left_to_bottom_right";
    private static final String LIGHTS_LOOSE_LEFT_TOP_KEY = "string_lights_loose_left_top";
    private static final String LIGHTS_LOOSE_RIGHT_TOP_KEY = "string_lights_loose_right_top";

    public StringLightsDecorationOverlayBuilder() {
        super("string_lights");
    }

    @Override
    public @NotNull StringLightsDecorationOverlay buildDefaultInstance() {
        return new StringLightsDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull StringLightsDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {

        instanceToWrite.stringLightsColorHex = Objects.requireNonNullElse(deserializeFrom.getValue(LIGHTS_COLOR_KEY), instanceToWrite.stringLightsColorHex);
        instanceToWrite.stringLightsScale = Objects.requireNonNullElse(deserializeFrom.getValue(LIGHTS_SCALE_KEY), instanceToWrite.stringLightsScale);
        instanceToWrite.stringLightsWindStrength = Objects.requireNonNullElse(deserializeFrom.getValue(LIGHTS_WIND_STRENGTH_KEY), instanceToWrite.stringLightsWindStrength);
        instanceToWrite.stringLightsFlickerSpeed = Objects.requireNonNullElse(deserializeFrom.getValue(LIGHTS_FLICKER_SPEED_KEY), instanceToWrite.stringLightsFlickerSpeed);
        instanceToWrite.stringLightsChristmasMode = deserializeBoolean(instanceToWrite.stringLightsChristmasMode, deserializeFrom.getValue(LIGHTS_CHRISTMAS_MODE_KEY));
        instanceToWrite.stringLightsLeftCenterToTopCenter = deserializeBoolean(instanceToWrite.stringLightsLeftCenterToTopCenter, deserializeFrom.getValue(LIGHTS_LEFT_CENTER_TO_TOP_CENTER_KEY));
        instanceToWrite.stringLightsRightCenterToTopCenter = deserializeBoolean(instanceToWrite.stringLightsRightCenterToTopCenter, deserializeFrom.getValue(LIGHTS_RIGHT_CENTER_TO_TOP_CENTER_KEY));
        instanceToWrite.stringLightsBottomLeftToTopCenter = deserializeBoolean(instanceToWrite.stringLightsBottomLeftToTopCenter, deserializeFrom.getValue(LIGHTS_BOTTOM_LEFT_TO_TOP_CENTER_KEY));
        instanceToWrite.stringLightsTopLeftToTopRight = deserializeBoolean(instanceToWrite.stringLightsTopLeftToTopRight, deserializeFrom.getValue(LIGHTS_TOP_LEFT_TO_TOP_RIGHT_KEY));
        instanceToWrite.stringLightsBottomLeftToBottomRight = deserializeBoolean(instanceToWrite.stringLightsBottomLeftToBottomRight, deserializeFrom.getValue(LIGHTS_BOTTOM_LEFT_TO_BOTTOM_RIGHT_KEY));
        instanceToWrite.stringLightsLooseLeftTop = deserializeBoolean(instanceToWrite.stringLightsLooseLeftTop, deserializeFrom.getValue(LIGHTS_LOOSE_LEFT_TOP_KEY));
        instanceToWrite.stringLightsLooseRightTop = deserializeBoolean(instanceToWrite.stringLightsLooseRightTop, deserializeFrom.getValue(LIGHTS_LOOSE_RIGHT_TOP_KEY));

    }

    @Override
    protected void serialize(@NotNull StringLightsDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {

        serializeTo.putProperty(LIGHTS_COLOR_KEY, instanceToSerialize.stringLightsColorHex);
        serializeTo.putProperty(LIGHTS_SCALE_KEY, instanceToSerialize.stringLightsScale);
        serializeTo.putProperty(LIGHTS_WIND_STRENGTH_KEY, instanceToSerialize.stringLightsWindStrength);
        serializeTo.putProperty(LIGHTS_FLICKER_SPEED_KEY, instanceToSerialize.stringLightsFlickerSpeed);
        serializeTo.putProperty(LIGHTS_CHRISTMAS_MODE_KEY, instanceToSerialize.stringLightsChristmasMode);
        serializeTo.putProperty(LIGHTS_LEFT_CENTER_TO_TOP_CENTER_KEY, instanceToSerialize.stringLightsLeftCenterToTopCenter);
        serializeTo.putProperty(LIGHTS_RIGHT_CENTER_TO_TOP_CENTER_KEY, instanceToSerialize.stringLightsRightCenterToTopCenter);
        serializeTo.putProperty(LIGHTS_BOTTOM_LEFT_TO_TOP_CENTER_KEY, instanceToSerialize.stringLightsBottomLeftToTopCenter);
        serializeTo.putProperty(LIGHTS_TOP_LEFT_TO_TOP_RIGHT_KEY, instanceToSerialize.stringLightsTopLeftToTopRight);
        serializeTo.putProperty(LIGHTS_BOTTOM_LEFT_TO_BOTTOM_RIGHT_KEY, instanceToSerialize.stringLightsBottomLeftToBottomRight);
        serializeTo.putProperty(LIGHTS_LOOSE_LEFT_TOP_KEY, instanceToSerialize.stringLightsLooseLeftTop);
        serializeTo.putProperty(LIGHTS_LOOSE_RIGHT_TOP_KEY, instanceToSerialize.stringLightsLooseRightTop);

    }

    @Override
    protected void buildConfigurationMenu(@NotNull StringLightsDecorationOverlay instance, @NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "string_lights_christmas_mode",
                        () -> instance.stringLightsChristmasMode,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            instance.stringLightsChristmasMode = aBoolean;
                        },
                        "fancymenu.decoration_overlays.string_lights.christmas_mode")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.christmas_mode.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "string_lights_color", Component.translatable("fancymenu.decoration_overlays.string_lights.color"),
                        () -> instance.stringLightsColorHex,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.stringLightsColorHex = s;
                        }, true,
                        "#FFD27A", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.color.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "string_lights_scale", Component.translatable("fancymenu.decoration_overlays.string_lights.scale"),
                        () -> instance.stringLightsScale,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.stringLightsScale = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.scale.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "string_lights_wind_strength", Component.translatable("fancymenu.decoration_overlays.string_lights.wind_strength"),
                        () -> instance.stringLightsWindStrength,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.stringLightsWindStrength = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.wind_strength.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "string_lights_flicker_speed", Component.translatable("fancymenu.decoration_overlays.string_lights.flicker_speed"),
                        () -> instance.stringLightsFlickerSpeed,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.stringLightsFlickerSpeed = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.flicker_speed.desc")));

        menu.addSeparatorEntry("separator_before_string_light_positions");

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "string_lights_left_center_to_top_center",
                        () -> instance.stringLightsLeftCenterToTopCenter,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            instance.stringLightsLeftCenterToTopCenter = aBoolean;
                        },
                        "fancymenu.decoration_overlays.string_lights.position.left_center_to_top_center")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.position.left_center_to_top_center.desc")));

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "string_lights_right_center_to_top_center",
                        () -> instance.stringLightsRightCenterToTopCenter,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            instance.stringLightsRightCenterToTopCenter = aBoolean;
                        },
                        "fancymenu.decoration_overlays.string_lights.position.right_center_to_top_center")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.position.right_center_to_top_center.desc")));

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "string_lights_bottom_left_to_top_center",
                        () -> instance.stringLightsBottomLeftToTopCenter,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            instance.stringLightsBottomLeftToTopCenter = aBoolean;
                        },
                        "fancymenu.decoration_overlays.string_lights.position.bottom_left_to_top_center")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.position.bottom_left_to_top_center.desc")));

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "string_lights_top_left_to_top_right",
                        () -> instance.stringLightsTopLeftToTopRight,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            instance.stringLightsTopLeftToTopRight = aBoolean;
                        },
                        "fancymenu.decoration_overlays.string_lights.position.top_left_to_top_right")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.position.top_left_to_top_right.desc")));

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "string_lights_bottom_left_to_bottom_right",
                        () -> instance.stringLightsBottomLeftToBottomRight,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            instance.stringLightsBottomLeftToBottomRight = aBoolean;
                        },
                        "fancymenu.decoration_overlays.string_lights.position.bottom_left_to_bottom_right")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.position.bottom_left_to_bottom_right.desc")));

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "string_lights_loose_left_top",
                        () -> instance.stringLightsLooseLeftTop,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            instance.stringLightsLooseLeftTop = aBoolean;
                        },
                        "fancymenu.decoration_overlays.string_lights.position.loose_left_top")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.position.loose_left_top.desc")));

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "string_lights_loose_right_top",
                        () -> instance.stringLightsLooseRightTop,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            instance.stringLightsLooseRightTop = aBoolean;
                        },
                        "fancymenu.decoration_overlays.string_lights.position.loose_right_top")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.position.loose_right_top.desc")));

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.string_lights");
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }
}
