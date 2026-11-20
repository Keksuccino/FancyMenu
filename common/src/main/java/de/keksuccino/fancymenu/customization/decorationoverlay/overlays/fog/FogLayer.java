package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fog;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * One layer of mist: two bands of long wisps across the middle of the screen, or the ground fog that thickens toward the bottom edge.<br><br>
 *
 * Mist is read from two pre-built noise textures that tile horizontally. The layer slides them past each other at different speeds,
 * so its banks drift and slowly change shape with two lookups per pixel and no noise math per frame. The textures only depend on the
 * layer, so all overlays share them, and they only get built again when a taller screen needs more rows. Building them takes a few
 * milliseconds, so it runs off the render thread and the layer shows once they are ready.<br>
 * Everything else here runs on the render thread.
 */
final class FogLayer {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Width of the noise textures, they repeat after it. Has to be a power of two. */
    static final int TEXTURE_WIDTH = 512;
    private static final int TEXTURE_MASK = TEXTURE_WIDTH - 1;
    /** How far the second texture is shifted against the first one. */
    private static final int SECOND_TEXTURE_SHIFT = 171;
    /** Radius around the pointer that the fog clears in, and how much it clears at the center. */
    static final float CLEAR_RADIUS = 50.0F;
    private static final float CLEAR_RADIUS_SQUARED = CLEAR_RADIUS * CLEAR_RADIUS;
    private static final float CLEAR_STRENGTH = 0.32F;
    /** Opacities of the thin haze under the ground fog's banks, which thickens in three steps toward the bottom edge. */
    private static final int[] HAZE_ALPHAS = {0, 30, 52, 74};
    /** Level states: clear air, then every density level with and without clear air above it. */
    private static final int LEVEL_STATES = 9;
    /** Where the noise textures get built. Tests replace it with one that builds right away. */
    static Executor noiseBuilder = ForkJoinPool.commonPool();

    static final FogLayer[] LAYERS = {
            new FogLayer(92.0F / 270.0F, 198.0F / 270.0F, 3.5F, 13.0F, 0.5F, new int[]{0x93A6B2, 0xA9BAC4, 0xBFCDD5}, new int[]{44, 72, 98}, false, 1),
            new FogLayer(128.0F / 270.0F, 248.0F / 270.0F, 7.0F, 15.0F, 0.48F, new int[]{0xA7B8C2, 0xBECCD4, 0xD3DEE4}, new int[]{48, 80, 110}, false, 8),
            new FogLayer(170.0F / 270.0F, 1.0F, 12.0F, 17.0F, 0.5F, new int[]{0xBCCAD2, 0xD2DDE3, 0xE6EDF0}, new int[]{54, 90, 124, 158}, true, 15)
    };

    /** Top and bottom edge as parts of the screen height */
    private final float topFraction;
    private final float bottomFraction;
    /** Pixels per second the first texture slides, the second one slides at 45% of it */
    final float speed;
    /** Height of the noise cells, the size of the banks */
    private final float cellHeight;
    private final float threshold;
    /** Ground fog thickens toward the bottom edge and has a fourth, densest level. Bands thin out toward both of their edges. */
    final boolean ground;
    private final int seed;
    /** ARGB of a pixel with haze state {@code h} and level state {@code l} at {@code h * LEVEL_STATES + l}, the level painted over the haze. */
    private final int[] composite = new int[HAZE_ALPHAS.length * LEVEL_STATES];
    /** The finished noise textures, NULL until the first ones are built. Gets replaced from the builder thread. */
    @Nullable
    private volatile Noise noise = null;
    /** Rows of the biggest textures that got requested so far. Only touched on the render thread. */
    private int requestedRows = 0;

    private FogLayer(float topFraction, float bottomFraction, float speed, float cellHeight, float threshold, @NotNull int[] colors, @NotNull int[] alphas, boolean ground, int seed) {
        this.topFraction = topFraction;
        this.bottomFraction = bottomFraction;
        this.speed = speed;
        this.cellHeight = cellHeight;
        this.threshold = threshold;
        this.ground = ground;
        this.seed = seed;
        for (int haze = 0; haze < HAZE_ALPHAS.length; haze++) {
            int hazeColor = (haze == 0) ? 0 : PixelMath.withAlpha(colors[0], HAZE_ALPHAS[haze]);
            for (int state = 0; state < LEVEL_STATES; state++) {
                int levelColor = 0;
                int level = (state + 1) / 2;
                // Bands have no densest level, so their table keeps it empty
                if ((state > 0) && (level <= alphas.length)) {
                    boolean clearAbove = (state & 1) == 0;
                    // The top edge of a bank catches the light: one tone brighter where the pixel above is clear air
                    levelColor = PixelMath.withAlpha(colors[Math.min(2, level - (clearAbove ? 0 : 1))], alphas[level - 1]);
                }
                this.composite[haze * LEVEL_STATES + state] = PixelMath.blendOver(hazeColor, levelColor);
            }
        }
    }

