package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.overgrowth;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.CanvasPainter;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.TiledCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything that grows on one element: the moss cap with its shadow and short drips over the top edge, vines hanging from both
 * ends and a few near them (the label in the middle stays clear), tendrils underneath, a clump of leaves around each corner,
 * and flowers on the moss cushions.<br>
 * It grows on the clock of the "On Elements" part, from the moment the element showed up, and only invalidates the canvas where
 * its look changed.
 */
final class ElementGrowth {

    /** Rows of moss texture kept per column, more than the highest cushion */
    private static final int MOSS_ROWS = 8;
    private static final float MOSS_MAX_HEIGHT = 5.6F;
    /** Clock units a moss column takes to grow to its full height */
    private static final float MOSS_GROW_TIME = 1.6F;
    private static final float DRIP_DELAY = 1.2F;
    private static final float LEAF_BUD_AGE = 0.5F;
    /** Element orders above this all start at the same time */
    private static final int MAX_ORDER_DELAY = 7;

    private static final int CLUMP_HIDDEN = 0;
    private static final int CLUMP_BUD = 1;
    private static final int CLUMP_OPEN = 2;

    final int left;
    final int top;
    final int width;
    final int height;
    /** The part's clock time at which this element started to grow */
    float startClock = 0.0F;
    /** Position in the growth order: elements further down start a little later */
    int order = 0;

    private boolean built = false;
    private int columns;
    private int x0;
    /** Moss height per column. NaN marks the bare foot of a cushion, see {@link #mossHeights}. */
    private float[] heights;
    private float[] born;
    private float[] texture;
    private int[] dripIndex;
    private int[] dripLength;
    private HangingVine[] vines;
    private HangingVine[] under;
    private int[] clumpX;
    private int[] clumpY;
    private byte[] clumpKind;
    private byte[] clumpPalette;
    private boolean[] clumpFlip;
    private float[] clumpBirth;
    private OvergrowthFlower[] flowers;
    /** Clock time after which the moss, drips and corner leaves don't change anymore */
    private float settledAt;
    private final int[] bounds = new int[4];

    private byte[] mossShown;
    private byte[] dripShown;
    private byte[] clumpStages;
    private byte[] flowerStages;
    private float lastClock = -1.0F;

    ElementGrowth(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
    }

    boolean isBuilt() {
        return this.built;
    }

