package de.keksuccino.fancymenu.util.rendering.eastereggs;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SnowfallOverlay extends AbstractWidget {

    private static final int AREA_PER_SNOWFLAKE = 9000;
    private static final int MIN_SNOWFLAKES = 80;
    private static final int MAX_SNOWFLAKES = 450;
    private static final float MIN_FALL_SPEED = 12.0F;
    private static final float MAX_FALL_SPEED = 40.0F;
    private static final float MIN_DRIFT_SPEED = -10.0F;
    private static final float MAX_DRIFT_SPEED = 10.0F;
    private static final float MIN_SWAY_AMPLITUDE = 2.0F;
    private static final float MAX_SWAY_AMPLITUDE = 10.0F;
    private static final float MIN_SWAY_SPEED = 0.35F;
    private static final float MAX_SWAY_SPEED = 1.1F;
    private static final float WIND_MIN = -8.0F;
    private static final float WIND_MAX = 8.0F;
    private static final float MAX_DELTA_SECONDS = 0.1F;
    private static final int MIN_ALPHA = 150;
    private static final int MAX_ALPHA = 255;

    private final RandomSource random = RandomSource.create();
    private final List<Snowflake> snowflakes = new ArrayList<>();
    private int lastWidth = -1;
    private int lastHeight = -1;
    private long lastUpdateMs = -1L;
    private float wind = 0.0F;
    private float windTarget = 0.0F;
    private long nextWindChangeMs = 0L;

    public SnowfallOverlay(int width, int height) {
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

        ensureSnowflakes(overlayWidth, overlayHeight);
        updateSnowflakes(overlayWidth, overlayHeight);

        for (Snowflake flake : this.snowflakes) {
            float swayOffset = Mth.sin(flake.swayTime) * flake.swayAmplitude;
            int renderX = overlayX + Mth.floor(flake.x + swayOffset);
            int renderY = overlayY + Mth.floor(flake.y);
            if (renderX + flake.size <= overlayX || renderX >= overlayX + overlayWidth) {
                continue;
            }
            if (renderY + flake.size <= overlayY || renderY >= overlayY + overlayHeight) {
                continue;
            }
            graphics.fill(renderX, renderY, renderX + flake.size, renderY + flake.size, flake.color);
        }

    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }

    private void ensureSnowflakes(int width, int height) {
        int desiredCount = Mth.clamp((width * height) / AREA_PER_SNOWFLAKE, MIN_SNOWFLAKES, MAX_SNOWFLAKES);
        if (this.lastWidth != width || this.lastHeight != height) {
            this.snowflakes.clear();
            for (int i = 0; i < desiredCount; i++) {
                this.snowflakes.add(createSnowflake(width, height, true));
            }
            this.lastWidth = width;
            this.lastHeight = height;
            this.lastUpdateMs = System.currentTimeMillis();
            return;
        }

        while (this.snowflakes.size() < desiredCount) {
            this.snowflakes.add(createSnowflake(width, height, true));
        }
        while (this.snowflakes.size() > desiredCount) {
            this.snowflakes.remove(this.snowflakes.size() - 1);
        }
    }

    private void updateSnowflakes(int width, int height) {
        long now = System.currentTimeMillis();
        if (this.lastUpdateMs < 0L) {
            this.lastUpdateMs = now;
            return;
        }
        float deltaSeconds = (now - this.lastUpdateMs) / 1000.0F;
        if (deltaSeconds <= 0.0F) {
            return;
        }
        if (deltaSeconds > MAX_DELTA_SECONDS) {
            deltaSeconds = MAX_DELTA_SECONDS;
        }
        this.lastUpdateMs = now;

        updateWind(now, deltaSeconds);

        float wrapPadding = 12.0F;
        for (Snowflake flake : this.snowflakes) {
            flake.y += flake.fallSpeed * deltaSeconds;
            flake.x += (flake.driftSpeed + this.wind) * deltaSeconds;
            flake.swayTime += flake.swaySpeed * deltaSeconds;

            if (flake.y > height + flake.size) {
                resetSnowflake(flake, width, height, false);
                continue;
            }

            if (flake.x < -wrapPadding) {
                flake.x = width + this.random.nextFloat() * wrapPadding;
            } else if (flake.x > width + wrapPadding) {
                flake.x = -this.random.nextFloat() * wrapPadding;
            }
        }
    }

    private void updateWind(long now, float deltaSeconds) {
        if (now >= this.nextWindChangeMs) {
            this.windTarget = nextRange(WIND_MIN, WIND_MAX);
            this.nextWindChangeMs = now + 3500L + this.random.nextInt(4000);
        }
        float response = 0.15F * deltaSeconds;
        this.wind += (this.windTarget - this.wind) * response;
    }

    private Snowflake createSnowflake(int width, int height, boolean spawnInside) {
        Snowflake flake = new Snowflake();
        resetSnowflake(flake, width, height, spawnInside);
        return flake;
    }

    private void resetSnowflake(Snowflake flake, int width, int height, boolean spawnInside) {
        flake.size = (this.random.nextFloat() < 0.2F) ? 2 : 1;
        flake.x = this.random.nextFloat() * width;
        flake.y = spawnInside ? (this.random.nextFloat() * height) : -this.random.nextFloat() * (height * 0.5F);
        flake.fallSpeed = nextRange(MIN_FALL_SPEED, MAX_FALL_SPEED) * (flake.size == 2 ? 0.85F : 1.0F);
        flake.driftSpeed = nextRange(MIN_DRIFT_SPEED, MAX_DRIFT_SPEED);
        flake.swayAmplitude = nextRange(MIN_SWAY_AMPLITUDE, MAX_SWAY_AMPLITUDE);
        flake.swaySpeed = nextRange(MIN_SWAY_SPEED, MAX_SWAY_SPEED);
        flake.swayTime = this.random.nextFloat() * ((float)Math.PI * 2.0F);
        flake.color = (nextInt(MIN_ALPHA, MAX_ALPHA) << 24) | 0xFFFFFF;
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

    private static final class Snowflake {
        private float x;
        private float y;
        private float fallSpeed;
        private float driftSpeed;
        private float swayAmplitude;
        private float swaySpeed;
        private float swayTime;
        private int size;
        private int color;
    }

}
