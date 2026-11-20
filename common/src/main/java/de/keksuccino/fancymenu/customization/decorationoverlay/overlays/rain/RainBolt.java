package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

/**
 * A lightning bolt of the Rain overlay: a jagged main channel from the top of the screen with two side branches.<br>
 * A strike runs through five stages: the bright strike with a soft glow beside the channel, a short dark gap, a restrike,
 * then a dithered afterimage that fades in two steps. The bolt gets drawn into a canvas once per stage, so it costs one quad while it shows.
 * Its points are art pixels, which get drawn as {@code scale} by {@code scale} GUI pixels.
 */
final class RainBolt {

    static final int STAGE_NONE = -1;
    static final int STAGE_STRIKE = 0;
    static final int STAGE_GAP = 1;
    static final int STAGE_RESTRIKE = 2;
    static final int STAGE_AFTERGLOW = 3;
    static final int STAGE_AFTERGLOW_FAINT = 4;
    private static final float STRIKE_END = 0.07F;
    private static final float GAP_END = 0.12F;
    private static final float RESTRIKE_END = 0.2F;
    private static final float AFTERGLOW_END = 0.32F;
    private static final float BOLT_END = 0.46F;
    /** Room for the bolt of a 270 pixel high screen. The arrays grow for taller screens. */
    private static final int START_POINTS = 512;
    private static final int START_NODES = 32;
    /** A segment of the channel or a branch has at most this many pixels: it is never more than 16 pixels long on either axis */
    private static final int MAX_SEGMENT_POINTS = 32;
    private static final int GLOW = 0xBFD8FF;
    private static final int CORE = 0xFFFFFF;
    private static final int BRANCH = 0xDCE9FF;
    private static final int RESTRIKE_CORE = 0xE6F0FF;
    private static final int RESTRIKE_BRANCH = 0xA9C2EC;
    private static final int AFTERGLOW = 0x8FA8D8;

    /** The pixels of the main channel, from the top down */
    int[] mainX = new int[START_POINTS];
    int[] mainY = new int[START_POINTS];
    int mainCount = 0;
    private int[] branchX = new int[START_POINTS];
    private int[] branchY = new int[START_POINTS];
    private int branchCount = 0;
    private float[] nodeX = new float[START_NODES];
    private float[] nodeY = new float[START_NODES];
    private int nodeCount = 0;
    private float age = -1.0F;
    private int drawnStage = STAGE_NONE;
    private int scale = 1;
    /** Area the bolt covers in the canvas, in GUI pixels, to clear only that */
    private int minX;
    private int minY;
    private int maxX;
    private int maxY;

    boolean isActive() {
        return this.age >= 0.0F;
    }

    int stage() {
        if (this.age < 0.0F) return STAGE_NONE;
        if (this.age < STRIKE_END) return STAGE_STRIKE;
        if (this.age < GAP_END) return STAGE_GAP;
        if (this.age < RESTRIKE_END) return STAGE_RESTRIKE;
        if (this.age < AFTERGLOW_END) return STAGE_AFTERGLOW;
        return STAGE_AFTERGLOW_FAINT;
    }

    void advance(float deltaSeconds) {
        if (this.age < 0.0F) return;
        this.age += deltaSeconds;
        if (this.age > BOLT_END) this.age = -1.0F;
    }

    /**
     * Ends the bolt. The next {@link #drawInto} removes it from the canvas.
     */
    void stop() {
        this.age = -1.0F;
    }

    /**
     * Ends the bolt after its canvas got cleared some other way (a new screen size), so nothing is left to remove.
     */
    void reset() {
        this.age = -1.0F;
        this.drawnStage = STAGE_NONE;
    }

    /**
     * Builds a new bolt on a screen of {@code artWidth} by {@code artHeight} art pixels and starts its strike.
     */
    void strike(@NotNull PixelRandom random, int artWidth, int artHeight, int scale) {
        this.scale = Math.max(1, scale);
        this.mainCount = 0;
        this.branchCount = 0;
        this.nodeCount = 0;
        // The mockup stage keeps the bolt 70 of 480 pixels away from the sides and ends it between rows 120 and 172 of 270
        float margin = artWidth * (70.0F / 480.0F);
        float x = random.range(margin, Math.max(margin, artWidth - margin));
        float y = 0.0F;
        float endY = artHeight * random.range(120.0F / 270.0F, 172.0F / 270.0F);
        this.addNode(x, y);
        while (y < endY) {
            float nextY = y + random.range(7.0F, 15.0F);
            float nextX = x + random.range(-11.0F, 11.0F);
            this.mainX = grow(this.mainX, this.mainCount + MAX_SEGMENT_POINTS);
            this.mainY = grow(this.mainY, this.mainCount + MAX_SEGMENT_POINTS);
            this.mainCount = segment(x, y, nextX, nextY, this.mainX, this.mainY, this.mainCount);
            x = nextX;
            y = nextY;
            this.addNode(x, y);
        }
        for (int k = 0; k < 2; k++) {
            int node = random.rangeInt(1, Math.max(1, this.nodeCount - 3));
            node = Math.min(node, this.nodeCount - 1);
            float bx = this.nodeX[node];
            float by = this.nodeY[node];
            int direction = random.sign();
            for (int j = random.rangeInt(2, 4); j > 0; j--) {
                float nx = bx + direction * random.range(5.0F, 12.0F);
                float ny = by + random.range(5.0F, 11.0F);
                this.branchX = grow(this.branchX, this.branchCount + MAX_SEGMENT_POINTS);
                this.branchY = grow(this.branchY, this.branchCount + MAX_SEGMENT_POINTS);
                this.branchCount = segment(bx, by, nx, ny, this.branchX, this.branchY, this.branchCount);
                bx = nx;
                by = ny;
            }
        }
        this.updateBounds();
        this.age = 0.0F;
        this.drawnStage = STAGE_NONE;
    }

