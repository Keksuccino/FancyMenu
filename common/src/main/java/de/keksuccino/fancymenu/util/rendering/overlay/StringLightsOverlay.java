package de.keksuccino.fancymenu.util.rendering.overlay;

import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class StringLightsOverlay extends AbstractWidget implements NavigatableWidget {

    private static final float BASE_BULB_SPACING = 30.0F;
    private static final float BASE_BULB_SIZE = 5.5F;
    private static final float BASE_STRING_THICKNESS = 1.6F;
    private static final float MIN_SCALE = 0.35F;
    private static final float MAX_SCALE = 2.5F;
    private static final float MIN_WIND_STRENGTH = 0.0F;
    private static final float MAX_WIND_STRENGTH = 2.0F;
    private static final float MIN_FLICKER_SPEED = 0.0F;
    private static final float MAX_FLICKER_SPEED = 2.0F;
    private static final float MAX_DELTA_SECONDS = 0.1F;
    private static final int STRING_RGB = 0x2B2B2B;
    private static final int MIN_BULBS = 3;
    private static final int MAX_BULBS = 36;

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
    private final EnumMap<StringLightsPosition, Boolean> enabledPositions = new EnumMap<>(StringLightsPosition.class);
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

    public void setPositionEnabled(@NotNull StringLightsPosition position, boolean enabled) {
        this.enabledPositions.put(position, enabled);
    }

    public boolean isPositionEnabled(@NotNull StringLightsPosition position) {
        return Boolean.TRUE.equals(this.enabledPositions.get(position));
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
            this.strings.put(position, createString(position, width, height));
        }
        this.lastWidth = width;
        this.lastHeight = height;
        this.lastScale = this.scale;
    }

    private StringLight createString(StringLightsPosition position, int width, int height) {
        StringLight light = new StringLight(position);
        light.startX = position.startX * Math.max(1, width - 1);
        light.startY = position.startY * Math.max(1, height - 1);
        light.endX = position.endX * Math.max(1, width - 1);
        light.endY = position.endY * Math.max(1, height - 1);
        float dx = light.endX - light.startX;
        float dy = light.endY - light.startY;
        light.length = Mth.sqrt(dx * dx + dy * dy);
        light.sag = light.length * position.sagFactor * (0.75F + 0.25F * this.scale);
        light.windAmplitude = light.length * 0.015F * position.windMultiplier;
        light.windSpeed = nextRange(0.35F, 0.9F);
        light.windPhase = this.random.nextFloat() * ((float)Math.PI * 2.0F);
        light.windTime = this.random.nextFloat() * ((float)Math.PI * 2.0F);

        int bulbCount = computeBulbCount(light.length, position.densityMultiplier);
        float bulbBaseSize = BASE_BULB_SIZE * this.scale * position.sizeMultiplier;
        for (int i = 0; i < bulbCount; i++) {
            LightBulb bulb = new LightBulb();
            bulb.t = (i + 1) / (float)(bulbCount + 1);
            bulb.size = bulbBaseSize * nextRange(0.75F, 1.25F);
            bulb.baseBrightness = nextRange(0.78F, 1.15F);
            bulb.flickerSpeed = nextRange(0.25F, 0.65F);
            bulb.flickerPhase = this.random.nextFloat() * ((float)Math.PI * 2.0F);
            bulb.flickerTime = this.random.nextFloat() * ((float)Math.PI * 2.0F);
            bulb.christmasRgb = CHRISTMAS_COLORS[this.random.nextInt(CHRISTMAS_COLORS.length)];
            light.bulbs.add(bulb);
        }

        return light;
    }

    private int computeBulbCount(float length, float densityMultiplier) {
        if (length <= 0.1F) {
            return 0;
        }
        float spacing = BASE_BULB_SPACING * this.scale;
        int count = Mth.floor((length / spacing) * densityMultiplier + 0.5F);
        return Mth.clamp(count, MIN_BULBS, MAX_BULBS);
    }

    private void updateAnimation(float deltaSeconds) {
        for (StringLight light : this.strings.values()) {
            light.windTime += deltaSeconds * light.windSpeed;
            for (LightBulb bulb : light.bulbs) {
                bulb.flickerTime += deltaSeconds * bulb.flickerSpeed * this.flickerSpeed;
            }
        }
    }

    private void renderStrings(GuiGraphics graphics, int overlayX, int overlayY, int overlayWidth, int overlayHeight) {
        int baseRgb = this.baseColor & 0x00FFFFFF;
        float stringThickness = Math.max(1.0F, BASE_STRING_THICKNESS * this.scale);
        int stringAlpha = Mth.clamp(Mth.floor(210 * this.baseAlphaScale), 0, 255);
        int stringColor = (stringAlpha << 24) | STRING_RGB;

        for (var entry : this.strings.entrySet()) {
            StringLightsPosition position = entry.getKey();
            if (!isPositionEnabled(position)) {
                continue;
            }
            StringLight light = entry.getValue();
            float midX = (light.startX + light.endX) * 0.5F;
            float midY = (light.startY + light.endY) * 0.5F;
            float windScale = this.windStrength * light.position.windMultiplier;
            float windOffset = Mth.sin(light.windTime + light.windPhase) * light.windAmplitude * windScale;
            float windOffsetY = Mth.cos(light.windTime * 0.7F + light.windPhase) * light.windAmplitude * 0.2F * windScale;
            float controlX = midX + windOffset;
            float controlY = midY + light.sag + windOffsetY;

            renderStringLine(graphics, overlayX, overlayY, overlayWidth, overlayHeight, light, controlX, controlY, stringThickness, stringColor);
            renderBulbs(graphics, overlayX, overlayY, overlayWidth, overlayHeight, light, controlX, controlY, baseRgb);
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

    private void renderBulbs(GuiGraphics graphics, int overlayX, int overlayY, int overlayWidth, int overlayHeight, StringLight light, float controlX, float controlY, int baseRgb) {
        for (LightBulb bulb : light.bulbs) {
            float x = bezier(light.startX, controlX, light.endX, bulb.t);
            float y = bezier(light.startY, controlY, light.endY, bulb.t);

            float flicker = 0.75F + 0.25F * Mth.sin(bulb.flickerTime + bulb.flickerPhase);
            float intensity = bulb.baseBrightness * (0.65F + 0.35F * flicker);
            float colorBrightness = Mth.clamp(intensity, 0.4F, 1.4F);
            int rgb = applyBrightness(this.christmasMode ? bulb.christmasRgb : baseRgb, colorBrightness);
            float alphaScale = this.baseAlphaScale * Mth.clamp(0.55F + 0.45F * intensity, 0.0F, 1.0F);
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
        int red = Mth.clamp(Mth.floor(FastColor.ARGB32.red(rgb) * brightness), 0, 255);
        int green = Mth.clamp(Mth.floor(FastColor.ARGB32.green(rgb) * brightness), 0, 255);
        int blue = Mth.clamp(Mth.floor(FastColor.ARGB32.blue(rgb) * brightness), 0, 255);
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
        int alpha = FastColor.ARGB32.alpha(color);
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
        LEFT_CENTER_TO_TOP_CENTER(0.0F, 0.5F, 0.5F, 0.0F, 0.22F, 1.0F, 1.0F, 1.0F),
        RIGHT_CENTER_TO_TOP_CENTER(1.0F, 0.5F, 0.5F, 0.0F, 0.22F, 1.0F, 1.0F, 1.0F),
        BOTTOM_LEFT_TO_TOP_CENTER(0.0F, 0.95F, 0.5F, 0.0F, 0.28F, 1.05F, 1.0F, 1.05F),
        TOP_LEFT_TO_TOP_RIGHT(0.0F, 0.07F, 1.0F, 0.07F, 0.18F, 1.0F, 1.0F, 1.0F),
        BOTTOM_LEFT_TO_BOTTOM_RIGHT(0.0F, 0.88F, 1.0F, 0.88F, 0.14F, 1.0F, 1.0F, 1.0F),
        LOOSE_LEFT_TOP(0.0F, 0.02F, 0.28F, 0.18F, 0.38F, 0.85F, 1.1F, 1.2F),
        LOOSE_RIGHT_TOP(1.0F, 0.02F, 0.72F, 0.18F, 0.38F, 0.85F, 1.1F, 1.2F);

        public final float startX;
        public final float startY;
        public final float endX;
        public final float endY;
        public final float sagFactor;
        public final float densityMultiplier;
        public final float sizeMultiplier;
        public final float windMultiplier;

        StringLightsPosition(float startX, float startY, float endX, float endY, float sagFactor, float densityMultiplier, float sizeMultiplier, float windMultiplier) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
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

        private StringLight(StringLightsPosition position) {
            this.position = position;
        }
    }

    private static final class LightBulb {
        private float t;
        private float size;
        private float baseBrightness;
        private float flickerSpeed;
        private float flickerPhase;
        private float flickerTime;
        private int christmasRgb;
    }
}
