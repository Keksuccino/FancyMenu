package de.keksuccino.fancymenu.util.rendering.overlay;

import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

public class StringLightsOverlay extends AbstractWidget implements NavigatableWidget {

    /** Bulb count that lets the overlay calculate the bulb count of a string from its length. */
    public static final int AUTO_BULB_COUNT = 0;
    public static final int MAX_CUSTOM_BULB_COUNT = 100;
    public static final float MIN_BULB_SIZE_MULTIPLIER = 0.0F;
    public static final float MAX_BULB_SIZE_MULTIPLIER = 10.0F;
    public static final int DEFAULT_STRING_COLOR = 0xD22B2B2B;

    private static final float BASE_BULB_SPACING = 30.0F;
    private static final float BASE_BULB_SIZE = 5.5F;
    private static final float BASE_STRING_THICKNESS = 1.6F;
    private static final float BOTTOM_STRING_BOTTOM_MARGIN = 30.0F;
    private static final float MIN_SCALE = 0.35F;
    private static final float MAX_SCALE = 2.5F;
    private static final float MIN_WIND_STRENGTH = 0.0F;
    private static final float MAX_WIND_STRENGTH = 2.0F;
    private static final float MIN_FLICKER_SPEED = 0.0F;
    private static final float MAX_FLICKER_SPEED = 2.0F;
    private static final float MAX_DELTA_SECONDS = 0.1F;
    private static final int MIN_AUTO_BULBS = 3;
    private static final int MAX_AUTO_BULBS = 36;

    private static final int[] CHRISTMAS_COLORS = new int[] {
            0xE04B4B,
            0x3CBF6B,
            0x3E7DDC,
            0xF0D458,
            0xF28A3C,
            0xE45CA4,
            0x4FD1C5
    };

    private final RandomSource random = RandomSource.create();
    private final EnumMap<StringLightsPosition, StringLight> strings = new EnumMap<>(StringLightsPosition.class);
    private final EnumMap<StringLightsPosition, PositionSettings> positionSettings = createPositionSettings();
    private int lastWidth = -1;
    private int lastHeight = -1;
    private float lastScale = -1.0F;
    private long lastUpdateMs = -1L;
    private float scale = 1.0F;
    private float windStrength = 1.0F;
    private float flickerSpeed = 1.0F;
    private int baseColor = 0xFFFFD27A;
    private float baseAlphaScale = 1.0F;
    private boolean christmasMode = false;

    public StringLightsOverlay(int width, int height) {
        super(0, 0, width, height, Component.empty());
    }

