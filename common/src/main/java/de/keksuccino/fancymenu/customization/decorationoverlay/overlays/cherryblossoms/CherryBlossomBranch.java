package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.cherryblossoms;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A blossoming branch that reaches in from a top corner of the screen. It grows procedurally from a fixed seed, so it looks the same
 * every time: a main limb with side limbs and twigs, tapering bark with a lit top and a shaded bottom row, and clusters of blossoms
 * at the twig tips. It gets painted into a canvas once and drawn as one quad. Its tips are where petals let go.
 */
final class CherryBlossomBranch {

    /** Length of the main limb on a screen of the mockup's size (480 x 270) */
    private static final double BASE_LENGTH = 120.0;
    private static final int CLUSTER_SIZE = 5;

    private final int seed;
    private final boolean mirrored;
    private final PixelCanvas canvas;
    /** Where the canvas sits on the screen */
    private int canvasX;
    private int canvasY;
    /** Bark pixels as x, y, color, in the order they were grown (later ones cover earlier ones) */
    private final List<int[]> bark = new ArrayList<>();
    /** Blossom clusters as x, y, tone, sorted by tone so the pale ones lie on top */
    private final List<int[]> clusters = new ArrayList<>();
    /** Twig tips on the screen as x, y pairs */
    private float[] tips = new float[0];
    private int tipCount = 0;

    CherryBlossomBranch(int seed, boolean mirrored) {
        this.seed = seed;
        this.mirrored = mirrored;
        this.canvas = new PixelCanvas(mirrored ? "cherry_blossoms_right_branch" : "cherry_blossoms_left_branch");
    }

    /**
     * Grows the branch for a screen of the given size and paints it.
     *
     * @param lengthFactor how long the branch is, relative to the mockup's branch
     * @param simple true for the flat clusters and plain bark of the simple look
     */
    void build(int screenWidth, int screenHeight, double lengthFactor, boolean simple) {
        this.grow(screenWidth, lengthFactor);
        this.paint(screenWidth, simple);
    }

    /**
     * Repaints the branch in another look, without growing it again.
     */
    void repaint(int screenWidth, boolean simple) {
        this.paint(screenWidth, simple);
    }

    private void grow(int screenWidth, double lengthFactor) {
        this.bark.clear();
        this.clusters.clear();
        List<double[]> tipList = new ArrayList<>();
        // The branch only uses doubles and the full precision of the random numbers, so it grows exactly like the mockup's branch
        PixelRandom random = new PixelRandom(this.seed);
        this.limb(random, tipList, -3.0, 12.0, 0.16, BASE_LENGTH * lengthFactor, 4, 0);
        for (double[] tip : tipList) {
            int count = 2 + (int)Math.floor(next(random) * 3.0);
            for (int k = 0; k < count; k++) {
                int x = (int)Math.round(tip[0] + (next(random) - 0.5) * 9.0) - 2;
                int y = (int)Math.round(tip[1] + (next(random) - 0.5) * 7.0) - 2;
                this.clusters.add(new int[]{x, y, (int)Math.floor(next(random) * 4.0)});
            }
        }
        // A stable sort keeps the growth order within each tone
        this.clusters.sort(Comparator.comparingInt(c -> c[2]));
        if (this.mirrored) {
            for (int[] pixel : this.bark) pixel[0] = screenWidth - 1 - pixel[0];
            for (int[] cluster : this.clusters) cluster[0] = screenWidth - CLUSTER_SIZE - cluster[0];
            for (double[] tip : tipList) tip[0] = screenWidth - 1 - tip[0];
        }
        this.tipCount = 0;
        this.tips = new float[tipList.size() * 2];
        for (double[] tip : tipList) {
            if ((tip[0] < 0.0) || (tip[0] >= screenWidth) || (tip[1] < 0.0)) continue;
            this.tips[this.tipCount * 2] = (float)tip[0];
            this.tips[this.tipCount * 2 + 1] = (float)tip[1];
            this.tipCount++;
        }
    }

