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
import java.util.Iterator;
import java.util.List;

public class ConfettiOverlay extends AbstractWidget implements NavigatableWidget {

    private static final float MIN_SCALE = 0.25F;
    private static final float MAX_SCALE = 5.0F;
    private static final float MIN_DENSITY = 0.01F;
    private static final float MAX_DENSITY = 5.0F;
    private static final float MIN_AMOUNT = 0.01F;
    private static final float MAX_AMOUNT = 5.0F;
    private static final float MAX_DELTA_SECONDS = 0.1F;

    private static final int AREA_PER_ACTIVE = 14000;
    private static final int MIN_ACTIVE = 60;
    private static final int MAX_ACTIVE = 520;
    private static final int AREA_PER_SETTLED = 1800;
    private static final int MIN_SETTLED = 120;
    private static final int MAX_SETTLED = 1800;

    private static final int AREA_PER_BURST = 160000;
    private static final float MIN_BASE_RATE = 0.25F;
    private static final float MAX_BASE_RATE = 2.5F;
    private static final float MIN_EFFECTIVE_RATE = 0.05F;
    private static final float MAX_EFFECTIVE_RATE = 6.0F;

    private static final int MIN_BURST_PARTICLES = 12;
    private static final int MAX_BURST_PARTICLES = 36;
    private static final int MAX_BURST_PARTICLE_CAP = 140;

    private static final float SIDE_MIN_SPEED = 90.0F;
    private static final float SIDE_MAX_SPEED = 190.0F;
    private static final float VERTICAL_SCATTER = 90.0F;
    private static final float HORIZONTAL_SCATTER = 90.0F;
    private static final float CLICK_MIN_SPEED = 90.0F;
    private static final float CLICK_MAX_SPEED = 210.0F;
    private static final float GRAVITY = 28.0F;
    private static final float DRAG = 0.12F;
    private static final float MAX_FALL_SPEED = 220.0F;
    private static final float DESPAWN_PADDING = 48.0F;

    private static final int MIN_ALPHA = 170;
    private static final int MAX_ALPHA = 255;

    private static final int[] COLOR_PALETTE = new int[] {
            0xF44336, 0xFF7043, 0xFFB300, 0xFFEB3B, 0x8BC34A, 0x4CAF50,
            0x00BCD4, 0x03A9F4, 0x2196F3, 0x3F51B5, 0x9C27B0, 0xE91E63
    };

    private final RandomSource random = RandomSource.create();
    private final List<ConfettiPiece> activePieces = new ArrayList<>();
    private final List<ConfettiPiece> settledPieces = new ArrayList<>();
    private final List<LandingArea> collisionAreas = new ArrayList<>();
    private LandingArea bottomArea;
    private int lastWidth = -1;
    private int lastHeight = -1;
    private float lastScale = -1.0F;
    private long lastUpdateMs = -1L;
    private float spawnTimer = 0.0F;
    private float scale = 1.0F;
    private float burstDensity = 1.0F;
    private float burstAmount = 1.0F;
    private boolean colorMixEnabled = true;
    private boolean autoSpawnEnabled = true;
    private int baseColor = 0xFFFFFFFF;
    private float baseAlphaScale = 1.0F;

    public ConfettiOverlay(int width, int height) {
        super(0, 0, width, height, Component.empty());
    }