    public void setScale(float scale) {
        this.scale = Mth.clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    public float getScale() {
        return this.scale;
    }

    public void setWindStrength(float windStrength) {
        this.windStrength = Mth.clamp(windStrength, MIN_WIND_STRENGTH, MAX_WIND_STRENGTH);
    }

    public float getWindStrength() {
        return this.windStrength;
    }

    public void setFlickerSpeed(float flickerSpeed) {
        this.flickerSpeed = Mth.clamp(flickerSpeed, MIN_FLICKER_SPEED, MAX_FLICKER_SPEED);
    }

    public float getFlickerSpeed() {
        return this.flickerSpeed;
    }

    public void setColor(int color) {
        this.baseColor = color;
        this.baseAlphaScale = resolveAlphaScale(color);
    }

    public int getColor() {
        return this.baseColor;
    }

    public void setChristmasMode(boolean christmasMode) {
        this.christmasMode = christmasMode;
    }

    public boolean isChristmasMode() {
        return this.christmasMode;
    }

    public void setPositionChristmasMode(@NotNull StringLightsPosition position, boolean christmasMode) {
        this.positionSettings.get(position).christmasMode = christmasMode;
    }

    public void clearPositionChristmasMode(@NotNull StringLightsPosition position) {
        this.positionSettings.get(position).christmasMode = null;
    }

    public void setPositionEnabled(@NotNull StringLightsPosition position, boolean enabled) {
        this.positionSettings.get(position).enabled = enabled;
    }

    public boolean isPositionEnabled(@NotNull StringLightsPosition position) {
        return this.positionSettings.get(position).enabled;
    }

    public void setPositionColor(@NotNull StringLightsPosition position, int color) {
        PositionSettings settings = this.positionSettings.get(position);
        if (settings.color == null) {
            settings.color = new ColorSettings();
        }
        settings.color.color = color;
        settings.color.alphaScale = resolveAlphaScale(color);
    }

    public void clearPositionColor(@NotNull StringLightsPosition position) {
        this.positionSettings.get(position).color = null;
    }

    /**
     * Sets the ARGB color of the string itself. Its alpha channel is used as-is, so an alpha of 0 hides the string.
     */
    public void setPositionStringColor(@NotNull StringLightsPosition position, int color) {
        this.positionSettings.get(position).stringColor = color;
    }

    public int getPositionStringColor(@NotNull StringLightsPosition position) {
        return this.positionSettings.get(position).stringColor;
    }

    /**
     * Sets the number of bulbs on a string. {@link #AUTO_BULB_COUNT} calculates the count from the string length.
     */
    public void setPositionBulbCount(@NotNull StringLightsPosition position, int bulbCount) {
        int resolved = Mth.clamp(bulbCount, AUTO_BULB_COUNT, MAX_CUSTOM_BULB_COUNT);
        PositionSettings settings = this.positionSettings.get(position);
        if (settings.bulbCount != resolved) {
            settings.bulbCount = resolved;
            settings.rebuildRequested = true;
        }
    }

    public int getPositionBulbCount(@NotNull StringLightsPosition position) {
        return this.positionSettings.get(position).bulbCount;
    }

    /**
     * Sets per-bulb size multipliers, going from the start to the end of the string.<br>
     * The list repeats when the string has more bulbs than entries.<br>
     * {@code null} entries keep the default (randomly varied) size of their bulbs.
     */
    public void setPositionBulbSizes(@NotNull StringLightsPosition position, @NotNull List<Float> sizeMultipliers) {
        List<Float> sanitized = new ArrayList<>(sizeMultipliers.size());
        for (Float multiplier : sizeMultipliers) {
            sanitized.add(((multiplier != null) && Float.isFinite(multiplier)) ? Mth.clamp(multiplier, MIN_BULB_SIZE_MULTIPLIER, MAX_BULB_SIZE_MULTIPLIER) : null);
        }
        PositionSettings settings = this.positionSettings.get(position);
        if (settings.bulbSizes.equals(sanitized)) {
            return;
        }
        settings.bulbSizes = Collections.unmodifiableList(sanitized);
        StringLight light = this.strings.get(position);
        if (light != null) {
            applyBulbSizes(light, settings);
        }
    }

    @NotNull
    public List<Float> getPositionBulbSizes(@NotNull StringLightsPosition position) {
        return this.positionSettings.get(position).bulbSizes;
    }

    /**
     * Sets per-bulb ARGB colors, going from the start to the end of the string.<br>
     * The list repeats when the string has more bulbs than entries.<br>
     * {@code null} entries keep the default color of their bulbs. Custom colors override the christmas mode.
     */
    public void setPositionBulbColors(@NotNull StringLightsPosition position, @NotNull List<Integer> colors) {
        PositionSettings settings = this.positionSettings.get(position);
        if (settings.bulbColors.equals(colors)) {
            return;
        }
        settings.bulbColors = Collections.unmodifiableList(new ArrayList<>(colors));
        StringLight light = this.strings.get(position);
        if (light != null) {
            applyBulbColors(light, settings);
        }
    }

    @NotNull
    public List<Integer> getPositionBulbColors(@NotNull StringLightsPosition position) {
        return this.positionSettings.get(position).bulbColors;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        int overlayX = this.getX();
        int overlayY = this.getY();
        int overlayWidth = this.getWidth();
        int overlayHeight = this.getHeight();

        if (overlayWidth <= 0 || overlayHeight <= 0) {
            return;
        }

        boolean sizeChanged = overlayWidth != this.lastWidth || overlayHeight != this.lastHeight;
        boolean scaleChanged = Math.abs(this.scale - this.lastScale) > 0.001F;
        if (sizeChanged || scaleChanged || this.strings.isEmpty()) {
            rebuildStrings(overlayWidth, overlayHeight);
        } else {
            rebuildRequestedStrings(overlayWidth, overlayHeight);
        }

        float deltaSeconds = computeDeltaSeconds(sizeChanged || scaleChanged);
        if (deltaSeconds > 0.0F) {
            updateAnimation(deltaSeconds);
        }

        renderStrings(graphics, overlayX, overlayY, overlayWidth, overlayHeight);
    }

    private void rebuildStrings(int width, int height) {
        this.strings.clear();
        for (StringLightsPosition position : StringLightsPosition.values()) {
            this.strings.put(position, createString(position, width, height, null));
            this.positionSettings.get(position).rebuildRequested = false;
        }
        this.lastWidth = width;
        this.lastHeight = height;
        this.lastScale = this.scale;
    }

    private void rebuildRequestedStrings(int width, int height) {
        for (var entry : this.positionSettings.entrySet()) {
            if (entry.getValue().rebuildRequested) {
                StringLightsPosition position = entry.getKey();
                this.strings.put(position, createString(position, width, height, this.strings.get(position)));
                entry.getValue().rebuildRequested = false;
            }
        }
    }

    private StringLight createString(StringLightsPosition position, int width, int height, @Nullable StringLight previous) {
        PositionSettings settings = this.positionSettings.get(position);
        StringLight light = new StringLight(position);
        light.startX = position.startX * Math.max(1, width - 1) + position.startOffsetX;
        light.startY = position.startY * Math.max(1, height - 1) + position.startOffsetY;
        light.endX = position.endX * Math.max(1, width - 1) + position.endOffsetX;
        light.endY = position.endY * Math.max(1, height - 1) + position.endOffsetY;
        float dx = light.endX - light.startX;
        float dy = light.endY - light.startY;
        light.length = Mth.sqrt(dx * dx + dy * dy);
        light.sag = light.length * position.sagFactor * (0.75F + 0.25F * this.scale);
        light.windAmplitude = light.length * 0.015F * position.windMultiplier;
        if (previous != null) {
            // Strings get rebuilt when their bulb count changes, so keep their sway going to avoid a visible jump.
            light.windSpeed = previous.windSpeed;
            light.windPhase = previous.windPhase;
            light.windTime = previous.windTime;
        } else {
            light.windSpeed = nextRange(0.35F, 0.9F);
            light.windPhase = this.random.nextFloat() * ((float)Math.PI * 2.0F);
            light.windTime = this.random.nextFloat() * ((float)Math.PI * 2.0F);
        }

        int bulbCount = resolveBulbCount(settings.bulbCount, light.length, BASE_BULB_SPACING * this.scale, position.densityMultiplier);
        for (int i = 0; i < bulbCount; i++) {
            LightBulb bulb = new LightBulb();
            bulb.t = (i + 1) / (float)(bulbCount + 1);
            bulb.randomSizeFactor = nextRange(0.75F, 1.25F);
            bulb.baseBrightness = nextRange(0.78F, 1.15F);
            bulb.flickerSpeed = nextRange(0.25F, 0.65F);
            bulb.flickerPhase = this.random.nextFloat() * ((float)Math.PI * 2.0F);
            bulb.flickerTime = this.random.nextFloat() * ((float)Math.PI * 2.0F);
            bulb.christmasRgb = CHRISTMAS_COLORS[this.random.nextInt(CHRISTMAS_COLORS.length)];
            light.bulbs.add(bulb);
        }
        applyBulbSizes(light, settings);
        applyBulbColors(light, settings);

        return light;
    }

    static int resolveBulbCount(int configuredBulbCount, float length, float spacing, float densityMultiplier) {
        if (configuredBulbCount > AUTO_BULB_COUNT) {
            return Math.min(configuredBulbCount, MAX_CUSTOM_BULB_COUNT);
        }
        if (length <= 0.1F) {
            return 0;
        }
        int count = Mth.floor((length / spacing) * densityMultiplier + 0.5F);
        return Mth.clamp(count, MIN_AUTO_BULBS, MAX_AUTO_BULBS);
    }

    /**
     * Returns the pattern entry of the bulb at the given index, repeating the pattern when there are more bulbs than entries.
     * Returns {@code null} for empty patterns and for entries that should keep the bulb's default value.
     */
    @Nullable
    static <T> T resolvePatternEntry(@NotNull List<T> pattern, int bulbIndex) {
        if (pattern.isEmpty()) {
            return null;
        }
        return pattern.get(bulbIndex % pattern.size());
    }

    private void applyBulbSizes(@NotNull StringLight light, @NotNull PositionSettings settings) {
        float bulbBaseSize = BASE_BULB_SIZE * this.scale * light.position.sizeMultiplier;
        float maxBulbSize = 0.0F;
        for (int i = 0; i < light.bulbs.size(); i++) {
            LightBulb bulb = light.bulbs.get(i);
            Float customMultiplier = resolvePatternEntry(settings.bulbSizes, i);
            bulb.size = bulbBaseSize * ((customMultiplier != null) ? customMultiplier : bulb.randomSizeFactor);
            maxBulbSize = Math.max(maxBulbSize, bulb.size);
        }
        light.maxBulbSize = maxBulbSize;
    }

    private void applyBulbColors(@NotNull StringLight light, @NotNull PositionSettings settings) {
        for (int i = 0; i < light.bulbs.size(); i++) {
            LightBulb bulb = light.bulbs.get(i);
            Integer customColor = resolvePatternEntry(settings.bulbColors, i);
            bulb.hasCustomColor = (customColor != null);
            if (customColor != null) {
                bulb.customRgb = customColor & 0x00FFFFFF;
                bulb.customAlphaScale = resolveAlphaScale(customColor);
            }
        }
    }

    private void updateAnimation(float deltaSeconds) {
        for (StringLight light : this.strings.values()) {
            if (!this.positionSettings.get(light.position).enabled) {
                continue;
            }
            light.windTime += deltaSeconds * light.windSpeed;
            for (LightBulb bulb : light.bulbs) {
                bulb.flickerTime += deltaSeconds * bulb.flickerSpeed * this.flickerSpeed;
            }
        }
    }

    private void renderStrings(GuiGraphics graphics, int overlayX, int overlayY, int overlayWidth, int overlayHeight) {
        float stringThickness = Math.max(1.0F, BASE_STRING_THICKNESS * this.scale);

        for (StringLight light : this.strings.values()) {
            PositionSettings settings = this.positionSettings.get(light.position);
            if (!settings.enabled) {
                continue;
            }
            boolean christmasMode = (settings.christmasMode != null) ? settings.christmasMode : this.christmasMode;
            int baseColor = (settings.color != null) ? settings.color.color : this.baseColor;
            float alphaScale = (settings.color != null) ? settings.color.alphaScale : this.baseAlphaScale;
            int baseRgb = baseColor & 0x00FFFFFF;
            float midX = (light.startX + light.endX) * 0.5F;
            float midY = (light.startY + light.endY) * 0.5F;
            float windScale = this.windStrength * light.position.windMultiplier;
            float windOffset = Mth.sin(light.windTime + light.windPhase) * light.windAmplitude * windScale;
            float windOffsetY = Mth.cos(light.windTime * 0.7F + light.windPhase) * light.windAmplitude * 0.2F * windScale;
            float controlX = midX + windOffset;
            float controlY = midY + light.sag + windOffsetY;
            if (light.position == StringLightsPosition.BOTTOM_LEFT_TO_BOTTOM_RIGHT) {
                float glowHalf = light.maxBulbSize * 1.05F;
                float margin = BOTTOM_STRING_BOTTOM_MARGIN + glowHalf + stringThickness * 0.5F;
                float maxCurveY = overlayHeight - margin;
                float maxControlY = (maxCurveY * 2.0F) - midY;
                if (controlY > maxControlY) {
                    controlY = maxControlY;
                }
            }

            if (ARGB.alpha(settings.stringColor) > 0) {
                renderStringLine(graphics, overlayX, overlayY, overlayWidth, overlayHeight, light, controlX, controlY, stringThickness, settings.stringColor);
            }
            renderBulbs(graphics, overlayX, overlayY, overlayWidth, overlayHeight, light, controlX, controlY, christmasMode, baseRgb, alphaScale);
        }
    }

    private void renderStringLine(GuiGraphics graphics, int overlayX, int overlayY, int overlayWidth, int overlayHeight, StringLight light, float controlX, float controlY, float thickness, int color) {
        float step = Math.max(1.0F, thickness * 0.75F);
        int steps = Mth.clamp(Mth.ceil(light.length / step), 12, 260);
        float half = thickness * 0.5F;

        for (int i = 0; i < steps; i++) {
            float t = i / (float)(steps - 1);
            float x = bezier(light.startX, controlX, light.endX, t);
            float y = bezier(light.startY, controlY, light.endY, t);
            fillClamped(graphics, x - half, y - half, x + half, y + half, overlayX, overlayY, overlayWidth, overlayHeight, color);
        }
    }

    private void renderBulbs(GuiGraphics graphics, int overlayX, int overlayY, int overlayWidth, int overlayHeight, StringLight light, float controlX, float controlY, boolean christmasMode, int baseRgb, float baseAlphaScale) {
        for (LightBulb bulb : light.bulbs) {
            if (bulb.size <= 0.0F) {
                continue;
            }
            float x = bezier(light.startX, controlX, light.endX, bulb.t);
            float y = bezier(light.startY, controlY, light.endY, bulb.t);

            float flicker = 0.75F + 0.25F * Mth.sin(bulb.flickerTime + bulb.flickerPhase);
            float intensity = bulb.baseBrightness * (0.65F + 0.35F * flicker);
            float colorBrightness = Mth.clamp(intensity, 0.4F, 1.4F);
            int bulbRgb = bulb.hasCustomColor ? bulb.customRgb : (christmasMode ? bulb.christmasRgb : baseRgb);
            float bulbAlphaScale = bulb.hasCustomColor ? bulb.customAlphaScale : baseAlphaScale;
            int rgb = applyBrightness(bulbRgb, colorBrightness);
            float alphaScale = bulbAlphaScale * Mth.clamp(0.55F + 0.45F * intensity, 0.0F, 1.0F);
            int alpha = Mth.clamp(Mth.floor(255 * alphaScale), 0, 255);

            float glowSize = bulb.size * 2.1F;
            int glowAlpha = Mth.clamp(Mth.floor(alpha * 0.45F), 0, 255);
            int glowColor = (glowAlpha << 24) | rgb;
            float glowHalf = glowSize * 0.5F;
            fillClamped(graphics, x - glowHalf, y - glowHalf, x + glowHalf, y + glowHalf, overlayX, overlayY, overlayWidth, overlayHeight, glowColor);

            float coreHalf = bulb.size * 0.5F;
            int coreAlpha = Mth.clamp(Mth.floor(alpha * 0.9F + 12), 0, 255);
            int coreColor = (coreAlpha << 24) | rgb;
            fillClamped(graphics, x - coreHalf, y - coreHalf, x + coreHalf, y + coreHalf, overlayX, overlayY, overlayWidth, overlayHeight, coreColor);
        }
    }

    private float bezier(float start, float control, float end, float t) {
        float inv = 1.0F - t;
        return inv * inv * start + 2.0F * inv * t * control + t * t * end;
    }

    private int applyBrightness(int rgb, float brightness) {
        int red = Mth.clamp(Mth.floor(ARGB.red(rgb) * brightness), 0, 255);
        int green = Mth.clamp(Mth.floor(ARGB.green(rgb) * brightness), 0, 255);
        int blue = Mth.clamp(Mth.floor(ARGB.blue(rgb) * brightness), 0, 255);
        return (red << 16) | (green << 8) | blue;
    }

    private void fillClamped(GuiGraphics graphics, float minX, float minY, float maxX, float maxY, int overlayX, int overlayY, int overlayWidth, int overlayHeight, int color) {
        float maxXBounds = overlayX + overlayWidth;
        float maxYBounds = overlayY + overlayHeight;
        if (maxX <= overlayX || minX >= maxXBounds || maxY <= overlayY || minY >= maxYBounds) {
            return;
        }
        int x1 = Mth.floor(Math.max(minX, overlayX));
        int x2 = Mth.ceil(Math.min(maxX, maxXBounds));
        int y1 = Mth.floor(Math.max(minY, overlayY));
        int y2 = Mth.ceil(Math.min(maxY, maxYBounds));
        if (x1 >= x2 || y1 >= y2) {
            return;
        }
        graphics.fill(x1, y1, x2, y2, color);
    }

    private float nextRange(float min, float max) {
        return min + this.random.nextFloat() * (max - min);
    }

    private float resolveAlphaScale(int color) {
        int alpha = ARGB.alpha(color);
        if (alpha <= 0) {
            alpha = 255;
        }
        return Mth.clamp(alpha / 255.0F, 0.0F, 1.0F);
    }

    private float computeDeltaSeconds(boolean resetTimer) {
        long now = System.currentTimeMillis();
        if (this.lastUpdateMs < 0L || resetTimer) {
            this.lastUpdateMs = now;
            return 0.0F;
        }
        float deltaSeconds = (now - this.lastUpdateMs) / 1000.0F;
        if (deltaSeconds <= 0.0F) {
            return 0.0F;
        }
        if (deltaSeconds > MAX_DELTA_SECONDS) {
            deltaSeconds = MAX_DELTA_SECONDS;
        }
        this.lastUpdateMs = now;
        return deltaSeconds;
    }

    @NotNull
    private static EnumMap<StringLightsPosition, PositionSettings> createPositionSettings() {
        EnumMap<StringLightsPosition, PositionSettings> settings = new EnumMap<>(StringLightsPosition.class);
        for (StringLightsPosition position : StringLightsPosition.values()) {
            settings.put(position, new PositionSettings());
        }
        return settings;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }

    @Override
    public boolean isFocusable() {
        return false;
    }

    @Override
    public void setFocusable(boolean focusable) {
    }

    @Override
    public boolean isNavigatable() {
        return false;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
    }

    public enum StringLightsPosition {
        LEFT_CENTER_TO_TOP_CENTER(0.0F, 0.5F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.22F, 1.0F, 1.0F, 1.0F),
        RIGHT_CENTER_TO_TOP_CENTER(1.0F, 0.5F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.22F, 1.0F, 1.0F, 1.0F),
        BOTTOM_LEFT_TO_TOP_CENTER(0.0F, 0.95F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.28F, 1.05F, 1.0F, 1.05F),
        BOTTOM_RIGHT_TO_TOP_CENTER(1.0F, 0.95F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.28F, 1.05F, 1.0F, 1.05F),
        TOP_LEFT_TO_TOP_RIGHT(0.0F, 0.07F, 1.0F, 0.07F, 0.0F, 0.0F, 0.0F, 0.0F, 0.18F, 1.0F, 1.0F, 1.0F),
        BOTTOM_LEFT_TO_BOTTOM_RIGHT(0.0F, 0.85F, 1.0F, 0.85F, 0.0F, 0.0F, 0.0F, 0.0F, 0.14F, 1.0F, 1.0F, 1.0F),
        LOOSE_LEFT_TOP(0.1667F, 0.0F, 0.1667F, 0.48F, 0.0F, -11.0F, 0.0F, 0.0F, 0.0F, 0.85F, 1.1F, 1.2F),
        LOOSE_RIGHT_TOP(0.8333F, 0.0F, 0.8333F, 0.48F, 0.0F, -11.0F, 0.0F, 0.0F, 0.0F, 0.85F, 1.1F, 1.2F);

        public final float startX;
        public final float startY;
        public final float endX;
        public final float endY;
        public final float startOffsetX;
        public final float startOffsetY;
        public final float endOffsetX;
        public final float endOffsetY;
        public final float sagFactor;
        public final float densityMultiplier;
        public final float sizeMultiplier;
        public final float windMultiplier;

        StringLightsPosition(float startX, float startY, float endX, float endY, float startOffsetX, float startOffsetY, float endOffsetX, float endOffsetY, float sagFactor, float densityMultiplier, float sizeMultiplier, float windMultiplier) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.startOffsetX = startOffsetX;
            this.startOffsetY = startOffsetY;
            this.endOffsetX = endOffsetX;
            this.endOffsetY = endOffsetY;
            this.sagFactor = sagFactor;
            this.densityMultiplier = densityMultiplier;
            this.sizeMultiplier = sizeMultiplier;
            this.windMultiplier = windMultiplier;
        }
    }

