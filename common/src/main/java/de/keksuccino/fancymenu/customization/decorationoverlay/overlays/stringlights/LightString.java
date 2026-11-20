package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * One string of the String Lights overlay while it runs: its curve, its one-pixel raster, its bulbs and baubles.<br>
 * The string is a quadratic curve from start to end whose control point hangs below the middle and sways in the wind.
 * Every frame the curve gets rastered into a pixel-perfect one-pixel line. Wire and garland only get painted again
 * when that raster changed, so a calm string costs no texture upload.
 */
final class LightString {

    /** Bulbs a string can carry at most */
    static final int MAX_BULBS = 100;
    /** GUI pixels between two bulbs at size 1, when the bulb count follows the string length */
    static final float BULB_SPACING = 24.0F;
    /** Space the lowest string keeps from the bottom of the screen, plus its bulbs */
    private static final float BOTTOM_MARGIN = 30.0F;

    @NotNull
    final StringLightsPosition position;
    /** Index of the string, so strings twinkle differently */
    final int index;

    float startX;
    float startY;
    float endX;
    float endY;
    float length;
    float sag;
    float windAmplitude;
    final float windSpeed;
    final float windPhase;
    float windTime;

    /** The one-pixel raster of the curve: x, y and the position t (0 to 1) along the string of every point */
    int[] rasterX = new int[0];
    int[] rasterY = new int[0];
    float[] rasterT = new float[0];
    int rasterCount = 0;
    /** The raster that is painted on the wire canvas right now */
    private int[] paintedX = new int[0];
    private int[] paintedY = new int[0];
    private int paintedCount = -1;
    private int paintedMinX;
    private int paintedMinY;
    private int paintedMaxX;
    private int paintedMaxY;
    private boolean hasPaintedArea = false;
    /** Needle tufts of the garland per raster index, see {@link #paintGarland} */
    private byte[] tuftLow = new byte[0];
    private byte[] tuftHigh = new byte[0];
    private boolean[] tuftLitAbove = new boolean[0];
    private boolean[] tuftDarkBelow = new boolean[0];

    int bulbCount = 0;
    float[] bulbT = new float[0];
    /** Sprite scale of every bulb, 0 for hidden bulbs */
    int[] bulbScale = new int[0];
    StringLightsArt.BulbRamp[] bulbRamp = new StringLightsArt.BulbRamp[0];
    /** Raster index every bulb hangs from, updated with the raster */
    int[] bulbPoint = new int[0];
    int baubleCount = 0;
    float[] baubleT = new float[0];
    PixelPalette[] baublePalette = new PixelPalette[0];
    int[] baublePoint = new int[0];

    LightString(@NotNull StringLightsPosition position, @NotNull PixelRandom random) {
        this.position = position;
        this.index = position.ordinal();
        this.windSpeed = random.range(0.35F, 0.9F);
        this.windPhase = random.range(0.0F, PixelMath.TAU);
        this.windTime = random.range(0.0F, PixelMath.TAU);
    }

    /**
     * Places the string on a screen of the given size.
     *
     * @param scale the String Light Scale option, which makes strings sag a bit more when it is bigger
     */
    void layout(int screenWidth, int screenHeight, float scale) {
        StringLightsPosition p = this.position;
        this.startX = p.startX * Math.max(1, screenWidth - 1) + p.startOffsetX;
        this.startY = p.startY * Math.max(1, screenHeight - 1) + p.startOffsetY;
        this.endX = p.endX * Math.max(1, screenWidth - 1) + p.endOffsetX;
        this.endY = p.endY * Math.max(1, screenHeight - 1) + p.endOffsetY;
        float dx = this.endX - this.startX;
        float dy = this.endY - this.startY;
        this.length = (float)Math.sqrt(dx * dx + dy * dy);
        this.sag = this.length * p.sagFactor * (0.75F + 0.25F * scale);
        this.windAmplitude = this.length * 0.015F * p.windMultiplier;
        int capacity = (int)Math.ceil(this.length * 2.0F) + 1;
        if (this.rasterX.length < capacity) {
            this.rasterX = new int[capacity];
            this.rasterY = new int[capacity];
            this.rasterT = new float[capacity];
            this.paintedX = new int[capacity];
            this.paintedY = new int[capacity];
            this.buildTufts(capacity);
        }
        this.paintedCount = -1;
    }

