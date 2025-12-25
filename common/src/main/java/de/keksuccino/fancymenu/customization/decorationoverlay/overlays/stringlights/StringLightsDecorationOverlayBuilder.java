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
    private static final String DEFAULT_COLOR_HEX = "#FFD27A";
    private static final String LIGHTS_LEFT_CENTER_TO_TOP_CENTER_COLOR_KEY = "string_lights_left_center_to_top_center_color_hex";
    private static final String LIGHTS_RIGHT_CENTER_TO_TOP_CENTER_COLOR_KEY = "string_lights_right_center_to_top_center_color_hex";
    private static final String LIGHTS_BOTTOM_LEFT_TO_TOP_CENTER_COLOR_KEY = "string_lights_bottom_left_to_top_center_color_hex";
    private static final String LIGHTS_TOP_LEFT_TO_TOP_RIGHT_COLOR_KEY = "string_lights_top_left_to_top_right_color_hex";
    private static final String LIGHTS_BOTTOM_LEFT_TO_BOTTOM_RIGHT_COLOR_KEY = "string_lights_bottom_left_to_bottom_right_color_hex";
    private static final String LIGHTS_LOOSE_LEFT_TOP_COLOR_KEY = "string_lights_loose_left_top_color_hex";
    private static final String LIGHTS_LOOSE_RIGHT_TOP_COLOR_KEY = "string_lights_loose_right_top_color_hex";
    private static final String LIGHTS_SCALE_KEY = "string_lights_scale";
    private static final String LIGHTS_WIND_STRENGTH_KEY = "string_lights_wind_strength";
    private static final String LIGHTS_FLICKER_SPEED_KEY = "string_lights_flicker_speed";
    private static final String LIGHTS_CHRISTMAS_MODE_KEY = "string_lights_christmas_mode";
    private static final String LIGHTS_LEFT_CENTER_TO_TOP_CENTER_CHRISTMAS_KEY = "string_lights_left_center_to_top_center_christmas_mode";
    private static final String LIGHTS_RIGHT_CENTER_TO_TOP_CENTER_CHRISTMAS_KEY = "string_lights_right_center_to_top_center_christmas_mode";
    private static final String LIGHTS_BOTTOM_LEFT_TO_TOP_CENTER_CHRISTMAS_KEY = "string_lights_bottom_left_to_top_center_christmas_mode";
    private static final String LIGHTS_TOP_LEFT_TO_TOP_RIGHT_CHRISTMAS_KEY = "string_lights_top_left_to_top_right_christmas_mode";
    private static final String LIGHTS_BOTTOM_LEFT_TO_BOTTOM_RIGHT_CHRISTMAS_KEY = "string_lights_bottom_left_to_bottom_right_christmas_mode";
    private static final String LIGHTS_LOOSE_LEFT_TOP_CHRISTMAS_KEY = "string_lights_loose_left_top_christmas_mode";
    private static final String LIGHTS_LOOSE_RIGHT_TOP_CHRISTMAS_KEY = "string_lights_loose_right_top_christmas_mode";
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

        String legacyColor = Objects.requireNonNullElse(deserializeFrom.getValue(LIGHTS_COLOR_KEY), DEFAULT_COLOR_HEX);
        instanceToWrite.stringLightsLeftCenterToTopCenterColorHex = Objects.requireNonNullElse(deserializeFrom.getValue(LIGHTS_LEFT_CENTER_TO_TOP_CENTER_COLOR_KEY), legacyColor);
        instanceToWrite.stringLightsRightCenterToTopCenterColorHex = Objects.requireNonNullElse(deserializeFrom.getValue(LIGHTS_RIGHT_CENTER_TO_TOP_CENTER_COLOR_KEY), legacyColor);
        instanceToWrite.stringLightsBottomLeftToTopCenterColorHex = Objects.requireNonNullElse(deserializeFrom.getValue(LIGHTS_BOTTOM_LEFT_TO_TOP_CENTER_COLOR_KEY), legacyColor);
        instanceToWrite.stringLightsTopLeftToTopRightColorHex = Objects.requireNonNullElse(deserializeFrom.getValue(LIGHTS_TOP_LEFT_TO_TOP_RIGHT_COLOR_KEY), legacyColor);
        instanceToWrite.stringLightsBottomLeftToBottomRightColorHex = Objects.requireNonNullElse(deserializeFrom.getValue(LIGHTS_BOTTOM_LEFT_TO_BOTTOM_RIGHT_COLOR_KEY), legacyColor);
        instanceToWrite.stringLightsLooseLeftTopColorHex = Objects.requireNonNullElse(deserializeFrom.getValue(LIGHTS_LOOSE_LEFT_TOP_COLOR_KEY), legacyColor);
        instanceToWrite.stringLightsLooseRightTopColorHex = Objects.requireNonNullElse(deserializeFrom.getValue(LIGHTS_LOOSE_RIGHT_TOP_COLOR_KEY), legacyColor);
        instanceToWrite.stringLightsScale = Objects.requireNonNullElse(deserializeFrom.getValue(LIGHTS_SCALE_KEY), instanceToWrite.stringLightsScale);
        instanceToWrite.stringLightsWindStrength = Objects.requireNonNullElse(deserializeFrom.getValue(LIGHTS_WIND_STRENGTH_KEY), instanceToWrite.stringLightsWindStrength);
        instanceToWrite.stringLightsFlickerSpeed = Objects.requireNonNullElse(deserializeFrom.getValue(LIGHTS_FLICKER_SPEED_KEY), instanceToWrite.stringLightsFlickerSpeed);
        instanceToWrite.stringLightsLeftCenterToTopCenter = deserializeBoolean(instanceToWrite.stringLightsLeftCenterToTopCenter, deserializeFrom.getValue(LIGHTS_LEFT_CENTER_TO_TOP_CENTER_KEY));
        instanceToWrite.stringLightsRightCenterToTopCenter = deserializeBoolean(instanceToWrite.stringLightsRightCenterToTopCenter, deserializeFrom.getValue(LIGHTS_RIGHT_CENTER_TO_TOP_CENTER_KEY));
        instanceToWrite.stringLightsBottomLeftToTopCenter = deserializeBoolean(instanceToWrite.stringLightsBottomLeftToTopCenter, deserializeFrom.getValue(LIGHTS_BOTTOM_LEFT_TO_TOP_CENTER_KEY));
        instanceToWrite.stringLightsTopLeftToTopRight = deserializeBoolean(instanceToWrite.stringLightsTopLeftToTopRight, deserializeFrom.getValue(LIGHTS_TOP_LEFT_TO_TOP_RIGHT_KEY));
        instanceToWrite.stringLightsBottomLeftToBottomRight = deserializeBoolean(instanceToWrite.stringLightsBottomLeftToBottomRight, deserializeFrom.getValue(LIGHTS_BOTTOM_LEFT_TO_BOTTOM_RIGHT_KEY));
        instanceToWrite.stringLightsLooseLeftTop = deserializeBoolean(instanceToWrite.stringLightsLooseLeftTop, deserializeFrom.getValue(LIGHTS_LOOSE_LEFT_TOP_KEY));
        instanceToWrite.stringLightsLooseRightTop = deserializeBoolean(instanceToWrite.stringLightsLooseRightTop, deserializeFrom.getValue(LIGHTS_LOOSE_RIGHT_TOP_KEY));
        boolean legacyChristmasMode = deserializeBoolean(false, deserializeFrom.getValue(LIGHTS_CHRISTMAS_MODE_KEY));
        instanceToWrite.stringLightsLeftCenterToTopCenterChristmasMode = deserializeBoolean(legacyChristmasMode, deserializeFrom.getValue(LIGHTS_LEFT_CENTER_TO_TOP_CENTER_CHRISTMAS_KEY));
        instanceToWrite.stringLightsRightCenterToTopCenterChristmasMode = deserializeBoolean(legacyChristmasMode, deserializeFrom.getValue(LIGHTS_RIGHT_CENTER_TO_TOP_CENTER_CHRISTMAS_KEY));
        instanceToWrite.stringLightsBottomLeftToTopCenterChristmasMode = deserializeBoolean(legacyChristmasMode, deserializeFrom.getValue(LIGHTS_BOTTOM_LEFT_TO_TOP_CENTER_CHRISTMAS_KEY));
        instanceToWrite.stringLightsTopLeftToTopRightChristmasMode = deserializeBoolean(legacyChristmasMode, deserializeFrom.getValue(LIGHTS_TOP_LEFT_TO_TOP_RIGHT_CHRISTMAS_KEY));
        instanceToWrite.stringLightsBottomLeftToBottomRightChristmasMode = deserializeBoolean(legacyChristmasMode, deserializeFrom.getValue(LIGHTS_BOTTOM_LEFT_TO_BOTTOM_RIGHT_CHRISTMAS_KEY));
        instanceToWrite.stringLightsLooseLeftTopChristmasMode = deserializeBoolean(legacyChristmasMode, deserializeFrom.getValue(LIGHTS_LOOSE_LEFT_TOP_CHRISTMAS_KEY));
        instanceToWrite.stringLightsLooseRightTopChristmasMode = deserializeBoolean(legacyChristmasMode, deserializeFrom.getValue(LIGHTS_LOOSE_RIGHT_TOP_CHRISTMAS_KEY));

    }

    @Override
    protected void serialize(@NotNull StringLightsDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {

        serializeTo.putProperty(LIGHTS_LEFT_CENTER_TO_TOP_CENTER_COLOR_KEY, instanceToSerialize.stringLightsLeftCenterToTopCenterColorHex);
        serializeTo.putProperty(LIGHTS_RIGHT_CENTER_TO_TOP_CENTER_COLOR_KEY, instanceToSerialize.stringLightsRightCenterToTopCenterColorHex);
        serializeTo.putProperty(LIGHTS_BOTTOM_LEFT_TO_TOP_CENTER_COLOR_KEY, instanceToSerialize.stringLightsBottomLeftToTopCenterColorHex);
        serializeTo.putProperty(LIGHTS_TOP_LEFT_TO_TOP_RIGHT_COLOR_KEY, instanceToSerialize.stringLightsTopLeftToTopRightColorHex);
        serializeTo.putProperty(LIGHTS_BOTTOM_LEFT_TO_BOTTOM_RIGHT_COLOR_KEY, instanceToSerialize.stringLightsBottomLeftToBottomRightColorHex);
        serializeTo.putProperty(LIGHTS_LOOSE_LEFT_TOP_COLOR_KEY, instanceToSerialize.stringLightsLooseLeftTopColorHex);
        serializeTo.putProperty(LIGHTS_LOOSE_RIGHT_TOP_COLOR_KEY, instanceToSerialize.stringLightsLooseRightTopColorHex);
        serializeTo.putProperty(LIGHTS_SCALE_KEY, instanceToSerialize.stringLightsScale);
        serializeTo.putProperty(LIGHTS_WIND_STRENGTH_KEY, instanceToSerialize.stringLightsWindStrength);
        serializeTo.putProperty(LIGHTS_FLICKER_SPEED_KEY, instanceToSerialize.stringLightsFlickerSpeed);
        serializeTo.putProperty(LIGHTS_LEFT_CENTER_TO_TOP_CENTER_KEY, instanceToSerialize.stringLightsLeftCenterToTopCenter);
        serializeTo.putProperty(LIGHTS_RIGHT_CENTER_TO_TOP_CENTER_KEY, instanceToSerialize.stringLightsRightCenterToTopCenter);
        serializeTo.putProperty(LIGHTS_BOTTOM_LEFT_TO_TOP_CENTER_KEY, instanceToSerialize.stringLightsBottomLeftToTopCenter);
        serializeTo.putProperty(LIGHTS_TOP_LEFT_TO_TOP_RIGHT_KEY, instanceToSerialize.stringLightsTopLeftToTopRight);
        serializeTo.putProperty(LIGHTS_BOTTOM_LEFT_TO_BOTTOM_RIGHT_KEY, instanceToSerialize.stringLightsBottomLeftToBottomRight);
        serializeTo.putProperty(LIGHTS_LOOSE_LEFT_TOP_KEY, instanceToSerialize.stringLightsLooseLeftTop);
        serializeTo.putProperty(LIGHTS_LOOSE_RIGHT_TOP_KEY, instanceToSerialize.stringLightsLooseRightTop);
        serializeTo.putProperty(LIGHTS_LEFT_CENTER_TO_TOP_CENTER_CHRISTMAS_KEY, instanceToSerialize.stringLightsLeftCenterToTopCenterChristmasMode);
        serializeTo.putProperty(LIGHTS_RIGHT_CENTER_TO_TOP_CENTER_CHRISTMAS_KEY, instanceToSerialize.stringLightsRightCenterToTopCenterChristmasMode);
        serializeTo.putProperty(LIGHTS_BOTTOM_LEFT_TO_TOP_CENTER_CHRISTMAS_KEY, instanceToSerialize.stringLightsBottomLeftToTopCenterChristmasMode);
        serializeTo.putProperty(LIGHTS_TOP_LEFT_TO_TOP_RIGHT_CHRISTMAS_KEY, instanceToSerialize.stringLightsTopLeftToTopRightChristmasMode);
        serializeTo.putProperty(LIGHTS_BOTTOM_LEFT_TO_BOTTOM_RIGHT_CHRISTMAS_KEY, instanceToSerialize.stringLightsBottomLeftToBottomRightChristmasMode);
        serializeTo.putProperty(LIGHTS_LOOSE_LEFT_TOP_CHRISTMAS_KEY, instanceToSerialize.stringLightsLooseLeftTopChristmasMode);
        serializeTo.putProperty(LIGHTS_LOOSE_RIGHT_TOP_CHRISTMAS_KEY, instanceToSerialize.stringLightsLooseRightTopChristmasMode);

    }

    @Override
    protected void buildConfigurationMenu(@NotNull StringLightsDecorationOverlay instance, @NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

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

        menu.addSeparatorEntry("separator_before_string_light_strings");

        ContextMenu stringsMenu = new ContextMenu();
        menu.addSubMenuEntry("string_lights_strings", Component.translatable("fancymenu.decoration_overlays.string_lights.strings"), stringsMenu).setStackable(true);

        addStringLightSubMenu(stringsMenu, editor,
                "string_lights_left_center_to_top_center",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.left_center_to_top_center.label"),
                () -> instance.stringLightsLeftCenterToTopCenter,
                aBoolean -> instance.stringLightsLeftCenterToTopCenter = aBoolean,
                () -> instance.stringLightsLeftCenterToTopCenterChristmasMode,
                aBoolean -> instance.stringLightsLeftCenterToTopCenterChristmasMode = aBoolean,
                () -> instance.stringLightsLeftCenterToTopCenterColorHex,
                s -> instance.stringLightsLeftCenterToTopCenterColorHex = s,
                "fancymenu.decoration_overlays.string_lights.position.left_center_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.left_center_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.color.left_center_to_top_center.desc");

        addStringLightSubMenu(stringsMenu, editor,
                "string_lights_right_center_to_top_center",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.right_center_to_top_center.label"),
                () -> instance.stringLightsRightCenterToTopCenter,
                aBoolean -> instance.stringLightsRightCenterToTopCenter = aBoolean,
                () -> instance.stringLightsRightCenterToTopCenterChristmasMode,
                aBoolean -> instance.stringLightsRightCenterToTopCenterChristmasMode = aBoolean,
                () -> instance.stringLightsRightCenterToTopCenterColorHex,
                s -> instance.stringLightsRightCenterToTopCenterColorHex = s,
                "fancymenu.decoration_overlays.string_lights.position.right_center_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.right_center_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.color.right_center_to_top_center.desc");

        addStringLightSubMenu(stringsMenu, editor,
                "string_lights_bottom_left_to_top_center",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.bottom_left_to_top_center.label"),
                () -> instance.stringLightsBottomLeftToTopCenter,
                aBoolean -> instance.stringLightsBottomLeftToTopCenter = aBoolean,
                () -> instance.stringLightsBottomLeftToTopCenterChristmasMode,
                aBoolean -> instance.stringLightsBottomLeftToTopCenterChristmasMode = aBoolean,
                () -> instance.stringLightsBottomLeftToTopCenterColorHex,
                s -> instance.stringLightsBottomLeftToTopCenterColorHex = s,
                "fancymenu.decoration_overlays.string_lights.position.bottom_left_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.bottom_left_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.color.bottom_left_to_top_center.desc");

        addStringLightSubMenu(stringsMenu, editor,
                "string_lights_top_left_to_top_right",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.top_left_to_top_right.label"),
                () -> instance.stringLightsTopLeftToTopRight,
                aBoolean -> instance.stringLightsTopLeftToTopRight = aBoolean,
                () -> instance.stringLightsTopLeftToTopRightChristmasMode,
                aBoolean -> instance.stringLightsTopLeftToTopRightChristmasMode = aBoolean,
                () -> instance.stringLightsTopLeftToTopRightColorHex,
                s -> instance.stringLightsTopLeftToTopRightColorHex = s,
                "fancymenu.decoration_overlays.string_lights.position.top_left_to_top_right.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.top_left_to_top_right.desc",
                "fancymenu.decoration_overlays.string_lights.color.top_left_to_top_right.desc");

        addStringLightSubMenu(stringsMenu, editor,
                "string_lights_bottom_left_to_bottom_right",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.bottom_left_to_bottom_right.label"),
                () -> instance.stringLightsBottomLeftToBottomRight,
                aBoolean -> instance.stringLightsBottomLeftToBottomRight = aBoolean,
                () -> instance.stringLightsBottomLeftToBottomRightChristmasMode,
                aBoolean -> instance.stringLightsBottomLeftToBottomRightChristmasMode = aBoolean,
                () -> instance.stringLightsBottomLeftToBottomRightColorHex,
                s -> instance.stringLightsBottomLeftToBottomRightColorHex = s,
                "fancymenu.decoration_overlays.string_lights.position.bottom_left_to_bottom_right.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.bottom_left_to_bottom_right.desc",
                "fancymenu.decoration_overlays.string_lights.color.bottom_left_to_bottom_right.desc");

        addStringLightSubMenu(stringsMenu, editor,
                "string_lights_loose_left_top",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.loose_left_top.label"),
                () -> instance.stringLightsLooseLeftTop,
                aBoolean -> instance.stringLightsLooseLeftTop = aBoolean,
                () -> instance.stringLightsLooseLeftTopChristmasMode,
                aBoolean -> instance.stringLightsLooseLeftTopChristmasMode = aBoolean,
                () -> instance.stringLightsLooseLeftTopColorHex,
                s -> instance.stringLightsLooseLeftTopColorHex = s,
                "fancymenu.decoration_overlays.string_lights.position.loose_left_top.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.loose_left_top.desc",
                "fancymenu.decoration_overlays.string_lights.color.loose_left_top.desc");

        addStringLightSubMenu(stringsMenu, editor,
                "string_lights_loose_right_top",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.loose_right_top.label"),
                () -> instance.stringLightsLooseRightTop,
                aBoolean -> instance.stringLightsLooseRightTop = aBoolean,
                () -> instance.stringLightsLooseRightTopChristmasMode,
                aBoolean -> instance.stringLightsLooseRightTopChristmasMode = aBoolean,
                () -> instance.stringLightsLooseRightTopColorHex,
                s -> instance.stringLightsLooseRightTopColorHex = s,
                "fancymenu.decoration_overlays.string_lights.position.loose_right_top.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.loose_right_top.desc",
                "fancymenu.decoration_overlays.string_lights.color.loose_right_top.desc");

    }

    private void addStringLightSubMenu(@NotNull ContextMenu addTo, @NotNull LayoutEditorScreen editor, @NotNull String entryIdentifier, @NotNull Component label,
                                       @NotNull java.util.function.Supplier<Boolean> showGetter, @NotNull java.util.function.Consumer<Boolean> showSetter,
                                       @NotNull java.util.function.Supplier<Boolean> christmasGetter, @NotNull java.util.function.Consumer<Boolean> christmasSetter,
                                       @NotNull java.util.function.Supplier<String> colorGetter, @NotNull java.util.function.Consumer<String> colorSetter,
                                       @NotNull String showTooltipKey, @NotNull String christmasTooltipKey, @NotNull String colorTooltipKey) {
        ContextMenu subMenu = new ContextMenu();
        addTo.addSubMenuEntry(entryIdentifier + "_menu", label, subMenu).setStackable(true);

        ContextMenuUtils.addToggleContextMenuEntryTo(subMenu, entryIdentifier + "_show",
                        showGetter,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            showSetter.accept(aBoolean);
                        },
                        "fancymenu.decoration_overlays.string_lights.string.show")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable(showTooltipKey)));

        ContextMenuUtils.addToggleContextMenuEntryTo(subMenu, entryIdentifier + "_christmas_mode",
                        christmasGetter,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            christmasSetter.accept(aBoolean);
                        },
                        "fancymenu.decoration_overlays.string_lights.string.christmas_mode")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable(christmasTooltipKey)));

        ContextMenuUtils.addInputContextMenuEntryTo(subMenu, entryIdentifier + "_color", Component.translatable("fancymenu.decoration_overlays.string_lights.string.color"),
                        colorGetter,
                        s -> {
                            editor.history.saveSnapshot();
                            colorSetter.accept(s);
                        }, true,
                        "#FFD27A", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable(colorTooltipKey)));
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
