package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayLook;
import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayOptions;
import de.keksuccino.fancymenu.customization.decorationoverlay.PixelDecorationOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Strings of lights along the edges of the screen, in a detailed look (teardrop bulbs on a twisted wire) and a simple look
 * (flat lamps on a plain wire). A pine garland can replace the wire, with baubles between the bulbs. All bulbs run one of the
 * light programs. Everything is front content.
 */
public class StringLightsDecorationOverlay extends PixelDecorationOverlay<StringLightsDecorationOverlay> {

    private static final String DEFAULT_LIGHT_COLOR_HEX = "#FFD27A";
    private static final String DEFAULT_STRING_COLOR_HEX = "#2B2B2BD2";
    /** The default string color, which the detailed look draws as its green twisted wire */
    static final int DEFAULT_STRING_COLOR = 0xD22B2B2B;
    private static final String PATTERN_SEPARATOR = ",";
    private static final Set<StringLightsPosition> POSITIONS_SHOWN_BY_DEFAULT = Collections.unmodifiableSet(EnumSet.of(StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER, StringLightsPosition.RIGHT_CENTER_TO_TOP_CENTER, StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT));
    private static final float MIN_SCALE = 0.35F;
    private static final float MAX_SCALE = 2.5F;
    private static final float MAX_WIND_STRENGTH = 2.0F;
    private static final float MAX_FLICKER_SPEED = 2.0F;
    /** Reading the options parses placeholders, so they get read a few times per second instead of every frame */
    private static final float SETTINGS_REFRESH_SECONDS = 0.1F;

    public final DecorationOverlayOptions.Look look = new DecorationOverlayOptions.Look(this, DecorationOverlayLook.DETAILED);
    public final Property.BooleanProperty pineGarland = putProperty(Property.booleanProperty("string_lights_pine_garland", false, "fancymenu.decoration_overlays.string_lights.pine_garland"));
    public final Property<StringLightsProgram> program = putProperty(Property.enumProperty("string_lights_program", StringLightsProgram.CYCLE, "fancymenu.decoration_overlays.string_lights.program", StringLightsProgram::getLocalizationKey));
    public final Property.FloatProperty stringLightsScale = putProperty(Property.floatProperty("string_lights_scale", 1.0F, "fancymenu.decoration_overlays.string_lights.scale"));
    public final Property.FloatProperty stringLightsWindStrength = putProperty(Property.floatProperty("string_lights_wind_strength", 1.0F, "fancymenu.decoration_overlays.string_lights.wind_strength"));
    public final Property.FloatProperty stringLightsFlickerSpeed = putProperty(Property.floatProperty("string_lights_flicker_speed", 1.0F, "fancymenu.decoration_overlays.string_lights.flicker_speed"));
    public final Map<StringLightsPosition, StringLightProperties> stringLights = this.createStringLightProperties();

    private final PixelCanvas wireCanvas = this.createScreenCanvas("string_lights_wires");
    private final LightString[] strings;
    /** Ramps of the light colors in use, so equal colors share their palettes */
    private final Map<Integer, StringLightsArt.BulbRamp> rampCache = new HashMap<>();
    private int layoutWidth = 0;
    private int layoutHeight = 0;
    private boolean settingsRead = false;
    private float settingsTimer = 0.0F;
    private boolean geometryDirty = true;
    private boolean repaintRequested = true;
    private boolean simple = false;
    private boolean garland = false;
    @NotNull
    private StringLightsProgram lightProgram = StringLightsProgram.CYCLE;
    private float scale = 1.0F;
    private int spriteScale = 1;
    private float windStrength = 1.0F;
    private float flickerSpeed = 1.0F;
    /** Clock of the light programs, runs at the Flicker Speed */
    private float lightSeconds = 0.0F;
    /** Real time, for the switches of the cycle program */
    private float cycleSeconds = 0.0F;

    public StringLightsDecorationOverlay() {
        StringLightsPosition[] positions = StringLightsPosition.values();
        this.strings = new LightString[positions.length];
        for (StringLightsPosition position : positions) {
            this.strings[position.ordinal()] = new LightString(position, this.random);
        }
    }

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.look.addEntry(menu, this);

