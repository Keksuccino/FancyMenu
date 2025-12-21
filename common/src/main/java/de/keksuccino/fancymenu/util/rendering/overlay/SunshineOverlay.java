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

public class SunshineOverlay extends AbstractWidget implements NavigatableWidget {

    private static final int MIN_RAYS = 8;
    private static final int MAX_RAYS = 38;
    private static final float RAY_SPREAD_RADIANS = 0.85F;
    private static final float MAX_DELTA_SECONDS = 0.1F;

    private final RandomSource random = RandomSource.create();
    private final List<SunRay> rays = new ArrayList<>();
    private int lastWidth = -1;
    private int lastHeight = -1;
    private long lastUpdateMs = -1L;
    private float intensity = 1.0F;
    private float lastIntensity = -1.0F;
    private int sunColor = 0xFFE8B3;
    private float angleDegrees = 0.0F;
    private SunshineSide side = SunshineSide.TOP;
    private float shimmerTime = 0.0F;

    public SunshineOverlay(int width, int height) {
        super(0, 0, width, height, Component.empty());
    }

    public void setIntensity(float intensity) {
        this.intensity = Mth.clamp(intensity, 0.0F, 2.0F);
    }

    public float getIntensity() {
        return this.intensity;
    }

    public void setColor(int color) {
        this.sunColor = color;
    }

    public int getColor() {
        return this.sunColor;
    }

    public void setAngle(float angleDegrees) {
        this.angleDegrees = angleDegrees;
    }

    public float getAngle() {
        return this.angleDegrees;
    }

    public void setSide(@NotNull SunshineSide side) {
        this.side = side;
    }

    @NotNull
    public SunshineSide getSide() {
        return this.side;
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

        boolean sizeChanged = ensureSunElements(overlayWidth, overlayHeight);
        float deltaSeconds = computeDeltaSeconds(sizeChanged);
        updateSunElements(deltaSeconds);

        float effectiveIntensity = Mth.clamp(this.intensity, 0.0F, 2.0F);
        if (effectiveIntensity <= 0.01F) {
            return;
        }

        SunBasis basis = computeSunBasis(overlayX, overlayY, overlayWidth, overlayHeight, effectiveIntensity);
        renderSunRays(graphics, overlayX, overlayY, overlayWidth, overlayHeight, basis, effectiveIntensity);
    }

