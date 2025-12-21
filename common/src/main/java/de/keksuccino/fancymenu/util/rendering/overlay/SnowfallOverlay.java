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
import java.util.Arrays;
import java.util.List;

public class SnowfallOverlay extends AbstractWidget implements NavigatableWidget {

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
    private static final float MAX_ACCUMULATION_HEIGHT = 3.0F;
    private static final int ACCUMULATION_COLOR = (230 << 24) | 0xFFFFFF;
    private static final int STACK_MIN_CONNECTED = 5;

    private final RandomSource random = RandomSource.create();
    private final List<Snowflake> snowflakes = new ArrayList<>();
    private final List<AccumulationArea> collisionAreas = new ArrayList<>();
    private AccumulationArea bottomArea;
    private boolean accumulationEnabled = true;
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

        boolean sizeChanged = ensureSnowflakes(overlayWidth, overlayHeight);
        if (this.accumulationEnabled) {
            ensureAccumulationAreas(overlayWidth, overlayHeight, sizeChanged);
        }
        updateSnowflakes(overlayWidth, overlayHeight, this.accumulationEnabled);
        if (this.accumulationEnabled) {
            renderAccumulation(graphics, overlayX, overlayY, overlayWidth, overlayHeight);
        }

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

    public void addCollisionArea(int x, int y, int width, int height) {
        if (width <= 0 || height < 0) {
            return;
        }
        AccumulationArea area = new AccumulationArea(x, y, width, height, MAX_ACCUMULATION_HEIGHT);
        area.ensureHeights();
        this.collisionAreas.add(area);
    }

    public void clearCollisionAreas() {
        this.collisionAreas.clear();
    }

    public void setAccumulationEnabled(boolean accumulationEnabled) {
        this.accumulationEnabled = accumulationEnabled;
    }

    private boolean ensureSnowflakes(int width, int height) {
        int desiredCount = Mth.clamp((width * height) / AREA_PER_SNOWFLAKE, MIN_SNOWFLAKES, MAX_SNOWFLAKES);
        boolean sizeChanged = this.lastWidth != width || this.lastHeight != height;
        if (sizeChanged) {
            this.snowflakes.clear();
            for (int i = 0; i < desiredCount; i++) {
                this.snowflakes.add(createSnowflake(width, height, true));
            }
            this.lastWidth = width;
            this.lastHeight = height;
            this.lastUpdateMs = System.currentTimeMillis();
            return true;
        }

        while (this.snowflakes.size() < desiredCount) {
            this.snowflakes.add(createSnowflake(width, height, true));
        }
        while (this.snowflakes.size() > desiredCount) {
            this.snowflakes.remove(this.snowflakes.size() - 1);
        }
        return false;
    }

    private void ensureAccumulationAreas(int width, int height, boolean sizeChanged) {
        if (this.bottomArea == null || sizeChanged) {
            this.bottomArea = new AccumulationArea(0, height, width, 0, MAX_ACCUMULATION_HEIGHT);
        } else {
            this.bottomArea.setBounds(0, height, width, 0);
        }
        this.bottomArea.ensureHeights();
        if (sizeChanged) {
            this.bottomArea.clear();
        }

        for (AccumulationArea area : this.collisionAreas) {
            area.ensureHeights();
            if (sizeChanged) {
                area.clear();
            }
        }
    }