    /**
     * Grows everything of this element by simulation, once.
     *
     * @param order position of the element when sorted from top to bottom
     */
    void build(int order) {
        this.order = order;
        int si = Math.min(order, MAX_ORDER_DELAY);
        PixelRandom random = new PixelRandom(0x60606 + si * 7919 + this.width * 31 + this.height * 131);
        int n = this.width + 4;
        this.columns = n;
        this.x0 = this.left - 2;
        this.heights = mossHeights(random, n, MOSS_MAX_HEIGHT);
        double[] seeds = {random.next() * n, random.next() * n, random.next() * n};
        this.born = new float[n];
        for (int i = 0; i < n; i++) {
            double nearest = Math.min(Math.abs(i - seeds[0]), Math.min(Math.abs(i - seeds[1]), Math.abs(i - seeds[2])));
            this.born[i] = (float)(nearest * 0.085 + si * 0.5);
        }
        this.texture = new float[n * MOSS_ROWS];
        for (int i = 0; i < n; i++) {
            for (int r = 0; r < MOSS_ROWS; r++) this.texture[i * MOSS_ROWS + r] = PixelMath.hash(i * 3 + this.top, r);
        }

        List<int[]> drips = new ArrayList<>();
        for (int i = 3 + (int)Math.floor(random.next() * 5.0); i < n - 3; i += 4 + (int)Math.floor(random.next() * 8.0)) {
            drips.add(new int[]{i, 1 + (int)Math.floor(random.next() * 3.0)});
        }
        this.dripIndex = new int[drips.size()];
        this.dripLength = new int[drips.size()];
        for (int d = 0; d < drips.size(); d++) {
            this.dripIndex[d] = drips.get(d)[0];
            this.dripLength[d] = drips.get(d)[1];
        }

        // On narrow elements the vines near the ends move closer to them
        int nearReach = Math.max(1, this.width / 3);
        List<HangingVine> hanging = new ArrayList<>();
        this.hang(hanging, random, this.left - 2, 16 + (int)Math.floor(random.next() * 18.0), 0.0F);
        int nearLeft = this.left + 2 + (int)Math.floor(random.next() * 3.0);
        this.hang(hanging, random, nearLeft, 9 + (int)Math.floor(random.next() * 10.0), 0.6F);
        this.hang(hanging, random, this.left + this.width + 1, 16 + (int)Math.floor(random.next() * 18.0), 0.2F);
        int nearRight = this.left + this.width - 3 - (int)Math.floor(random.next() * 3.0);
        this.hang(hanging, random, nearRight, 9 + (int)Math.floor(random.next() * 10.0), 0.8F);
        for (int end = 0; end < 2; end++) {
            for (int k = 0; k < 2; k++) {
                int d = Math.min(9 + (int)Math.floor(random.next() * 20.0), nearReach);
                int x = (end == 1) ? (this.left + this.width - 1 - d) : (this.left + d);
                this.hang(hanging, random, x, 4 + (int)Math.floor(random.next() * 5.0), 1.2F + k);
            }
        }
        this.vines = hanging.toArray(new HangingVine[0]);

        List<HangingVine> tendrils = new ArrayList<>();
        for (int x = this.left + 6 + (int)Math.floor(random.next() * 14.0); x < this.left + this.width - 6; x += 14 + (int)Math.floor(random.next() * 26.0)) {
            int length = 3 + (int)Math.floor(random.next() * 6.0);
            float birth = (float)(6.0 + si + random.next() * 4.0);
            tendrils.add(HangingVine.make(random, x, this.top + this.height, length, birth, false, 5.0F, 0.8F));
        }
        this.under = tendrils.toArray(new HangingVine[0]);

        // Shaded leaves go down first and spread wider, lit leaves sit on top of them near the corner
        List<float[]> clump = new ArrayList<>();
        for (int end = 0; end < 2; end++) {
            int cornerX = (end == 1) ? (this.left + this.width - 1) : this.left;
            for (int k = 0; k < 8; k++) {
                boolean lit = k >= 4;
                double spread = lit ? 7.0 : 12.0;
                int x = cornerX + (int)Math.round((random.next() - 0.5) * spread) - 2;
                int y = this.top - (lit ? 5 : 3) + (int)Math.round(random.next() * (lit ? 6.0 : 9.0));
                int kind = (random.next() < 0.65) ? OvergrowthArt.LEAF_IVY : OvergrowthArt.LEAF_SMALL;
                boolean flip = random.next() < 0.5;
                double birth = 1.5 + si * 0.5 + (lit ? 1.5 : 0.0) + random.next() * 2.5;
                clump.add(new float[]{x, y, kind, lit ? 0 : 1, flip ? 1 : 0, (float)birth});
            }
            for (int k = 0; k < 2; k++) {
                int x = cornerX + (int)Math.round((random.next() - 0.5) * 6.0) - 2;
                int y = this.top + this.height - 4 + (int)Math.round(random.next() * 3.0);
                boolean flip = random.next() < 0.5;
                double birth = 5.0 + si * 0.5 + random.next() * 3.0;
                clump.add(new float[]{x, y, OvergrowthArt.LEAF_SMALL, 1, flip ? 1 : 0, (float)birth});
            }
        }
        int clumpCount = clump.size();
        this.clumpX = new int[clumpCount];
        this.clumpY = new int[clumpCount];
        this.clumpKind = new byte[clumpCount];
        this.clumpPalette = new byte[clumpCount];
        this.clumpFlip = new boolean[clumpCount];
        this.clumpBirth = new float[clumpCount];
        for (int c = 0; c < clumpCount; c++) {
            float[] leaf = clump.get(c);
            this.clumpX[c] = (int)leaf[0];
            this.clumpY[c] = (int)leaf[1];
            this.clumpKind[c] = (byte)leaf[2];
            this.clumpPalette[c] = (byte)leaf[3];
            this.clumpFlip[c] = leaf[4] != 0.0F;
            this.clumpBirth[c] = leaf[5];
        }

        this.flowers = this.growFlowers(random, si);

        this.settledAt = 0.0F;
        for (int i = 0; i < n; i++) this.settledAt = Math.max(this.settledAt, this.born[i] + MOSS_GROW_TIME);
        for (int d = 0; d < this.dripIndex.length; d++) this.settledAt = Math.max(this.settledAt, this.born[this.dripIndex[d]] + DRIP_DELAY + this.dripLength[d] / 2.0F);
        for (float birth : this.clumpBirth) this.settledAt = Math.max(this.settledAt, birth + LEAF_BUD_AGE);

        this.bounds[0] = this.x0;
        this.bounds[1] = this.top - MOSS_ROWS - 7;
        this.bounds[2] = this.x0 + n;
        this.bounds[3] = this.top + 2;
        for (int c = 0; c < clumpCount; c++) {
            this.bounds[0] = Math.min(this.bounds[0], this.clumpX[c]);
            this.bounds[1] = Math.min(this.bounds[1], this.clumpY[c]);
            this.bounds[2] = Math.max(this.bounds[2], this.clumpX[c] + 5);
            this.bounds[3] = Math.max(this.bounds[3], this.clumpY[c] + 5);
        }
        for (HangingVine vine : this.vines) vine.extendMaxBounds(this.bounds);
        for (HangingVine vine : this.under) vine.extendMaxBounds(this.bounds);

        this.mossShown = new byte[n];
        this.dripShown = new byte[this.dripIndex.length];
        this.clumpStages = new byte[clumpCount];
        this.flowerStages = new byte[this.flowers.length];
        this.built = true;
    }

