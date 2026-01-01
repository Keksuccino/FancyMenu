package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.StringLightsOverlay;
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

    public final Property.ColorProperty stringLightsLeftCenterToTopCenterColorHex = putProperty(Property.hexColorProperty("string_lights_left_center_to_top_center_color_hex", DEFAULT_COLOR_HEX, true, "fancymenu.decoration_overlays.string_lights.string.color"));
    public final Property.ColorProperty stringLightsRightCenterToTopCenterColorHex = putProperty(Property.hexColorProperty("string_lights_right_center_to_top_center_color_hex", DEFAULT_COLOR_HEX, true, "fancymenu.decoration_overlays.string_lights.string.color"));
    public final Property.ColorProperty stringLightsBottomLeftToTopCenterColorHex = putProperty(Property.hexColorProperty("string_lights_bottom_left_to_top_center_color_hex", DEFAULT_COLOR_HEX, true, "fancymenu.decoration_overlays.string_lights.string.color"));
    public final Property.ColorProperty stringLightsBottomRightToTopCenterColorHex = putProperty(Property.hexColorProperty("string_lights_bottom_right_to_top_center_color_hex", DEFAULT_COLOR_HEX, true, "fancymenu.decoration_overlays.string_lights.string.color"));
    public final Property.ColorProperty stringLightsTopLeftToTopRightColorHex = putProperty(Property.hexColorProperty("string_lights_top_left_to_top_right_color_hex", DEFAULT_COLOR_HEX, true, "fancymenu.decoration_overlays.string_lights.string.color"));
    public final Property.ColorProperty stringLightsBottomLeftToBottomRightColorHex = putProperty(Property.hexColorProperty("string_lights_bottom_left_to_bottom_right_color_hex", DEFAULT_COLOR_HEX, true, "fancymenu.decoration_overlays.string_lights.string.color"));
    public final Property.ColorProperty stringLightsLooseLeftTopColorHex = putProperty(Property.hexColorProperty("string_lights_loose_left_top_color_hex", DEFAULT_COLOR_HEX, true, "fancymenu.decoration_overlays.string_lights.string.color"));
    public final Property.ColorProperty stringLightsLooseRightTopColorHex = putProperty(Property.hexColorProperty("string_lights_loose_right_top_color_hex", DEFAULT_COLOR_HEX, true, "fancymenu.decoration_overlays.string_lights.string.color"));

    protected final StringLightsOverlay overlay = new StringLightsOverlay(0, 0);
    protected final EnumMap<StringLightsOverlay.StringLightsPosition, String> lastPositionColorStrings = new EnumMap<>(StringLightsOverlay.StringLightsPosition.class);
    protected String lastScaleString = null;
    protected String lastWindStrengthString = null;
    protected String lastFlickerSpeedString = null;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.addInputContextMenuEntryTo(menu, "string_lights_scale", StringLightsDecorationOverlay.class,
                        o -> o.stringLightsScale,
                        (o, s) -> o.stringLightsScale = s,
                        null, false, true,
                        Component.translatable("fancymenu.decoration_overlays.string_lights.scale"),
                        true, "1.0", null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.scale.desc")));

        this.addInputContextMenuEntryTo(menu, "string_lights_wind_strength", StringLightsDecorationOverlay.class,
                        o -> o.stringLightsWindStrength,
                        (o, s) -> o.stringLightsWindStrength = s,
                        null, false, true,
                        Component.translatable("fancymenu.decoration_overlays.string_lights.wind_strength"),
                        true, "1.0", null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.wind_strength.desc")));

        this.addInputContextMenuEntryTo(menu, "string_lights_flicker_speed", StringLightsDecorationOverlay.class,
                        o -> o.stringLightsFlickerSpeed,
                        (o, s) -> o.stringLightsFlickerSpeed = s,
                        null, false, true,
                        Component.translatable("fancymenu.decoration_overlays.string_lights.flicker_speed"),
                        true, "1.0", null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.flicker_speed.desc")));

        menu.addSeparatorEntry("separator_before_string_light_strings");

        ContextMenu stringsMenu = new ContextMenu();
        menu.addSubMenuEntry("string_lights_strings", Component.translatable("fancymenu.decoration_overlays.string_lights.strings"), stringsMenu).setStackable(true);

        addStringLightSubMenu(stringsMenu,
                "string_lights_left_center_to_top_center",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.left_center_to_top_center.label"),
                o -> o.stringLightsLeftCenterToTopCenter,
                (o, aBoolean) -> o.stringLightsLeftCenterToTopCenter = aBoolean,
                o -> o.stringLightsLeftCenterToTopCenterChristmasMode,
                (o, aBoolean) -> o.stringLightsLeftCenterToTopCenterChristmasMode = aBoolean,
                this.stringLightsLeftCenterToTopCenterColorHex,
                "fancymenu.decoration_overlays.string_lights.position.left_center_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.left_center_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.color.left_center_to_top_center.desc");

        addStringLightSubMenu(stringsMenu,
                "string_lights_right_center_to_top_center",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.right_center_to_top_center.label"),
                o -> o.stringLightsRightCenterToTopCenter,
                (o, aBoolean) -> o.stringLightsRightCenterToTopCenter = aBoolean,
                o -> o.stringLightsRightCenterToTopCenterChristmasMode,
                (o, aBoolean) -> o.stringLightsRightCenterToTopCenterChristmasMode = aBoolean,
                this.stringLightsRightCenterToTopCenterColorHex,
                "fancymenu.decoration_overlays.string_lights.position.right_center_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.right_center_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.color.right_center_to_top_center.desc");

        addStringLightSubMenu(stringsMenu,
                "string_lights_bottom_left_to_top_center",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.bottom_left_to_top_center.label"),
                o -> o.stringLightsBottomLeftToTopCenter,
                (o, aBoolean) -> o.stringLightsBottomLeftToTopCenter = aBoolean,
                o -> o.stringLightsBottomLeftToTopCenterChristmasMode,
                (o, aBoolean) -> o.stringLightsBottomLeftToTopCenterChristmasMode = aBoolean,
                this.stringLightsBottomLeftToTopCenterColorHex,
                "fancymenu.decoration_overlays.string_lights.position.bottom_left_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.bottom_left_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.color.bottom_left_to_top_center.desc");

        addStringLightSubMenu(stringsMenu,
                "string_lights_bottom_right_to_top_center",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.bottom_right_to_top_center.label"),
                o -> o.stringLightsBottomRightToTopCenter,
                (o, aBoolean) -> o.stringLightsBottomRightToTopCenter = aBoolean,
                o -> o.stringLightsBottomRightToTopCenterChristmasMode,
                (o, aBoolean) -> o.stringLightsBottomRightToTopCenterChristmasMode = aBoolean,
                this.stringLightsBottomRightToTopCenterColorHex,
                "fancymenu.decoration_overlays.string_lights.position.bottom_right_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.bottom_right_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.color.bottom_right_to_top_center.desc");

        addStringLightSubMenu(stringsMenu,
                "string_lights_top_left_to_top_right",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.top_left_to_top_right.label"),
                o -> o.stringLightsTopLeftToTopRight,
                (o, aBoolean) -> o.stringLightsTopLeftToTopRight = aBoolean,
                o -> o.stringLightsTopLeftToTopRightChristmasMode,
                (o, aBoolean) -> o.stringLightsTopLeftToTopRightChristmasMode = aBoolean,
                this.stringLightsTopLeftToTopRightColorHex,
                "fancymenu.decoration_overlays.string_lights.position.top_left_to_top_right.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.top_left_to_top_right.desc",
                "fancymenu.decoration_overlays.string_lights.color.top_left_to_top_right.desc");

        addStringLightSubMenu(stringsMenu,
                "string_lights_bottom_left_to_bottom_right",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.bottom_left_to_bottom_right.label"),
                o -> o.stringLightsBottomLeftToBottomRight,
                (o, aBoolean) -> o.stringLightsBottomLeftToBottomRight = aBoolean,
                o -> o.stringLightsBottomLeftToBottomRightChristmasMode,
                (o, aBoolean) -> o.stringLightsBottomLeftToBottomRightChristmasMode = aBoolean,
                this.stringLightsBottomLeftToBottomRightColorHex,
                "fancymenu.decoration_overlays.string_lights.position.bottom_left_to_bottom_right.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.bottom_left_to_bottom_right.desc",
                "fancymenu.decoration_overlays.string_lights.color.bottom_left_to_bottom_right.desc");

        addStringLightSubMenu(stringsMenu,
                "string_lights_loose_left_top",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.loose_left_top.label"),
                o -> o.stringLightsLooseLeftTop,
                (o, aBoolean) -> o.stringLightsLooseLeftTop = aBoolean,
                o -> o.stringLightsLooseLeftTopChristmasMode,
                (o, aBoolean) -> o.stringLightsLooseLeftTopChristmasMode = aBoolean,
                this.stringLightsLooseLeftTopColorHex,
                "fancymenu.decoration_overlays.string_lights.position.loose_left_top.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.loose_left_top.desc",
                "fancymenu.decoration_overlays.string_lights.color.loose_left_top.desc");

        addStringLightSubMenu(stringsMenu,
                "string_lights_loose_right_top",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.loose_right_top.label"),
                o -> o.stringLightsLooseRightTop,
                (o, aBoolean) -> o.stringLightsLooseRightTop = aBoolean,
                o -> o.stringLightsLooseRightTopChristmasMode,
                (o, aBoolean) -> o.stringLightsLooseRightTopChristmasMode = aBoolean,
                this.stringLightsLooseRightTopColorHex,
                "fancymenu.decoration_overlays.string_lights.position.loose_right_top.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.loose_right_top.desc",
                "fancymenu.decoration_overlays.string_lights.color.loose_right_top.desc");

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        updatePositionColor(StringLightsOverlay.StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER, this.stringLightsLeftCenterToTopCenterColorHex);
        updatePositionColor(StringLightsOverlay.StringLightsPosition.RIGHT_CENTER_TO_TOP_CENTER, this.stringLightsRightCenterToTopCenterColorHex);
        updatePositionColor(StringLightsOverlay.StringLightsPosition.BOTTOM_LEFT_TO_TOP_CENTER, this.stringLightsBottomLeftToTopCenterColorHex);
        updatePositionColor(StringLightsOverlay.StringLightsPosition.BOTTOM_RIGHT_TO_TOP_CENTER, this.stringLightsBottomRightToTopCenterColorHex);
        updatePositionColor(StringLightsOverlay.StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT, this.stringLightsTopLeftToTopRightColorHex);
        updatePositionColor(StringLightsOverlay.StringLightsPosition.BOTTOM_LEFT_TO_BOTTOM_RIGHT, this.stringLightsBottomLeftToBottomRightColorHex);
        updatePositionColor(StringLightsOverlay.StringLightsPosition.LOOSE_LEFT_TOP, this.stringLightsLooseLeftTopColorHex);
        updatePositionColor(StringLightsOverlay.StringLightsPosition.LOOSE_RIGHT_TOP, this.stringLightsLooseRightTopColorHex);

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

    private void updatePositionColor(@NotNull StringLightsOverlay.StringLightsPosition position, @NotNull Property.ColorProperty colorProperty) {
        String resolvedColor = colorProperty.getHex();
        String lastColor = this.lastPositionColorStrings.get(position);
        if (!Objects.equals(resolvedColor, lastColor)) {
            this.lastPositionColorStrings.put(position, resolvedColor);
            this.overlay.setPositionColor(position, colorProperty.getDrawable().getColorInt());
        }
    }

    private void addStringLightSubMenu(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Component label,
                                       @NotNull de.keksuccino.fancymenu.util.ConsumingSupplier<StringLightsDecorationOverlay, Boolean> showGetter,
                                       @NotNull java.util.function.BiConsumer<StringLightsDecorationOverlay, Boolean> showSetter,
                                       @NotNull de.keksuccino.fancymenu.util.ConsumingSupplier<StringLightsDecorationOverlay, Boolean> christmasGetter,
                                       @NotNull java.util.function.BiConsumer<StringLightsDecorationOverlay, Boolean> christmasSetter,
                                       @NotNull Property.ColorProperty colorProperty,
                                       @NotNull String showTooltipKey, @NotNull String christmasTooltipKey, @NotNull String colorTooltipKey) {
        ContextMenu subMenu = new ContextMenu();
        addTo.addSubMenuEntry(entryIdentifier + "_menu", label, subMenu).setStackable(true);

        this.addToggleContextMenuEntryTo(subMenu, entryIdentifier + "_show", StringLightsDecorationOverlay.class,
                        showGetter,
                        showSetter,
                        "fancymenu.decoration_overlays.string_lights.string.show")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable(showTooltipKey)));

        this.addToggleContextMenuEntryTo(subMenu, entryIdentifier + "_christmas_mode", StringLightsDecorationOverlay.class,
                        christmasGetter,
                        christmasSetter,
                        "fancymenu.decoration_overlays.string_lights.string.christmas_mode")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable(christmasTooltipKey)));

        colorProperty.buildContextMenuEntryAndAddTo(subMenu, this)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable(colorTooltipKey)));
    }

}
