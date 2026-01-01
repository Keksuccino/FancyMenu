package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.overlay.StringLightsOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.ContextMenuUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Objects;

public class StringLightsDecorationOverlay extends AbstractDecorationOverlay<StringLightsDecorationOverlay> {

    private static final String DEFAULT_COLOR_HEX = "#FFD27A";

    @NotNull
    public String stringLightsLeftCenterToTopCenterColorHex = "#FFD27A";
    @NotNull
    public String stringLightsRightCenterToTopCenterColorHex = "#FFD27A";
    @NotNull
    public String stringLightsBottomLeftToTopCenterColorHex = "#FFD27A";
    @NotNull
    public String stringLightsBottomRightToTopCenterColorHex = "#FFD27A";
    @NotNull
    public String stringLightsTopLeftToTopRightColorHex = "#FFD27A";
    @NotNull
    public String stringLightsBottomLeftToBottomRightColorHex = "#FFD27A";
    @NotNull
    public String stringLightsLooseLeftTopColorHex = "#FFD27A";
    @NotNull
    public String stringLightsLooseRightTopColorHex = "#FFD27A";
    @NotNull
    public String stringLightsScale = "1.0";
    @NotNull
    public String stringLightsWindStrength = "1.0";
    @NotNull
    public String stringLightsFlickerSpeed = "1.0";
    public boolean stringLightsLeftCenterToTopCenter = true;
    public boolean stringLightsRightCenterToTopCenter = true;
    public boolean stringLightsBottomLeftToTopCenter = false;
    public boolean stringLightsBottomRightToTopCenter = false;
    public boolean stringLightsTopLeftToTopRight = true;
    public boolean stringLightsBottomLeftToBottomRight = false;
    public boolean stringLightsLooseLeftTop = false;
    public boolean stringLightsLooseRightTop = false;
    public boolean stringLightsLeftCenterToTopCenterChristmasMode = false;
    public boolean stringLightsRightCenterToTopCenterChristmasMode = false;
    public boolean stringLightsBottomLeftToTopCenterChristmasMode = false;
    public boolean stringLightsBottomRightToTopCenterChristmasMode = false;
    public boolean stringLightsTopLeftToTopRightChristmasMode = false;
    public boolean stringLightsBottomLeftToBottomRightChristmasMode = false;
    public boolean stringLightsLooseLeftTopChristmasMode = false;
    public boolean stringLightsLooseRightTopChristmasMode = false;
    protected final StringLightsOverlay overlay = new StringLightsOverlay(0, 0);
    protected final EnumMap<StringLightsOverlay.StringLightsPosition, String> lastPositionColorStrings = new EnumMap<>(StringLightsOverlay.StringLightsPosition.class);
    protected String lastScaleString = null;
    protected String lastWindStrengthString = null;
    protected String lastFlickerSpeedString = null;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "string_lights_scale", Component.translatable("fancymenu.decoration_overlays.string_lights.scale"),
                        () -> this.stringLightsScale,
                        s -> {
                            editor.history.saveSnapshot();
                            this.stringLightsScale = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.scale.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "string_lights_wind_strength", Component.translatable("fancymenu.decoration_overlays.string_lights.wind_strength"),
                        () -> this.stringLightsWindStrength,
                        s -> {
                            editor.history.saveSnapshot();
                            this.stringLightsWindStrength = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.wind_strength.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "string_lights_flicker_speed", Component.translatable("fancymenu.decoration_overlays.string_lights.flicker_speed"),
                        () -> this.stringLightsFlickerSpeed,
                        s -> {
                            editor.history.saveSnapshot();
                            this.stringLightsFlickerSpeed = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.flicker_speed.desc")));

        menu.addSeparatorEntry("separator_before_string_light_strings");

        ContextMenu stringsMenu = new ContextMenu();
        menu.addSubMenuEntry("string_lights_strings", Component.translatable("fancymenu.decoration_overlays.string_lights.strings"), stringsMenu).setStackable(true);

        addStringLightSubMenu(stringsMenu, editor,
                "string_lights_left_center_to_top_center",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.left_center_to_top_center.label"),
                () -> this.stringLightsLeftCenterToTopCenter,
                aBoolean -> this.stringLightsLeftCenterToTopCenter = aBoolean,
                () -> this.stringLightsLeftCenterToTopCenterChristmasMode,
                aBoolean -> this.stringLightsLeftCenterToTopCenterChristmasMode = aBoolean,
                () -> this.stringLightsLeftCenterToTopCenterColorHex,
                s -> this.stringLightsLeftCenterToTopCenterColorHex = s,
                "fancymenu.decoration_overlays.string_lights.position.left_center_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.left_center_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.color.left_center_to_top_center.desc");

        addStringLightSubMenu(stringsMenu, editor,
                "string_lights_right_center_to_top_center",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.right_center_to_top_center.label"),
                () -> this.stringLightsRightCenterToTopCenter,
                aBoolean -> this.stringLightsRightCenterToTopCenter = aBoolean,
                () -> this.stringLightsRightCenterToTopCenterChristmasMode,
                aBoolean -> this.stringLightsRightCenterToTopCenterChristmasMode = aBoolean,
                () -> this.stringLightsRightCenterToTopCenterColorHex,
                s -> this.stringLightsRightCenterToTopCenterColorHex = s,
                "fancymenu.decoration_overlays.string_lights.position.right_center_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.right_center_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.color.right_center_to_top_center.desc");

        addStringLightSubMenu(stringsMenu, editor,
                "string_lights_bottom_left_to_top_center",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.bottom_left_to_top_center.label"),
                () -> this.stringLightsBottomLeftToTopCenter,
                aBoolean -> this.stringLightsBottomLeftToTopCenter = aBoolean,
                () -> this.stringLightsBottomLeftToTopCenterChristmasMode,
                aBoolean -> this.stringLightsBottomLeftToTopCenterChristmasMode = aBoolean,
                () -> this.stringLightsBottomLeftToTopCenterColorHex,
                s -> this.stringLightsBottomLeftToTopCenterColorHex = s,
                "fancymenu.decoration_overlays.string_lights.position.bottom_left_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.bottom_left_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.color.bottom_left_to_top_center.desc");

        addStringLightSubMenu(stringsMenu, editor,
                "string_lights_bottom_right_to_top_center",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.bottom_right_to_top_center.label"),
                () -> this.stringLightsBottomRightToTopCenter,
                aBoolean -> this.stringLightsBottomRightToTopCenter = aBoolean,
                () -> this.stringLightsBottomRightToTopCenterChristmasMode,
                aBoolean -> this.stringLightsBottomRightToTopCenterChristmasMode = aBoolean,
                () -> this.stringLightsBottomRightToTopCenterColorHex,
                s -> this.stringLightsBottomRightToTopCenterColorHex = s,
                "fancymenu.decoration_overlays.string_lights.position.bottom_right_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.bottom_right_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.color.bottom_right_to_top_center.desc");

        addStringLightSubMenu(stringsMenu, editor,
                "string_lights_top_left_to_top_right",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.top_left_to_top_right.label"),
                () -> this.stringLightsTopLeftToTopRight,
                aBoolean -> this.stringLightsTopLeftToTopRight = aBoolean,
                () -> this.stringLightsTopLeftToTopRightChristmasMode,
                aBoolean -> this.stringLightsTopLeftToTopRightChristmasMode = aBoolean,
                () -> this.stringLightsTopLeftToTopRightColorHex,
                s -> this.stringLightsTopLeftToTopRightColorHex = s,
                "fancymenu.decoration_overlays.string_lights.position.top_left_to_top_right.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.top_left_to_top_right.desc",
                "fancymenu.decoration_overlays.string_lights.color.top_left_to_top_right.desc");

        addStringLightSubMenu(stringsMenu, editor,
                "string_lights_bottom_left_to_bottom_right",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.bottom_left_to_bottom_right.label"),
                () -> this.stringLightsBottomLeftToBottomRight,
                aBoolean -> this.stringLightsBottomLeftToBottomRight = aBoolean,
                () -> this.stringLightsBottomLeftToBottomRightChristmasMode,
                aBoolean -> this.stringLightsBottomLeftToBottomRightChristmasMode = aBoolean,
                () -> this.stringLightsBottomLeftToBottomRightColorHex,
                s -> this.stringLightsBottomLeftToBottomRightColorHex = s,
                "fancymenu.decoration_overlays.string_lights.position.bottom_left_to_bottom_right.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.bottom_left_to_bottom_right.desc",
                "fancymenu.decoration_overlays.string_lights.color.bottom_left_to_bottom_right.desc");

        addStringLightSubMenu(stringsMenu, editor,
                "string_lights_loose_left_top",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.loose_left_top.label"),
                () -> this.stringLightsLooseLeftTop,
                aBoolean -> this.stringLightsLooseLeftTop = aBoolean,
                () -> this.stringLightsLooseLeftTopChristmasMode,
                aBoolean -> this.stringLightsLooseLeftTopChristmasMode = aBoolean,
                () -> this.stringLightsLooseLeftTopColorHex,
                s -> this.stringLightsLooseLeftTopColorHex = s,
                "fancymenu.decoration_overlays.string_lights.position.loose_left_top.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.loose_left_top.desc",
                "fancymenu.decoration_overlays.string_lights.color.loose_left_top.desc");

        addStringLightSubMenu(stringsMenu, editor,
                "string_lights_loose_right_top",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.loose_right_top.label"),
                () -> this.stringLightsLooseRightTop,
                aBoolean -> this.stringLightsLooseRightTop = aBoolean,
                () -> this.stringLightsLooseRightTopChristmasMode,
                aBoolean -> this.stringLightsLooseRightTopChristmasMode = aBoolean,
                () -> this.stringLightsLooseRightTopColorHex,
                s -> this.stringLightsLooseRightTopColorHex = s,
                "fancymenu.decoration_overlays.string_lights.position.loose_right_top.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.loose_right_top.desc",
                "fancymenu.decoration_overlays.string_lights.color.loose_right_top.desc");

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        updatePositionColor(StringLightsOverlay.StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER, this.stringLightsLeftCenterToTopCenterColorHex, DEFAULT_COLOR_HEX);
        updatePositionColor(StringLightsOverlay.StringLightsPosition.RIGHT_CENTER_TO_TOP_CENTER, this.stringLightsRightCenterToTopCenterColorHex, DEFAULT_COLOR_HEX);
        updatePositionColor(StringLightsOverlay.StringLightsPosition.BOTTOM_LEFT_TO_TOP_CENTER, this.stringLightsBottomLeftToTopCenterColorHex, DEFAULT_COLOR_HEX);
        updatePositionColor(StringLightsOverlay.StringLightsPosition.BOTTOM_RIGHT_TO_TOP_CENTER, this.stringLightsBottomRightToTopCenterColorHex, DEFAULT_COLOR_HEX);
        updatePositionColor(StringLightsOverlay.StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT, this.stringLightsTopLeftToTopRightColorHex, DEFAULT_COLOR_HEX);
        updatePositionColor(StringLightsOverlay.StringLightsPosition.BOTTOM_LEFT_TO_BOTTOM_RIGHT, this.stringLightsBottomLeftToBottomRightColorHex, DEFAULT_COLOR_HEX);
        updatePositionColor(StringLightsOverlay.StringLightsPosition.LOOSE_LEFT_TOP, this.stringLightsLooseLeftTopColorHex, DEFAULT_COLOR_HEX);
        updatePositionColor(StringLightsOverlay.StringLightsPosition.LOOSE_RIGHT_TOP, this.stringLightsLooseRightTopColorHex, DEFAULT_COLOR_HEX);

        String scaleString = PlaceholderParser.replacePlaceholders(this.stringLightsScale);
        if (!Objects.equals(scaleString, this.lastScaleString)) {
            this.lastScaleString = scaleString;
            float scaleValue;
            if (MathUtils.isFloat(scaleString)) {
                scaleValue = Float.parseFloat(scaleString);
            } else {
                scaleValue = 1.0F;
            }
            this.overlay.setScale(scaleValue);
        }

        String windStrengthString = PlaceholderParser.replacePlaceholders(this.stringLightsWindStrength);
        if (!Objects.equals(windStrengthString, this.lastWindStrengthString)) {
            this.lastWindStrengthString = windStrengthString;
            float windValue;
            if (MathUtils.isFloat(windStrengthString)) {
                windValue = Float.parseFloat(windStrengthString);
            } else {
                windValue = 1.0F;
            }
            this.overlay.setWindStrength(windValue);
        }

        String flickerSpeedString = PlaceholderParser.replacePlaceholders(this.stringLightsFlickerSpeed);
        if (!Objects.equals(flickerSpeedString, this.lastFlickerSpeedString)) {
            this.lastFlickerSpeedString = flickerSpeedString;
            float speedValue;
            if (MathUtils.isFloat(flickerSpeedString)) {
                speedValue = Float.parseFloat(flickerSpeedString);
            } else {
                speedValue = 1.0F;
            }
            this.overlay.setFlickerSpeed(speedValue);
        }

        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER, this.stringLightsLeftCenterToTopCenter);
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.RIGHT_CENTER_TO_TOP_CENTER, this.stringLightsRightCenterToTopCenter);
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.BOTTOM_LEFT_TO_TOP_CENTER, this.stringLightsBottomLeftToTopCenter);
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.BOTTOM_RIGHT_TO_TOP_CENTER, this.stringLightsBottomRightToTopCenter);
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT, this.stringLightsTopLeftToTopRight);
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.BOTTOM_LEFT_TO_BOTTOM_RIGHT, this.stringLightsBottomLeftToBottomRight);
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.LOOSE_LEFT_TOP, this.stringLightsLooseLeftTop);
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.LOOSE_RIGHT_TOP, this.stringLightsLooseRightTop);
        this.overlay.setPositionChristmasMode(StringLightsOverlay.StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER, this.stringLightsLeftCenterToTopCenterChristmasMode);
        this.overlay.setPositionChristmasMode(StringLightsOverlay.StringLightsPosition.RIGHT_CENTER_TO_TOP_CENTER, this.stringLightsRightCenterToTopCenterChristmasMode);
        this.overlay.setPositionChristmasMode(StringLightsOverlay.StringLightsPosition.BOTTOM_LEFT_TO_TOP_CENTER, this.stringLightsBottomLeftToTopCenterChristmasMode);
        this.overlay.setPositionChristmasMode(StringLightsOverlay.StringLightsPosition.BOTTOM_RIGHT_TO_TOP_CENTER, this.stringLightsBottomRightToTopCenterChristmasMode);
        this.overlay.setPositionChristmasMode(StringLightsOverlay.StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT, this.stringLightsTopLeftToTopRightChristmasMode);
        this.overlay.setPositionChristmasMode(StringLightsOverlay.StringLightsPosition.BOTTOM_LEFT_TO_BOTTOM_RIGHT, this.stringLightsBottomLeftToBottomRightChristmasMode);
        this.overlay.setPositionChristmasMode(StringLightsOverlay.StringLightsPosition.LOOSE_LEFT_TOP, this.stringLightsLooseLeftTopChristmasMode);
        this.overlay.setPositionChristmasMode(StringLightsOverlay.StringLightsPosition.LOOSE_RIGHT_TOP, this.stringLightsLooseRightTopChristmasMode);

        this.overlay.setWidth(getScreenWidth());
        this.overlay.setHeight(getScreenHeight());
        this.overlay.render(graphics, mouseX, mouseY, partial);

    }

    private void updatePositionColor(@NotNull StringLightsOverlay.StringLightsPosition position, @NotNull String colorHex, @NotNull String defaultColorHex) {
        String resolvedColor = PlaceholderParser.replacePlaceholders(colorHex);
        if (resolvedColor == null || resolvedColor.isBlank()) {
            resolvedColor = defaultColorHex;
        }
        String lastColor = this.lastPositionColorStrings.get(position);
        if (!Objects.equals(resolvedColor, lastColor)) {
            this.lastPositionColorStrings.put(position, resolvedColor);
            this.overlay.setPositionColor(position, DrawableColor.of(resolvedColor).getColorInt());
        }
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
}
