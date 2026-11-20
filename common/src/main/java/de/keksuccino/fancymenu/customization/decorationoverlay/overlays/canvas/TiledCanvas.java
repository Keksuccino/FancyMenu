package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

/**
 * A canvas for pixel overlays whose art covers only parts of the screen and changes in small places at a time, like vines, ice or
 * ash drifts. It is split into tiles: a tile only exists once something got painted into it, every tile is one {@link PixelCanvas}
 * (one texture, one quad), and every tile uploads only its own changed rows. A vine swaying in one corner never uploads the rest
 * of the screen, and empty parts of the screen cost no memory.<br><br>
 *
 * Coordinates are screen coordinates in GUI pixels. The canvas covers {@code width x height} pixels from its origin.<br>
 * Art gets into it in two ways:
 * <ul>
 *     <li>pixel by pixel with {@link #set} and {@link #get}, for art whose pixels change one at a time (like ice)</li>
 *     <li>retained: {@link #invalidate} marks areas whose content changed, and {@link #repaint} clears every marked area and lets the
 *     owner paint all of its art that overlaps the area again, in its normal order (for art made of overlapping items, like a vine
 *     with its leaves)</li>
 * </ul>
 * Like every canvas, it must be drawn once per frame at most, and its textures must be released when the screen closes.
 * Everything here runs on the render thread.
 */
public final class TiledCanvas {

    /** Size of the tiles of screen-wide canvases. Small enough for cheap uploads, big enough to keep the quad count low. */
    public static final int DEFAULT_TILE_SIZE = 64;

    @NotNull
    private final String name;
    private int originX = 0;
    private int originY = 0;
    private int width = 0;
    private int height = 0;
    private int tileWidth = DEFAULT_TILE_SIZE;
    private int tileHeight = DEFAULT_TILE_SIZE;
    private int columns = 0;
    private int rows = 0;
    /** Row-major, NULL where nothing got painted yet */
    @NotNull
    private PixelCanvas[] tiles = new PixelCanvas[0];
    /** Per tile the invalidated area waiting for a repaint, as x0, y0, x1, y1 in screen coordinates (x0 >= x1 means none) */
    @NotNull
    private int[] pending = new int[0];
    private boolean anyPending = false;

    /**
     * @param name a short name for debugging
     */
    public TiledCanvas(@NotNull String name) {
        this.name = name;
    }

    /**
     * Places and sizes the canvas. A new placement or size drops all tiles, so everything has to be painted again.
     *
     * @return true if anything changed
     */
    public boolean resize(int originX, int originY, int width, int height, int tileWidth, int tileHeight) {
        width = Math.max(0, width);
        height = Math.max(0, height);
        tileWidth = Math.max(1, tileWidth);
        tileHeight = Math.max(1, tileHeight);
        if ((originX == this.originX) && (originY == this.originY) && (width == this.width) && (height == this.height) && (tileWidth == this.tileWidth) && (tileHeight == this.tileHeight)) return false;
        this.close();
        this.originX = originX;
        this.originY = originY;
        this.width = width;
        this.height = height;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.columns = (width + tileWidth - 1) / tileWidth;
        this.rows = (height + tileHeight - 1) / tileHeight;
        this.tiles = new PixelCanvas[this.columns * this.rows];
        this.pending = new int[this.columns * this.rows * 4];
        this.clearPending();
        return true;
    }

    /**
     * Sizes the canvas to cover the whole screen with tiles of the {@link #DEFAULT_TILE_SIZE default size}.
     */
    public boolean resizeToScreen(int screenWidth, int screenHeight) {
        return this.resize(0, 0, screenWidth, screenHeight, DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE);
    }

    public int originX() {
        return this.originX;
    }

