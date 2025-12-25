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
import java.util.List;

public class LeavesOverlay extends AbstractWidget implements NavigatableWidget {

    private static final int AREA_PER_LEAF = 12000;
    private static final int MIN_LEAVES = 55;
    private static final int MAX_LEAVES = 320;
    private static final float MIN_FALL_SPEED = 16.0F;
    private static final float MAX_FALL_SPEED = 52.0F;
    private static final float MIN_DRIFT_SPEED = -16.0F;
    private static final float MAX_DRIFT_SPEED = 16.0F;
    private static final float MIN_SWIRL_RADIUS = 2.0F;
    private static final float MAX_SWIRL_RADIUS = 8.0F;
    private static final float MIN_SWIRL_SPEED = 0.6F;
    private static final float MAX_SWIRL_SPEED = 1.8F;
    private static final float MIN_SCALE = 1.0F;
    private static final float MAX_SCALE = 5.0F;
    private static final float WIND_MIN = -10.0F;
    private static final float WIND_MAX = 10.0F;
    private static final float MAX_DELTA_SECONDS = 0.1F;
    private static final float LAND_MIN_TIME = 2.0F;
    private static final float LAND_MAX_TIME = 6.5F;
    private static final float FADE_MIN_TIME = 1.0F;
    private static final float FADE_MAX_TIME = 3.0F;
    private static final float GUST_MIN_INTERVAL = 6.0F;
    private static final float GUST_MAX_INTERVAL = 14.0F;
    private static final float GUST_MIN_DURATION = 0.9F;
    private static final float GUST_MAX_DURATION = 1.8F;
    private static final float GUST_MIN_STRENGTH = 45.0F;
    private static final float GUST_MAX_STRENGTH = 95.0F;
    private static final float GUST_POWER_MIN = 0.65F;
    private static final float GUST_POWER_MAX = 2.2F;
    private static final float GUST_LAND_BLOW_CHANCE = 0.85F;
    private static final float GUST_AIR_BLOW_CHANCE = 0.45F;
    private static final int WIND_RGB = 0xE3F2FF;
    private static final int WIND_ALPHA_MIN = 75;
    private static final int WIND_ALPHA_MAX = 160;

    private static final LeafShape[] LEAF_SHAPES = new LeafShape[] {
            new LeafShape(3, 3, new int[][] {{0, 1, 3, 1}, {1, 0, 1, 1}, {1, 2, 1, 1}}),
            new LeafShape(4, 3, new int[][] {{1, 0, 2, 1}, {0, 1, 4, 1}, {1, 2, 2, 1}}),
            new LeafShape(3, 3, new int[][] {{1, 0, 1, 1}, {0, 1, 3, 1}, {0, 2, 3, 1}})
    };

    private final RandomSource random = RandomSource.create();
    private final List<Leaf> leaves = new ArrayList<>();
    private final List<LandingArea> collisionAreas = new ArrayList<>();
    private LandingArea bottomArea;
    private int lastWidth = -1;
    private int lastHeight = -1;
    private float lastScale = -1.0F;
    private long lastUpdateMs = -1L;
    private float wind = 0.0F;
    private float windTarget = 0.0F;
    private long nextWindChangeMs = 0L;
    private float intensity = 1.0F;
    private float windStrength = 1.0F;
    private boolean windBlowsEnabled = true;
    private float fallSpeedMultiplier = 1.0F;
    private float scale = 1.0F;
    private int leafColorStart = 0xFF7BA84F;
    private int leafColorEnd = 0xFFD58A3B;
    private WindGust windGust;
    private int gustId = 0;
    private long nextGustMs = 0L;

    public LeavesOverlay(int width, int height) {
        super(0, 0, width, height, Component.empty());
    }

    public void setIntensity(float intensity) {
        this.intensity = Mth.clamp(intensity, 0.0F, 2.0F);
    }

    public float getIntensity() {
        return this.intensity;
    }

    public void setWindStrength(float windStrength) {
        this.windStrength = Mth.clamp(windStrength, 0.01F, 5.0F);
    }

    public float getWindStrength() {
        return this.windStrength;
    }

