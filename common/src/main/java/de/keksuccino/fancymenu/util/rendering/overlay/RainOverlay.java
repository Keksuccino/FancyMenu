package de.keksuccino.fancymenu.util.rendering.overlay;

import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RainOverlay extends AbstractWidget implements NavigatableWidget {

    private static final int AREA_PER_DROP = 5200;
    private static final int MIN_DROPS = 120;
    private static final int MAX_DROPS = 900;
    private static final float MIN_FALL_SPEED = 260.0F;
    private static final float MAX_FALL_SPEED = 620.0F;
    private static final float MIN_DRIFT_SPEED = -18.0F;
    private static final float MAX_DRIFT_SPEED = 18.0F;
    private static final float MIN_LENGTH = 7.0F;
    private static final float MAX_LENGTH = 18.0F;
    private static final float WIND_MIN = -35.0F;
    private static final float WIND_MAX = 35.0F;
    private static final float MAX_DELTA_SECONDS = 0.1F;
    private static final int MIN_ALPHA = 90;
    private static final int MAX_ALPHA = 170;
    private static final int DROP_RGB = 0xCFE7FF;
    private static final float SLANT_FACTOR = 0.045F;

    private static final int PUDDLE_MIN_RADIUS = 4;
    private static final int PUDDLE_MAX_RADIUS = 12;
    private static final float PUDDLE_MIN_LIFE = 3.5F;
    private static final float PUDDLE_MAX_LIFE = 9.0F;
    private static final float PUDDLE_FADE_FRACTION = 0.22F;
    private static final float PUDDLE_SPAWN_RATE = 1.2F;
    private static final int PUDDLE_MIN_ALPHA = 55;
    private static final int PUDDLE_MAX_ALPHA = 120;
    private static final int PUDDLE_RGB = 0x3D5F84;
    private static final float PUDDLE_SPLASH_CHANCE = 0.12F;
    private static final int MAX_PUDDLES = 160;

    private static final float DRIP_MIN_LENGTH = 2.0F;
    private static final float DRIP_MAX_LENGTH = 6.0F;
    private static final float DRIP_MIN_SPEED = 14.0F;
    private static final float DRIP_MAX_SPEED = 32.0F;
    private static final float DRIP_MIN_LIFE = 1.4F;
    private static final float DRIP_MAX_LIFE = 3.2F;
    private static final int DRIP_MIN_ALPHA = 80;
    private static final int DRIP_MAX_ALPHA = 150;
    private static final int DRIP_RGB = 0xA9C9E8;

    private final RandomSource random = RandomSource.create();
    private final List<Raindrop> raindrops = new ArrayList<>();
    private final List<PuddleLine> puddles = new ArrayList<>();
    private final List<Drip> drips = new ArrayList<>();
    private final List<SurfaceArea> collisionAreas = new ArrayList<>();
    private SurfaceArea bottomArea;
    private int lastWidth = -1;
    private int lastHeight = -1;
    private long lastUpdateMs = -1L;
    private float wind = 0.0F;
    private float windTarget = 0.0F;
    private long nextWindChangeMs = 0L;
    private int currentPuddleTarget = 0;

    public RainOverlay(int width, int height) {
        super(0, 0, width, height, Component.empty());
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

        boolean sizeChanged = ensureRaindrops(overlayWidth, overlayHeight);
        ensureSurfaceAreas(overlayWidth, overlayHeight, sizeChanged);
        float deltaSeconds = computeDeltaSeconds(sizeChanged);
        if (deltaSeconds <= 0.0F) {
            renderPuddles(graphics, overlayX, overlayY, overlayWidth, overlayHeight);
            renderDrips(graphics, overlayX, overlayY, overlayWidth, overlayHeight);
            renderRaindrops(graphics, overlayX, overlayY, overlayWidth, overlayHeight);
            return;
        }

        updateWind(System.currentTimeMillis(), deltaSeconds);
        updatePuddles(overlayWidth, overlayHeight, deltaSeconds, sizeChanged);
        updateRaindrops(overlayWidth, overlayHeight, deltaSeconds);
        updateDrips(overlayHeight, deltaSeconds, sizeChanged);

        renderPuddles(graphics, overlayX, overlayY, overlayWidth, overlayHeight);
        renderDrips(graphics, overlayX, overlayY, overlayWidth, overlayHeight);
        renderRaindrops(graphics, overlayX, overlayY, overlayWidth, overlayHeight);
    }

    private boolean ensureRaindrops(int width, int height) {
        int desiredCount = Mth.clamp((width * height) / AREA_PER_DROP, MIN_DROPS, MAX_DROPS);
        boolean sizeChanged = this.lastWidth != width || this.lastHeight != height;
        if (sizeChanged) {
            this.raindrops.clear();
            for (int i = 0; i < desiredCount; i++) {
                this.raindrops.add(createRaindrop(width, height, true));
            }
            this.lastWidth = width;
            this.lastHeight = height;
            return true;
        }

        while (this.raindrops.size() < desiredCount) {
            this.raindrops.add(createRaindrop(width, height, true));
        }
        while (this.raindrops.size() > desiredCount) {
            this.raindrops.remove(this.raindrops.size() - 1);
        }
        return false;
    }

    public void addCollisionArea(int x, int y, int width, int height) {
        if (width <= 0 || height < 0) {
            return;
        }
        SurfaceArea area = new SurfaceArea(x, y, width, height);
        this.collisionAreas.add(area);
    }

    public void clearCollisionAreas() {
        this.collisionAreas.clear();
        this.puddles.clear();
        this.drips.clear();
    }

    private void ensureSurfaceAreas(int width, int height, boolean sizeChanged) {
        int surfaceY = Math.max(0, height - 1);
        if (this.bottomArea == null || sizeChanged) {
            this.bottomArea = new SurfaceArea(0, surfaceY, width, 0);
        } else {
            this.bottomArea.setBounds(0, surfaceY, width, 0);
        }

        if (sizeChanged) {
            this.puddles.clear();
            this.drips.clear();
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

    private void updateWind(long now, float deltaSeconds) {
        if (now >= this.nextWindChangeMs) {
            this.windTarget = nextRange(WIND_MIN, WIND_MAX);
            this.nextWindChangeMs = now + 2500L + this.random.nextInt(3000);
        }
        float response = 0.2F * deltaSeconds;
        this.wind += (this.windTarget - this.wind) * response;
    }

    private void updateRaindrops(int width, int height, float deltaSeconds) {
        float wrapPadding = 22.0F;
        for (Raindrop drop : this.raindrops) {
            float previousY = drop.y;
            drop.y += drop.fallSpeed * deltaSeconds;
            drop.x += (drop.driftSpeed + this.wind) * deltaSeconds;

            if (handleRainCollision(drop, previousY, width, height)) {
                continue;
            }

            if (drop.y > height + drop.length) {
                if (this.puddles.size() < this.currentPuddleTarget + 2 && this.random.nextFloat() < PUDDLE_SPLASH_CHANCE) {
                    spawnRandomPuddle(width);
                }
                resetRaindrop(drop, width, height, false);
                continue;
            }

            if (drop.x < -wrapPadding) {
                drop.x = width + this.random.nextFloat() * wrapPadding;
            } else if (drop.x > width + wrapPadding) {
                drop.x = -this.random.nextFloat() * wrapPadding;
            }
        }
    }

    private boolean handleRainCollision(Raindrop drop, float previousY, int width, int height) {
        float effectiveX = drop.x;
        if (this.bottomArea != null && checkCollisionAndPuddle(drop, this.bottomArea, effectiveX, previousY, width, height)) {
            return true;
        }
        for (SurfaceArea area : this.collisionAreas) {
            if (checkCollisionAndPuddle(drop, area, effectiveX, previousY, width, height)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkCollisionAndPuddle(Raindrop drop, SurfaceArea area, float effectiveX, float previousY, int width, int height) {
        if (!area.isValid()) {
            return false;
        }
        if (effectiveX < area.x || effectiveX >= area.x + area.width) {
            return false;
        }
        float surfaceY = area.y;
        float currentBottom = drop.y + drop.length;
        float previousBottom = previousY + drop.length;
        if (currentBottom >= surfaceY && previousBottom < surfaceY) {
            spawnPuddleAt(area, effectiveX, true);
            resetRaindrop(drop, width, height, false);
            return true;
        }
        return false;
    }

    private void updatePuddles(int width, int height, float deltaSeconds, boolean sizeChanged) {
        if (sizeChanged) {
            this.puddles.clear();
            this.drips.clear();
        }

        for (Iterator<PuddleLine> iterator = this.puddles.iterator(); iterator.hasNext(); ) {
            PuddleLine puddle = iterator.next();
            puddle.age += deltaSeconds;
            if (puddle.age >= puddle.lifeTime) {
                iterator.remove();
            }
        }

        this.currentPuddleTarget = getTargetPuddleCount(width);
        int missing = this.currentPuddleTarget - this.puddles.size();
        if (missing <= 0) {
            return;
        }
        float expectedSpawns = missing * PUDDLE_SPAWN_RATE * deltaSeconds;
        int spawnCount = Mth.floor(expectedSpawns);
        if (this.random.nextFloat() < expectedSpawns - spawnCount) {
            spawnCount++;
        }
        for (int i = 0; i < spawnCount && this.puddles.size() < this.currentPuddleTarget; i++) {
            spawnRandomPuddle(width);
        }
    }

    private void updateDrips(int height, float deltaSeconds, boolean sizeChanged) {
        if (sizeChanged) {
            this.drips.clear();
        }
        for (Iterator<Drip> iterator = this.drips.iterator(); iterator.hasNext(); ) {
            Drip drip = iterator.next();
            drip.age += deltaSeconds;
            drip.y += drip.speed * deltaSeconds;
            if (drip.age >= drip.lifeTime || drip.y > height + drip.length) {
                iterator.remove();
            }
        }
    }

    private void renderRaindrops(GuiGraphics graphics, int overlayX, int overlayY, int overlayWidth, int overlayHeight) {
        for (Raindrop drop : this.raindrops) {
            float slant = (drop.driftSpeed + this.wind) * SLANT_FACTOR;
            int segments = Math.max(2, Mth.ceil(drop.length));
            float stepX = slant / (float)segments;
            float stepY = drop.length / (float)segments;
            int baseX = overlayX + Mth.floor(drop.x);
            int baseY = overlayY + Mth.floor(drop.y);

            for (int i = 0; i < segments; i++) {
                int x = baseX + Mth.floor(stepX * i);
                int y = baseY + Mth.floor(stepY * i);
                if (x < overlayX || x >= overlayX + overlayWidth) {
                    continue;
                }
                if (y < overlayY || y >= overlayY + overlayHeight) {
                    continue;
                }
                float progress = i / (float)segments;
                float fade = 0.6F + progress * 0.25F;
                int alpha = Mth.clamp(Mth.floor(drop.alpha * fade), 0, 255);
                int color = (alpha << 24) | DROP_RGB;
                graphics.fill(x, y, x + 1, y + 1, color);
            }
        }
    }

    private void renderPuddles(GuiGraphics graphics, int overlayX, int overlayY, int overlayWidth, int overlayHeight) {
        if (this.puddles.isEmpty()) {
            return;
        }
        for (PuddleLine puddle : this.puddles) {
            float fade = getPuddleFade(puddle);
            int alpha = Mth.clamp(Mth.floor(puddle.baseAlpha * fade), 0, 255);
            if (alpha <= 0) {
                continue;
            }
            int color = (alpha << 24) | PUDDLE_RGB;
            float halfLength = puddle.radius;
            int baseY = Mth.floor(overlayY + puddle.y);
            if (baseY < overlayY || baseY >= overlayY + overlayHeight) {
                continue;
            }
            int left = Mth.floor(overlayX + puddle.x - halfLength);
            int right = Mth.ceil(overlayX + puddle.x + halfLength);
            int minX = overlayX + puddle.minX;
            int maxX = overlayX + puddle.maxX;
            left = Math.max(left, minX);
            right = Math.min(right, maxX);
            if (left < right) {
                graphics.fill(left, baseY, right, baseY + 1, color);
            }
        }
    }

    private void renderDrips(GuiGraphics graphics, int overlayX, int overlayY, int overlayWidth, int overlayHeight) {
        if (this.drips.isEmpty()) {
            return;
        }
        for (Drip drip : this.drips) {
            float fade = getDripFade(drip);
            int alpha = Mth.clamp(Mth.floor(drip.alpha * fade), 0, 255);
            if (alpha <= 0) {
                continue;
            }
            int color = (alpha << 24) | DRIP_RGB;
            int x = overlayX + Mth.floor(drip.x);
            int top = overlayY + Mth.floor(drip.y);
            int bottom = overlayY + Mth.ceil(drip.y + drip.length);
            if (x < overlayX || x >= overlayX + overlayWidth) {
                continue;
            }
            if (bottom <= overlayY || top >= overlayY + overlayHeight) {
                continue;
            }
            top = Math.max(top, overlayY);
            bottom = Math.min(bottom, overlayY + overlayHeight);
            if (top < bottom) {
                graphics.fill(x, top, x + 1, bottom, color);
            }
        }
    }

    private float getPuddleFade(PuddleLine puddle) {
        float fadeDuration = puddle.lifeTime * PUDDLE_FADE_FRACTION;
        if (fadeDuration <= 0.0F) {
            return 1.0F;
        }
        if (puddle.age < fadeDuration) {
            return puddle.age / fadeDuration;
        }
        if (puddle.age > puddle.lifeTime - fadeDuration) {
            return (puddle.lifeTime - puddle.age) / fadeDuration;
        }
        return 1.0F;
    }

    private float getDripFade(Drip drip) {
        float fadeDuration = drip.lifeTime * 0.25F;
        if (fadeDuration <= 0.0F) {
            return 1.0F;
        }
        if (drip.age < fadeDuration) {
            return drip.age / fadeDuration;
        }
        if (drip.age > drip.lifeTime - fadeDuration) {
            return (drip.lifeTime - drip.age) / fadeDuration;
        }
        return 1.0F;
    }

    private int getTargetPuddleCount(int width) {
        int surfaceWidth = width;
        for (SurfaceArea area : this.collisionAreas) {
            surfaceWidth += Math.max(0, area.width);
        }
        int target = surfaceWidth / 55;
        return Mth.clamp(target, 4, 28);
    }

    private Raindrop createRaindrop(int width, int height, boolean spawnInside) {
        Raindrop drop = new Raindrop();
        resetRaindrop(drop, width, height, spawnInside);
        return drop;
    }

    private void resetRaindrop(Raindrop drop, int width, int height, boolean spawnInside) {
        drop.length = nextRange(MIN_LENGTH, MAX_LENGTH);
        drop.x = this.random.nextFloat() * width;
        drop.y = spawnInside ? (this.random.nextFloat() * height) : -this.random.nextFloat() * (height * 0.35F);
        drop.fallSpeed = nextRange(MIN_FALL_SPEED, MAX_FALL_SPEED);
        drop.driftSpeed = nextRange(MIN_DRIFT_SPEED, MAX_DRIFT_SPEED);
        drop.alpha = nextInt(MIN_ALPHA, MAX_ALPHA);
    }

    private void spawnRandomPuddle(int width) {
        if (this.puddles.size() >= MAX_PUDDLES) {
            return;
        }
        SurfaceArea area = pickSurfaceArea(width);
        if (area == null) {
            return;
        }
        spawnPuddleAt(area, area.x + this.random.nextFloat() * area.width, true);
    }

    private SurfaceArea pickSurfaceArea(int width) {
        int totalWidth = width;
        for (SurfaceArea area : this.collisionAreas) {
            totalWidth += Math.max(0, area.width);
        }
        if (totalWidth <= 0) {
            return null;
        }
        int roll = this.random.nextInt(totalWidth);
        if (roll < width) {
            return this.bottomArea;
        }
        roll -= width;
        for (SurfaceArea area : this.collisionAreas) {
            int areaWidth = Math.max(0, area.width);
            if (roll < areaWidth) {
                return area;
            }
            roll -= areaWidth;
        }
        return this.bottomArea;
    }

    private void spawnPuddleAt(SurfaceArea area, float x, boolean allowDrip) {
        if (area == null || !area.isValid()) {
            return;
        }
        if (this.puddles.size() >= MAX_PUDDLES) {
            this.puddles.remove(0);
        }
        PuddleLine puddle = new PuddleLine();
        puddle.radius = nextRange(PUDDLE_MIN_RADIUS, PUDDLE_MAX_RADIUS);
        float minCenter = area.x + puddle.radius;
        float maxCenter = area.x + area.width - puddle.radius;
        if (maxCenter < minCenter) {
            puddle.x = area.x + area.width * 0.5F;
        } else {
            puddle.x = Mth.clamp(x, minCenter, maxCenter);
        }
        puddle.y = area.y;
        puddle.minX = area.x;
        puddle.maxX = area.x + area.width;
        puddle.age = 0.0F;
        puddle.lifeTime = nextRange(PUDDLE_MIN_LIFE, PUDDLE_MAX_LIFE);
        puddle.baseAlpha = nextInt(PUDDLE_MIN_ALPHA, PUDDLE_MAX_ALPHA);
        this.puddles.add(puddle);

        if (allowDrip && area != this.bottomArea) {
            maybeSpawnDripForPuddle(puddle, area);
        }
    }

    private void maybeSpawnDripForPuddle(PuddleLine puddle, SurfaceArea area) {
        float leftEdge = puddle.x - puddle.radius;
        float rightEdge = puddle.x + puddle.radius;
        boolean atLeft = leftEdge <= area.x + 0.6F;
        boolean atRight = rightEdge >= area.x + area.width - 0.6F;
        if (!atLeft && !atRight) {
            return;
        }
        if (this.drips.size() >= MAX_PUDDLES) {
            return;
        }
        float dripX = atLeft ? area.x : area.x + area.width - 1.0F;
        Drip drip = new Drip();
        drip.x = dripX;
        drip.y = area.y + 1.0F;
        drip.length = nextRange(DRIP_MIN_LENGTH, DRIP_MAX_LENGTH);
        drip.speed = nextRange(DRIP_MIN_SPEED, DRIP_MAX_SPEED);
        drip.lifeTime = nextRange(DRIP_MIN_LIFE, DRIP_MAX_LIFE);
        drip.alpha = nextInt(DRIP_MIN_ALPHA, DRIP_MAX_ALPHA);
        drip.age = 0.0F;
        this.drips.add(drip);
    }

    private float nextRange(float min, float max) {
        return min + this.random.nextFloat() * (max - min);
    }

    private int nextInt(int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + this.random.nextInt(max - min + 1);
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

    private static final class Raindrop {
        private float x;
        private float y;
        private float fallSpeed;
        private float driftSpeed;
        private float length;
        private int alpha;
    }

    private static final class PuddleLine {
        private float x;
        private float y;
        private float radius;
        private int minX;
        private int maxX;
        private float age;
        private float lifeTime;
        private int baseAlpha;
    }

    private static final class Drip {
        private float x;
        private float y;
        private float length;
        private float speed;
        private float age;
        private float lifeTime;
        private int alpha;
    }

    private static final class SurfaceArea {
        private int x;
        private int y;
        private int width;
        private int height;

        private SurfaceArea(int x, int y, int width, int height) {
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

}
