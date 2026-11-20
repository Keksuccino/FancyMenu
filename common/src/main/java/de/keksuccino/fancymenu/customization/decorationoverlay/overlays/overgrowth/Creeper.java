package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.overgrowth;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.CanvasPainter;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.TiledCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.jetbrains.annotations.NotNull;

/**
 * One vine of the climbing plants around the screen: a woody stem, a shoot or a tendril, grown once by {@link CreeperGrower}.<br>
 * The vine gets revealed pixel by pixel as the clock of its screen side runs. It remembers how it looks right now (how far it has
 * grown, which pixels are young or woody, its leaf and flower stages and how far it sways), so every frame it only invalidates
 * the canvas where its look changed.
 */
final class Creeper {

    static final int STEM = 0;
    static final int SHOOT = 1;
    static final int TENDRIL = 2;

    /** Age after which a pixel of the stem turns from fresh to normal green */
    private static final float YOUNG_AGE = 0.5F;
    /** Age after which a pixel of a main stem turns to bark */
    private static final float BARK_AGE = 5.0F;
    /** Age up to which an ivy leaf still shows as a small leaf */
    private static final float SMALL_LEAF_AGE = 1.2F;
    private static final double SWAY_SPEED = 1.2;
    private static final double SWAY_STEP = 0.05;
    private static final double SWAY_STEP_SIN = Math.sin(SWAY_STEP);
    private static final double SWAY_STEP_COS = Math.cos(SWAY_STEP);
    /** How far leaves and flowers reach beyond the pixel of the vine they sit on */
    private static final int DECOR_REACH = 8;
    /** Pixels per stretch of a vine with its own bounds, so a repaint only walks the stretches it touches */
    private static final int CHUNK = 32;

    private static final int LEAF_HIDDEN = 0;
    private static final int LEAF_BUD = 1;
    private static final int LEAF_SMALL = 2;
    private static final int LEAF_OWN = 3;

    final int kind;
    final float birth;
    /** Pixels per clock unit */
    final float speed;
    /** How far the tip sways, 0 for vines that hold still */
    final float sway;
    final float phase;
    final int[] xs;
    final int[] ys;
    /** Direction toward the inside of the screen per pixel, where an old stem gets its second pixel of thickness */
    final byte[] thickX;
    final byte[] thickY;
    final int[] leafIndex;
    final int[] leafOffsetX;
    final int[] leafOffsetY;
    final byte[] leafKind;
    final byte[] leafPalette;
    final boolean[] leafFlip;
    final OvergrowthFlower[] blooms;

    private int grown = 0;
    private int green = 0;
    private int bark = 0;
    private final byte[] offsets;
    private final byte[] leafStages;
    private final byte[] bloomStages;
    /** Bounds of every stretch of {@link #CHUNK} pixels of the grown stem, as x0, y0, x1, y1 */
    private final int[] chunkBounds;
    private boolean visible = false;
    private int boundsX0;
    private int boundsY0;
    private int boundsX1;
    private int boundsY1;

    Creeper(int kind, float birth, float speed, float sway, float phase, @NotNull int[] xs, @NotNull int[] ys, @NotNull byte[] thickX, @NotNull byte[] thickY, @NotNull int[] leafIndex, @NotNull int[] leafOffsetX, @NotNull int[] leafOffsetY, @NotNull byte[] leafKind, @NotNull byte[] leafPalette, @NotNull boolean[] leafFlip, @NotNull OvergrowthFlower[] blooms) {
        this.kind = kind;
        this.birth = birth;
        this.speed = speed;
        this.sway = sway;
        this.phase = phase;
        this.xs = xs;
        this.ys = ys;
        this.thickX = thickX;
        this.thickY = thickY;
        this.leafIndex = leafIndex;
        this.leafOffsetX = leafOffsetX;
        this.leafOffsetY = leafOffsetY;
        this.leafKind = leafKind;
        this.leafPalette = leafPalette;
        this.leafFlip = leafFlip;
        this.blooms = blooms;
        this.offsets = new byte[xs.length];
        this.leafStages = new byte[leafIndex.length];
        this.bloomStages = new byte[blooms.length];
        this.chunkBounds = new int[((xs.length + CHUNK - 1) / CHUNK) * 4];
    }

    int length() {
        return this.xs.length;
    }