    public void setScale(float scale) {
        this.scale = Mth.clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    public float getScale() {
        return this.scale;
    }

    public void setFallSpeedMultiplier(float fallSpeedMultiplier) {
        this.fallSpeedMultiplier = Mth.clamp(fallSpeedMultiplier, 0.01F, 5.0F);
    }

    public float getFallSpeedMultiplier() {
        return this.fallSpeedMultiplier;
    }

    public void setWindBlowsEnabled(boolean windBlowsEnabled) {
        this.windBlowsEnabled = windBlowsEnabled;
    }

    public boolean isWindBlowsEnabled() {
        return this.windBlowsEnabled;
    }

    public void setColorRange(int startColor, int endColor) {
        this.leafColorStart = startColor;
        this.leafColorEnd = endColor;
        if (!this.leaves.isEmpty()) {
            for (Leaf leaf : this.leaves) {
                leaf.baseColor = resolveLeafColor(leaf.colorFactor);
            }
        }
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

        boolean scaleChanged = Math.abs(this.scale - this.lastScale) > 0.001F;
        boolean sizeChanged = ensureLeaves(overlayWidth, overlayHeight, scaleChanged);
        ensureLandingAreas(overlayWidth, overlayHeight, sizeChanged);
        float deltaSeconds = computeDeltaSeconds(sizeChanged);
        if (deltaSeconds > 0.0F) {
            updateWind(System.currentTimeMillis(), deltaSeconds);
            updateGust(System.currentTimeMillis(), overlayWidth, overlayHeight, deltaSeconds);
            updateLeaves(overlayWidth, overlayHeight, deltaSeconds);
        }

        renderLeaves(graphics, overlayX, overlayY, overlayWidth, overlayHeight);
        renderWindGust(graphics, overlayX, overlayY, overlayWidth, overlayHeight);
    }

    public void addCollisionArea(int x, int y, int width, int height) {
        if (width <= 0 || height < 0) {
            return;
        }
        this.collisionAreas.add(new LandingArea(x, y, width, height));
    }

    public void clearCollisionAreas() {
        this.collisionAreas.clear();
    }

    private boolean ensureLeaves(int width, int height, boolean scaleChanged) {
        float effectiveIntensity = Mth.clamp(this.intensity, 0.0F, 2.0F);
        int baseCount = Mth.clamp((width * height) / AREA_PER_LEAF, MIN_LEAVES, MAX_LEAVES);
        int desiredCount = Mth.clamp(Mth.floor(baseCount * effectiveIntensity), 0, MAX_LEAVES);
        boolean sizeChanged = this.lastWidth != width || this.lastHeight != height || scaleChanged;
        if (sizeChanged) {
            this.leaves.clear();
            for (int i = 0; i < desiredCount; i++) {
                this.leaves.add(createLeaf(width, height, true));
            }
            this.lastWidth = width;
            this.lastHeight = height;
            this.lastScale = this.scale;
            return true;
        }

        while (this.leaves.size() < desiredCount) {
            this.leaves.add(createLeaf(width, height, true));
        }
        while (this.leaves.size() > desiredCount) {
            this.leaves.remove(this.leaves.size() - 1);
        }
        return false;
    }

    private void ensureLandingAreas(int width, int height, boolean sizeChanged) {
        if (this.bottomArea == null || sizeChanged) {
            this.bottomArea = new LandingArea(0, height, width, 0);
        } else {
            this.bottomArea.setBounds(0, height, width, 0);
        }
    }

    private float computeDeltaSeconds(boolean sizeChanged) {
        long now = System.currentTimeMillis();
        if (this.lastUpdateMs < 0L || sizeChanged) {
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

    private void updateLeaves(int width, int height, float deltaSeconds) {
        WindGust activeGust = this.windBlowsEnabled ? this.windGust : null;
        float gustWind = 0.0F;
        float gustX = 0.0F;
        float gustBand = 0.0F;
        float gustPower = 0.0F;
        int currentGustId = -1;
        if (activeGust != null) {
            float strengthScale = activeGust.getStrengthScale();
            gustPower = activeGust.power;
            gustWind = strengthScale * activeGust.strength * activeGust.direction;
            gustX = activeGust.getX(width, this.scale);
            gustBand = activeGust.getBandWidth(this.scale);
            currentGustId = activeGust.id;
        }

        float effectiveWind = this.wind + gustWind;
        float wrapPadding = 18.0F * this.scale;
        for (Leaf leaf : this.leaves) {
            if (leaf.landed) {
                leaf.landTime += deltaSeconds;
                if (currentGustId >= 0 && leaf.lastGustId != currentGustId && isGustHittingLeaf(leaf, gustX, gustBand)) {
                    leaf.lastGustId = currentGustId;
                    float blowChance = Mth.clamp(GUST_LAND_BLOW_CHANCE * (0.7F + gustPower * 0.35F), 0.25F, 0.98F);
                    if (this.random.nextFloat() < blowChance) {
                        blowLeaf(leaf, gustWind, gustPower);
                    }
                }
                if (leaf.landTime >= leaf.landDuration + leaf.fadeDuration) {
                    resetLeaf(leaf, width, height, false);
                }
                continue;
            }

            if (currentGustId >= 0 && leaf.lastGustId != currentGustId && isGustHittingLeaf(leaf, gustX, gustBand)) {
                leaf.lastGustId = currentGustId;
                float blowChance = Mth.clamp(GUST_AIR_BLOW_CHANCE * (0.75F + gustPower * 0.3F), 0.1F, 0.9F);
                if (this.random.nextFloat() < blowChance) {
                    applyGustKick(leaf, gustWind, gustPower, 1.0F);
                }
            }

            float previousSwirlTime = leaf.swirlTime;
            float previousY = leaf.y;
            leaf.y += leaf.fallSpeed * this.fallSpeedMultiplier * deltaSeconds;
            leaf.x += (leaf.driftSpeed + effectiveWind + leaf.gustBoost) * deltaSeconds;
            updateGustKick(leaf, deltaSeconds);
            float swirlBoost = 0.0F;
            if (leaf.gustSwirlTime > 0.0F && leaf.gustSwirlDuration > 0.0F) {
                swirlBoost = (leaf.gustSwirlTime / leaf.gustSwirlDuration) * leaf.gustSwirlBoost;
            }
            float swirlRadius = leaf.swirlRadius * (1.0F + swirlBoost);
            float swirlSpeed = leaf.swirlSpeed * (1.0F + swirlBoost * 0.45F);
            leaf.swirlTime += swirlSpeed * deltaSeconds;

            float swirlOffsetX = Mth.sin(leaf.swirlTime) * swirlRadius;
            float swirlOffsetY = Mth.cos(leaf.swirlTime) * swirlRadius * 0.35F;
            float prevSwirlOffsetY = Mth.cos(previousSwirlTime) * swirlRadius * 0.35F;

            if (handleLeafCollision(leaf, leaf.x + swirlOffsetX, leaf.y + swirlOffsetY, previousY + prevSwirlOffsetY)) {
                continue;
            }

            if (leaf.y > height + leaf.height) {
                resetLeaf(leaf, width, height, false);
                continue;
            }

            if (leaf.x < -wrapPadding) {
                leaf.x = width + this.random.nextFloat() * wrapPadding;
            } else if (leaf.x > width + wrapPadding) {
                leaf.x = -this.random.nextFloat() * wrapPadding;
            }
        }
    }

    private boolean isGustHittingLeaf(Leaf leaf, float gustX, float gustBand) {
        float centerX = leaf.x + (leaf.width * 0.5F);
        return Math.abs(centerX - gustX) <= gustBand;
    }

    private boolean handleLeafCollision(Leaf leaf, float effectiveX, float effectiveY, float previousEffectiveY) {
        if (this.bottomArea != null && checkCollisionAndLand(leaf, this.bottomArea, effectiveX, effectiveY, previousEffectiveY)) {
            return true;
        }
        for (LandingArea area : this.collisionAreas) {
            if (checkCollisionAndLand(leaf, area, effectiveX, effectiveY, previousEffectiveY)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkCollisionAndLand(Leaf leaf, LandingArea area, float effectiveX, float effectiveY, float previousEffectiveY) {
        if (!area.isValid()) {
            return false;
        }
        float centerX = effectiveX + (leaf.width * 0.5F);
        if (centerX < area.x || centerX >= area.x + area.width) {
            return false;
        }
        float surfaceY = area.y;
        float currentBottom = effectiveY + leaf.height;
        float previousBottom = previousEffectiveY + leaf.height;
        if (currentBottom >= surfaceY && previousBottom < surfaceY) {
            landLeaf(leaf, area, effectiveX, surfaceY);
            return true;
        }
        return false;
    }

    private void landLeaf(Leaf leaf, LandingArea area, float effectiveX, float surfaceY) {
        leaf.landed = true;
        leaf.landTime = 0.0F;
        leaf.landDuration = nextRange(LAND_MIN_TIME, LAND_MAX_TIME);
        leaf.fadeDuration = nextRange(FADE_MIN_TIME, FADE_MAX_TIME);
        if (area.width > leaf.width) {
            leaf.x = Mth.clamp(effectiveX, area.x, area.x + area.width - leaf.width);
        } else if (area.width > 0) {
            leaf.x = area.x;
        } else {
            leaf.x = effectiveX;
        }
        leaf.y = surfaceY - leaf.height;
        leaf.gustBoost = 0.0F;
        leaf.gustTime = 0.0F;
    }

    private void blowLeaf(Leaf leaf, float gustWind, float gustPower) {
        leaf.landed = false;
        leaf.landTime = 0.0F;
        leaf.y = Math.max(-leaf.height, leaf.y - nextRange(6.0F, 18.0F) * this.scale);
        resetLeafMotion(leaf);
        applyGustKick(leaf, gustWind, gustPower, 1.35F);
    }

    private void updateGustKick(Leaf leaf, float deltaSeconds) {
        if (leaf.gustTime <= 0.0F) {
            leaf.gustBoost = 0.0F;
        } else {
            leaf.gustTime -= deltaSeconds;
            if (leaf.gustTime <= 0.0F) {
                leaf.gustTime = 0.0F;
                leaf.gustBoost = 0.0F;
            }
        }
        if (leaf.gustSwirlTime > 0.0F) {
            leaf.gustSwirlTime -= deltaSeconds;
            if (leaf.gustSwirlTime <= 0.0F) {
                leaf.gustSwirlTime = 0.0F;
                leaf.gustSwirlBoost = 0.0F;
                leaf.gustSwirlDuration = 0.0F;
            }
        }
    }

    private void applyGustKick(Leaf leaf, float gustWind, float gustPower, float swirlMultiplier) {
        if (Math.abs(gustWind) < 0.01F) {
            return;
        }
        float powerScale = 0.65F + gustPower * 0.35F;
        leaf.gustBoost = gustWind * nextRange(0.55F, 1.1F) * powerScale;
        leaf.gustTime = nextRange(0.35F, 0.95F) * (0.85F + gustPower * 0.25F);
        leaf.gustSwirlBoost = nextRange(0.9F, 1.9F) * gustPower * swirlMultiplier;
        leaf.gustSwirlDuration = nextRange(0.55F, 1.25F);
        leaf.gustSwirlTime = leaf.gustSwirlDuration;
    }

    private void updateWind(long now, float deltaSeconds) {
        if (this.windStrength <= 0.01F) {
            this.wind = 0.0F;
            this.windTarget = 0.0F;
            return;
        }
        if (now >= this.nextWindChangeMs) {
            this.windTarget = nextRange(WIND_MIN, WIND_MAX) * this.windStrength;
            this.nextWindChangeMs = now + 2800L + this.random.nextInt(3200);
        }
        float response = 0.18F * deltaSeconds;
        this.wind += (this.windTarget - this.wind) * response;
    }

    private void updateGust(long now, int width, int height, float deltaSeconds) {
        if (!this.windBlowsEnabled) {
            if (this.windGust != null) {
                this.windGust = null;
            }
            return;
        }
        if (this.windStrength <= 0.01F || this.intensity <= 0.0F) {
            this.windGust = null;
            return;
        }
        if (this.windGust == null) {
            if (now >= this.nextGustMs) {
                startGust(now, width, height);
            }
            return;
        }
        this.windGust.time += deltaSeconds;
        if (this.windGust.time >= this.windGust.duration) {
            this.windGust = null;
            scheduleNextGust(now);
        }
    }

    private void startGust(long now, int width, int height) {
        float duration = nextRange(GUST_MIN_DURATION, GUST_MAX_DURATION);
        float power = computeGustPower();
        float strength = nextRange(GUST_MIN_STRENGTH, GUST_MAX_STRENGTH) * this.windStrength * power;
        int direction = this.random.nextBoolean() ? 1 : -1;
        int streakCount = Mth.clamp(3 + this.random.nextInt(4) + Mth.floor(power - 1.0F), 3, 7);
        WindStreak[] streaks = new WindStreak[streakCount];
        float minY = height * 0.2F;
        float maxY = height * 0.85F;
        for (int i = 0; i < streakCount; i++) {
            float y = nextRange(minY, maxY);
            float length = nextRange(16.0F, 30.0F) * (0.85F + power * 0.25F);
            float wobble = nextRange(3.0F, 9.0F) * (0.75F + power * 0.25F);
            float phase = this.random.nextFloat() * ((float)Math.PI * 2.0F);
            float offset = nextRange(-6.0F, 6.0F) * (0.9F + power * 0.1F);
            streaks[i] = new WindStreak(y, length, wobble, phase, offset);
        }
        float bandWidth = nextRange(32.0F, 50.0F) * (0.7F + power * 0.4F);
        this.windGust = new WindGust(++this.gustId, direction, strength, duration, power, bandWidth, streaks);
        scheduleNextGust(now);
    }

    private void scheduleNextGust(long now) {
        float intensityScale = Mth.clamp(this.windStrength, 0.3F, 5.0F);
        float baseInterval = nextRange(GUST_MIN_INTERVAL, GUST_MAX_INTERVAL);
        float interval = baseInterval / intensityScale;
        this.nextGustMs = now + (long)(interval * 1000.0F);
    }

    private float computeGustPower() {
        float power = nextRange(0.7F, 1.25F);
        if (this.random.nextFloat() < 0.22F) {
            power += nextRange(0.35F, 0.75F);
        }
        if (this.random.nextFloat() < 0.08F) {
            power += nextRange(0.45F, 0.9F);
        }
        return Mth.clamp(power, GUST_POWER_MIN, GUST_POWER_MAX);
    }

    private void renderLeaves(GuiGraphics graphics, int overlayX, int overlayY, int overlayWidth, int overlayHeight) {
        for (Leaf leaf : this.leaves) {
            float swirlOffsetX = 0.0F;
            float swirlOffsetY = 0.0F;
            if (!leaf.landed) {
                float swirlBoost = leaf.gustSwirlTime > 0.0F && leaf.gustSwirlDuration > 0.0F
                        ? (leaf.gustSwirlTime / leaf.gustSwirlDuration) * leaf.gustSwirlBoost
                        : 0.0F;
                float swirlRadius = leaf.swirlRadius * (1.0F + swirlBoost);
                swirlOffsetX = Mth.sin(leaf.swirlTime) * swirlRadius;
                swirlOffsetY = Mth.cos(leaf.swirlTime) * swirlRadius * 0.35F;
            }
            int renderX = overlayX + Mth.floor(leaf.x + swirlOffsetX);
            int renderY = overlayY + Mth.floor(leaf.y + swirlOffsetY);

            if (renderX + leaf.width <= overlayX || renderX >= overlayX + overlayWidth) {
                continue;
            }
            if (renderY + leaf.height <= overlayY || renderY >= overlayY + overlayHeight) {
                continue;
            }

            float alphaScale = 1.0F;
            if (leaf.landed && leaf.landTime > leaf.landDuration) {
                float fadeProgress = (leaf.landTime - leaf.landDuration) / leaf.fadeDuration;
                alphaScale = 1.0F - fadeProgress;
            }
            if (alphaScale <= 0.01F) {
                continue;
            }

            int color = applyAlpha(leaf.baseColor, Mth.clamp(alphaScale, 0.0F, 1.0F));
            renderLeafShape(graphics, renderX, renderY, leaf, color);
        }
    }

    private void renderLeafShape(GuiGraphics graphics, int x, int y, Leaf leaf, int color) {
        LeafShape shape = leaf.shape;
        int unit = leaf.size;
        for (int[] rect : shape.rects) {
            int rectX = rect[0];
            int rectY = rect[1];
            int rectW = rect[2];
            int rectH = rect[3];
            if (leaf.flip) {
                rectX = shape.width - rectX - rectW;
            }
            int startX = x + rectX * unit;
            int startY = y + rectY * unit;
            int endX = startX + rectW * unit;
            int endY = startY + rectH * unit;
            graphics.fill(startX, startY, endX, endY, color);
        }
    }

    private void renderWindGust(GuiGraphics graphics, int overlayX, int overlayY, int overlayWidth, int overlayHeight) {
        if (!this.windBlowsEnabled || this.windGust == null) {
            return;
        }
        float strengthScale = this.windGust.getStrengthScale();
        float alphaScale = strengthScale * Mth.clamp(0.7F + this.windGust.power * 0.35F, 0.7F, 1.6F);
        if (alphaScale <= 0.01F) {
            return;
        }
        int alpha = Mth.clamp(Math.round(Mth.lerp(alphaScale, WIND_ALPHA_MIN, WIND_ALPHA_MAX)), 0, 255);
        int color = (alpha << 24) | WIND_RGB;
        float gustX = this.windGust.getX(overlayWidth, this.scale);
        float padding = 26.0F * this.scale;
        if (gustX < -padding || gustX > overlayWidth + padding) {
            return;
        }

        boolean directionRight = this.windGust.direction > 0;
        float wobbleTime = this.windGust.time * 4.0F;
        float thicknessScale = Mth.clamp(0.9F + this.windGust.power * 0.25F, 0.9F, 1.6F);
        for (WindStreak streak : this.windGust.streaks) {
            float wobble = Mth.sin(wobbleTime + streak.phase) * streak.wobble;
            int baseY = overlayY + Mth.floor(streak.y + wobble);
            if (baseY < overlayY || baseY > overlayY + overlayHeight) {
                continue;
            }
            int baseX = overlayX + Mth.floor(gustX + streak.offset);
            renderWindStreak(graphics, baseX, baseY, streak.length, color, directionRight, thicknessScale);
        }
    }

    private void renderWindStreak(GuiGraphics graphics, int baseX, int baseY, float length, int color, boolean directionRight, float thicknessScale) {
        int totalLength = Math.max(6, Math.round(length * this.scale));
        int segmentCount = 3;
        int segment = Math.max(2, totalLength / (segmentCount + 1));
        int gap = Math.max(1, (totalLength - segment * segmentCount) / Math.max(1, segmentCount - 1));
        int thickness = Math.max(1, Math.round(1.0F * this.scale * thicknessScale));

        for (int i = 0; i < segmentCount; i++) {
            int offset = i * (segment + gap);
            int segX = directionRight ? baseX + offset : baseX - offset;
            int segY = baseY + ((i % 2 == 0) ? 0 : -thickness);
            int startX = directionRight ? segX : segX - segment;
            int endX = directionRight ? segX + segment : segX;
            graphics.fill(startX, segY, endX, segY + thickness, color);
        }
    }

    private Leaf createLeaf(int width, int height, boolean spawnInside) {
        Leaf leaf = new Leaf();
        resetLeaf(leaf, width, height, spawnInside);
        return leaf;
    }

    private void resetLeaf(Leaf leaf, int width, int height, boolean spawnInside) {
        leaf.shape = LEAF_SHAPES[this.random.nextInt(LEAF_SHAPES.length)];
        leaf.flip = this.random.nextBoolean();
        int baseSize = this.random.nextFloat() < 0.4F ? 2 : 3;
        if (this.random.nextFloat() < 0.18F) {
            baseSize += 1;
        }
        leaf.size = Math.max(1, Mth.ceil(baseSize * this.scale));
        leaf.width = leaf.shape.width * leaf.size;
        leaf.height = leaf.shape.height * leaf.size;
        leaf.x = this.random.nextFloat() * width;
        leaf.y = spawnInside ? (this.random.nextFloat() * height) : -this.random.nextFloat() * (height * 0.35F);
        leaf.colorFactor = this.random.nextFloat();
        leaf.baseColor = resolveLeafColor(leaf.colorFactor);
        leaf.lastGustId = -1;
        resetLeafMotion(leaf);
    }

    private void resetLeafMotion(Leaf leaf) {
        leaf.fallSpeed = nextRange(MIN_FALL_SPEED, MAX_FALL_SPEED) * (leaf.size <= 2 ? 0.9F : 1.0F);
        leaf.driftSpeed = nextRange(MIN_DRIFT_SPEED, MAX_DRIFT_SPEED) * 0.85F;
        leaf.swirlRadius = nextRange(MIN_SWIRL_RADIUS, MAX_SWIRL_RADIUS) * (0.6F + this.scale * 0.35F);
        leaf.swirlSpeed = nextRange(MIN_SWIRL_SPEED, MAX_SWIRL_SPEED);
        leaf.swirlTime = this.random.nextFloat() * ((float)Math.PI * 2.0F);
        leaf.gustBoost = 0.0F;
        leaf.gustTime = 0.0F;
        leaf.gustSwirlBoost = 0.0F;
        leaf.gustSwirlTime = 0.0F;
        leaf.gustSwirlDuration = 0.0F;
        leaf.landed = false;
        leaf.landTime = 0.0F;
        leaf.landDuration = 0.0F;
        leaf.fadeDuration = 0.0F;
    }

    private float nextRange(float min, float max) {
        return min + this.random.nextFloat() * (max - min);
    }

    private int resolveLeafColor(float t) {
        int a1 = FastColor.ARGB32.alpha(this.leafColorStart);
        int r1 = FastColor.ARGB32.red(this.leafColorStart);
        int g1 = FastColor.ARGB32.green(this.leafColorStart);
        int b1 = FastColor.ARGB32.blue(this.leafColorStart);
        int a2 = FastColor.ARGB32.alpha(this.leafColorEnd);
        int r2 = FastColor.ARGB32.red(this.leafColorEnd);
        int g2 = FastColor.ARGB32.green(this.leafColorEnd);
        int b2 = FastColor.ARGB32.blue(this.leafColorEnd);
        int a = Mth.clamp(Math.round(Mth.lerp(t, a1, a2)), 0, 255);
        int r = Mth.clamp(Math.round(Mth.lerp(t, r1, r2)), 0, 255);
        int g = Mth.clamp(Math.round(Mth.lerp(t, g1, g2)), 0, 255);
        int b = Mth.clamp(Math.round(Mth.lerp(t, b1, b2)), 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int applyAlpha(int color, float alphaScale) {
        int alpha = FastColor.ARGB32.alpha(color);
        int scaledAlpha = Mth.clamp(Math.round(alpha * alphaScale), 0, 255);
        return (scaledAlpha << 24) | (color & 0x00FFFFFF);
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

    private static final class LandingArea {
        private int x;
        private int y;
        private int width;
        private int height;

        private LandingArea(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private void setBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private boolean isValid() {
            return this.width > 0;
        }
    }

    private static final class Leaf {
        private float x;
        private float y;
        private float fallSpeed;
        private float driftSpeed;
        private float swirlRadius;
        private float swirlSpeed;
        private float swirlTime;
        private float gustBoost;
        private float gustTime;
        private float gustSwirlBoost;
        private float gustSwirlTime;
        private float gustSwirlDuration;
        private float landTime;
        private float landDuration;
        private float fadeDuration;
        private int size;
        private int width;
        private int height;
        private int baseColor;
        private float colorFactor;
        private boolean flip;
        private boolean landed;
        private LeafShape shape;
        private int lastGustId;
    }

    private static final class WindGust {
        private final int id;
        private final int direction;
        private final float strength;
        private final float duration;
        private final float power;
        private final float bandWidth;
        private final WindStreak[] streaks;
        private float time;

        private WindGust(int id, int direction, float strength, float duration, float power, float bandWidth, WindStreak[] streaks) {
            this.id = id;
            this.direction = direction;
            this.strength = strength;
            this.duration = duration;
            this.power = power;
            this.bandWidth = bandWidth;
            this.streaks = streaks;
        }

        private float getProgress() {
            return Mth.clamp(this.time / this.duration, 0.0F, 1.0F);
        }

        private float getStrengthScale() {
            float progress = getProgress();
            return Mth.sin(progress * (float)Math.PI);
        }

        private float getX(int width, float scale) {
            float padding = 30.0F * scale;
            float startX = this.direction > 0 ? -padding : width + padding;
            float endX = this.direction > 0 ? width + padding : -padding;
            return Mth.lerp(getProgress(), startX, endX);
        }

        private float getBandWidth(float scale) {
            return this.bandWidth * scale;
        }
    }

    private static final class WindStreak {
        private final float y;
        private final float length;
        private final float wobble;
        private final float phase;
        private final float offset;

        private WindStreak(float y, float length, float wobble, float phase, float offset) {
            this.y = y;
            this.length = length;
            this.wobble = wobble;
            this.phase = phase;
            this.offset = offset;
        }
    }

    private static final class LeafShape {
        private final int width;
        private final int height;
        private final int[][] rects;

        private LeafShape(int width, int height, int[][] rects) {
            this.width = width;
            this.height = height;
            this.rects = rects;
        }
    }

}