package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.frost;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.jetbrains.annotations.NotNull;

/**
 * Shapes the ice of the Frost overlay, once: the bands and icicles along the screen edges, the four frozen corners and the ice on
 * an element. The shapes are designed with fixed random seeds, so the same screen always freezes the same way.
 */
final class FrostShaper {

    /** Clusters of shards keep this far away from the screen corners */
    private static final int CLUSTER_MARGIN = 58;
    private static final int TOP = 0;
    private static final int BOTTOM = 1;
    private static final int LEFT = 2;
    private static final int RIGHT = 3;

    /**
     * No two screen corners freeze alike. Per corner (top left, top right, bottom right, bottom left):
     * size (overall scale of the frost), spreadX and spreadY (how far the ice spreads along the horizontal and the vertical edge),
     * cell (size of the ice plates: small cells give a fine crackle, large ones broad plates), dense (shifts the opacity steps toward
     * thicker or thinner ice), reachX and reachY (how long the crystals grow along the horizontal and the vertical edge),
     * crystals (number of crystals besides the five of the fan) and chunk (how broad the crystals are: 0 needles, 1 blades).
     */
    private static final double[][] CORNER_STYLES = {
            {1.2, 1.5, 0.75, 11, 0.4, 1.25, 0.6, 13, 1.0},
            {0.62, 0.9, 1.2, 6, -0.3, 0.55, 0.8, 6, 0.5},
            {1.05, 0.75, 1.6, 8, 0.1, 0.6, 1.3, 10, 0.5},
            {0.9, 1.7, 0.55, 15, 0.7, 0.45, 0.3, 5, 1.0}};
    private static final int[] FAN_LENGTHS = {30, 40, 54, 40, 30};

    private FrostShaper() {
    }

    /**
     * Freezes the four screen edges: a glazed band whose inner rim is a row of ice teeth, each edge with its own thickness,
     * and clusters of shards growing out of it. Along the top edge they hang as icicles.
     */
    static void edges(@NotNull IceLayer layer, @NotNull PixelRandom random, int width, int height) {
        band(layer, random, width, height, width, TOP, 0.9);
        band(layer, random, width, height, width, BOTTOM, 1.35);
        band(layer, random, width, height, height, LEFT, 1.15);
        band(layer, random, width, height, height, RIGHT, 0.7);
        clusters(layer, random, width, height, width, TOP, FrostArt.DOWN);
        clusters(layer, random, width, height, width, BOTTOM, FrostArt.UP);
        clusters(layer, random, width, height, height, LEFT, FrostArt.RIGHT);
        clusters(layer, random, width, height, height, RIGHT, FrostArt.LEFT);
    }

    private static int edgeX(int edge, int along, int depth, int width) {
        return switch (edge) {
            case LEFT -> depth;
            case RIGHT -> width - 1 - depth;
            default -> along;
        };
    }

    private static int edgeY(int edge, int along, int depth, int height) {
        return switch (edge) {
            case TOP -> depth;
            case BOTTOM -> height - 1 - depth;
            default -> along;
        };
    }

    private static void band(@NotNull IceLayer layer, @NotNull PixelRandom random, int width, int height, int n, int edge, double thickness) {
        int[] teeth = FrostArt.teeth(random, n, 1, 9);
        for (int t = 0; t < n; t++) {
            int depth = (int)Math.round((2.0 + 3.5 * PixelMath.valueNoise(t * 0.04F, edge * 9.7F + 3.0F)) * thickness) + teeth[t];
            for (int d = 0; d < depth; d++) {
                int x = edgeX(edge, t, d, width);
                int y = edgeY(edge, t, d, height);
                layer.glaze(x, y, d * 0.3 + PixelMath.hash(x, y) * 0.5, (d < depth * 0.34) ? 2 : ((d < depth * 0.67) ? 1 : 0));
            }
            layer.crystal(edgeX(edge, t, depth - 1, width), edgeY(edge, t, depth - 1, height), depth * 0.3 + 0.6, (teeth[t] >= 5) ? 1 : 2);
        }
    }