    int top(int screenHeight) {
        return Math.round(this.topFraction * screenHeight);
    }

    int bottom(int screenHeight) {
        return Math.min(screenHeight, Math.round(this.bottomFraction * screenHeight));
    }

    /**
     * Starts building noise textures with at least {@code rows} rows if there are none yet, and tells whether they are ready.
     * Rows don't depend on how many there are, so taller textures serve shorter layers too.
     */
    boolean prepareNoise(int rows) {
        Noise ready = this.noise;
        if ((ready != null) && (ready.rows >= rows)) return true;
        if (rows > this.requestedRows) {
            this.requestedRows = rows;
            noiseBuilder.execute(() -> {
                try {
                    this.publish(new Noise(noiseTexture(this.seed, this.cellHeight, rows), noiseTexture(this.seed + 3, this.cellHeight * 0.8F, rows), rows));
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to build the noise of a fog layer!", ex);
                }
            });
        }
        ready = this.noise;
        return (ready != null) && (ready.rows >= rows);
    }

    /**
     * Keeps the tallest textures when builds of different sizes finish out of order.
     */
    private synchronized void publish(@NotNull Noise built) {
        Noise current = this.noise;
        if ((current == null) || (built.rows > current.rows)) this.noise = built;
    }

    /**
     * Offset of the first texture at a time, it changes {@link #speed} times per second.
     */
    int offsetA(float time) {
        return PixelMath.floor(time * this.speed);
    }

    /**
     * Offset of the second texture at a time.
     */
    int offsetB(float time) {
        return PixelMath.floor(time * this.speed * 0.45F);
    }

    /**
     * A slow wave of the threshold, so the banks breathe a little.
     */
    float thresholdWave(float time) {
        return 0.015F * (float)Math.sin(time * 0.25F + this.speed);
    }

    /**
     * Paints the rows {@code r0} to {@code r1} (exclusive, row 0 is the top edge of the layer) and the columns {@code x0} to {@code x1}
     * (exclusive) of the layer into its canvas pixels, and stores the density level of every painted pixel.
     * The pixels above the area need valid levels, because the top edges of banks look at them. Needs ready noise textures, see {@link #prepareNoise}.
     *
     * @param pixels the layer's ARGB canvas pixels, {@code width} by {@code rows}
     * @param levels the density levels of the layer's pixels, the same size
     * @param top the screen row of the layer's top edge
     * @param clearX the pointer position the fog clears around, NaN for none
     */
    void paint(@NotNull int[] pixels, @NotNull byte[] levels, int width, int rows, int r0, int r1, int x0, int x1, int offsetA, int offsetB, float thresholdWave, int top, float clearX, float clearY) {
        Noise ready = this.noise;
        if ((ready == null) || (ready.rows < rows)) return;
        float[] a = ready.first;
        float[] b = ready.second;
        int[] composite = this.composite;
        boolean clearing = !Float.isNaN(clearX);
        for (int r = r0; r < r1; r++) {
            float v = r / (float)rows;
            float envelope = this.ground ? (0.55F + 0.75F * v) : (float)Math.pow(Math.sin(Math.PI * v), 0.7);
            float thr = this.threshold + (1.0F - envelope) * 0.42F + thresholdWave;
            float thr2 = thr + 0.03F;
            float thr3 = thr + 0.075F;
            float thr4 = thr + 0.15F;
            int textureRow = r * TEXTURE_WIDTH;
            int row = r * width;
            int rowAbove = row - width;
            // The columns near enough to the pointer to clear
            int clearFrom = x1;
            int clearTo = x1;
            float dySquared = 0.0F;
            if (clearing) {
                float dy = (top + r) - clearY;
                dySquared = dy * dy;
                if (dySquared < CLEAR_RADIUS_SQUARED) {
                    float half = (float)Math.sqrt(CLEAR_RADIUS_SQUARED - dySquared);
                    clearFrom = Math.max(x0, PixelMath.floor(clearX - half));
                    clearTo = Math.min(x1, PixelMath.floor(clearX + half) + 2);
                }
            }
            for (int x = x0; x < x1; x++) {
                float cut = 0.0F;
                if ((x >= clearFrom) && (x < clearTo)) {
                    float dx = x - clearX;
                    float q = dx * dx + dySquared;
                    if (q < CLEAR_RADIUS_SQUARED) cut = (1.0F - q / CLEAR_RADIUS_SQUARED) * CLEAR_STRENGTH;
                }
                float first = a[textureRow + ((x + offsetA) & TEXTURE_MASK)];
                float density = 0.55F * first + 0.45F * b[textureRow + ((x + SECOND_TEXTURE_SHIFT + offsetB) & TEXTURE_MASK)] - cut;
                int level;
                if (this.ground && (density > thr4)) level = 4;
                else if (density > thr3) level = 3;
                else if (density > thr2) level = 2;
                else if (density > thr) level = 1;
                else level = 0;
                int haze = 0;
                if (this.ground) {
                    float hazeValue = v + (first - 0.5F) * 0.7F - cut * 1.6F;
                    haze = (hazeValue > 0.72F) ? 3 : ((hazeValue > 0.46F) ? 2 : ((hazeValue > 0.2F) ? 1 : 0));
                }
                int state = 0;
                if (level > 0) {
                    boolean clearAbove = (r == 0) || (levels[rowAbove + x] == 0);
                    state = level * 2 - (clearAbove ? 0 : 1);
                }
                pixels[row + x] = composite[haze * LEVEL_STATES + state];
                levels[row + x] = (byte)level;
            }
        }
    }

