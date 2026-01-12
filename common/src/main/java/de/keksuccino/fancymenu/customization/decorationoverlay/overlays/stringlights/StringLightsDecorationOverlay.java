package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.StringLightsOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.EnumMap;
import java.util.Objects;

public class StringLightsDecorationOverlay extends AbstractDecorationOverlay<StringLightsDecorationOverlay> {

    private static final String DEFAULT_COLOR_HEX = "#FFD27A";

    public final Property.StringProperty stringLightsScale = putProperty(Property.stringProperty("string_lights_scale", "1.0", false, true, "fancymenu.decoration_overlays.string_lights.scale"));
    public final Property.StringProperty stringLightsWindStrength = putProperty(Property.stringProperty("string_lights_wind_strength", "1.0", false, true, "fancymenu.decoration_overlays.string_lights.wind_strength"));
    public final Property.StringProperty stringLightsFlickerSpeed = putProperty(Property.stringProperty("string_lights_flicker_speed", "1.0", false, true, "fancymenu.decoration_overlays.string_lights.flicker_speed"));
    public final Property<Boolean> stringLightsLeftCenterToTopCenter = putProperty(Property.booleanProperty("string_lights_left_center_to_top_center", true, "fancymenu.decoration_overlays.string_lights.string.show"));
    public final Property<Boolean> stringLightsRightCenterToTopCenter = putProperty(Property.booleanProperty("string_lights_right_center_to_top_center", true, "fancymenu.decoration_overlays.string_lights.string.show"));
    public final Property<Boolean> stringLightsBottomLeftToTopCenter = putProperty(Property.booleanProperty("string_lights_bottom_left_to_top_center", false, "fancymenu.decoration_overlays.string_lights.string.show"));
    public final Property<Boolean> stringLightsBottomRightToTopCenter = putProperty(Property.booleanProperty("string_lights_bottom_right_to_top_center", false, "fancymenu.decoration_overlays.string_lights.string.show"));
    public final Property<Boolean> stringLightsTopLeftToTopRight = putProperty(Property.booleanProperty("string_lights_top_left_to_top_right", true, "fancymenu.decoration_overlays.string_lights.string.show"));
    public final Property<Boolean> stringLightsBottomLeftToBottomRight = putProperty(Property.booleanProperty("string_lights_bottom_left_to_bottom_right", false, "fancymenu.decoration_overlays.string_lights.string.show"));
    public final Property<Boolean> stringLightsLooseLeftTop = putProperty(Property.booleanProperty("string_lights_loose_left_top", false, "fancymenu.decoration_overlays.string_lights.string.show"));
    public final Property<Boolean> stringLightsLooseRightTop = putProperty(Property.booleanProperty("string_lights_loose_right_top", false, "fancymenu.decoration_overlays.string_lights.string.show"));
    public final Property<Boolean> stringLightsLeftCenterToTopCenterChristmasMode = putProperty(Property.booleanProperty("string_lights_left_center_to_top_center_christmas_mode", false, "fancymenu.decoration_overlays.string_lights.string.christmas_mode"));
    public final Property<Boolean> stringLightsRightCenterToTopCenterChristmasMode = putProperty(Property.booleanProperty("string_lights_right_center_to_top_center_christmas_mode", false, "fancymenu.decoration_overlays.string_lights.string.christmas_mode"));
    public final Property<Boolean> stringLightsBottomLeftToTopCenterChristmasMode = putProperty(Property.booleanProperty("string_lights_bottom_left_to_top_center_christmas_mode", false, "fancymenu.decoration_overlays.string_lights.string.christmas_mode"));
    public final Property<Boolean> stringLightsBottomRightToTopCenterChristmasMode = putProperty(Property.booleanProperty("string_lights_bottom_right_to_top_center_christmas_mode", false, "fancymenu.decoration_overlays.string_lights.string.christmas_mode"));
    public final Property<Boolean> stringLightsTopLeftToTopRightChristmasMode = putProperty(Property.booleanProperty("string_lights_top_left_to_top_right_christmas_mode", false, "fancymenu.decoration_overlays.string_lights.string.christmas_mode"));
    public final Property<Boolean> stringLightsBottomLeftToBottomRightChristmasMode = putProperty(Property.booleanProperty("string_lights_bottom_left_to_bottom_right_christmas_mode", false, "fancymenu.decoration_overlays.string_lights.string.christmas_mode"));
    public final Property<Boolean> stringLightsLooseLeftTopChristmasMode = putProperty(Property.booleanProperty("string_lights_loose_left_top_christmas_mode", false, "fancymenu.decoration_overlays.string_lights.string.christmas_mode"));
    public final Property<Boolean> stringLightsLooseRightTopChristmasMode = putProperty(Property.booleanProperty("string_lights_loose_right_top_christmas_mode", false, "fancymenu.decoration_overlays.string_lights.string.christmas_mode"));
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

