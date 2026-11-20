package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

/**
 * Math and color helpers for pixel-art decoration overlays.<br>
 * They work like the helpers of the overlay redesign mockup, so simulation code ports over line by line:
 * {@code clamp}, {@code lerp}, {@code mix}, {@code tri}, {@code hsh} ({@link #hash(float, float)}),
 * {@code vnH} ({@link #latticeHash(int, int)}) and {@code vnoise} ({@link #valueNoise(float, float)}).<br>
 * Colors are ARGB ints everywhere in the toolkit, so an opaque color needs its alpha byte ({@code 0xFF......}), see {@link #opaque(int)}.
 */
public final class PixelMath {

    public static final float TAU = (float)(Math.PI * 2.0);
    private static final int[] BAYER_4 = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    private PixelMath() {
    }

    public static float clamp(float value, float min, float max) {
        return (value < min) ? min : Math.min(value, max);
    }

    public static int clamp(int value, int min, int max) {
        return (value < min) ? min : Math.min(value, max);
    }

    public static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    /**
     * Eases a value between 0 and 1 in and out ({@code t * t * (3 - 2 * t)}). Values outside of 0 to 1 get clamped first.
     */
    public static float smoothstep(float t) {
        t = clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    /**
     * Rounds down to the next whole number, also for negative values (unlike a plain {@code (int)} cast).
     */
    public static int floor(float value) {
        int i = (int)value;
        return (value < i) ? (i - 1) : i;
    }

    /**
     * A triangle wave between 0 and 1 with a period of {@link #TAU}, like a sine wave with straight slopes.
     * It is 0 at 0 and 1 at half the period.
     */
    public static float tri(float u) {
        float v = u / TAU;
        return 2.0F * Math.abs(v - (float)Math.floor(v + 0.5F));
    }

    /**
     * A fast, repeatable hash of two numbers to a value from 0 (inclusive) to 1 (exclusive), the mockup's {@code hsh}.
     * Good for per-column or per-pixel variation that must not change between frames.
     */
    public static float hash(float i, float k) {
        double x = Math.sin(i * 127.1 + k * 311.7) * 43758.5453;
        float f = (float)(x - Math.floor(x));
        // Rounding to float can turn 0.99999999 into 1.0
        return (f >= 1.0F) ? 0.0F : f;
    }

    /**
     * Hash of a lattice point to a value from 0 (inclusive) to 1 (exclusive), the mockup's {@code vnH}.
     */
    public static float latticeHash(int i, int j) {
        int h = i * 374761393 + j * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h = h ^ (h >>> 16);
        // The top 24 bits fit a float exactly, so the result never rounds up to 1.0
        return (h >>> 8) / 16777216.0F;
    }

    /**
     * Smooth 2D value noise between 0 and 1 for soft, organic shapes, the mockup's {@code vnoise}.
     * Neighboring whole-number coordinates are independent, so scale the input to set the size of the features.
     */
    public static float valueNoise(float x, float y) {
        int xi = floor(x);
        int yi = floor(y);
        float fx = x - xi;
        float fy = y - yi;
        float u = fx * fx * (3.0F - 2.0F * fx);
        float v = fy * fy * (3.0F - 2.0F * fy);
        float a = latticeHash(xi, yi);
        float b = latticeHash(xi + 1, yi);
        float c = latticeHash(xi, yi + 1);
        float d = latticeHash(xi + 1, yi + 1);
        return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v;
    }

    /**
     * The 4x4 ordered dither threshold of a pixel, from 0 to 15. A pixel is "on" for a coverage of n / 16 when {@code bayer4(x, y) < n}.
     */
    public static int bayer4(int x, int y) {
        return BAYER_4[((y & 3) << 2) | (x & 3)];
    }

    /**
     * Blends two ARGB colors channel by channel, alpha included. {@code t = 0} gives the first color, {@code t = 1} the second.
     */
    public static int mix(int argbFrom, int argbTo, float t) {
        int a = Math.round(lerp(alpha(argbFrom), alpha(argbTo), t));
        int r = Math.round(lerp(red(argbFrom), red(argbTo), t));
        int g = Math.round(lerp(green(argbFrom), green(argbTo), t));
        int b = Math.round(lerp(blue(argbFrom), blue(argbTo), t));
        return argb(a, r, g, b);
    }

    /**
     * Returns the RGB color with the given alpha (0 to 255). The alpha byte of {@code rgb} is ignored.
     */
    public static int withAlpha(int rgb, int alpha) {
        return (clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    /**
     * Returns the RGB color fully opaque. The alpha byte of {@code rgb} is ignored.
     */
    public static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    /**
     * Multiplies the alpha of an ARGB color by a factor from 0 to 1.
     */
    public static int scaleAlpha(int argb, float factor) {
        return withAlpha(argb, Math.round(alpha(argb) * clamp(factor, 0.0F, 1.0F)));
    }

    public static int argb(int alpha, int red, int green, int blue) {
        return (clamp(alpha, 0, 255) << 24) | (clamp(red, 0, 255) << 16) | (clamp(green, 0, 255) << 8) | clamp(blue, 0, 255);
    }

    public static int alpha(int argb) {
        return argb >>> 24;
    }

    public static int red(int argb) {
        return (argb >> 16) & 0xFF;
    }

    public static int green(int argb) {
        return (argb >> 8) & 0xFF;
    }

    public static int blue(int argb) {
        return argb & 0xFF;
    }

    /**
     * Draws the ARGB color {@code src} over {@code dst} (straight, not premultiplied alpha), like painting a translucent color onto a pixel.
     */
    public static int blendOver(int dst, int src) {
        int sa = src >>> 24;
        if (sa == 255) return src;
        if (sa == 0) return dst;
        int da = dst >>> 24;
        if (da == 0) return src;
        // Everything is scaled by 255 * 255 to stay in integers
        int inv = 255 - sa;
        int outA255 = sa * 255 + da * inv;
        int r = ((src >> 16 & 0xFF) * sa * 255 + (dst >> 16 & 0xFF) * da * inv + (outA255 >> 1)) / outA255;
        int g = ((src >> 8 & 0xFF) * sa * 255 + (dst >> 8 & 0xFF) * da * inv + (outA255 >> 1)) / outA255;
        int b = ((src & 0xFF) * sa * 255 + (dst & 0xFF) * da * inv + (outA255 >> 1)) / outA255;
        int a = (outA255 + 127) / 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

}