    private void hang(@NotNull List<HangingVine> vines, @NotNull PixelRandom random, int x, int length, float delay) {
        float birth = this.born[PixelMath.clamp(x - this.x0, 0, this.columns - 1)] + 1.0F + delay;
        vines.add(HangingVine.make(random, x, this.top, length, birth, false, 6.0F, 1.6F));
    }

    @NotNull
    private OvergrowthFlower[] growFlowers(@NotNull PixelRandom random, int si) {
        int n = this.columns;
        float[] hm = this.heights;
        List<OvergrowthFlower> flowers = new ArrayList<>();
        // Flowers stand on the tops of the cushions. Comparisons with the bare foot of a cushion (NaN) are false, so none stands next to one.
        for (int i = 4, last = -9; i < n - 4; i++) {
            if ((hm[i] >= 2.4F) && (hm[i] >= hm[i - 1]) && (hm[i] >= hm[i + 1]) && (i - last >= 6) && (random.next() < 0.8)) {
                int kind = (int)Math.floor(random.next() * OvergrowthArt.MOSS_KIND_COUNT);
                boolean small = random.next() < 0.35;
                int stem = 1 + (int)Math.floor(random.next() * 2.0);
                float birth = (float)(this.born[i] + 2.5 + random.next() * 4.0);
                flowers.add(new OvergrowthFlower(i, kind, small, birth, false, 0, 0, stem, 0.0F, false));
                last = i;
            }
        }
        // Spare flowers stand between the first ones. They have their own random source, so the rest of the growth stays as it is.
        PixelRandom spare = new PixelRandom(4242 + si * 31);
        List<Integer> used = new ArrayList<>();
        for (OvergrowthFlower flower : flowers) used.add(flower.position);
        for (int i = 4; i < n - 4; i++) {
            if (!(hm[i] >= 1.6F) || !isClear(used, i) || !(spare.next() < 0.42)) continue;
            int kind = (int)Math.floor(spare.next() * OvergrowthArt.MOSS_KIND_COUNT);
            boolean small = spare.next() < 0.5;
            int stem = 1 + (int)Math.floor(spare.next() * 2.0);
            float birth = (float)(this.born[i] + 2.5 + spare.next() * 4.0);
            flowers.add(new OvergrowthFlower(i, kind, small, birth, true, 0, 0, stem, 0.0F, false));
            used.add(i);
        }
        flowers.sort((a, b) -> (a.spare != b.spare) ? Boolean.compare(a.spare, b.spare) : Integer.compare(a.position, b.position));
        OvergrowthArt.rank(flowers);
        return flowers.toArray(new OvergrowthFlower[0]);
    }

    private static boolean isClear(@NotNull List<Integer> used, int i) {
        for (int j : used) {
            if (Math.abs(j - i) < 4) return false;
        }
        return true;
    }