        this.pineGarland.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.PARK)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.pine_garland.desc")));

        this.program.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.AUTO_AWESOME)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.string_lights.program.desc")));

        menu.addSeparatorEntry("separator_after_string_lights_look");

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
    protected void onResize(int width, int height) {
        this.layoutWidth = width;
        this.layoutHeight = height;
        // The canvas is empty after a resize
        for (LightString string : this.strings) string.forgetPainted();
        this.geometryDirty = true;
    }

    @Override
    protected void update(float deltaSeconds) {
        this.settingsTimer -= deltaSeconds;
        if (!this.settingsRead || (this.settingsTimer <= 0.0F)) {
            this.refreshSettings();
            this.settingsTimer = SETTINGS_REFRESH_SECONDS;
        }
        if (this.geometryDirty) {
            for (LightString string : this.strings) {
                string.layout(this.layoutWidth, this.layoutHeight, this.scale);
                this.layoutBulbs(string);
            }
            this.geometryDirty = false;
            this.repaintRequested = true;
        }
        this.lightSeconds += deltaSeconds * this.flickerSpeed;
        this.cycleSeconds += deltaSeconds;
        boolean rasterChanged = this.repaintRequested;
        for (LightString string : this.strings) {
            string.windTime += deltaSeconds * string.windSpeed;
            if (!this.isShown(string)) continue;
            rasterChanged |= string.raster(this.layoutHeight, this.windStrength, this.spriteScale);
            string.anchorBulbs();
        }
        if (rasterChanged) this.repaintWires();
        this.repaintRequested = false;
    }

    /**
     * Paints all wires again. Strings overlap, so clearing one would cut holes into the others.
     */
    private void repaintWires() {
        for (LightString string : this.strings) string.clearPainted(this.wireCanvas);
        for (LightString string : this.strings) {
            if (!this.isShown(string)) continue;
            StringLightProperties properties = this.getProperties(string);
            int wire = properties.wireColor;
            boolean detailedDefault = !this.simple && (wire == DEFAULT_STRING_COLOR);
            int dark = detailedDefault ? StringLightsArt.WIRE_DARK : wire;
            int light = detailedDefault ? StringLightsArt.WIRE_LIGHT : PixelMath.withAlpha(PixelMath.mix(PixelMath.opaque(wire), 0xFFFFFFFF, 0.15F), wire >>> 24);
            string.paint(this.wireCanvas, this.simple, this.garland, dark, light);
        }
    }

    @Override
    protected void drawFront(@NotNull PixelGraphics graphics) {
        graphics.canvas(this.wireCanvas, 0, 0);
        StringLightsProgram running = this.lightProgram.resolve(this.cycleSeconds);
        for (LightString string : this.strings) {
            if (!this.isShown(string) || (string.rasterCount == 0)) continue;
            if (this.garland) this.drawBaubles(graphics, string);
            this.drawBulbs(graphics, string, running);
        }
    }

    private void drawBaubles(@NotNull PixelGraphics graphics, @NotNull LightString string) {
        int s = this.spriteScale;
        for (int j = 0; j < string.baubleCount; j++) {
            int point = string.baublePoint[j];
            int x = string.rasterX[point] - 2 * s - ((s - 1) >> 1);
            graphics.sprite(StringLightsArt.BAUBLE, string.baublePalette[j], x, string.rasterY[point] + 2, false, false, s);
        }
    }

    private void drawBulbs(@NotNull PixelGraphics graphics, @NotNull LightString string, @NotNull StringLightsProgram running) {
        // On the garland a teardrop bulb hangs one pixel lower, so its socket shows below the needles
        int drop = (this.garland && !this.simple) ? 1 : 0;
        for (int i = 0; i < string.bulbCount; i++) {
            int s = string.bulbScale[i];
            if (s <= 0) continue;
            StringLightsArt.BulbRamp ramp = string.bulbRamp[i];
            int level = running.level(i, string.index, this.lightSeconds);
            int point = string.bulbPoint[i];
            int px = string.rasterX[point];
            int py = string.rasterY[point];
            int center = (s - 1) >> 1;
            if (this.simple) {
                if (level > 0) drawCentered(graphics, StringLightsArt.SIMPLE_HALOS[level], ramp.simpleHalo[level], px, py, s, ramp.tint);
                graphics.sprite(StringLightsArt.SIMPLE_BULB, ramp.simpleBulb[level], px - 2 * s - center, py - 2 * s - center, false, false, s, ramp.tint);
            } else {
                int sway = Math.round((float)Math.sin(string.windTime * 2.2F + i * 0.9F) * 0.9F * this.windStrength);
                int x = px + sway - 2 * s - center;
                int y = py + 1 + drop;
                if (level > 0) drawCentered(graphics, StringLightsArt.HALOS[level], ramp.halo[level], px + sway, y + 4 * s + center, s, ramp.tint);
                graphics.sprite(StringLightsArt.BULB, ramp.bulb[level], x, y, false, false, s, ramp.tint);
            }
        }
    }

    /**
     * Draws an odd-sized sprite with its middle pixel on the given GUI pixel.
     */
    private static void drawCentered(@NotNull PixelGraphics graphics, @NotNull PixelSprite sprite, @NotNull PixelPalette palette, int x, int y, int scale, int tint) {
        int center = (scale - 1) >> 1;
        graphics.sprite(sprite, palette, x - (sprite.width() >> 1) * scale - center, y - (sprite.height() >> 1) * scale - center, false, false, scale, tint);
    }

    private boolean isShown(@NotNull LightString string) {
        return this.getProperties(string).enabled;
    }

    @NotNull
    private StringLightProperties getProperties(@NotNull LightString string) {
        return Objects.requireNonNull(this.stringLights.get(string.position));
    }

    @NotNull
    LightString getString(@NotNull StringLightsPosition position) {
        return this.strings[position.ordinal()];
    }

    int getSpriteScale() {
        return this.spriteScale;
    }

    boolean isSimple() {
        return this.simple;
    }

    /**
     * Reads all options and marks what has to be built again.
     */
    void refreshSettings() {
        this.settingsRead = true;
        boolean simple = this.look.isSimple();
        boolean garland = this.pineGarland.getBoolean();
        if ((simple != this.simple) || (garland != this.garland)) this.repaintRequested = true;
        this.simple = simple;
        this.garland = garland;
        this.lightProgram = this.program.tryGetNonNullElse(StringLightsProgram.CYCLE);
        float scale = PixelMath.clamp(finiteOr(this.stringLightsScale.getFloat(), 1.0F), MIN_SCALE, MAX_SCALE);
        this.windStrength = PixelMath.clamp(finiteOr(this.stringLightsWindStrength.getFloat(), 1.0F), 0.0F, MAX_WIND_STRENGTH);
        this.flickerSpeed = PixelMath.clamp(finiteOr(this.stringLightsFlickerSpeed.getFloat(), 1.0F), 0.0F, MAX_FLICKER_SPEED);
        if (scale != this.scale) {
            this.scale = scale;
            this.spriteScale = PixelMath.clamp(Math.round(scale), 1, 3);
            this.geometryDirty = true;
        }
        for (StringLightProperties properties : this.stringLights.values()) {
            int changes = properties.refresh(this.rampCache);
            if ((changes & StringLightProperties.CHANGED_WIRE) != 0) this.repaintRequested = true;
            if (((changes & StringLightProperties.CHANGED_BULBS) != 0) && !this.geometryDirty) this.layoutBulbs(this.getString(properties.position));
        }
    }

    private void layoutBulbs(@NotNull LightString string) {
        StringLightProperties properties = this.getProperties(string);
        string.layoutBulbs(properties.configuredCount, LightString.BULB_SPACING * this.scale, this.spriteScale, properties.ramp, properties.christmas, properties.sizes, properties.customRamps);
    }

    private static float finiteOr(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    @NotNull
    private Map<StringLightsPosition, StringLightProperties> createStringLightProperties() {
        EnumMap<StringLightsPosition, StringLightProperties> properties = new EnumMap<>(StringLightsPosition.class);
        for (StringLightsPosition position : StringLightsPosition.values()) {
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

    @NotNull
    private static StringLightsArt.BulbRamp cachedRamp(@NotNull Map<Integer, StringLightsArt.BulbRamp> cache, int argb) {
        return cache.computeIfAbsent(argb, StringLightsArt::rampOf);
    }

    /**
     * The properties of one string of the overlay.<br>
     * The property keys are built from the name of the string's {@link StringLightsPosition}.
     * Layouts store these keys, so neither the keys nor the position names should ever change.
     */
    public final class StringLightProperties {

        static final int CHANGED_BULBS = 1;
        static final int CHANGED_WIRE = 2;

        @NotNull
        public final StringLightsPosition position;
        public final Property.BooleanProperty show;
        public final Property.BooleanProperty christmasMode;
        public final Property.ColorProperty lightColor;
        public final Property.IntegerProperty lightCount;
        public final Property.ColorProperty stringColor;
        public final Property.StringProperty lightSizes;
        public final Property.StringProperty lightColors;
        @NotNull
        private final String positionName;
        boolean enabled = false;
        boolean christmas = false;
        @NotNull
        StringLightsArt.BulbRamp ramp = StringLightsArt.WARM;
        int configuredCount = 0;
        int wireColor = DEFAULT_STRING_COLOR;
        @NotNull
        List<Float> sizes = List.of();
        @NotNull
        List<StringLightsArt.BulbRamp> customRamps = List.of();
        @Nullable
        private String lastLightColorHex = null;
        @Nullable
        private String lastStringColorHex = null;
        @Nullable
        private String lastLightSizes = null;
        @Nullable
        private String lastLightColors = null;

        private StringLightProperties(@NotNull StringLightsPosition position) {
            this.position = position;
            this.positionName = position.name().toLowerCase(Locale.ROOT);
            String keyBase = "string_lights_" + this.positionName;
            this.show = putProperty(Property.booleanProperty(keyBase, POSITIONS_SHOWN_BY_DEFAULT.contains(position), "fancymenu.decoration_overlays.string_lights.string.show"));
            this.christmasMode = putProperty(Property.booleanProperty(keyBase + "_christmas_mode", false, "fancymenu.decoration_overlays.string_lights.string.christmas_mode"));
            this.lightColor = putProperty(Property.hexColorProperty(keyBase + "_color_hex", DEFAULT_LIGHT_COLOR_HEX, true, "fancymenu.decoration_overlays.string_lights.string.color"));
            this.lightCount = putProperty(Property.integerProperty(keyBase + "_light_count", 0, "fancymenu.decoration_overlays.string_lights.string.light_count", Property.NumericInputBehavior.<Integer>builder().rangeInput(0, LightString.MAX_BULBS).build()));
            this.stringColor = putProperty(Property.hexColorProperty(keyBase + "_string_color_hex", DEFAULT_STRING_COLOR_HEX, true, "fancymenu.decoration_overlays.string_lights.string.string_color"));
            this.lightSizes = putProperty(Property.stringProperty(keyBase + "_light_sizes", "", false, true, "fancymenu.decoration_overlays.string_lights.string.light_sizes"));
            this.lightColors = putProperty(Property.stringProperty(keyBase + "_light_colors", "", false, true, "fancymenu.decoration_overlays.string_lights.string.light_colors"));
        }

        /**
         * Reads the properties of the string. Returns what changed, as {@link #CHANGED_BULBS} and {@link #CHANGED_WIRE} flags.
         */
        int refresh(@NotNull Map<Integer, StringLightsArt.BulbRamp> rampCache) {
            int changes = 0;
            boolean enabled = this.show.getBoolean();
            if (enabled != this.enabled) changes |= CHANGED_WIRE;
            this.enabled = enabled;
            boolean christmas = this.christmasMode.getBoolean();
            if (christmas != this.christmas) changes |= CHANGED_BULBS;
            this.christmas = christmas;
            int count = PixelMath.clamp(this.lightCount.getInteger(), 0, LightString.MAX_BULBS);
            if (count != this.configuredCount) changes |= CHANGED_BULBS;
            this.configuredCount = count;

            String lightColorHex = this.lightColor.getHex();
            if (!lightColorHex.equals(this.lastLightColorHex)) {
                this.lastLightColorHex = lightColorHex;
                this.ramp = cachedRamp(rampCache, this.lightColor.getDrawable().getColorInt());
                changes |= CHANGED_BULBS;
            }

            String stringColorHex = this.stringColor.getHex();
            if (!stringColorHex.equals(this.lastStringColorHex)) {
                this.lastStringColorHex = stringColorHex;
                this.wireColor = this.stringColor.getDrawable().getColorInt();
                changes |= CHANGED_WIRE;
            }

            String sizes = resolvePatternText(this.lightSizes);
            if (!sizes.equals(this.lastLightSizes)) {
                this.lastLightSizes = sizes;
                this.sizes = parseLightSizes(sizes);
                changes |= CHANGED_BULBS;
            }

            String colors = resolvePatternText(this.lightColors);
            if (!colors.equals(this.lastLightColors)) {
                this.lastLightColors = colors;
                List<Integer> parsed = parseLightColors(colors);
                List<StringLightsArt.BulbRamp> ramps = new ArrayList<>(parsed.size());
                for (Integer color : parsed) ramps.add((color != null) ? cachedRamp(rampCache, color) : null);
                this.customRamps = Collections.unmodifiableList(ramps);
                changes |= CHANGED_BULBS;
            }

            return changes;
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