    private void buildTufts(int capacity) {
        this.tuftLow = new byte[capacity];
        this.tuftHigh = new byte[capacity];
        this.tuftLitAbove = new boolean[capacity];
        this.tuftDarkBelow = new boolean[capacity];
        for (int i = 0; i < capacity; i++) {
            // Keyed by the raster index, so the needles stay put while the wire sways
            float h = PixelMath.hash(i, 11);
            this.tuftLow[i] = (byte)((PixelMath.hash(i, 17) > 0.8F) ? -2 : -1);
            this.tuftHigh[i] = (byte)((PixelMath.hash(i, 19) < 0.15F) ? 3 : ((PixelMath.hash(i, 13) > 0.5F) ? 2 : 1));
            this.tuftLitAbove[i] = h > 0.5F;
            this.tuftDarkBelow[i] = h > 0.3F;
        }
    }

    /**
     * Rasters the curve at its current sway into a one-pixel line without corners, keeping the position along the string of
     * every point. Returns true if the raster differs from the painted one.
     *
     * @param windStrength the Wind Strength option
     * @param bulbScale size of the bulbs, so the lowest string keeps them on the screen
     */
    boolean raster(int screenHeight, float windStrength, int bulbScale) {
        float windScale = windStrength * this.position.windMultiplier;
        float midX = (this.startX + this.endX) * 0.5F;
        float midY = (this.startY + this.endY) * 0.5F;
        float controlX = midX + (float)Math.sin(this.windTime + this.windPhase) * this.windAmplitude * windScale;
        float controlY = midY + this.sag + (float)Math.cos(this.windTime * 0.7F + this.windPhase) * this.windAmplitude * 0.2F * windScale;
        if (this.position == StringLightsPosition.BOTTOM_LEFT_TO_BOTTOM_RIGHT) {
            float maxCurveY = screenHeight - BOTTOM_MARGIN - 9.0F * bulbScale;
            controlY = Math.min(controlY, maxCurveY * 2.0F - midY);
        }
        int samples = Math.max(1, (int)Math.ceil(this.length * 2.0F));
        int count = 0;
        int lastX = Integer.MIN_VALUE;
        int lastY = Integer.MIN_VALUE;
        for (int i = 0; i <= samples; i++) {
            float t = i / (float)samples;
            float inv = 1.0F - t;
            int x = Math.round(inv * inv * this.startX + 2.0F * inv * t * controlX + t * t * this.endX);
            int y = Math.round(inv * inv * this.startY + 2.0F * inv * t * controlY + t * t * this.endY);
            if ((x != lastX) || (y != lastY)) {
                if (count >= this.rasterX.length) break;
                this.rasterX[count] = x;
                this.rasterY[count] = y;
                this.rasterT[count] = t;
                count++;
                lastX = x;
                lastY = y;
            }
        }
        this.rasterCount = removeCorners(this.rasterX, this.rasterY, this.rasterT, count);
        return this.rasterDiffersFromPainted();
    }

    /**
     * Drops the middle point of every L-shaped corner (a point whose neighbors touch diagonally), so the line stays one pixel thin.
     * Works like removing the points one by one while walking forward, where the point that moves into the place of a
     * removed one is not checked again, in one pass and in place. Returns the new point count.
     */
    static int removeCorners(@NotNull int[] xs, @NotNull int[] ys, @NotNull float[] ts, int count) {
        if (count < 3) return count;
        int write = 1;
        int read = 1;
        while (read < count - 1) {
            int dx = Math.abs(xs[write - 1] - xs[read + 1]);
            int dy = Math.abs(ys[write - 1] - ys[read + 1]);
            if ((dx == 1) && (dy == 1)) {
                // The corner point goes, and its successor takes its place without getting checked itself
                read++;
            }
            xs[write] = xs[read];
            ys[write] = ys[read];
            ts[write] = ts[read];
            write++;
            read++;
        }
        while (read < count) {
            xs[write] = xs[read];
            ys[write] = ys[read];
            ts[write] = ts[read];
            write++;
            read++;
        }
        return write;
    }

    private boolean rasterDiffersFromPainted() {
        if (this.rasterCount != this.paintedCount) return true;
        for (int i = 0; i < this.rasterCount; i++) {
            if ((this.rasterX[i] != this.paintedX[i]) || (this.rasterY[i] != this.paintedY[i])) return true;
        }
        return false;
    }