    private void addNode(float x, float y) {
        if (this.nodeCount >= this.nodeX.length) {
            this.nodeX = Arrays.copyOf(this.nodeX, this.nodeX.length * 2);
            this.nodeY = Arrays.copyOf(this.nodeY, this.nodeY.length * 2);
        }
        this.nodeX[this.nodeCount] = x;
        this.nodeY[this.nodeCount] = y;
        this.nodeCount++;
    }

    @NotNull
    private static int[] grow(@NotNull int[] array, int needed) {
        return (needed <= array.length) ? array : Arrays.copyOf(array, Math.max(needed, array.length * 2));
    }

    /**
     * Adds the pixels of a line between two rounded points, both ends included, like the mockup's {@code seg}.
     * The end of one segment and the start of the next are the same pixel, which the glow of the strike makes a bit brighter.
     */
    private static int segment(float x0f, float y0f, float x1f, float y1f, @NotNull int[] xs, @NotNull int[] ys, int count) {
        int x0 = Math.round(x0f);
        int y0 = Math.round(y0f);
        int x1 = Math.round(x1f);
        int y1 = Math.round(y1f);
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;
        int error = dx + dy;
        for (int n = 0; (n < 200) && (count < xs.length); n++) {
            xs[count] = x0;
            ys[count] = y0;
            count++;
            if ((x0 == x1) && (y0 == y1)) break;
            int doubled = 2 * error;
            if (doubled >= dy) {
                error += dy;
                x0 += sx;
            }
            if (doubled <= dx) {
                error += dx;
                y0 += sy;
            }
        }
        return count;
    }

    private void updateBounds() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int i = 0; i < this.mainCount; i++) {
            minX = Math.min(minX, this.mainX[i] - 1);
            maxX = Math.max(maxX, this.mainX[i] + 1);
            minY = Math.min(minY, this.mainY[i]);
            maxY = Math.max(maxY, this.mainY[i]);
        }
        for (int i = 0; i < this.branchCount; i++) {
            minX = Math.min(minX, this.branchX[i]);
            maxX = Math.max(maxX, this.branchX[i]);
            minY = Math.min(minY, this.branchY[i]);
            maxY = Math.max(maxY, this.branchY[i]);
        }
        this.minX = minX * this.scale;
        this.minY = minY * this.scale;
        this.maxX = (maxX + 1) * this.scale;
        this.maxY = (maxY + 1) * this.scale;
    }

    /**
     * Draws the current stage into the canvas if it changed since the last call. Returns true if the canvas shows something to draw.
     */
    boolean drawInto(@NotNull PixelCanvas canvas) {
        int stage = this.stage();
        if (stage != this.drawnStage) {
            if (this.drawnStage != STAGE_NONE) canvas.fillRect(this.minX, this.minY, this.maxX - this.minX, this.maxY - this.minY, 0);
            this.drawnStage = stage;
            this.drawStage(canvas, stage);
        }
        return (stage != STAGE_NONE) && (stage != STAGE_GAP);
    }

    private void drawStage(@NotNull PixelCanvas canvas, int stage) {
        int n = this.scale;
        switch (stage) {
            case STAGE_STRIKE -> {
                int glow = PixelMath.withAlpha(GLOW, 190);
                for (int i = 0; i < this.mainCount; i++) {
                    canvas.blendRect((this.mainX[i] - 1) * n, this.mainY[i] * n, n, n, glow);
                    canvas.blendRect((this.mainX[i] + 1) * n, this.mainY[i] * n, n, n, glow);
                }
                int core = PixelMath.opaque(CORE);
                for (int i = 0; i < this.mainCount; i++) canvas.fillRect(this.mainX[i] * n, this.mainY[i] * n, n, n, core);
                int branch = PixelMath.opaque(BRANCH);
                for (int i = 0; i < this.branchCount; i++) canvas.fillRect(this.branchX[i] * n, this.branchY[i] * n, n, n, branch);
            }
            case STAGE_RESTRIKE -> {
                int core = PixelMath.opaque(RESTRIKE_CORE);
                for (int i = 0; i < this.mainCount; i++) canvas.fillRect(this.mainX[i] * n, this.mainY[i] * n, n, n, core);
                int branch = PixelMath.withAlpha(RESTRIKE_BRANCH, 200);
                for (int i = 0; i < this.branchCount; i++) {
                    if (((this.branchX[i] + this.branchY[i]) & 1) != 0) canvas.blendRect(this.branchX[i] * n, this.branchY[i] * n, n, n, branch);
                }
            }
            case STAGE_AFTERGLOW, STAGE_AFTERGLOW_FAINT -> {
                int afterglow = PixelMath.withAlpha(AFTERGLOW, (stage == STAGE_AFTERGLOW) ? 130 : 70);
                for (int i = 0; i < this.mainCount; i++) {
                    if (((this.mainX[i] + this.mainY[i]) & 1) != 0) canvas.blendRect(this.mainX[i] * n, this.mainY[i] * n, n, n, afterglow);
                }
            }
            default -> {
            }
        }
    }

}