    /**
     * Brings the vine to the clock time of its side and invalidates the canvas where its look changed.
     *
     * @param clock clock of the screen side
     * @param time seconds the overlay has been running, for the sway
     * @param show false while the side is switched off, which hides the vine
     * @param bloomThreshold flowers with a rank below it show, 0 for none
     */
    void update(float clock, float time, boolean show, float bloomThreshold, @NotNull TiledCanvas canvas) {
        int length = this.xs.length;
        int oldGrown = this.grown;
        int newGrown = show ? Math.max(0, Math.min(length, (int)Math.floor((clock - this.birth) * this.speed))) : 0;
        // The part that swayed: the area between the old and the new place of every grown pixel that moved
        int movedX0 = Integer.MAX_VALUE;
        int movedY0 = Integer.MAX_VALUE;
        int movedX1 = Integer.MIN_VALUE;
        int movedY1 = Integer.MIN_VALUE;
        if ((this.sway != 0.0F) && (newGrown > 0)) {
            // sin(time * 1.2 + phase + i * 0.05) by angle addition, one step per pixel
            double angle = time * SWAY_SPEED + this.phase;
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);
            double scale = (double)this.sway / length;
            int common = Math.min(oldGrown, newGrown);
            for (int i = 0; i < newGrown; i++) {
                byte offset = (byte)(int)Math.round(sin * scale * i);
                byte old = this.offsets[i];
                if ((i < common) && (offset != old)) {
                    int x = this.xs[i];
                    movedX0 = Math.min(movedX0, x + Math.min(old, offset));
                    movedX1 = Math.max(movedX1, x + Math.max(old, offset));
                    movedY0 = Math.min(movedY0, this.ys[i]);
                    movedY1 = Math.max(movedY1, this.ys[i]);
                }
                this.offsets[i] = offset;
                double nextSin = sin * SWAY_STEP_COS + cos * SWAY_STEP_SIN;
                cos = cos * SWAY_STEP_COS - sin * SWAY_STEP_SIN;
                sin = nextSin;
            }
        }
        boolean swayed = movedX1 >= movedX0;
        // Leaves and flowers hang up to a few pixels beside the pixel they sit on
        if (swayed) canvas.invalidate(movedX0 - DECOR_REACH, movedY0 - DECOR_REACH, movedX1 + DECOR_REACH + 1, movedY1 + DECOR_REACH + 1);
        int newGreen = this.countOlderThan(clock, YOUNG_AGE, this.green, newGrown, false);
        int newBark = (this.kind == STEM) ? this.countOlderThan(clock, BARK_AGE, this.bark, newGrown, true) : 0;
        boolean changed = swayed || (newGrown != oldGrown) || (newGreen != this.green) || (newBark != this.bark);
        // The tip of a tendril is lighter, so the old tip changes too when it grows
        if (newGrown != oldGrown) this.invalidatePixels(canvas, Math.min(oldGrown, newGrown) - 1, Math.max(oldGrown, newGrown));
        this.invalidatePixels(canvas, Math.min(this.green, newGreen), Math.max(this.green, newGreen));
        this.invalidatePixels(canvas, Math.min(this.bark, newBark), Math.max(this.bark, newBark));
        this.grown = newGrown;
        this.green = newGreen;
        this.bark = newBark;
        // Where a leaf or flower moved with the sway, its old place is part of the swayed area already
        for (int l = 0; l < this.leafIndex.length; l++) {
            int stage = this.leafStage(l, clock);
            if (stage == this.leafStages[l]) continue;
            this.invalidateLeaf(canvas, l, this.leafStages[l]);
            this.invalidateLeaf(canvas, l, stage);
            this.leafStages[l] = (byte)stage;
            changed = true;
        }
        for (int f = 0; f < this.blooms.length; f++) {
            OvergrowthFlower flower = this.blooms[f];
            int stage = OvergrowthArt.BLOOM_HIDDEN;
            if (show) {
                float age = flower.age(clock, flower.isLetIn(bloomThreshold));
                if (flower.position < newGrown) stage = OvergrowthArt.bloomStage(age, flower.small);
            }
            if (stage == this.bloomStages[f]) continue;
            this.invalidateBloom(canvas, f);
            this.bloomStages[f] = (byte)stage;
            changed = true;
        }
        if (changed) this.updateBounds();
    }

    /**
     * Counts the pixels from the start of the vine that are older than {@code age}, starting the search at the last count.
     * Pixels further along the vine are younger, so they form one run from the start.
     */
    private int countOlderThan(float clock, float age, int last, int grown, boolean strictly) {
        int count = Math.min(last, grown);
        while ((count > 0) && !this.isOlderThan(clock, count - 1, age, strictly)) count--;
        while ((count < grown) && this.isOlderThan(clock, count, age, strictly)) count++;
        return count;
    }

    private boolean isOlderThan(float clock, int index, float age, boolean strictly) {
        float pixelAge = clock - this.birth - index / this.speed;
        return strictly ? (pixelAge > age) : (pixelAge >= age);
    }

    private int leafStage(int leaf, float clock) {
        int index = this.leafIndex[leaf];
        if (index >= this.grown) return LEAF_HIDDEN;
        float age = clock - this.birth - index / this.speed;
        if (age < YOUNG_AGE) return LEAF_BUD;
        if ((age < SMALL_LEAF_AGE) && (this.leafKind[leaf] == OvergrowthArt.LEAF_IVY)) return LEAF_SMALL;
        return LEAF_OWN;
    }

    @NotNull
    private PixelSprite leafSprite(int leaf, int stage) {
        return switch (stage) {
            case LEAF_BUD -> OvergrowthArt.LEAF[OvergrowthArt.LEAF_BUD];
            case LEAF_SMALL -> OvergrowthArt.LEAF[OvergrowthArt.LEAF_SMALL];
            default -> OvergrowthArt.LEAF[this.leafKind[leaf]];
        };
    }

    private int leafX(int leaf, @NotNull PixelSprite sprite) {
        int index = this.leafIndex[leaf];
        return this.xs[index] + this.offsets[index] + this.leafOffsetX[leaf] - (sprite.width() >> 1);
    }

    private int leafY(int leaf) {
        return this.ys[this.leafIndex[leaf]] + this.leafOffsetY[leaf];
    }

    private int bloomX(int bloom) {
        OvergrowthFlower flower = this.blooms[bloom];
        return this.xs[flower.position] + this.offsets[flower.position] + flower.offsetX;
    }

    private int bloomY(int bloom) {
        OvergrowthFlower flower = this.blooms[bloom];
        return this.ys[flower.position] + flower.offsetY;
    }

    private void invalidatePixels(@NotNull TiledCanvas canvas, int from, int to) {
        from = Math.max(0, from);
        to = Math.min(to, this.xs.length);
        if (to <= from) return;
        int x0 = Integer.MAX_VALUE;
        int y0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE;
        int y1 = Integer.MIN_VALUE;
        for (int i = from; i < to; i++) {
            int x = this.xs[i] + this.offsets[i];
            int y = this.ys[i];
            if (x < x0) x0 = x;
            if (x > x1) x1 = x;
            if (y < y0) y0 = y;
            if (y > y1) y1 = y;
        }
        // One more pixel all around covers the thickness of old stems
        canvas.invalidate(x0 - 1, y0 - 1, x1 + 2, y1 + 2);
    }

    private void invalidateLeaf(@NotNull TiledCanvas canvas, int leaf, int stage) {
        if (stage == LEAF_HIDDEN) return;
        PixelSprite sprite = this.leafSprite(leaf, stage);
        int x = this.leafX(leaf, sprite);
        int y = this.leafY(leaf);
        canvas.invalidate(x, y, x + sprite.width(), y + sprite.height());
    }

    private void invalidateBloom(@NotNull TiledCanvas canvas, int bloom) {
        int x = this.bloomX(bloom);
        int y = this.bloomY(bloom);
        canvas.invalidate(x - 2, y - 4, x + 3, y + 1);
    }

    private void updateBounds() {
        int x0 = Integer.MAX_VALUE;
        int y0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE;
        int y1 = Integer.MIN_VALUE;
        for (int chunk = 0; chunk * CHUNK < this.grown; chunk++) {
            int cx0 = Integer.MAX_VALUE;
            int cy0 = Integer.MAX_VALUE;
            int cx1 = Integer.MIN_VALUE;
            int cy1 = Integer.MIN_VALUE;
            for (int i = chunk * CHUNK; i < Math.min(this.grown, (chunk + 1) * CHUNK); i++) {
                int x = this.xs[i] + this.offsets[i];
                int y = this.ys[i];
                if (x < cx0) cx0 = x;
                if (x > cx1) cx1 = x;
                if (y < cy0) cy0 = y;
                if (y > cy1) cy1 = y;
            }
            // One more pixel all around covers the thickness of old stems
            int c = chunk * 4;
            this.chunkBounds[c] = cx0 - 1;
            this.chunkBounds[c + 1] = cy0 - 1;
            this.chunkBounds[c + 2] = cx1 + 2;
            this.chunkBounds[c + 3] = cy1 + 2;
            x0 = Math.min(x0, cx0 - 1);
            y0 = Math.min(y0, cy0 - 1);
            x1 = Math.max(x1, cx1 + 2);
            y1 = Math.max(y1, cy1 + 2);
        }
        for (int l = 0; l < this.leafIndex.length; l++) {
            int stage = this.leafStages[l];
            if (stage == LEAF_HIDDEN) continue;
            PixelSprite sprite = this.leafSprite(l, stage);
            int x = this.leafX(l, sprite);
            int y = this.leafY(l);
            x0 = Math.min(x0, x);
            y0 = Math.min(y0, y);
            x1 = Math.max(x1, x + sprite.width());
            y1 = Math.max(y1, y + sprite.height());
        }
        for (int f = 0; f < this.blooms.length; f++) {
            if (this.bloomStages[f] == OvergrowthArt.BLOOM_HIDDEN) continue;
            int x = this.bloomX(f);
            int y = this.bloomY(f);
            x0 = Math.min(x0, x - 2);
            y0 = Math.min(y0, y - 4);
            x1 = Math.max(x1, x + 3);
            y1 = Math.max(y1, y + 1);
        }
        this.visible = this.grown > 0;
        this.boundsX0 = x0;
        this.boundsY0 = y0;
        this.boundsX1 = x1;
        this.boundsY1 = y1;
    }

    private boolean overlaps(@NotNull CanvasPainter painter) {
        return this.visible && painter.overlaps(this.boundsX0, this.boundsY0, this.boundsX1, this.boundsY1);
    }

    /**
     * Paints the grown part of the stem: fresh green near the tip, darker green with a pattern behind it, and bark with a second
     * pixel of thickness on old main stems. A tendril is light green with a lighter tip.
     */
    void paintStem(@NotNull CanvasPainter painter) {
        if (!this.overlaps(painter)) return;
        int n = this.grown;
        for (int chunk = 0; chunk * CHUNK < n; chunk++) {
            int c = chunk * 4;
            if (!painter.overlaps(this.chunkBounds[c], this.chunkBounds[c + 1], this.chunkBounds[c + 2], this.chunkBounds[c + 3])) continue;
            this.paintStretch(painter, chunk * CHUNK, Math.min(n, (chunk + 1) * CHUNK));
        }
    }

    private void paintStretch(@NotNull CanvasPainter painter, int from, int to) {
        int[] green = OvergrowthArt.GREEN;
        int n = this.grown;
        for (int i = from; i < to; i++) {
            int x = this.xs[i] + this.offsets[i];
            int y = this.ys[i];
            if (this.kind == TENDRIL) {
                painter.pixel(x, y, (i >= n - 1) ? green[0] : green[1]);
            } else if ((this.kind == STEM) && (i < this.bark)) {
                painter.pixel(x, y, OvergrowthArt.BARK_LIT);
                painter.pixel(x + this.thickX[i], y + this.thickY[i], OvergrowthArt.BARK_SHADE);
            } else {
                painter.pixel(x, y, (i >= this.green) ? green[1] : ((i % 3 != 0) ? green[3] : green[2]));
            }
        }
    }

    /**
     * Paints the leaves and flowers on top of the stems.
     */
    void paintDecor(@NotNull CanvasPainter painter) {
        if (!this.overlaps(painter)) return;
        for (int l = 0; l < this.leafIndex.length; l++) {
            int stage = this.leafStages[l];
            if (stage == LEAF_HIDDEN) continue;
            PixelSprite sprite = this.leafSprite(l, stage);
            painter.sprite(sprite, OvergrowthArt.LEAF_PALETTE[this.leafPalette[l]], this.leafX(l, sprite), this.leafY(l), this.leafFlip[l]);
        }
        for (int f = 0; f < this.blooms.length; f++) {
            int stage = this.bloomStages[f];
            if (stage != OvergrowthArt.BLOOM_HIDDEN) OvergrowthArt.paintBloom(painter, this.bloomX(f), this.bloomY(f), this.blooms[f].kind, stage);
        }
    }

    /**
     * Whether any part of the vine shows right now.
     */
    boolean isVisible() {
        return this.visible;
    }

    int grownPixels() {
        return this.grown;
    }

    int woodyPixels() {
        return this.bark;
    }

    int bloomStage(int bloom) {
        return this.bloomStages[bloom];
    }

}