    /**
     * Finds the raster point every bulb and bauble hangs from: the first point at or past its position along the string.
     */
    void anchorBulbs() {
        int last = this.rasterCount - 1;
        int k = 0;
        for (int i = 0; i < this.bulbCount; i++) {
            while ((k < last) && (this.rasterT[k] < this.bulbT[i])) k++;
            this.bulbPoint[i] = Math.max(0, k);
        }
        k = 0;
        for (int i = 0; i < this.baubleCount; i++) {
            while ((k < last) && (this.rasterT[k] < this.baubleT[i])) k++;
            this.baublePoint[i] = Math.max(0, k);
        }
    }

    /**
     * Clears what this string painted on the wire canvas the last time.
     */
    void clearPainted(@NotNull PixelCanvas canvas) {
        if (this.hasPaintedArea) {
            canvas.fillRect(this.paintedMinX, this.paintedMinY, this.paintedMaxX - this.paintedMinX + 1, this.paintedMaxY - this.paintedMinY + 1, 0);
        }
        this.hasPaintedArea = false;
        this.paintedCount = -1;
    }

    /**
     * Forgets what is painted, after the canvas got cleared as a whole (for example by a resize).
     */
    void forgetPainted() {
        this.hasPaintedArea = false;
        this.paintedCount = -1;
    }