    /**
     * Grows one limb step by step. It wanders a little, the main limb bends back to a gentle upward slope, the bark tapers to a third
     * of its thickness, side limbs branch off along the way and twigs end in tips.
     */
    private void limb(@NotNull PixelRandom random, @NotNull List<double[]> tipList, double x, double y, double angle, double length, int thickness, int depth) {
        for (int i = 0; i < length; i++) {
            angle += (next(random) - 0.5) * 0.14;
            if (depth == 0) angle += (0.16 - angle) * 0.03;
            x += Math.cos(angle);
            y += Math.sin(angle) * 0.9;
            int t = (int)Math.max(1, Math.round(thickness * (1.0 - i / length * 0.75)));
            for (int k = 0; k < t; k++) {
                int color = ((k == 0) && (t > 1)) ? CherryBlossomArt.BARK_LIT : (((k == t - 1) && (t > 2)) ? CherryBlossomArt.BARK_SHADE : CherryBlossomArt.BARK);
                this.bark.add(new int[]{(int)Math.round(x), (int)Math.round(y) + k, color});
            }
            if ((depth < 2) && (i > length * 0.2) && (i < length * 0.9) && (next(random) < ((depth == 0) ? 0.05 : 0.035))) {
                double turn = ((next(random) < 0.7) ? 1.0 : -1.0) * (0.45 + next(random) * 0.5);
                double sideLength = length * (0.3 + next(random) * 0.25);
                this.limb(random, tipList, x, y, angle + turn, sideLength, Math.max(1, t - 1), depth + 1);
            }
            boolean tip = (depth > 0) ? ((i % 5 == 2) && (next(random) < 0.55)) : ((i > 20) && (i % 9 == 4) && (next(random) < 0.5));
            if (tip) tipList.add(new double[]{x, y});
        }
        tipList.add(new double[]{x, y});
        tipList.add(new double[]{x - 3.0, y + 1.0});
    }

    /**
     * The next random number in full precision, like the mockup's generator.
     */
    private static double next(@NotNull PixelRandom random) {
        return (random.nextInt() & 0xFFFFFFFFL) / 4294967296.0;
    }

    private void paint(int screenWidth, boolean simple) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int[] pixel : this.bark) {
            minX = Math.min(minX, pixel[0]);
            maxX = Math.max(maxX, pixel[0]);
            minY = Math.min(minY, pixel[1]);
            maxY = Math.max(maxY, pixel[1]);
        }
        for (int[] cluster : this.clusters) {
            minX = Math.min(minX, cluster[0]);
            maxX = Math.max(maxX, cluster[0] + CLUSTER_SIZE - 1);
            minY = Math.min(minY, cluster[1]);
            maxY = Math.max(maxY, cluster[1] + CLUSTER_SIZE - 1);
        }
        // Only the part on the screen needs a canvas
        minX = Math.max(0, minX);
        minY = Math.max(0, minY);
        maxX = Math.min(screenWidth - 1, maxX);
        if ((maxX < minX) || (maxY < minY)) {
            this.canvas.resize(0, 0);
            return;
        }
        this.canvasX = minX;
        this.canvasY = minY;
        this.canvas.resize(maxX - minX + 1, maxY - minY + 1);
        this.canvas.clear();
        for (int[] pixel : this.bark) {
            this.canvas.set(pixel[0] - minX, pixel[1] - minY, simple ? CherryBlossomArt.BARK : pixel[2]);
        }
        PixelPalette[] tones = simple ? CherryBlossomArt.SIMPLE_CLUSTER_TONES : CherryBlossomArt.CLUSTER_TONES;
        for (int[] cluster : this.clusters) {
            this.canvas.stamp(CherryBlossomArt.CLUSTER, tones[cluster[2]], cluster[0] - minX, cluster[1] - minY, this.mirrored, false, 1);
        }
    }

    void draw(@NotNull PixelGraphics graphics) {
        graphics.canvas(this.canvas, this.canvasX, this.canvasY);
    }

    int tipCount() {
        return this.tipCount;
    }

    float tipX(int index) {
        return this.tips[index * 2];
    }

    float tipY(int index) {
        return this.tips[index * 2 + 1];
    }

    int barkPixelCount() {
        return this.bark.size();
    }

    int clusterCount() {
        return this.clusters.size();
    }

    void releaseTexture() {
        this.canvas.releaseTexture();
    }

}