    /**
     * Moss heights along n columns: overlapping round cushions over a thin base layer with a few gaps. The column at the foot of
     * every cushion that starts inside the element stays bare (NaN), a small notch that sets the cushions apart.
     */
    @NotNull
    static float[] mossHeights(@NotNull PixelRandom random, int n, float max) {
        float[] hm = new float[n];
        for (double c = random.next() * 6.0; c < n; ) {
            double r = 4.0 + random.next() * 9.0;
            double top = 1.8 + random.next() * (max - 1.8);
            for (int i = Math.max(0, (int)Math.floor(c - r)); i < Math.min(n, c + r); i++) {
                double u = (i - c) / r;
                // The square root of a negative number is NaN at the foot of the cushion, and Math.max keeps NaN from then on
                hm[i] = Math.max(hm[i], (float)(top * Math.sqrt(1.0 - u * u)));
            }
            c += r * (0.9 + random.next() * 1.4);
        }
        for (int i = 0; i < n; i++) {
            hm[i] = Math.min(Math.max(hm[i], (PixelMath.hash(i, n) > 0.1F) ? 1.0F : 0.0F), 1 + Math.min(i, n - 1 - i));
        }
        return hm;
    }

    /**
     * Brings the growth to the part's clock time and invalidates the canvas where its look changed.
     *
     * @param show false while the part is switched off, which hides everything
     * @param bloomThreshold flowers with a rank below it show, 0 for none
     */
    void update(float partClock, float time, boolean show, float bloomThreshold, @NotNull TiledCanvas canvas) {
        if (!this.built) return;
        float clock = show ? Math.max(0.0F, partClock - this.startClock) : 0.0F;
        for (HangingVine vine : this.under) vine.update(clock, time, canvas);
        for (HangingVine vine : this.vines) vine.update(clock, time, canvas);
        boolean settled = (clock >= this.settledAt) && (this.lastClock >= this.settledAt);
        if ((clock != this.lastClock) && !settled) {
            this.updateMoss(clock, canvas);
            this.updateDrips(clock, canvas);
            this.updateClump(clock, canvas);
        }
        this.lastClock = clock;
        for (int f = 0; f < this.flowers.length; f++) {
            OvergrowthFlower flower = this.flowers[f];
            int stage = OvergrowthArt.BLOOM_HIDDEN;
            if (show) stage = OvergrowthArt.bloomStage(flower.age(clock, flower.isLetIn(bloomThreshold)), flower.small);
            if (stage == this.flowerStages[f]) continue;
            this.flowerStages[f] = (byte)stage;
            int x = this.x0 + flower.position;
            int y = this.flowerBaseY(flower);
            canvas.invalidate(x - 2, y - flower.stem - 4, x + 3, y + 1);
        }
    }

    private void updateMoss(float clock, @NotNull TiledCanvas canvas) {
        int from = Integer.MAX_VALUE;
        int to = Integer.MIN_VALUE;
        for (int i = 0; i < this.columns; i++) {
            int shown = this.mossHeight(i, clock);
            if (shown == this.mossShown[i]) continue;
            this.mossShown[i] = (byte)shown;
            from = Math.min(from, i);
            to = Math.max(to, i);
        }
        // The top color of a column depends on the column before it
        if (from <= to) canvas.invalidate(this.x0 + from, this.top - MOSS_ROWS, this.x0 + to + 2, this.top + 2);
    }

    private int mossHeight(int column, float clock) {
        float value = this.heights[column] * PixelMath.clamp((clock - this.born[column]) / MOSS_GROW_TIME, 0.0F, 1.0F);
        return Float.isNaN(value) ? 0 : (int)Math.round(value);
    }

    private void updateDrips(float clock, @NotNull TiledCanvas canvas) {
        for (int d = 0; d < this.dripIndex.length; d++) {
            float grown = clock - this.born[this.dripIndex[d]] - DRIP_DELAY;
            int length = (grown <= 0.0F) ? 0 : Math.min(this.dripLength[d], (int)Math.ceil(grown * 2.0F));
            if (length == this.dripShown[d]) continue;
            this.dripShown[d] = (byte)length;
            int x = this.x0 + this.dripIndex[d];
            canvas.invalidate(x, this.top, x + 1, this.top + 3);
        }
    }

    private void updateClump(float clock, @NotNull TiledCanvas canvas) {
        for (int c = 0; c < this.clumpBirth.length; c++) {
            float age = clock - this.clumpBirth[c];
            int stage = (age <= 0.0F) ? CLUMP_HIDDEN : ((age < LEAF_BUD_AGE) ? CLUMP_BUD : CLUMP_OPEN);
            if (stage == this.clumpStages[c]) continue;
            this.clumpStages[c] = (byte)stage;
            canvas.invalidate(this.clumpX[c], this.clumpY[c], this.clumpX[c] + 5, this.clumpY[c] + 5);
        }
    }

    private int flowerBaseY(@NotNull OvergrowthFlower flower) {
        return this.top - 1 - (int)Math.round(this.heights[flower.position]);
    }

