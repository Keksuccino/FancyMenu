package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.cobwebs;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

/**
 * The web in the top left corner of the screen, as a de-duplicated pixel list and as one sprite: radial threads from the corner and
 * sagging threads between them. Both kinds are fixed to the top and the side edge of the screen. The web of the right corner is
 * its mirror image.<br>
 * Open, the radial threads run on past the last row and end on the screen. Closed, a rim joins the thread ends, so the front of the
 * web carries itself and nothing sticks out.
 */
final class CobwebShape {

    static final int THREAD_COLOR = 0xD8DCE8;
    private static final int RADIAL_ALPHA = 185;
    private static final int RIM_ALPHA = 160;
    private static final int RING_ALPHA = 125;
    /** Angles of the radial threads, from along the top edge to down the side edge */
    private static final double[] ANGLES = {0.0, 0.27, 0.56, 0.87, 1.19, 1.5708};
    private static final int[] OPEN_LENGTHS = {96, 108, 116, 104, 88, 72};
    private static final int[] CLOSED_LENGTHS = {94, 102, 108, 102, 90, 78};
    private static final int[] RING_RADII = {12, 24, 38, 54, 72, 90};
    private static final PixelPalette PALETTE = PixelPalette.builder()
            .rgb('a', THREAD_COLOR, RADIAL_ALPHA)
            .rgb('b', THREAD_COLOR, RIM_ALPHA)
            .rgb('c', THREAD_COLOR, RING_ALPHA)
            .build();

    static final CobwebShape OPEN = new CobwebShape(false);
    static final CobwebShape CLOSED = new CobwebShape(true);

    /** Pixels in the order they got added, x and y of the top left web */
    final int[] xs;
    final int[] ys;
    final PixelSprite sprite;

    private CobwebShape(boolean closed) {
        Builder builder = new Builder();
        int[] lengths = closed ? CLOSED_LENGTHS : OPEN_LENGTHS;
        for (int i = 0; i < ANGLES.length; i++) builder.line(0.0, 0.0, Math.cos(ANGLES[i]) * lengths[i], Math.sin(ANGLES[i]) * lengths[i], 'a');
        if (closed) {
            for (int i = 0; i < ANGLES.length - 1; i++) builder.arc(i, lengths[i], lengths[i + 1], 'b', 0.87);
        }
        for (int radius : RING_RADII) {
            for (int i = 0; i < ANGLES.length - 1; i++) {
                if (radius <= Math.min(lengths[i], lengths[i + 1]) - (closed ? 9 : 0)) builder.arc(i, radius, radius, 'c', 0.8);
            }
        }
        this.xs = Arrays.copyOf(builder.xs, builder.count);
        this.ys = Arrays.copyOf(builder.ys, builder.count);
        int width = 0;
        int height = 0;
        for (int i = 0; i < builder.count; i++) {
            width = Math.max(width, this.xs[i] + 1);
            height = Math.max(height, this.ys[i] + 1);
        }
        char[] codes = new char[width * height];
        Arrays.fill(codes, '.');
        for (int i = 0; i < builder.count; i++) codes[this.ys[i] * width + this.xs[i]] = builder.codes[i];
        this.sprite = PixelSprite.fromCodes(width, height, codes);
    }

    @NotNull
    static PixelPalette palette() {
        return PALETTE;
    }

    int pixelCount() {
        return this.xs.length;
    }

    /**
     * Collects the web pixels. A pixel keeps the thread kind it got first.
     */
    private static final class Builder {

        private final IntOpenHashSet seen = new IntOpenHashSet();
        int[] xs = new int[1024];
        int[] ys = new int[1024];
        char[] codes = new char[1024];
        int count = 0;

        void add(double x, double y, char code) {
            int px = (int)Math.floor(x + 0.5);
            int py = (int)Math.floor(y + 0.5);
            if ((px < 0) || (py < 0)) return;
            if (!this.seen.add(py * 1000 + px)) return;
            if (this.count == this.xs.length) {
                this.xs = Arrays.copyOf(this.xs, this.count * 2);
                this.ys = Arrays.copyOf(this.ys, this.count * 2);
                this.codes = Arrays.copyOf(this.codes, this.count * 2);
            }
            this.xs[this.count] = px;
            this.ys[this.count] = py;
            this.codes[this.count] = code;
            this.count++;
        }

        void line(double x0, double y0, double x1, double y1, char code) {
            int n = (int)Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)));
            if (n == 0) {
                this.add(x0, y0, code);
                return;
            }
            for (int i = 0; i <= n; i++) this.add(x0 + (x1 - x0) * i / n, y0 + (y1 - y0) * i / n, code);
        }

        /**
         * A sagging thread between two neighboring radial threads, from radius r0 on the first to r1 on the second.
         */
        void arc(int thread, double r0, double r1, char code, double sag) {
            double a0 = ANGLES[thread];
            double a1 = ANGLES[thread + 1];
            double p0x = Math.cos(a0) * r0;
            double p0y = Math.sin(a0) * r0;
            double p1x = Math.cos(a1) * r1;
            double p1y = Math.sin(a1) * r1;
            double middleAngle = (a0 + a1) / 2.0;
            double middleRadius = (r0 + r1) / 2.0 * sag;
            double mx = Math.cos(middleAngle) * middleRadius;
            double my = Math.sin(middleAngle) * middleRadius;
            double lastX = p0x;
            double lastY = p0y;
            for (int k = 1; k <= 10; k++) {
                double t = k / 10.0;
                double x = bezier(p0x, mx, p1x, t);
                double y = bezier(p0y, my, p1y, t);
                this.line(lastX, lastY, x, y, code);
                lastX = x;
                lastY = y;
            }
        }

        private static double bezier(double a, double control, double b, double t) {
            double u = 1.0 - t;
            return u * u * a + 2.0 * u * t * control + t * t * b;
        }

    }

}