    /**
     * Paints the wire, or the garland in its place, onto the canvas and remembers the painted raster.
     *
     * @param simple true for the plain wire of the simple look, false for the twisted wire of the detailed look
     * @param wireDark color of the plain wire, or of the darker strand of the twisted wire; fully transparent hides the wire
     * @param wireLight color of the lighter strand of the twisted wire
     */
    void paint(@NotNull PixelCanvas canvas, boolean simple, boolean garland, int wireDark, int wireLight) {
        int[] xs = this.rasterX;
        int[] ys = this.rasterY;
        int count = this.rasterCount;
        if (garland) {
            this.paintGarland(canvas);
        } else if ((wireDark >>> 24) != 0) {
            for (int i = 0; i < count; i++) {
                canvas.set(xs[i], ys[i], (simple || ((i / 3) & 1) == 0) ? wireDark : wireLight);
            }
        }
        System.arraycopy(xs, 0, this.paintedX, 0, count);
        System.arraycopy(ys, 0, this.paintedY, 0, count);
        this.paintedCount = count;
        if (count == 0) return;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int i = 0; i < count; i++) {
            minX = Math.min(minX, xs[i]);
            maxX = Math.max(maxX, xs[i]);
            minY = Math.min(minY, ys[i]);
            maxY = Math.max(maxY, ys[i]);
        }
        // Garland needles reach up to 3 pixels away from the wire, a bit more where it runs diagonally
        int reach = garland ? 5 : 0;
        this.paintedMinX = minX - reach;
        this.paintedMinY = minY - reach;
        this.paintedMaxX = maxX + reach;
        this.paintedMaxY = maxY + reach;
        this.hasPaintedArea = true;
    }

    /**
     * The pine garland: one tuft of needles per wire pixel in three greens, lit on the upper side and shaded below.
     * A tuft stands up and down where the wire runs flat, and sideways where it runs steep (towards the upper side then).
     * It is stretched by the slope of the wire, so the garland is equally thick at every angle.
     */
    private void paintGarland(@NotNull PixelCanvas canvas) {
        int[] xs = this.rasterX;
        int[] ys = this.rasterY;
        int last = this.rasterCount - 1;
        for (int i = 0; i <= last; i++) {
            int x = xs[i];
            int y = ys[i];
            int a = Math.max(0, i - 3);
            int c = Math.min(last, i + 3);
            int dx = xs[c] - xs[a];
            int dy = ys[c] - ys[a];
            int ax = Math.abs(dx);
            int ay = Math.abs(dy);
            int side = (ay > ax) ? (((dx * dy) < 0) ? -1 : 1) : 0;
            int longer = Math.max(ax, ay);
            float r = Math.min(ax, ay) / (float)((longer == 0) ? 1 : longer);
            float stretch = (float)Math.sqrt(1.0F + r * r);
            int low = this.tuftLow[i];
            int high = this.tuftHigh[i];
            int from = (int)Math.ceil((low - 0.5F) * stretch);
            int to = (int)Math.floor((high + 0.5F) * stretch);
            for (int e = from; e <= to; e++) {
                int d = PixelMath.clamp(Math.round(e / stretch), low, high);
                int color;
                if (d < -1) {
                    color = StringLightsArt.PINE_LIT;
                } else if (d < 0) {
                    color = this.tuftLitAbove[i] ? StringLightsArt.PINE_LIT : StringLightsArt.PINE_MID;
                } else if (d < 1) {
                    color = StringLightsArt.PINE_MID;
                } else if (d < 2) {
                    color = this.tuftDarkBelow[i] ? StringLightsArt.PINE_SHADE : StringLightsArt.PINE_MID;
                } else {
                    color = StringLightsArt.PINE_SHADE;
                }
                if (side != 0) {
                    canvas.set(x - e * side, y, color);
                } else {
                    canvas.set(x, y + e, color);
                }
            }
        }
    }

    /**
     * Lays out the bulbs and baubles of the string.
     *
     * @param configuredCount the Light Count option, 0 to follow the string length
     * @param spacing GUI pixels between two bulbs when the count follows the length
     * @param spriteScale the bulb size of the overlay
     * @param lightRamp the ramp of the string's light color
     * @param christmas true if the bulbs cycle through the Christmas colors
     * @param sizes the Individual Light Sizes, NULL entries keep the default size
     * @param customRamps the ramps of the Individual Light Colors, NULL entries keep the default color
     */
    void layoutBulbs(int configuredCount, float spacing, int spriteScale, @NotNull StringLightsArt.BulbRamp lightRamp, boolean christmas, @NotNull List<Float> sizes, @NotNull List<StringLightsArt.BulbRamp> customRamps) {
        int count = resolveBulbCount(configuredCount, this.length * this.position.densityMultiplier, spacing);
        if (this.bulbT.length < count) {
            this.bulbT = new float[count];
            this.bulbScale = new int[count];
            this.bulbRamp = new StringLightsArt.BulbRamp[count];
            this.bulbPoint = new int[count];
        }
        this.bulbCount = count;
        boolean customColors = false;
        for (int i = 0; i < count; i++) {
            this.bulbT[i] = (i + 1) / (float)(count + 1);
            Float size = resolvePatternEntry(sizes, i);
            this.bulbScale[i] = resolveBulbScale(size, spriteScale);
            StringLightsArt.BulbRamp custom = resolvePatternEntry(customRamps, i);
            if (custom != null) customColors = true;
            this.bulbRamp[i] = (custom != null) ? custom : (christmas ? StringLightsArt.CHRISTMAS[i % StringLightsArt.CHRISTMAS.length] : lightRamp);
        }
        // A bauble hangs in every second gap. Warm white strings get red and gold, strings with colored bulbs gold and silver,
        // so a bauble never passes for a bulb.
        boolean warm = !christmas && !customColors && (lightRamp.base == StringLightsArt.WARM_RGB);
        PixelPalette first = warm ? StringLightsArt.BAUBLE_RED : StringLightsArt.BAUBLE_GOLD;
        PixelPalette second = warm ? StringLightsArt.BAUBLE_GOLD : StringLightsArt.BAUBLE_SILVER;
        int baubles = count / 2;
        if (this.baubleT.length < baubles) {
            this.baubleT = new float[baubles];
            this.baublePalette = new PixelPalette[baubles];
            this.baublePoint = new int[baubles];
        }
        this.baubleCount = baubles;
        for (int j = 0; j < baubles; j++) {
            this.baubleT[j] = (2 * j + 1.5F) / (count + 1);
            this.baublePalette[j] = ((j & 1) == 0) ? first : second;
        }
    }

    /**
     * The number of bulbs on a string: the configured count, or one bulb per {@code spacing} pixels of length.
     */
    static int resolveBulbCount(int configuredCount, float length, float spacing) {
        if (configuredCount > 0) return Math.min(configuredCount, MAX_BULBS);
        if (length <= 0.1F) return 0;
        return PixelMath.clamp(Math.round(length / spacing), 1, MAX_BULBS);
    }

    /**
     * The sprite scale of a bulb with an Individual Light Size: NULL keeps the size of the overlay, 0 or less hides the bulb,
     * other sizes multiply it, rounded to whole pixels, from 1x to 4x.
     */
    static int resolveBulbScale(@Nullable Float size, int spriteScale) {
        if (size == null) return spriteScale;
        if (size <= 0.0F) return 0;
        return PixelMath.clamp(Math.round(size * spriteScale), 1, 4);
    }

    /**
     * Returns the pattern entry of the bulb at the given index, repeating the pattern when there are more bulbs than entries.
     * Returns NULL for empty patterns and for entries that keep the bulb's default.
     */
    @Nullable
    static <T> T resolvePatternEntry(@NotNull List<T> pattern, int bulbIndex) {
        if (pattern.isEmpty()) return null;
        return pattern.get(bulbIndex % pattern.size());
    }

}