    private boolean ensureSunElements(int width, int height) {
        float effectiveIntensity = Mth.clamp(this.intensity, 0.0F, 2.0F);
        boolean sizeChanged = this.lastWidth != width || this.lastHeight != height;
        if (sizeChanged || Math.abs(effectiveIntensity - this.lastIntensity) > 0.02F) {
            this.rays.clear();
            if (effectiveIntensity > 0.01F) {
                int rayCount = Mth.clamp(MIN_RAYS + Mth.floor((MAX_RAYS - MIN_RAYS) * (effectiveIntensity / 2.0F)), MIN_RAYS, MAX_RAYS);
                float maxDim = Math.max(width, height);
                for (int i = 0; i < rayCount; i++) {
                    this.rays.add(createRay(maxDim, effectiveIntensity));
                }
            }
            this.lastWidth = width;
            this.lastHeight = height;
            this.lastIntensity = effectiveIntensity;
            this.lastUpdateMs = System.currentTimeMillis();
            return true;
        }
        return false;
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

    private void updateSunElements(float deltaSeconds) {
        if (deltaSeconds <= 0.0F) {
            return;
        }
        this.shimmerTime += deltaSeconds * 0.35F;
        for (SunRay ray : this.rays) {
            ray.time += deltaSeconds * ray.pulseSpeed;
        }
    }

    private SunBasis computeSunBasis(int overlayX, int overlayY, int width, int height, float intensity) {
        float centerX = overlayX + width * 0.5F;
        float centerY = overlayY + height * 0.5F;
        float maxDim = Math.max(width, height);
        float distance = maxDim * (0.75F + 0.15F * intensity) + 120.0F;

        float baseX;
        float baseY;
        switch (this.side) {
            case LEFT -> {
                baseX = 1.0F;
                baseY = 0.0F;
            }
            case RIGHT -> {
                baseX = -1.0F;
                baseY = 0.0F;
            }
            case BOTTOM -> {
                baseX = 0.0F;
                baseY = -1.0F;
            }
            default -> {
                baseX = 0.0F;
                baseY = 1.0F;
            }
        }

        float angleRadians = (float)Math.toRadians(this.angleDegrees);
        float dirX = baseX * Mth.cos(angleRadians) - baseY * Mth.sin(angleRadians);
        float dirY = baseX * Mth.sin(angleRadians) + baseY * Mth.cos(angleRadians);
        float invLen = Mth.invSqrt(dirX * dirX + dirY * dirY);
        dirX *= invLen;
        dirY *= invLen;

        float originX = centerX - dirX * distance;
        float originY = centerY - dirY * distance;

        SunBasis basis = new SunBasis();
        basis.originX = originX;
        basis.originY = originY;
        basis.dirX = dirX;
        basis.dirY = dirY;
        basis.maxDim = maxDim;
        return basis;
    }

    private void renderSunRays(GuiGraphics graphics, int overlayX, int overlayY, int overlayWidth, int overlayHeight, SunBasis basis, float intensity) {
        if (this.rays.isEmpty()) {
            return;
        }

        int baseAlpha = resolveBaseAlpha(this.sunColor, intensity);
        int baseRgb = this.sunColor & 0x00FFFFFF;
        float pulse = 0.85F + 0.15F * Mth.sin(this.shimmerTime);

        for (SunRay ray : this.rays) {
            float shimmer = 0.7F + 0.3F * Mth.sin(ray.time + ray.pulsePhase);
            int alpha = Mth.clamp(Mth.floor(baseAlpha * ray.baseAlpha * shimmer * pulse), 0, 255);
            if (alpha <= 0) {
                continue;
            }
            float rayLength = ray.length * (0.75F + 0.25F * shimmer);
            float angle = ray.angleOffset;
            float dirX = basis.dirX * Mth.cos(angle) - basis.dirY * Mth.sin(angle);
            float dirY = basis.dirX * Mth.sin(angle) + basis.dirY * Mth.cos(angle);
            float invLen = Mth.invSqrt(dirX * dirX + dirY * dirY);
            dirX *= invLen;
            dirY *= invLen;
            renderRay(graphics, overlayX, overlayY, overlayWidth, overlayHeight, basis.originX, basis.originY, dirX, dirY, rayLength, ray.width, alpha, baseRgb);
        }
    }

    private void renderRay(GuiGraphics graphics, int overlayX, int overlayY, int overlayWidth, int overlayHeight, float originX, float originY, float dirX, float dirY, float length, float width, int alpha, int rgb) {
        float step = Math.max(1.0F, width * 0.75F);
        int steps = Mth.clamp(Mth.ceil(length / step), 12, 220);
        float maxX = overlayX + overlayWidth;
        float maxY = overlayY + overlayHeight;

        for (int i = 0; i < steps; i++) {
            float progress = i / (float)(steps - 1);
            float fade = 1.0F - progress;
            int segAlpha = Mth.clamp(Mth.floor(alpha * fade), 0, 255);
            if (segAlpha <= 0) {
                continue;
            }
            float thickness = Math.max(1.0F, width * (0.9F - 0.7F * progress));
            float half = thickness * 0.5F;
            float x = originX + dirX * (progress * length);
            float y = originY + dirY * (progress * length);
            float minX = x - half;
            float maxXSeg = x + half;
            float minY = y - half;
            float maxYSeg = y + half;
            if (maxXSeg <= overlayX || minX >= maxX || maxYSeg <= overlayY || minY >= maxY) {
                continue;
            }
            int x1 = Mth.floor(Math.max(minX, overlayX));
            int x2 = Mth.ceil(Math.min(maxXSeg, maxX));
            int y1 = Mth.floor(Math.max(minY, overlayY));
            int y2 = Mth.ceil(Math.min(maxYSeg, maxY));
            if (x1 >= x2 || y1 >= y2) {
                continue;
            }
            int color = (segAlpha << 24) | rgb;
            graphics.fill(x1, y1, x2, y2, color);
        }
    }

    private SunRay createRay(float maxDim, float intensity) {
        SunRay ray = new SunRay();
        ray.angleOffset = nextRange(-RAY_SPREAD_RADIANS, RAY_SPREAD_RADIANS);
        ray.length = maxDim * nextRange(0.55F, 1.25F) * (0.65F + 0.35F * intensity);
        ray.width = nextRange(2.0F, 6.5F) * (0.65F + 0.35F * intensity);
        ray.baseAlpha = nextRange(0.25F, 0.85F);
        ray.pulseSpeed = nextRange(0.35F, 1.1F);
        ray.pulsePhase = this.random.nextFloat() * ((float)Math.PI * 2.0F);
        ray.time = this.random.nextFloat() * ((float)Math.PI * 2.0F);
        return ray;
    }

    private float nextRange(float min, float max) {
        return min + this.random.nextFloat() * (max - min);
    }

    private int resolveBaseAlpha(int color, float intensity) {
        int baseAlpha = FastColor.ARGB32.alpha(color);
        if (baseAlpha <= 0) {
            baseAlpha = 255;
        }
        return Mth.clamp(Mth.floor(baseAlpha * intensity), 0, 255);
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

    public enum SunshineSide {
        TOP,
        RIGHT,
        BOTTOM,
        LEFT
    }

    private static final class SunRay {
        private float angleOffset;
        private float length;
        private float width;
        private float baseAlpha;
        private float pulseSpeed;
        private float pulsePhase;
        private float time;
    }

    private static final class SunBasis {
        private float originX;
        private float originY;
        private float dirX;
        private float dirY;
        private float maxDim;
    }

}