        this.stringLightsScale.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.scale.desc")));

        this.stringLightsWindStrength.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.wind_strength.desc")));

        this.stringLightsFlickerSpeed.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.flicker_speed.desc")));

        menu.addSeparatorEntry("separator_before_string_light_strings");

        ContextMenu stringsMenu = new ContextMenu();
        menu.addSubMenuEntry("string_lights_strings", Component.translatable("fancymenu.decoration_overlays.string_lights.strings"), stringsMenu).setStackable(true);

        addStringLightSubMenu(stringsMenu,
                "string_lights_left_center_to_top_center",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.left_center_to_top_center.label"),
                this.stringLightsLeftCenterToTopCenter,
                this.stringLightsLeftCenterToTopCenterChristmasMode,
                this.stringLightsLeftCenterToTopCenterColorHex,
                "fancymenu.decoration_overlays.string_lights.position.left_center_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.left_center_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.color.left_center_to_top_center.desc");

        addStringLightSubMenu(stringsMenu,
                "string_lights_right_center_to_top_center",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.right_center_to_top_center.label"),
                this.stringLightsRightCenterToTopCenter,
                this.stringLightsRightCenterToTopCenterChristmasMode,
                this.stringLightsRightCenterToTopCenterColorHex,
                "fancymenu.decoration_overlays.string_lights.position.right_center_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.right_center_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.color.right_center_to_top_center.desc");

        addStringLightSubMenu(stringsMenu,
                "string_lights_bottom_left_to_top_center",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.bottom_left_to_top_center.label"),
                this.stringLightsBottomLeftToTopCenter,
                this.stringLightsBottomLeftToTopCenterChristmasMode,
                this.stringLightsBottomLeftToTopCenterColorHex,
                "fancymenu.decoration_overlays.string_lights.position.bottom_left_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.bottom_left_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.color.bottom_left_to_top_center.desc");

        addStringLightSubMenu(stringsMenu,
                "string_lights_bottom_right_to_top_center",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.bottom_right_to_top_center.label"),
                this.stringLightsBottomRightToTopCenter,
                this.stringLightsBottomRightToTopCenterChristmasMode,
                this.stringLightsBottomRightToTopCenterColorHex,
                "fancymenu.decoration_overlays.string_lights.position.bottom_right_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.bottom_right_to_top_center.desc",
                "fancymenu.decoration_overlays.string_lights.color.bottom_right_to_top_center.desc");

        addStringLightSubMenu(stringsMenu,
                "string_lights_top_left_to_top_right",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.top_left_to_top_right.label"),
                this.stringLightsTopLeftToTopRight,
                this.stringLightsTopLeftToTopRightChristmasMode,
                this.stringLightsTopLeftToTopRightColorHex,
                "fancymenu.decoration_overlays.string_lights.position.top_left_to_top_right.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.top_left_to_top_right.desc",
                "fancymenu.decoration_overlays.string_lights.color.top_left_to_top_right.desc");

        addStringLightSubMenu(stringsMenu,
                "string_lights_bottom_left_to_bottom_right",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.bottom_left_to_bottom_right.label"),
                this.stringLightsBottomLeftToBottomRight,
                this.stringLightsBottomLeftToBottomRightChristmasMode,
                this.stringLightsBottomLeftToBottomRightColorHex,
                "fancymenu.decoration_overlays.string_lights.position.bottom_left_to_bottom_right.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.bottom_left_to_bottom_right.desc",
                "fancymenu.decoration_overlays.string_lights.color.bottom_left_to_bottom_right.desc");

        addStringLightSubMenu(stringsMenu,
                "string_lights_loose_left_top",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.loose_left_top.label"),
                this.stringLightsLooseLeftTop,
                this.stringLightsLooseLeftTopChristmasMode,
                this.stringLightsLooseLeftTopColorHex,
                "fancymenu.decoration_overlays.string_lights.position.loose_left_top.desc",
                "fancymenu.decoration_overlays.string_lights.christmas_mode.loose_left_top.desc",
                "fancymenu.decoration_overlays.string_lights.color.loose_left_top.desc");

