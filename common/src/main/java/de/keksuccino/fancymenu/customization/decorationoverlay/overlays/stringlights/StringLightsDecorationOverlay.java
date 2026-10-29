package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.overlay.StringLightsOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class StringLightsDecorationOverlay extends AbstractDecorationOverlay<StringLightsDecorationOverlay> {

    private static final String DEFAULT_LIGHT_COLOR_HEX = "#FFD27A";
    private static final String DEFAULT_STRING_COLOR_HEX = "#2B2B2BD2";
    private static final String PATTERN_SEPARATOR = ",";
    private static final Set<StringLightsOverlay.StringLightsPosition> POSITIONS_SHOWN_BY_DEFAULT = Collections.unmodifiableSet(EnumSet.of(StringLightsOverlay.StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER, StringLightsOverlay.StringLightsPosition.RIGHT_CENTER_TO_TOP_CENTER, StringLightsOverlay.StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT));

    public final Property.FloatProperty stringLightsScale = putProperty(Property.floatProperty("string_lights_scale", 1.0F, "fancymenu.decoration_overlays.string_lights.scale"));
    public final Property.FloatProperty stringLightsWindStrength = putProperty(Property.floatProperty("string_lights_wind_strength", 1.0F, "fancymenu.decoration_overlays.string_lights.wind_strength"));
    public final Property.FloatProperty stringLightsFlickerSpeed = putProperty(Property.floatProperty("string_lights_flicker_speed", 1.0F, "fancymenu.decoration_overlays.string_lights.flicker_speed"));
    public final Map<StringLightsOverlay.StringLightsPosition, StringLightProperties> stringLights = this.createStringLightProperties();

    protected final StringLightsOverlay overlay = new StringLightsOverlay(0, 0);

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.stringLightsScale.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.STRAIGHTEN)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.scale.desc")));

        this.stringLightsWindStrength.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.AIR)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.wind_strength.desc")));

        this.stringLightsFlickerSpeed.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.SHUTTER_SPEED)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.flicker_speed.desc")));

        menu.addSeparatorEntry("separator_before_string_light_strings");

        ContextMenu stringsMenu = new ContextMenu();
        menu.addSubMenuEntry("string_lights_strings", Component.translatable("fancymenu.decoration_overlays.string_lights.strings"), stringsMenu)
                .setIcon(MaterialIcons.LIGHTBULB)
                .setStackable(true);

        for (StringLightProperties stringLight : this.stringLights.values()) {
            stringLight.addConfigSubMenuTo(stringsMenu);
        }

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.updateOverlay();

        this.overlay.setWidth(getScreenWidth());
        this.overlay.setHeight(getScreenHeight());
        this.overlay.render(graphics, mouseX, mouseY, partial);

    }

    /**
     * Pushes the current property values to the {@link StringLightsOverlay}.
     */
    protected void updateOverlay() {
        this.overlay.setScale(this.stringLightsScale.getFloat());
        this.overlay.setWindStrength(this.stringLightsWindStrength.getFloat());
        this.overlay.setFlickerSpeed(this.stringLightsFlickerSpeed.getFloat());
        for (StringLightProperties stringLight : this.stringLights.values()) {
            stringLight.applyTo(this.overlay);
        }
    }

    @NotNull
    private Map<StringLightsOverlay.StringLightsPosition, StringLightProperties> createStringLightProperties() {
        EnumMap<StringLightsOverlay.StringLightsPosition, StringLightProperties> properties = new EnumMap<>(StringLightsOverlay.StringLightsPosition.class);
        for (StringLightsOverlay.StringLightsPosition position : StringLightsOverlay.StringLightsPosition.values()) {
            properties.put(position, new StringLightProperties(position));
        }
        return Collections.unmodifiableMap(properties);
    }

    /**
     * Parses a comma-separated list of light size multipliers.<br>
     * Empty or invalid entries become {@code null}, so they keep the default size of their light without shifting the following entries.
     */
    @NotNull
    static List<Float> parseLightSizes(@NotNull String serialized) {
        if (serialized.isBlank()) {
            return List.of();
        }
        String[] entries = serialized.split(PATTERN_SEPARATOR);
        List<Float> sizes = new ArrayList<>(entries.length);
        for (String entry : entries) {
            Float size = null;
            try {
                float parsed = Float.parseFloat(entry.trim());
                if (Float.isFinite(parsed)) {
                    size = parsed;
                }
            } catch (NumberFormatException ignored) {
            }
            sizes.add(size);
        }
        return sizes;
    }

    /**
     * Parses a comma-separated list of HEX light colors into ARGB colors.<br>
     * Empty or invalid entries become {@code null}, so they keep the default color of their light without shifting the following entries.
     */
    @NotNull
    static List<Integer> parseLightColors(@NotNull String serialized) {
        if (serialized.isBlank()) {
            return List.of();
        }
        String[] entries = serialized.split(PATTERN_SEPARATOR);
        List<Integer> colors = new ArrayList<>(entries.length);
        for (String entry : entries) {
            String trimmed = entry.trim();
            Integer color = null;
            if (!trimmed.isEmpty()) {
                DrawableColor parsed = DrawableColor.of(trimmed);
                // DrawableColor returns its shared EMPTY instance when it fails to parse the HEX code.
                if (parsed != DrawableColor.EMPTY) {
                    color = parsed.getColorInt();
                }
            }
            colors.add(color);
        }
        return colors;
    }

    @NotNull
    private static String resolvePatternText(@NotNull Property.StringProperty property) {
        String raw = property.get();
        // Most strings don't use patterns, so skip placeholder parsing for them.
        if ((raw == null) || raw.isBlank()) {
            return "";
        }
        return Objects.requireNonNullElse(property.getString(), "");
    }

    /**
     * The properties of one string of the overlay.<br>
     * The property keys are built from the name of the string's {@link StringLightsOverlay.StringLightsPosition}.
     * Layouts store these keys, so neither the keys nor the position names should ever change.
     */
    public final class StringLightProperties {

        @NotNull
        public final StringLightsOverlay.StringLightsPosition position;
        public final Property.BooleanProperty show;
        public final Property.BooleanProperty christmasMode;
        public final Property.ColorProperty lightColor;
        public final Property.IntegerProperty lightCount;
        public final Property.ColorProperty stringColor;
        public final Property.StringProperty lightSizes;
        public final Property.StringProperty lightColors;
        @NotNull
        private final String positionName;
        @Nullable
        private String lastLightColorHex = null;
        @Nullable
        private String lastStringColorHex = null;
        @Nullable
        private String lastLightSizes = null;
        @Nullable
        private String lastLightColors = null;

        private StringLightProperties(@NotNull StringLightsOverlay.StringLightsPosition position) {
            this.position = position;
            this.positionName = position.name().toLowerCase(Locale.ROOT);
            String keyBase = "string_lights_" + this.positionName;
            this.show = putProperty(Property.booleanProperty(keyBase, POSITIONS_SHOWN_BY_DEFAULT.contains(position), "fancymenu.decoration_overlays.string_lights.string.show"));
            this.christmasMode = putProperty(Property.booleanProperty(keyBase + "_christmas_mode", false, "fancymenu.decoration_overlays.string_lights.string.christmas_mode"));
            this.lightColor = putProperty(Property.hexColorProperty(keyBase + "_color_hex", DEFAULT_LIGHT_COLOR_HEX, true, "fancymenu.decoration_overlays.string_lights.string.color"));
            this.lightCount = putProperty(Property.integerProperty(keyBase + "_light_count", StringLightsOverlay.AUTO_BULB_COUNT, "fancymenu.decoration_overlays.string_lights.string.light_count", Property.NumericInputBehavior.<Integer>builder().rangeInput(StringLightsOverlay.AUTO_BULB_COUNT, StringLightsOverlay.MAX_CUSTOM_BULB_COUNT).build()));
            this.stringColor = putProperty(Property.hexColorProperty(keyBase + "_string_color_hex", DEFAULT_STRING_COLOR_HEX, true, "fancymenu.decoration_overlays.string_lights.string.string_color"));
            this.lightSizes = putProperty(Property.stringProperty(keyBase + "_light_sizes", "", false, true, "fancymenu.decoration_overlays.string_lights.string.light_sizes"));
            this.lightColors = putProperty(Property.stringProperty(keyBase + "_light_colors", "", false, true, "fancymenu.decoration_overlays.string_lights.string.light_colors"));
        }

        private void applyTo(@NotNull StringLightsOverlay overlay) {

            overlay.setPositionEnabled(this.position, this.show.getBoolean());
            overlay.setPositionChristmasMode(this.position, this.christmasMode.getBoolean());
            overlay.setPositionBulbCount(this.position, this.lightCount.getInteger());

            String lightColorHex = this.lightColor.getHex();
            if (!lightColorHex.equals(this.lastLightColorHex)) {
                this.lastLightColorHex = lightColorHex;
                overlay.setPositionColor(this.position, this.lightColor.getDrawable().getColorInt());
            }

            String stringColorHex = this.stringColor.getHex();
            if (!stringColorHex.equals(this.lastStringColorHex)) {
                this.lastStringColorHex = stringColorHex;
                overlay.setPositionStringColor(this.position, this.stringColor.getDrawable().getColorInt());
            }

            String sizes = resolvePatternText(this.lightSizes);
            if (!sizes.equals(this.lastLightSizes)) {
                this.lastLightSizes = sizes;
                overlay.setPositionBulbSizes(this.position, parseLightSizes(sizes));
            }

            String colors = resolvePatternText(this.lightColors);
            if (!colors.equals(this.lastLightColors)) {
                this.lastLightColors = colors;
                overlay.setPositionBulbColors(this.position, parseLightColors(colors));
            }

        }

        private void addConfigSubMenuTo(@NotNull ContextMenu addTo) {

            String showTooltipKey = "fancymenu.decoration_overlays.string_lights.position." + this.positionName + ".desc";
            String lightColorTooltipKey = "fancymenu.decoration_overlays.string_lights.color." + this.positionName + ".desc";
            String christmasModeTooltipKey = "fancymenu.decoration_overlays.string_lights.christmas_mode." + this.positionName + ".desc";

            ContextMenu subMenu = new ContextMenu();
            addTo.addSubMenuEntry("string_lights_" + this.positionName + "_menu", Component.translatable("fancymenu.decoration_overlays.string_lights.position." + this.positionName + ".label"), subMenu)
                    .setIcon(MaterialIcons.LIGHT)
                    .setStackable(true);

            this.show.buildContextMenuEntryAndAddTo(subMenu, StringLightsDecorationOverlay.this)
                    .setIcon(MaterialIcons.VISIBILITY)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable(showTooltipKey)));

            subMenu.addSeparatorEntry("separator_after_show_string_light");

            this.lightCount.buildContextMenuEntryAndAddTo(subMenu, StringLightsDecorationOverlay.this)
                    .setIcon(MaterialIcons.NUMBERS)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.string.light_count.desc")));

            this.lightColor.buildContextMenuEntryAndAddTo(subMenu, StringLightsDecorationOverlay.this)
                    .setIcon(MaterialIcons.PALETTE)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable(lightColorTooltipKey)));

            this.christmasMode.buildContextMenuEntryAndAddTo(subMenu, StringLightsDecorationOverlay.this)
                    .setIcon(MaterialIcons.CELEBRATION)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable(christmasModeTooltipKey)));

            this.stringColor.buildContextMenuEntryAndAddTo(subMenu, StringLightsDecorationOverlay.this)
                    .setIcon(MaterialIcons.LINE_CURVE)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.string.string_color.desc")));

            subMenu.addSeparatorEntry("separator_before_individual_lights");

            this.lightSizes.buildContextMenuEntryAndAddTo(subMenu, StringLightsDecorationOverlay.this)
                    .setIcon(MaterialIcons.PHOTO_SIZE_SELECT_SMALL)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.string.light_sizes.desc")));

            this.lightColors.buildContextMenuEntryAndAddTo(subMenu, StringLightsDecorationOverlay.this)
                    .setIcon(MaterialIcons.COLORS)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.string.light_colors.desc")));

        }

    }

}