    /**
     * Builds one noise texture, {@link #TEXTURE_WIDTH} wide and {@code rows} high, with values from 0 to 1.
     * A slow sideways warp bends the banks into long curling wisps.
     */
    @NotNull
    static float[] noiseTexture(int seed, float cellHeight, int rows) {
        float[] texture = new float[TEXTURE_WIDTH * rows];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < TEXTURE_WIDTH; x++) {
                float warpX = x + 34.0F * (tiledNoise(x, y, 128, cellHeight * 1.6F, seed + 500) - 0.5F);
                float warpY = y + cellHeight * 0.5F * (tiledNoise(x, y, 64, cellHeight * 2.0F, seed + 700) - 0.5F);
                texture[y * TEXTURE_WIDTH + x] = 0.56F * tiledNoise(warpX, warpY, 128, cellHeight, seed)
                        + 0.29F * tiledNoise(warpX, warpY, 64, cellHeight * 0.55F, seed + 1000)
                        + 0.15F * tiledNoise(warpX, warpY, 32, cellHeight * 0.3F, seed + 2000);
            }
        }
        return texture;
    }

    /**
     * Value noise with a quintic fade that repeats every {@link #TEXTURE_WIDTH} pixels horizontally.
     *
     * @param cellWidth has to divide {@link #TEXTURE_WIDTH}
     */
    static float tiledNoise(float x, float y, int cellWidth, float cellHeight, int seed) {
        float gx = x / cellWidth;
        float gy = y / cellHeight;
        int xi = PixelMath.floor(gx);
        int yi = PixelMath.floor(gy);
        float fx = gx - xi;
        float fy = gy - yi;
        float u = fx * fx * fx * (fx * (fx * 6.0F - 15.0F) + 10.0F);
        float v = fy * fy * fy * (fy * (fy * 6.0F - 15.0F) + 10.0F);
        int period = TEXTURE_WIDTH / cellWidth;
        // floorMod keeps the lattice repeating for warped positions left of 0 too, so the texture has no seam where it wraps
        int left = Math.floorMod(xi, period) + seed;
        int right = Math.floorMod(xi + 1, period) + seed;
        float a = PixelMath.latticeHash(left, yi);
        float b = PixelMath.latticeHash(right, yi);
        float c = PixelMath.latticeHash(left, yi + 1);
        float d = PixelMath.latticeHash(right, yi + 1);
        return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v;
    }

    /**
     * The two noise textures of a layer, {@link #TEXTURE_WIDTH} wide and {@code rows} high.
     */
    private record Noise(@NotNull float[] first, @NotNull float[] second, int rows) {
    }

}