        addStringLightSubMenu(stringsMenu,
                "string_lights_loose_right_top",
                Component.translatable("fancymenu.decoration_overlays.string_lights.position.loose_right_top.label"),
                this.stringLightsLooseRightTop,
                this.stringLightsLooseRightTopChristmasMode,
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

        String scaleString = this.stringLightsScale.getString();
        if (scaleString == null) scaleString = "1.0";
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

        String windStrengthString = this.stringLightsWindStrength.getString();
        if (windStrengthString == null) windStrengthString = "1.0";
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

        String flickerSpeedString = this.stringLightsFlickerSpeed.getString();
        if (flickerSpeedString == null) flickerSpeedString = "1.0";
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

        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER, this.stringLightsLeftCenterToTopCenter.tryGetNonNullElse(true));
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.RIGHT_CENTER_TO_TOP_CENTER, this.stringLightsRightCenterToTopCenter.tryGetNonNullElse(true));
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.BOTTOM_LEFT_TO_TOP_CENTER, this.stringLightsBottomLeftToTopCenter.tryGetNonNullElse(false));
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.BOTTOM_RIGHT_TO_TOP_CENTER, this.stringLightsBottomRightToTopCenter.tryGetNonNullElse(false));
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT, this.stringLightsTopLeftToTopRight.tryGetNonNullElse(true));
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.BOTTOM_LEFT_TO_BOTTOM_RIGHT, this.stringLightsBottomLeftToBottomRight.tryGetNonNullElse(false));
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.LOOSE_LEFT_TOP, this.stringLightsLooseLeftTop.tryGetNonNullElse(false));
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.LOOSE_RIGHT_TOP, this.stringLightsLooseRightTop.tryGetNonNullElse(false));
        this.overlay.setPositionChristmasMode(StringLightsOverlay.StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER, this.stringLightsLeftCenterToTopCenterChristmasMode.tryGetNonNullElse(false));
        this.overlay.setPositionChristmasMode(StringLightsOverlay.StringLightsPosition.RIGHT_CENTER_TO_TOP_CENTER, this.stringLightsRightCenterToTopCenterChristmasMode.tryGetNonNullElse(false));
        this.overlay.setPositionChristmasMode(StringLightsOverlay.StringLightsPosition.BOTTOM_LEFT_TO_TOP_CENTER, this.stringLightsBottomLeftToTopCenterChristmasMode.tryGetNonNullElse(false));
        this.overlay.setPositionChristmasMode(StringLightsOverlay.StringLightsPosition.BOTTOM_RIGHT_TO_TOP_CENTER, this.stringLightsBottomRightToTopCenterChristmasMode.tryGetNonNullElse(false));
        this.overlay.setPositionChristmasMode(StringLightsOverlay.StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT, this.stringLightsTopLeftToTopRightChristmasMode.tryGetNonNullElse(false));
        this.overlay.setPositionChristmasMode(StringLightsOverlay.StringLightsPosition.BOTTOM_LEFT_TO_BOTTOM_RIGHT, this.stringLightsBottomLeftToBottomRightChristmasMode.tryGetNonNullElse(false));
        this.overlay.setPositionChristmasMode(StringLightsOverlay.StringLightsPosition.LOOSE_LEFT_TOP, this.stringLightsLooseLeftTopChristmasMode.tryGetNonNullElse(false));
        this.overlay.setPositionChristmasMode(StringLightsOverlay.StringLightsPosition.LOOSE_RIGHT_TOP, this.stringLightsLooseRightTopChristmasMode.tryGetNonNullElse(false));

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
                                       @NotNull Property<Boolean> showProperty,
                                       @NotNull Property<Boolean> christmasProperty,
                                       @NotNull Property.ColorProperty colorProperty,
                                       @NotNull String showTooltipKey, @NotNull String christmasTooltipKey, @NotNull String colorTooltipKey) {
        ContextMenu subMenu = new ContextMenu();
        addTo.addSubMenuEntry(entryIdentifier + "_menu", label, subMenu).setStackable(true);

        showProperty.buildContextMenuEntryAndAddTo(subMenu, this)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable(showTooltipKey)));

        christmasProperty.buildContextMenuEntryAndAddTo(subMenu, this)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable(christmasTooltipKey)));

        colorProperty.buildContextMenuEntryAndAddTo(subMenu, this)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable(colorTooltipKey)));
    }

}