    /**
     * Invalidates everything the growth can ever cover, so it gets erased when the element is gone.
     */
    void invalidateAll(@NotNull TiledCanvas canvas) {
        if (this.built) canvas.invalidate(this.bounds[0], this.bounds[1], this.bounds[2], this.bounds[3]);
    }

    /**
     * Paints the growth in its order: tendrils underneath, moss with its shadow, drips, hanging vines, corner leaves and flowers.
     */
    void paint(@NotNull CanvasPainter painter) {
        if (!this.built || !painter.overlaps(this.bounds[0], this.bounds[1], this.bounds[2], this.bounds[3])) return;
        for (HangingVine vine : this.under) vine.paint(painter);
        this.paintMoss(painter);
        int[] green = OvergrowthArt.GREEN;
        for (int d = 0; d < this.dripIndex.length; d++) {
            int length = this.dripShown[d];
            for (int r = 0; r < length; r++) painter.pixel(this.x0 + this.dripIndex[d], this.top + r, (r == length - 1) ? green[3] : green[2]);
        }
        for (HangingVine vine : this.vines) vine.paint(painter);
        for (int c = 0; c < this.clumpBirth.length; c++) {
            int stage = this.clumpStages[c];
            if (stage == CLUMP_HIDDEN) continue;
            PixelSprite sprite = OvergrowthArt.LEAF[(stage == CLUMP_BUD) ? OvergrowthArt.LEAF_BUD : this.clumpKind[c]];
            painter.sprite(sprite, OvergrowthArt.LEAF_PALETTE[this.clumpPalette[c]], this.clumpX[c], this.clumpY[c], this.clumpFlip[c]);
        }
        for (int f = 0; f < this.flowers.length; f++) {
            int stage = this.flowerStages[f];
            if (stage == OvergrowthArt.BLOOM_HIDDEN) continue;
            OvergrowthFlower flower = this.flowers[f];
            int x = this.x0 + flower.position;
            int y = this.flowerBaseY(flower);
            for (int k = 0; k < flower.stem; k++) painter.pixel(x, y - k, green[3]);
            OvergrowthArt.paintBloom(painter, x, y - flower.stem, flower.kind, stage);
        }
    }

    /**
     * Paints the moss whose lowest row is the row above the element. Each column is lit on top, brighter where the cushion rises
     * toward the light, darker along its foot and speckled in between. Thick moss throws a soft shadow onto the element's top edge.
     */
    private void paintMoss(@NotNull CanvasPainter painter) {
        int[] green = OvergrowthArt.GREEN;
        int from = Math.max(0, painter.clipX0() - this.x0);
        int to = Math.min(this.columns, painter.clipX1() - this.x0);
        int bottom = this.top - 1;
        for (int i = from; i < to; i++) {
            int h = this.mossShown[i];
            // The column after a bare foot never counts as rising, like a comparison with NaN
            boolean rises = (i == 0) || (!Float.isNaN(this.heights[i - 1]) && (h > this.mossShown[i - 1]));
            int base = i * MOSS_ROWS;
            for (int r = 0; r < h; r++) {
                float tex = this.texture[base + Math.min(r, MOSS_ROWS - 1)];
                int color;
                if (r == h - 1) {
                    color = (rises || (tex > 0.8F)) ? green[0] : green[1];
                } else if ((r == 0) && (h > 2)) {
                    color = green[3];
                } else if ((r == h - 2) && (h > 3)) {
                    color = (tex > 0.3F) ? green[1] : green[2];
                } else {
                    color = (tex > 0.85F) ? green[3] : ((tex < 0.1F) ? green[1] : green[2]);
                }
                painter.pixel(this.x0 + i, bottom - r, color);
            }
            if ((i >= 2) && (i < this.columns - 2) && (h >= 2)) painter.pixel(this.x0 + i, this.top + 1, OvergrowthArt.MOSS_SHADOW);
        }
    }

    boolean isSettled(float partClock) {
        return this.built && (Math.max(0.0F, partClock - this.startClock) >= this.settledAt);
    }

    int mossColumns() {
        return this.columns;
    }

    int shownMossHeight(int column) {
        return this.mossShown[column];
    }

    int flowerCount() {
        return this.flowers.length;
    }

    int flowerStage(int flower) {
        return this.flowerStages[flower];
    }

    @NotNull
    OvergrowthFlower flower(int flower) {
        return this.flowers[flower];
    }

}
