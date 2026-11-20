package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.overgrowth;

/**
 * One flower of the Overgrowth overlay, on a vine, on an element's moss or in the grass.<br>
 * A flower shows while its {@link #rank} is below the threshold of the flower amount (see {@link OvergrowthArt#rank}).
 * A flower that is let in later, by its option or by a higher amount, starts over as a bud.
 */
final class OvergrowthFlower {

    /** Where the flower sits: a pixel index along its vine, a column of its moss, or a column of the screen. */
    final int position;
    final int kind;
    final boolean small;
    /** Clock time at which the flower starts as a bud. */
    final float birth;
    /** Whether this is one of the spare flowers that only show above 100 %. */
    final boolean spare;
    /** Offset of the bloom from its spot, for flowers along vines. Mirrored with the vines of the right side. */
    int offsetX;
    final int offsetY;
    /** Height of its stem: pixels on moss, the full height in the grass. */
    final int stem;
    final float lean;
    final boolean leaf;
    float rank = 0.0F;
    /** Whether the flower showed at the last check. Unknown before the first check, which only takes over the state. */
    private int shown = -1;
    /** Clock time at which the flower got let in again, or 0. */
    private float letInAt = 0.0F;

    OvergrowthFlower(int position, int kind, boolean small, float birth, boolean spare, int offsetX, int offsetY, int stem, float lean, boolean leaf) {
        this.position = position;
        this.kind = kind;
        this.small = small;
        this.birth = birth;
        this.spare = spare;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.stem = stem;
        this.lean = lean;
        this.leaf = leaf;
    }

    /**
     * Returns the age of the flower at clock time {@code clock}, or 0 or less while it is hidden, and remembers whether it showed.
     * The first call only takes over the state, so a fresh overlay doesn't restart its flowers.
     *
     * @param show whether the options and the flower amount let the flower in
     */
    float age(float clock, boolean show) {
        if (this.shown < 0) {
            this.shown = show ? 1 : 0;
            this.letInAt = 0.0F;
        }
        if (!show) {
            this.shown = 0;
            return 0.0F;
        }
        if (this.shown == 0) {
            this.shown = 1;
            this.letInAt = clock - 0.05F;
        }
        // The part's clock started over, so the flower grows in with it
        if (this.letInAt > clock) this.letInAt = 0.0F;
        return clock - Math.max(this.birth, this.letInAt);
    }

    /**
     * Whether the amount threshold lets this flower in.
     *
     * @param threshold a third of the flower amount, 0 for no flowers
     */
    boolean isLetIn(float threshold) {
        return this.rank < threshold;
    }

}