    /**
     * One main crystal with smaller ones fanned out beside it, every 9 to 25 pixels along the edge.
     */
    private static void clusters(@NotNull IceLayer layer, @NotNull PixelRandom random, int width, int height, int n, int edge, int direction) {
        for (int t = CLUSTER_MARGIN + (int)Math.floor(random.next() * 12.0); t < n - CLUSTER_MARGIN; t += 9 + (int)Math.floor(random.next() * 17.0)) {
            int count = 1 + (int)Math.floor(random.next() * 3.6);
            int big = 6 + (int)Math.floor(random.next() * random.next() * 15.0);
            double birth = 1.0 + random.next() * 2.5;
            for (int c = 0; c < count; c++) {
                int offset = (c == 0) ? 0 : (((c % 2 == 1) ? 1 : -1) * (int)Math.ceil(c / 2.0));
                int x = edgeX(edge, t + offset * 2, 0, width);
                int y = edgeY(edge, t + offset * 2, 0, height);
                int length = Math.max(3, (int)Math.round(big * ((c != 0) ? (0.4 + random.next() * 0.3) : 1.0)));
                int half = ((c == 0) && (big > 13)) ? 2 : (((c == 0) || (random.next() < 0.4)) ? 1 : 0);
                FrostArt.shard(layer, x, y, direction + offset, length, half, birth + c * 0.3, 1.1);
            }
        }
    }

    /**
     * Freezes all four screen corners, each in its own style.
     */
    static void corners(@NotNull IceLayer layer, @NotNull PixelRandom random, int width, int height) {
        int[][] corners = {{0, 0, 0}, {width - 1, 0, 4}, {width - 1, height - 1, 8}, {0, height - 1, 12}};
        for (int c = 0; c < 4; c++) corner(layer, random, corners[c][0], corners[c][1], corners[c][2], c, CORNER_STYLES[c]);
    }

    /**
     * Freezes one screen corner. The glaze is a sheet of ice plates: irregular cells in three opacity steps, thicker toward the
     * corner, with fine cracks between them. Crystals fan out across the quarter turn from the corner and from the two edges.
     *
     * @param firstDirection the first of the corner's five directions
     */
    private static void corner(@NotNull IceLayer layer, @NotNull PixelRandom random, int cornerX, int cornerY, int firstDirection, int index, @NotNull double[] style) {
        double size = style[0];
        double spreadX = style[1];
        double spreadY = style[2];
        int cell = (int)style[3];
        double dense = style[4];
        double reachX = style[5];
        double reachY = style[6];
        int crystals = (int)style[7];
        double chunk = style[8];
        int[] first = FrostArt.DIRECTIONS[firstDirection & 15];
        int[] quarter = FrostArt.DIRECTIONS[(firstDirection + 4) & 15];
        int sx = first[0] + quarter[0];
        int sy = first[1] + quarter[1];
        double spanX = 64.0 * size * spreadX;
        double spanY = 64.0 * size * spreadY;
        // The jittered center and the id of every cell, worked out once instead of for every pixel next to it
        int cellsX = (int)Math.ceil(spanX / cell) + 3;
        int cellsY = (int)Math.ceil(spanY / cell) + 3;
        float[] centerX = new float[cellsX * cellsY];
        float[] centerY = new float[cellsX * cellsY];
        float[] cellId = new float[cellsX * cellsY];
        for (int cy = -1; cy < cellsY - 1; cy++) {
            for (int cx = -1; cx < cellsX - 1; cx++) {
                int c = (cy + 1) * cellsX + (cx + 1);
                centerX[c] = (cx + PixelMath.hash(cx + index * 31, cy)) * cell;
                centerY[c] = (cy + PixelMath.hash(cy, cx + index * 17)) * cell;
                cellId[c] = PixelMath.hash(cx * 7 + index, cy * 13);
            }
        }
        for (int j = 0; j < spanY; j++) {
            for (int i = 0; i < spanX; i++) {
                double u = i / spreadX;
                double v = j / spreadY;
                double dist = Math.hypot(u, v);
                double angle = Math.atan2(v, u);
                double rim = (26.0 + 20.0 * PixelMath.valueNoise((float)(angle * 2.4), (float)(index * 5.1)) + 6.0 * PixelMath.tri((float)(angle * 26.0 + index))) * size;
                if (dist >= rim) continue;
                // The two nearest cell centers: the cell decides the opacity step, a near tie is a crack between two plates
                int gx = Math.floorDiv(i, cell);
                int gy = Math.floorDiv(j, cell);
                double d1 = 1e9;
                double d2 = 1e9;
                float id = 0.0F;
                for (int b = -1; b <= 1; b++) {
                    for (int a = -1; a <= 1; a++) {
                        int c = (gy + b + 1) * cellsX + (gx + a + 1);
                        float dx = i - centerX[c];
                        float dy = j - centerY[c];
                        double dd = Math.hypot(dx, dy);
                        if (dd < d1) {
                            d2 = d1;
                            d1 = dd;
                            id = cellId[c];
                        } else if (dd < d2) {
                            d2 = dd;
                        }
                    }
                }
                int x = cornerX + sx * i;
                int y = cornerY + sy * j;
                layer.glaze(x, y, dist * 0.09, PixelMath.clamp((int)Math.round((1.0 - dist / rim) * 2.2 + (id - 0.5) * 1.5 + dense), 0, 2));
                if ((d2 - d1 < 1.0) && (dist < rim - 2.0)) layer.crystal(x, y, dist * 0.09 + 0.5, 3);
            }
        }
        for (int k = 0; k < 5 + crystals; k++) {
            boolean fan = k < 5;
            int turn = fan ? k : (1 + (int)Math.floor(random.next() * 3.0));
            int[] d = FrostArt.DIRECTIONS[(firstDirection + turn) & 15];
            int a = d[0];
            int c = d[1];
            boolean onX = random.next() < 0.5;
            int along = (int)Math.round((6.0 + random.next() * 36.0) * size * (onX ? spreadX : spreadY));
            double base = fan ? (FAN_LENGTHS[k] * (0.8 + random.next() * 0.4)) : (9.0 + random.next() * random.next() * 30.0);
            int length = (int)Math.round(base * size * (reachX * Math.abs(a) + reachY * Math.abs(c)) / (Math.abs(a) + Math.abs(c)));
            if (length < 3) continue;
            int x = fan ? (cornerX + ((a != 0) ? 0 : sx * 3)) : (cornerX + (onX ? sx * along : 0));
            int y = fan ? (cornerY + ((c != 0) ? 0 : sy * 3)) : (cornerY + (onX ? 0 : sy * along));
            int half;
            if (chunk >= 1.0) {
                half = (length > 30) ? 3 : ((length > 14) ? 2 : 1);
            } else if (chunk > 0.0) {
                half = (length > 30) ? 2 : 1;
            } else {
                half = (length > 22) ? 1 : 0;
            }
            double birth = fan ? (random.next() * 0.5) : (1.0 + random.next() * 2.0);
            FrostArt.shard(layer, x, y, firstDirection + turn, length, half, birth, 1.0);
        }
    }

