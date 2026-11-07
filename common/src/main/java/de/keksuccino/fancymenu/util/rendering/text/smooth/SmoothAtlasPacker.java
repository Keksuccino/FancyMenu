package de.keksuccino.fancymenu.util.rendering.text.smooth;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Shelf packer for glyph atlas pages. Glyphs go into horizontal shelves; a glyph uses the shelf whose height wastes the least
 * space, and a new shelf gets opened below the last one when no shelf fits. Shelf heights get rounded up a little, so glyphs
 * of similar height share shelves.
 */
final class SmoothAtlasPacker {

    static final long NO_SPACE = -1L;
    private static final int SHELF_HEIGHT_STEP = 4;

    private final int width;
    private final int height;
    private final IntArrayList shelfY = new IntArrayList();
    private final IntArrayList shelfHeight = new IntArrayList();
    private final IntArrayList shelfUsedWidth = new IntArrayList();
    private int nextShelfY;

    SmoothAtlasPacker(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Reserves a {@code width x height} area.
     *
     * @return the position packed as {@code (x << 32) | y}, or {@link #NO_SPACE} when the page is full
     */
    long allocate(int width, int height) {
        if (width <= 0 || height <= 0 || width > this.width || height > this.height) {
            return NO_SPACE;
        }
        int best = -1;
        int bestWaste = Integer.MAX_VALUE;
        for (int i = 0; i < this.shelfY.size(); i++) {
            int shelfHeight = this.shelfHeight.getInt(i);
            int waste = shelfHeight - height;
            // Tall shelves stay reserved for tall glyphs, so small glyphs don't fragment them.
            if (waste < 0 || waste > Math.max(SHELF_HEIGHT_STEP, height / 2) || this.shelfUsedWidth.getInt(i) + width > this.width) {
                continue;
            }
            if (waste < bestWaste) {
                best = i;
                bestWaste = waste;
            }
        }
        if (best < 0) {
            int shelfHeight = Math.min(this.height, ((height + SHELF_HEIGHT_STEP - 1) / SHELF_HEIGHT_STEP) * SHELF_HEIGHT_STEP);
            if (this.nextShelfY + shelfHeight > this.height) {
                return NO_SPACE;
            }
            best = this.shelfY.size();
            this.shelfY.add(this.nextShelfY);
            this.shelfHeight.add(shelfHeight);
            this.shelfUsedWidth.add(0);
            this.nextShelfY += shelfHeight;
        }
        int x = this.shelfUsedWidth.getInt(best);
        this.shelfUsedWidth.set(best, x + width);
        return ((long) x << 32) | (this.shelfY.getInt(best) & 0xFFFFFFFFL);
    }

    void reset() {
        this.shelfY.clear();
        this.shelfHeight.clear();
        this.shelfUsedWidth.clear();
        this.nextShelfY = 0;
    }

    static int x(long position) {
        return (int) (position >>> 32);
    }

    static int y(long position) {
        return (int) position;
    }

}
