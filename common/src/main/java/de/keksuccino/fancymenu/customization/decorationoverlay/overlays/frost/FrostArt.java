package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.frost;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.jetbrains.annotations.NotNull;

/**
 * Colors and the pixel-perfect drawing rules of the Frost overlay.<br>
 * Ice is two things: a translucent glaze in three opacity steps, and opaque crystals in four tones from lit edge to shaded edge.
 * Every crystal edge follows one of sixteen directions whose lines stay pixel-perfect: flat, 2:1, diagonal and 1:2 slopes.
 */
final class FrostArt {

    static final int GLAZE = 0xC4DCF6;
    /** Alpha of the three glaze steps, from thin to thick */
    static final int[] GLAZE_ALPHA = {52, 86, 124};
    /** Crystal tones from the lit edge to the shaded edge */
    static final int[] TONE = {0xFFFFFF, 0xE4F0FF, 0xBFD8F5, 0x9ABBE8};
    /** The shaded tone is a little translucent */
    static final int SHADED_TONE_ALPHA = 190;
    /** Crack lines on crystals */
    static final int CRACK_ON_CRYSTAL = 0x4A6CA8;
    static final int GLINT_ARM = 0xEAF4FF;

    /** Sixteen directions, index + 4 is a quarter turn clockwise. */
    static final int[][] DIRECTIONS = {{1, 0}, {2, 1}, {1, 1}, {1, 2}, {0, 1}, {-1, 2}, {-1, 1}, {-2, 1}, {-1, 0}, {-2, -1}, {-1, -1}, {-1, -2}, {0, -1}, {1, -2}, {1, -1}, {2, -1}};
    static final int RIGHT = 0;
    static final int DOWN = 4;
    static final int LEFT = 8;
    static final int UP = 12;

    private FrostArt() {
    }

    /**
     * Takes one pixel-perfect step along a direction. The walker keeps its position and how far it has slid sideways.
     */
    static void step(@NotNull Walker walker, int direction) {
        int[] d = DIRECTIONS[direction & 15];
        int a = d[0];
        int b = d[1];
        if (Math.abs(a) >= Math.abs(b)) {
            walker.x += Integer.signum(a);
            walker.slide += Math.abs((double)b / a);
            if (walker.slide >= 1.0) {
                walker.y += Integer.signum(b);
                walker.slide -= 1.0;
            }
        } else {
            walker.y += Integer.signum(b);
            walker.slide += Math.abs((double)a / b);
            if (walker.slide >= 1.0) {
                walker.x += Integer.signum(a);
                walker.slide -= 1.0;
            }
        }
    }

    /**
     * An ice shard: a crystal that tapers to a point. Its upper or left edge and facet are lit, the other facet and edge shaded.
     * Pointing down it is an icicle.
     *
     * @param half its half width at the root
     * @param birth when its root freezes, the rest follows along its length
     * @param taper how early it thins out
     */
    static void shard(@NotNull IceLayer layer, int x, int y, int direction, int length, int half, double birth, double taper) {
        Walker walker = new Walker(x, y);
        int[] d = DIRECTIONS[direction & 15];
        boolean vertical = Math.abs(d[1]) > Math.abs(d[0]);
        for (int i = 0; i < length; i++) {
            step(walker, direction);
            int width = (int)Math.round(half * Math.pow(1.0 - (double)i / length, taper));
            double time = birth + i * 0.09;
            for (int k = -width; k <= width; k++) {
                int tone;
                if (width == 0) {
                    tone = ((i >= length - 1) && (length > 3)) ? 1 : 0;
                } else if (k == -width) {
                    tone = 0;
                } else if (k == width) {
                    tone = 3;
                } else {
                    tone = (k <= 0) ? 1 : 2;
                }
                layer.crystal(vertical ? (walker.x + k) : walker.x, vertical ? walker.y : (walker.y + k), time, tone);
            }
        }
    }

    static void shard(@NotNull IceLayer layer, int x, int y, int direction, int length, int half, double birth) {
        shard(layer, x, y, direction, length, half, birth, 1.6);
    }

    /**
     * A jagged row of ice teeth: n depths, each tooth a small triangle between lo and hi pixels high.
     */
    @NotNull
    static int[] teeth(@NotNull PixelRandom random, int n, int lo, int hi) {
        int[] depths = new int[Math.max(0, n)];
        for (int i = 0; i < n; ) {
            int w = 3 + (int)Math.floor(random.next() * 5.0);
            double height = (random.next() < 0.12) ? hi : (lo + Math.floor(random.next() * (hi - lo) * 0.6));
            for (int j = 0; (j < w) && (i + j < n); j++) {
                depths[i + j] = Math.max(0, (int)Math.round(height * (1.0 - Math.abs(j - (w - 1) / 2.0) / ((w + 1) / 2.0))));
            }
            i += w;
        }
        return depths;
    }


    /**
     * A position walking along a direction, see {@link #step}.
     */
    static final class Walker {

        int x;
        int y;
        double slide = 0.0;

        Walker(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void reset(int x, int y) {
            this.x = x;
            this.y = y;
            this.slide = 0.0;
        }

    }

}