    private static final class StringLight {
        private final StringLightsPosition position;
        private final List<LightBulb> bulbs = new ArrayList<>();
        private float startX;
        private float startY;
        private float endX;
        private float endY;
        private float length;
        private float sag;
        private float windAmplitude;
        private float windSpeed;
        private float windPhase;
        private float windTime;
        private float maxBulbSize;

        private StringLight(StringLightsPosition position) {
            this.position = position;
        }
    }

    private static final class LightBulb {
        private float t;
        private float randomSizeFactor;
        private float size;
        private float baseBrightness;
        private float flickerSpeed;
        private float flickerPhase;
        private float flickerTime;
        private int christmasRgb;
        private boolean hasCustomColor;
        private int customRgb;
        private float customAlphaScale;
    }

    private static final class PositionSettings {
        private boolean enabled = false;
        @Nullable
        private Boolean christmasMode = null;
        @Nullable
        private ColorSettings color = null;
        private int stringColor = DEFAULT_STRING_COLOR;
        private int bulbCount = AUTO_BULB_COUNT;
        private boolean rebuildRequested = false;
        @NotNull
        private List<Float> bulbSizes = List.of();
        @NotNull
        private List<Integer> bulbColors = List.of();
    }

    private static final class ColorSettings {
        private int color;
        private float alphaScale;
    }

}
