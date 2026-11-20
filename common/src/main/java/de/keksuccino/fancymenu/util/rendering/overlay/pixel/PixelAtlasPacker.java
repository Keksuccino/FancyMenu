package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

/**
 * Places blocks on one atlas page in shelves: blocks go left to right in a row as high as the highest block of that row,
 * and a block that doesn't fit anymore starts the next row. Space of removed blocks is not reused; {@link PixelAtlas}
 * rebuilds its pages from scratch when too much of them is unused.
 */
final class PixelAtlasPacker {

    static final int NO_ROOM = -1;

    private final int width;
    private final int height;
    private int cursorX = 0;
    private int shelfY = 0;
    private int shelfHeight = 0;

    PixelAtlasPacker(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Finds room for a block and returns its position as {@code (x << 16) | y}, or {@link #NO_ROOM} if the page is full.
     */
    int place(int blockWidth, int blockHeight) {
        if ((blockWidth > this.width) || (blockHeight > this.height)) return NO_ROOM;
        if (this.cursorX + blockWidth > this.width) {
            this.shelfY += this.shelfHeight;
            this.cursorX = 0;
            this.shelfHeight = 0;
        }
        if (this.shelfY + blockHeight > this.height) return NO_ROOM;
        int x = this.cursorX;
        int y = this.shelfY;
        this.cursorX += blockWidth;
        this.shelfHeight = Math.max(this.shelfHeight, blockHeight);
        return (x << 16) | y;
    }

    static int unpackX(int packed) {
        return packed >>> 16;
    }

    static int unpackY(int packed) {
        return packed & 0xFFFF;
    }

}