    /**
     * Freezes one element: ice creeps in from both ends and thinly along the long edges, a jagged rime ridge sits on top and icicles
     * hang below. The crust wraps around both ends, so the ice has a finished edge there.
     *
     * @param up free rows above the element the ice may use
     * @param down free rows below the element the ice may use
     */
    static void element(@NotNull IceLayer layer, @NotNull PixelRandom random, int left, int top, int width, int height, int up, int down) {
        int right = left + width - 1;
        int bottom = top + height - 1;
        double mid = (height - 1) / 2.0;
        // Both ends frost over from the edge inward. The ice reaches deepest along the top and bottom rows, so the label stays clear,
        // and its inner rim is a row of crystal points: each is lit along its upper flank and shaded along its lower one.
        // A broad crystal grows out of each corner and a small one from the middle.
        for (int side = 0; side < 2; side++) {
            int[] teeth = FrostArt.teeth(random, height, 1, 6);
            int[] reach = new int[height];
            for (int j = 0; j < height; j++) {
                double edgeness = (mid > 0.0) ? Math.pow(Math.abs(j - mid) / mid, 1.6) : 0.0;
                reach[j] = 3 + (int)Math.round(8.0 * edgeness) + teeth[j];
            }
            for (int j = 0; j < height; j++) {
                int n = reach[j];
                int above = (j > 0) ? reach[j - 1] : n;
                int below = (j < height - 1) ? reach[j + 1] : n;
                double rowDelay = Math.abs(j - mid) * 0.03;
                for (int i = 0; i < n; i++) layer.glaze(endColumn(side, i, left, right), top + j, 2.0 + i * 0.14 + rowDelay, (i < n * 0.35) ? 2 : ((i < n * 0.7) ? 1 : 0));
                if (n > above) {
                    for (int i = above; i < n; i++) layer.crystal(endColumn(side, i, left, right), top + j, 2.0 + i * 0.14 + rowDelay + 0.5, 1);
                } else if (n > below) {
                    for (int i = below; i < n; i++) layer.crystal(endColumn(side, i, left, right), top + j, 2.0 + i * 0.14 + rowDelay + 0.5, 3);
                }
                layer.crystal(endColumn(side, n - 1, left, right), top + j, 2.0 + n * 0.14 + rowDelay + 0.5, (n > above) ? 0 : 2);
            }
            int edge = (side == 1) ? (right + 1) : (left - 1);
            int along = (side == 1) ? FrostArt.LEFT : FrostArt.RIGHT;
            double birth = 3.0 + random.next();
            FrostArt.shard(layer, edge, top + 3, along, (int)Math.round(height * (0.5 + random.next() * 0.35)), 2, birth, 1.0);
            FrostArt.shard(layer, edge, bottom - 3, along, (int)Math.round(height * (0.5 + random.next() * 0.35)), 2, birth + 0.3, 1.0);
            int middle = top + (int)Math.round(mid) + ((random.next() < 0.5) ? -1 : 1);
            FrostArt.shard(layer, edge, middle, along, 3 + (int)Math.floor(random.next() * 3.0), 1, birth + 1.0, 1.0);
        }
        int[] topTeeth = FrostArt.teeth(random, width, 0, 3);
        int[] bottomTeeth = FrostArt.teeth(random, width, 0, 3);
        for (int i = 0; i < width; i++) {
            for (int d = 0; d < 1 + topTeeth[i]; d++) layer.glaze(left + i, top + d, 2.5 + d * 0.3, (d != 0) ? 0 : 1);
            for (int d = 0; d < 1 + bottomTeeth[i]; d++) layer.glaze(left + i, bottom - d, 2.5 + d * 0.3, (d != 0) ? 0 : 1);
        }
        // Both ends: a rime edge of uneven thickness down the side, lit on the left and shaded on the right, with spikes pointing
        // outward from the middle and the two corners and a small icicle under each bottom corner.
        for (int side = 0; side < 2; side++) {
            int edge = (side == 0) ? (left - 1) : (right + 1);
            int sign = (side == 0) ? -1 : 1;
            int out = (side == 0) ? FrostArt.LEFT : FrostArt.RIGHT;
            int[] rime = FrostArt.teeth(random, height + 2, 1, 3);
            double birth = 3.4 + random.next();
            for (int j = -1; j <= height; j++) {
                int thickness = ((j < 0) || (j >= height)) ? 1 : (1 + Math.min(2, rime[j + 1]));
                for (int k = 0; k < thickness; k++) {
                    int tone = (k == thickness - 1) ? ((sign < 0) ? 0 : 2) : 1;
                    layer.crystal(edge + sign * k, top + j, birth + Math.abs(j - mid) * 0.05 + k * 0.3, tone);
                }
            }
            int spikeY = top + (int)Math.round(mid) + ((random.next() < 0.5) ? -2 : 2);
            FrostArt.shard(layer, edge - sign, spikeY, out, 4 + (int)Math.floor(random.next() * 4.0), 1, birth + 0.8);
            int upperLength = Math.min(up * 2 - 1, 3 + (int)Math.floor(random.next() * 4.0));
            FrostArt.shard(layer, edge - sign, top + 1, out - sign, upperLength, (random.next() < 0.5) ? 1 : 0, birth + 1.0);
            int lowerLength = Math.min(down * 2 - 1, 3 + (int)Math.floor(random.next() * 4.0));
            FrostArt.shard(layer, edge - sign, bottom - 1, out + sign, lowerLength, (random.next() < 0.5) ? 1 : 0, birth + 1.2);
            int icicle = Math.min(down - 1, 2 + (int)Math.floor(random.next() * 4.0));
            FrostArt.shard(layer, edge, bottom, FrostArt.DOWN, icicle, 0, 5.0 + random.next() * 3.0, 1.0);
        }
        // A jagged rime ridge on top
        int[] cap = FrostArt.teeth(random, width + 2, 1, 4);
        for (int i = -1; i <= width; i++) {
            boolean end = (i < 0) || (i >= width);
            int ridge = Math.min(up, end ? 1 : (1 + cap[i + 1]));
            boolean rises = cap[i + 1] >= ((i >= 0) ? cap[i] : 0);
            for (int k = 0; k < ridge; k++) {
                int tone = (k == ridge - 1) ? 0 : (rises ? 1 : 2);
                layer.crystal(left + i, top - 1 - k, 3.0 + Math.min(i + 1, width - i) * 0.03 + k * 0.4, tone);
            }
        }
        // Icicles underneath
        for (int i = 3 + (int)Math.floor(random.next() * 5.0); i < width - 3; i += ((down > 5) ? 4 : 6) + (int)Math.floor(random.next() * 9.0)) {
            int length = Math.min(down - 1, 2 + (int)Math.floor(random.next() * random.next() * 11.0));
            if (length >= 1) FrostArt.shard(layer, left + i, bottom, FrostArt.DOWN, length, (length >= 3) ? 1 : 0, 5.0 + random.next() * 4.0, 1.0);
        }
    }

    private static int endColumn(int side, int inward, int left, int right) {
        return (side == 1) ? (right - inward) : (left + inward);
    }

}