    public void setScale(float scale) {
        this.scale = Mth.clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    public float getScale() {
        return this.scale;
    }

    public void setBurstDensity(float density) {
        this.burstDensity = Mth.clamp(density, MIN_DENSITY, MAX_DENSITY);
    }

    public float getBurstDensity() {
        return this.burstDensity;
    }

    public void setBurstAmount(float amount) {
        this.burstAmount = Mth.clamp(amount, MIN_AMOUNT, MAX_AMOUNT);
    }

    public float getBurstAmount() {
        return this.burstAmount;
    }

    public void setAutoSpawnEnabled(boolean autoSpawnEnabled) {
        this.autoSpawnEnabled = autoSpawnEnabled;
    }

    public void setColorMixEnabled(boolean colorMixEnabled) {
        if (this.colorMixEnabled == colorMixEnabled) {
            return;
        }
        this.colorMixEnabled = colorMixEnabled;
        if (!colorMixEnabled) {
            applyBaseColor();
        }
    }

    public void setBaseColor(int baseColor) {
        float previousAlphaScale = this.baseAlphaScale;
        this.baseColor = baseColor;
        this.baseAlphaScale = resolveAlphaScale(baseColor);
        if (!this.colorMixEnabled) {
            applyBaseColor(previousAlphaScale);
        }
    }

    public void triggerBurstAt(float x, float y) {
        int width = this.getWidth();
        int height = this.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        int maxActive = getMaxActiveCount(width, height);
        if (this.activePieces.size() >= maxActive) {
            return;
        }
        int count = getParticlesPerBurst();
        int available = maxActive - this.activePieces.size();
        count = Math.min(count, available);
        if (count <= 0) {
            return;
        }
        spawnClickBurst(width, height, x, y, count);
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
        boolean sizeChanged = overlayWidth != this.lastWidth || overlayHeight != this.lastHeight || scaleChanged;
        if (sizeChanged) {
            this.lastWidth = overlayWidth;
            this.lastHeight = overlayHeight;
            this.lastScale = this.scale;
            this.lastUpdateMs = -1L;
            this.spawnTimer = 0.0F;
            this.activePieces.clear();
            this.settledPieces.clear();
        }

        ensureLandingAreas(overlayWidth, overlayHeight);
        int maxActive = getMaxActiveCount(overlayWidth, overlayHeight);
        int maxSettled = getMaxSettledCount(overlayWidth, overlayHeight);

        float deltaSeconds = computeDeltaSeconds(sizeChanged);
        if (deltaSeconds > 0.0F) {
            updateSpawn(overlayWidth, overlayHeight, deltaSeconds, maxActive);
            updatePieces(overlayWidth, overlayHeight, deltaSeconds, maxSettled);
        }

        renderPieces(graphics, overlayX, overlayY, overlayWidth, overlayHeight);
    }

    private void ensureLandingAreas(int width, int height) {
        if (this.bottomArea == null) {
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

    private void updateSpawn(int width, int height, float deltaSeconds, int maxActive) {
        if (!this.autoSpawnEnabled) {
            return;
        }
        float interval = getSpawnInterval(width, height);
        this.spawnTimer += deltaSeconds;
        while (this.spawnTimer >= interval) {
            this.spawnTimer -= interval;
            spawnEdgeBursts(width, height, maxActive);
        }
    }

    private void updatePieces(int width, int height, float deltaSeconds, int maxSettled) {
        if (this.activePieces.isEmpty()) {
            return;
        }
        float drag = Math.max(0.0F, 1.0F - DRAG * deltaSeconds);
        float maxFall = MAX_FALL_SPEED * (0.7F + 0.3F * this.scale);
        float padding = DESPAWN_PADDING * this.scale;
        Iterator<ConfettiPiece> iterator = this.activePieces.iterator();
        while (iterator.hasNext()) {
            ConfettiPiece piece = iterator.next();
            float previousY = piece.y;

            piece.vy += GRAVITY * deltaSeconds;
            piece.vx *= drag;
            piece.vy *= drag;
            piece.vy = Math.min(piece.vy, maxFall);

            float newX = piece.x + piece.vx * deltaSeconds;
            float newY = piece.y + piece.vy * deltaSeconds;

            if (piece.vy >= 0.0F) {
                float landingY = findLandingSurface(piece, previousY, newX, newY, width, height);
                if (!Float.isNaN(landingY)) {
                    piece.x = newX;
                    piece.y = landingY - piece.height;
                    piece.vx = 0.0F;
                    piece.vy = 0.0F;
                    iterator.remove();
                    addSettledPiece(piece, maxSettled);
                    continue;
                }
            }

            piece.x = newX;
            piece.y = newY;

            if (piece.y > height + padding || piece.x < -padding || piece.x > width + padding || piece.y < -padding * 1.5F) {
                iterator.remove();
            }
        }
    }

    private void renderPieces(GuiGraphics graphics, int overlayX, int overlayY, int overlayWidth, int overlayHeight) {
        for (ConfettiPiece piece : this.settledPieces) {
            renderPiece(graphics, overlayX, overlayY, overlayWidth, overlayHeight, piece);
        }
        for (ConfettiPiece piece : this.activePieces) {
            renderPiece(graphics, overlayX, overlayY, overlayWidth, overlayHeight, piece);
        }
    }

    private void renderPiece(GuiGraphics graphics, int overlayX, int overlayY, int overlayWidth, int overlayHeight, ConfettiPiece piece) {
        int renderX = overlayX + Mth.floor(piece.x);
        int renderY = overlayY + Mth.floor(piece.y);
        int width = piece.width;
        int height = piece.height;
        if (renderX + width <= overlayX || renderX >= overlayX + overlayWidth) {
            return;
        }
        if (renderY + height <= overlayY || renderY >= overlayY + overlayHeight) {
            return;
        }
        graphics.fill(renderX, renderY, renderX + width, renderY + height, piece.color);
    }

    private float getSpawnInterval(int width, int height) {
        float areaRate = (width * height) / (float)AREA_PER_BURST;
        float baseRate = Mth.clamp(areaRate, MIN_BASE_RATE, MAX_BASE_RATE);
        float effectiveRate = Mth.clamp(baseRate * this.burstAmount, MIN_EFFECTIVE_RATE, MAX_EFFECTIVE_RATE);
        return 1.0F / Math.max(0.001F, effectiveRate);
    }

    private int getParticlesPerBurst() {
        int baseCount = Mth.floor(nextRange(MIN_BURST_PARTICLES, MAX_BURST_PARTICLES));
        int count = Math.round(baseCount * this.burstDensity);
        return Mth.clamp(count, MIN_BURST_PARTICLES, MAX_BURST_PARTICLE_CAP);
    }

    private int getMaxActiveCount(int width, int height) {
        int baseCount = Mth.clamp((width * height) / AREA_PER_ACTIVE, MIN_ACTIVE, MAX_ACTIVE);
        float multiplier = 0.55F + 0.3F * Mth.clamp(this.burstDensity, 0.0F, 2.5F) + 0.15F * Mth.clamp(this.burstAmount, 0.0F, 2.5F);
        return Mth.clamp(Math.round(baseCount * multiplier), MIN_ACTIVE, MAX_ACTIVE);
    }

    private int getMaxSettledCount(int width, int height) {
        int baseCount = Mth.clamp((width * height) / AREA_PER_SETTLED, MIN_SETTLED, MAX_SETTLED);
        float multiplier = 0.65F + 0.35F * Mth.clamp(this.burstDensity, 0.0F, 2.5F);
        return Mth.clamp(Math.round(baseCount * multiplier), MIN_SETTLED, MAX_SETTLED);
    }

    private void spawnEdgeBursts(int width, int height, int maxActive) {
        int available = maxActive - this.activePieces.size();
        if (available <= 0) {
            return;
        }
        int total = Math.min(getParticlesPerBurst(), available);
        if (total <= 0) {
            return;
        }
        int perSide = total / 4;
        int remainder = total % 4;
        for (int i = 0; i < 4; i++) {
            int count = perSide + (i < remainder ? 1 : 0);
            if (count <= 0) {
                continue;
            }
            spawnSideBurst(width, height, ConfettiSide.fromIndex(i), count);
        }
    }

    private void spawnSideBurst(int width, int height, ConfettiSide side, int count) {
        float padding = 10.0F * this.scale;
        for (int i = 0; i < count; i++) {
            float originX;
            float originY;
            float speed = nextRange(SIDE_MIN_SPEED, SIDE_MAX_SPEED) * (0.8F + 0.2F * this.scale);
            float vx = 0.0F;
            float vy = 0.0F;

            switch (side) {
                case LEFT -> {
                    originX = -padding;
                    originY = this.random.nextFloat() * height;
                    vx = speed;
                    vy = nextRange(-VERTICAL_SCATTER, VERTICAL_SCATTER);
                }
                case RIGHT -> {
                    originX = width + padding;
                    originY = this.random.nextFloat() * height;
                    vx = -speed;
                    vy = nextRange(-VERTICAL_SCATTER, VERTICAL_SCATTER);
                }
                case TOP -> {
                    originX = this.random.nextFloat() * width;
                    originY = -padding;
                    vx = nextRange(-HORIZONTAL_SCATTER, HORIZONTAL_SCATTER);
                    vy = speed * 0.7F;
                }
                case BOTTOM -> {
                    originX = this.random.nextFloat() * width;
                    originY = height + padding;
                    vx = nextRange(-HORIZONTAL_SCATTER, HORIZONTAL_SCATTER);
                    vy = -speed * 0.7F;
                }
                default -> {
                    originX = this.random.nextFloat() * width;
                    originY = -padding;
                }
            }

            ConfettiPiece piece = createPiece(originX, originY, vx, vy);
            this.activePieces.add(piece);
        }
    }

    private void spawnClickBurst(int width, int height, float x, float y, int count) {
        float clampedX = Mth.clamp(x, 0.0F, width);
        float clampedY = Mth.clamp(y, 0.0F, height);
        for (int i = 0; i < count; i++) {
            float angle = this.random.nextFloat() * ((float)Math.PI * 2.0F);
            float speed = nextRange(CLICK_MIN_SPEED, CLICK_MAX_SPEED) * (0.8F + 0.2F * this.scale);
            float vx = Mth.cos(angle) * speed;
            float vy = Mth.sin(angle) * speed - nextRange(20.0F, 80.0F);
            ConfettiPiece piece = createPiece(clampedX, clampedY, vx, vy);
            this.activePieces.add(piece);
        }
    }

    private ConfettiPiece createPiece(float originX, float originY, float vx, float vy) {
        ConfettiPiece piece = new ConfettiPiece();
        float baseWidth = 1.2F + this.random.nextFloat() * 2.2F;
        float baseHeight = 2.0F + this.random.nextFloat() * 3.0F;
        if (this.random.nextFloat() < 0.25F) {
            float swap = baseWidth;
            baseWidth = baseHeight;
            baseHeight = swap;
        }
        piece.width = Math.max(1, Mth.ceil(baseWidth * this.scale));
        piece.height = Math.max(1, Mth.ceil(baseHeight * this.scale));
        piece.x = originX - piece.width * 0.5F;
        piece.y = originY - piece.height * 0.5F;
        piece.vx = vx;
        piece.vy = vy;
        piece.color = resolvePieceColor();
        return piece;
    }

    private float findLandingSurface(ConfettiPiece piece, float previousY, float newX, float newY, int width, int height) {
        float previousBottom = previousY + piece.height;
        float newBottom = newY + piece.height;
        float left = newX;
        float right = newX + piece.width;
        float bestSurface = Float.POSITIVE_INFINITY;

        float bottomSurface = this.bottomArea != null ? this.bottomArea.y : height;
        if (previousBottom <= bottomSurface && newBottom >= bottomSurface) {
            bestSurface = bottomSurface;
        }

        for (LandingArea area : this.collisionAreas) {
            float surfaceY = area.y;
            if (surfaceY < previousBottom || surfaceY > newBottom) {
                continue;
            }
            if (!overlapsX(left, right, area.x, area.x + area.width)) {
                continue;
            }
            if (surfaceY < bestSurface) {
                bestSurface = surfaceY;
            }
        }

        for (ConfettiPiece landed : this.settledPieces) {
            float surfaceY = landed.y;
            if (surfaceY < previousBottom || surfaceY > newBottom) {
                continue;
            }
            if (!overlapsX(left, right, landed.x, landed.x + landed.width)) {
                continue;
            }
            if (surfaceY < bestSurface) {
                bestSurface = surfaceY;
            }
        }

        return bestSurface == Float.POSITIVE_INFINITY ? Float.NaN : bestSurface;
    }

    private boolean overlapsX(float leftA, float rightA, float leftB, float rightB) {
        return rightA > leftB && leftA < rightB;
    }

    private int resolvePieceColor() {
        if (this.colorMixEnabled) {
            int rgb = COLOR_PALETTE[this.random.nextInt(COLOR_PALETTE.length)];
            int alpha = nextInt(MIN_ALPHA, MAX_ALPHA);
            return (alpha << 24) | rgb;
        }
        int rgb = this.baseColor & 0x00FFFFFF;
        int baseAlpha = FastColor.ARGB32.alpha(this.baseColor);
        if (baseAlpha <= 0) {
            baseAlpha = 255;
        }
        int alpha = Mth.clamp(Math.round(baseAlpha * (0.75F + 0.25F * this.random.nextFloat())), 0, 255);
        return (alpha << 24) | rgb;
    }

    private void applyBaseColor() {
        applyBaseColor(this.baseAlphaScale);
    }

    private void applyBaseColor(float previousAlphaScale) {
        int rgb = this.baseColor & 0x00FFFFFF;
        for (ConfettiPiece piece : this.activePieces) {
            piece.color = updatePieceColor(piece.color, rgb, previousAlphaScale, this.baseAlphaScale);
        }
        for (ConfettiPiece piece : this.settledPieces) {
            piece.color = updatePieceColor(piece.color, rgb, previousAlphaScale, this.baseAlphaScale);
        }
    }

    private int updatePieceColor(int existingColor, int rgb, float previousAlphaScale, float newAlphaScale) {
        int alpha = FastColor.ARGB32.alpha(existingColor);
        int unscaledAlpha = previousAlphaScale > 0.0F ? Mth.clamp(Math.round(alpha / previousAlphaScale), 0, 255) : alpha;
        int scaledAlpha = Mth.clamp(Math.round(unscaledAlpha * newAlphaScale), 0, 255);
        return (scaledAlpha << 24) | rgb;
    }

    private float resolveAlphaScale(int color) {
        int alpha = FastColor.ARGB32.alpha(color);
        if (alpha <= 0) {
            alpha = 255;
        }
        return Mth.clamp(alpha / 255.0F, 0.0F, 1.0F);
    }

    private void addSettledPiece(ConfettiPiece piece, int maxSettled) {
        if (piece.width <= 0 || piece.height <= 0) {
            return;
        }
        this.settledPieces.add(piece);
        trimSettledPieces(maxSettled);
    }

    private void trimSettledPieces(int maxSettled) {
        while (this.settledPieces.size() > maxSettled && !this.settledPieces.isEmpty()) {
            int index = 0;
            float highest = this.settledPieces.get(0).y;
            for (int i = 1; i < this.settledPieces.size(); i++) {
                float y = this.settledPieces.get(i).y;
                if (y < highest) {
                    highest = y;
                    index = i;
                }
            }
            this.settledPieces.remove(index);
        }
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

    private enum ConfettiSide {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM;

        private static ConfettiSide fromIndex(int index) {
            return switch (index) {
                case 0 -> LEFT;
                case 1 -> RIGHT;
                case 2 -> TOP;
                default -> BOTTOM;
            };
        }
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
    }

    private static final class ConfettiPiece {
        private float x;
        private float y;
        private float vx;
        private float vy;
        private int width;
        private int height;
        private int color;
    }

}