    public int originY() {
        return this.originY;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    /**
     * Returns the pixel at a screen position, 0 (transparent) outside of the canvas or in a tile that doesn't exist.
     */
    public int get(int x, int y) {
        x -= this.originX;
        y -= this.originY;
        if ((x < 0) || (y < 0) || (x >= this.width) || (y >= this.height)) return 0;
        PixelCanvas tile = this.tiles[(y / this.tileHeight) * this.columns + (x / this.tileWidth)];
        return (tile != null) ? tile.get(x % this.tileWidth, y % this.tileHeight) : 0;
    }

    /**
     * Replaces the pixel at a screen position. Pixels outside of the canvas are ignored.
     */
    public void set(int x, int y, int argb) {
        x -= this.originX;
        y -= this.originY;
        if ((x < 0) || (y < 0) || (x >= this.width) || (y >= this.height)) return;
        int index = (y / this.tileHeight) * this.columns + (x / this.tileWidth);
        PixelCanvas tile = this.tiles[index];
        if (tile == null) {
            // Nothing to erase where nothing got painted yet
            if (argb == 0) return;
            tile = this.createTile(index);
        }
        tile.set(x % this.tileWidth, y % this.tileHeight, argb);
    }

    /**
     * Paints a translucent ARGB color over the pixel at a screen position. Pixels outside of the canvas are ignored.
     */
    public void blend(int x, int y, int argb) {
        int alpha = argb >>> 24;
        if (alpha == 0) return;
        if (alpha == 255) {
            this.set(x, y, argb);
            return;
        }
        x -= this.originX;
        y -= this.originY;
        if ((x < 0) || (y < 0) || (x >= this.width) || (y >= this.height)) return;
        int index = (y / this.tileHeight) * this.columns + (x / this.tileWidth);
        PixelCanvas tile = this.tiles[index];
        if (tile == null) tile = this.createTile(index);
        int localX = x % this.tileWidth;
        int localY = y % this.tileHeight;
        tile.set(localX, localY, PixelMath.blendOver(tile.get(localX, localY), argb));
    }

    /**
     * Makes a screen area transparent. Area bounds: x0, y0 inclusive, x1, y1 exclusive.
     */
    public void clear(int x0, int y0, int x1, int y1) {
        int lx0 = Math.max(0, x0 - this.originX);
        int ly0 = Math.max(0, y0 - this.originY);
        int lx1 = Math.min(this.width, x1 - this.originX);
        int ly1 = Math.min(this.height, y1 - this.originY);
        if ((lx1 <= lx0) || (ly1 <= ly0)) return;
        for (int row = ly0 / this.tileHeight; row <= (ly1 - 1) / this.tileHeight; row++) {
            for (int column = lx0 / this.tileWidth; column <= (lx1 - 1) / this.tileWidth; column++) {
                PixelCanvas tile = this.tiles[row * this.columns + column];
                if (tile == null) continue;
                int tileX = column * this.tileWidth;
                int tileY = row * this.tileHeight;
                int fromX = Math.max(lx0, tileX) - tileX;
                int fromY = Math.max(ly0, tileY) - tileY;
                int toX = Math.min(lx1, tileX + this.tileWidth) - tileX;
                int toY = Math.min(ly1, tileY + this.tileHeight) - tileY;
                tile.fillRect(fromX, fromY, toX - fromX, toY - fromY, 0);
            }
        }
    }

    /**
     * Makes the whole canvas transparent, without dropping its tiles.
     */
    public void clearAll() {
        for (PixelCanvas tile : this.tiles) {
            if (tile != null) tile.clear();
        }
        this.clearPending();
    }

    /**
     * Marks a screen area whose content changed, for the next {@link #repaint}. Area bounds: x0, y0 inclusive, x1, y1 exclusive.
     */
    public void invalidate(int x0, int y0, int x1, int y1) {
        int lx0 = Math.max(0, x0 - this.originX);
        int ly0 = Math.max(0, y0 - this.originY);
        int lx1 = Math.min(this.width, x1 - this.originX);
        int ly1 = Math.min(this.height, y1 - this.originY);
        if ((lx1 <= lx0) || (ly1 <= ly0)) return;
        for (int row = ly0 / this.tileHeight; row <= (ly1 - 1) / this.tileHeight; row++) {
            for (int column = lx0 / this.tileWidth; column <= (lx1 - 1) / this.tileWidth; column++) {
                int tileX = column * this.tileWidth;
                int tileY = row * this.tileHeight;
                int ax0 = Math.max(lx0, tileX) + this.originX;
                int ay0 = Math.max(ly0, tileY) + this.originY;
                int ax1 = Math.min(lx1, tileX + this.tileWidth) + this.originX;
                int ay1 = Math.min(ly1, tileY + this.tileHeight) + this.originY;
                int p = (row * this.columns + column) * 4;
                if (this.pending[p] >= this.pending[p + 2]) {
                    this.pending[p] = ax0;
                    this.pending[p + 1] = ay0;
                    this.pending[p + 2] = ax1;
                    this.pending[p + 3] = ay1;
                } else {
                    if (ax0 < this.pending[p]) this.pending[p] = ax0;
                    if (ay0 < this.pending[p + 1]) this.pending[p + 1] = ay0;
                    if (ax1 > this.pending[p + 2]) this.pending[p + 2] = ax1;
                    if (ay1 > this.pending[p + 3]) this.pending[p + 3] = ay1;
                }
                this.anyPending = true;
            }
        }
    }

    /**
     * Marks the whole canvas for the next {@link #repaint}.
     */
    public void invalidateAll() {
        this.invalidate(this.originX, this.originY, this.originX + this.width, this.originY + this.height);
    }

    /**
     * Whether some area waits for a repaint.
     */
    public boolean hasPending() {
        return this.anyPending;
    }

    /**
     * Repaints every invalidated area, one tile at a time: clears it and lets the content paint everything that overlaps it again,
     * with the painter clipped to it.
     */
    public void repaint(@NotNull CanvasPainter painter, @NotNull Content content) {
        if (!this.anyPending) return;
        this.anyPending = false;
        for (int index = 0; index < this.tiles.length; index++) {
            int p = index * 4;
            int x0 = this.pending[p];
            int x1 = this.pending[p + 2];
            if (x0 >= x1) continue;
            int y0 = this.pending[p + 1];
            int y1 = this.pending[p + 3];
            this.pending[p] = 0;
            this.pending[p + 2] = 0;
            this.clear(x0, y0, x1, y1);
            painter.begin(this, index, x0, y0, x1, y1);
            content.paint(painter);
            painter.end();
        }
    }

    /**
     * Returns a tile by its index, creating it if needed. For the painter, which writes into the tile it repaints.
     */
    @NotNull
    PixelCanvas tile(int index) {
        PixelCanvas tile = this.tiles[index];
        return (tile != null) ? tile : this.createTile(index);
    }

    /**
     * Screen column of the left edge of a tile.
     */
    int tileX(int index) {
        return this.originX + (index % this.columns) * this.tileWidth;
    }

    /**
     * Screen row of the top edge of a tile.
     */
    int tileY(int index) {
        return this.originY + (index / this.columns) * this.tileHeight;
    }

    /**
     * Draws all tiles, uploading their changes first.
     */
    public void draw(@NotNull PixelGraphics graphics) {
        for (int index = 0; index < this.tiles.length; index++) {
            PixelCanvas tile = this.tiles[index];
            if (tile == null) continue;
            graphics.canvas(tile, this.tileX(index), this.tileY(index));
        }
    }

    /**
     * Number of tiles that exist, for checks.
     */
    public int tileCount() {
        int count = 0;
        for (PixelCanvas tile : this.tiles) {
            if (tile != null) count++;
        }
        return count;
    }

    /**
     * Releases the textures of all tiles. Their pixels stay and get uploaded again when the canvas gets drawn next.
     */
    public void releaseTextures() {
        for (PixelCanvas tile : this.tiles) {
            if (tile != null) tile.releaseTexture();
        }
    }

    /**
     * Drops all tiles and their textures.
     */
    public void close() {
        for (int i = 0; i < this.tiles.length; i++) {
            PixelCanvas tile = this.tiles[i];
            if (tile == null) continue;
            tile.close();
            this.tiles[i] = null;
        }
    }

    @NotNull
    private PixelCanvas createTile(int index) {
        int column = index % this.columns;
        int row = index / this.columns;
        PixelCanvas tile = new PixelCanvas(this.name + "_" + column + "_" + row);
        tile.resize(Math.min(this.tileWidth, this.width - column * this.tileWidth), Math.min(this.tileHeight, this.height - row * this.tileHeight));
        this.tiles[index] = tile;
        return tile;
    }

    private void clearPending() {
        Arrays.fill(this.pending, 0);
        this.anyPending = false;
    }

    /**
     * The art of a retained canvas. It paints all of its items that overlap the painter's clip area, in its normal drawing order.
     */
    @FunctionalInterface
    public interface Content {

        void paint(@NotNull CanvasPainter painter);

    }

}