    private void updateSnowflakes(int width, int height, boolean accumulateSnow) {
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
            float previousY = flake.y;
            flake.y += flake.fallSpeed * deltaSeconds;
            flake.x += (flake.driftSpeed + this.wind) * deltaSeconds;
            flake.swayTime += flake.swaySpeed * deltaSeconds;

            if (accumulateSnow) {
                float effectiveX = flake.x + Mth.sin(flake.swayTime) * flake.swayAmplitude;
                if (handleSnowCollision(flake, effectiveX, previousY, width, height)) {
                    continue;
                }
            }

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

    private boolean handleSnowCollision(Snowflake flake, float effectiveX, float previousY, int width, int height) {
        if (this.bottomArea != null && checkCollisionAndDeposit(flake, this.bottomArea, effectiveX, previousY, width, height)) {
            return true;
        }
        for (AccumulationArea area : this.collisionAreas) {
            if (checkCollisionAndDeposit(flake, area, effectiveX, previousY, width, height)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkCollisionAndDeposit(Snowflake flake, AccumulationArea area, float effectiveX, float previousY, int width, int height) {
        if (!area.isValid()) {
            return false;
        }
        if (effectiveX < area.x || effectiveX >= area.x + area.width) {
            return false;
        }
        int index = area.getIndex(effectiveX - area.x);
        int existingLayers = area.getLayerAt(index);
        float surfaceY = area.y - existingLayers;
        float currentBottom = flake.y + flake.size;
        float previousBottom = previousY + flake.size;
        if (currentBottom >= surfaceY && previousBottom < surfaceY) {
            area.deposit(index, existingLayers > 0);
            resetSnowflake(flake, width, height, false);
            return true;
        }
        return false;
    }

    private void updateWind(long now, float deltaSeconds) {
        if (now >= this.nextWindChangeMs) {
            this.windTarget = nextRange(WIND_MIN, WIND_MAX);
            this.nextWindChangeMs = now + 3500L + this.random.nextInt(4000);
        }
        float response = 0.15F * deltaSeconds;
        this.wind += (this.windTarget - this.wind) * response;
    }

    private void renderAccumulation(GuiGraphics graphics, int overlayX, int overlayY, int overlayWidth, int overlayHeight) {
        if (this.bottomArea != null) {
            renderAccumulationArea(graphics, this.bottomArea, overlayX, overlayY, overlayWidth, overlayHeight);
        }
        for (AccumulationArea area : this.collisionAreas) {
            renderAccumulationArea(graphics, area, overlayX, overlayY, overlayWidth, overlayHeight);
        }
    }

    private void renderAccumulationArea(GuiGraphics graphics, AccumulationArea area, int overlayX, int overlayY, int overlayWidth, int overlayHeight) {
        if (!area.isValid()) {
            return;
        }
        int startX = Math.max(0, area.x);
        int endX = Math.min(overlayWidth, area.x + area.width);
        if (startX >= endX) {
            return;
        }
        int baseY = overlayY + area.y;
        int maxY = overlayY + overlayHeight;
        if (baseY < overlayY || baseY > maxY) {
            return;
        }

        for (int x = startX; x < endX; x++) {
            int index = x - area.x;
            float height = area.getHeightAt(index);
            if (height <= 0.01F) {
                continue;
            }
            int intHeight = Mth.ceil(height);
            int topY = baseY - intHeight;
            if (topY < overlayY) {
                topY = overlayY;
            }
            if (topY >= baseY) {
                continue;
            }
            graphics.fill(overlayX + x, topY, overlayX + x + 1, baseY, ACCUMULATION_COLOR);
        }
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

    private static final class AccumulationArea {
        private int x;
        private int y;
        private int width;
        private int height;
        private final int maxLayers;
        private int[] layers;

        private AccumulationArea(int x, int y, int width, int height, float maxHeight) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.maxLayers = Math.max(1, Mth.floor(maxHeight));
        }

        private void setBounds(int x, int y, int width, int height) {
            if (this.x == x && this.y == y && this.width == width && this.height == height) {
                return;
            }
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.layers = null;
        }

        private boolean isValid() {
            return this.width > 0;
        }

        private void ensureHeights() {
            if (this.width <= 0) {
                this.layers = new int[0];
                return;
            }
            if (this.layers == null || this.layers.length != this.width) {
                this.layers = new int[this.width];
            }
        }

        private void clear() {
            if (this.layers != null && this.layers.length > 0) {
                Arrays.fill(this.layers, 0);
            }
        }

        private int getIndex(float localX) {
            return Mth.clamp(Mth.floor(localX), 0, this.width - 1);
        }

        private float getHeightAt(int index) {
            if (this.layers == null || index < 0 || index >= this.layers.length) {
                return 0.0F;
            }
            return this.layers[index];
        }

        private int getLayerAt(int index) {
            if (this.layers == null || index < 0 || index >= this.layers.length) {
                return 0;
            }
            return this.layers[index];
        }

        private void deposit(int index, boolean stacked) {
            if (this.layers == null || index < 0 || index >= this.layers.length) {
                return;
            }
            addLayerIfAllowed(index);
            if (stacked) {
                addLayerIfAllowed(index);
            }
        }

        private void addLayerIfAllowed(int index) {
            if (this.layers[index] >= this.maxLayers) {
                return;
            }
            int desiredLayer = this.layers[index] + 1;
            if (desiredLayer == 1) {
                this.layers[index]++;
                return;
            }
            int requiredLayer = desiredLayer - 1;
            if (!hasConnectedRun(index, requiredLayer, STACK_MIN_CONNECTED)) {
                return;
            }
            this.layers[index]++;
        }

        private boolean hasConnectedRun(int index, int requiredLayer, int minLength) {
            if (this.layers[index] < requiredLayer) {
                return false;
            }
            int count = 1;
            for (int i = index - 1; i >= 0; i--) {
                if (this.layers[i] < requiredLayer) {
                    break;
                }
                count++;
                if (count >= minLength) {
                    return true;
                }
            }
            for (int i = index + 1; i < this.layers.length; i++) {
                if (this.layers[i] < requiredLayer) {
                    break;
                }
                count++;
                if (count >= minLength) {
                    return true;
                }
            }
            return count >= minLength;
        }
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
