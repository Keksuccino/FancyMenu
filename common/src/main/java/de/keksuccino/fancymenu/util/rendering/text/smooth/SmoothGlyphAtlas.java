package de.keksuccino.fancymenu.util.rendering.text.smooth;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * GPU glyph cache of one {@link SmoothFont}: all font files, sizes and styles share fixed-size RGBA pages, so text that mixes
 * fonts (like emoji in a sentence) still renders from a single texture most of the time.
 * <p>
 * Pages never grow. When all pages are full, the least recently used page that was idle for a while gets cleared and reused;
 * that keeps memory bounded when GUI or UI scales change and old glyph sizes are no longer needed. Glyphs are uploaded one by
 * one when they get rasterized; the atlas keeps no CPU copy of the pages.
 * </p>
 */
final class SmoothGlyphAtlas implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();

    static final int DEFAULT_PAGE_SIZE = 1024;
    private static final int SOFT_PAGE_LIMIT = 8;
    private static final long PAGE_IDLE_NANOS = 3_000_000_000L;
    /** Every page starts with a small opaque white block, used to draw underlines and strikethroughs in the same batch as glyphs. */
    private static final int WHITE_BLOCK_SIZE = 4;
    private static final long ICON_KEY_FLAG = 1L << 40;

    private final String debugName;
    private final int pageSize;
    private final Long2ObjectOpenHashMap<Glyph> glyphs = new Long2ObjectOpenHashMap<>();
    private final List<Page> pages = new ArrayList<>();
    @Nullable
    private ByteBuffer staging;
    private boolean warnedAboutPageLimit;

    /**
     * @param pageSize edge length of the pages in pixels; fonts that only draw a few glyphs (like icon fonts) save memory with smaller pages
     */
    SmoothGlyphAtlas(@NotNull String debugName, int pageSize) {
        this.debugName = debugName;
        this.pageSize = pageSize;
    }

    /**
     * Packs everything that identifies a rasterized glyph into one cache key.
     *
     * @param sizePixels raster size in pixels per em ({@code 1..1023})
     * @param subpixelBin horizontal subpixel position ({@code 0..3}, quarter pixels)
     */
    static long key(int source, int glyphId, int styleIndex, int sizePixels, int subpixelBin) {
        return (glyphId & 0x1FFFFFL) | ((long) (source & 0x1F) << 21) | ((long) (styleIndex & 3) << 26) | ((long) (subpixelBin & 3) << 28) | ((long) (sizePixels & 0x3FF) << 30);
    }

    /**
     * Cache key of a glyph rasterized as icon by {@link SmoothGlyphRasterizer#rasterizeIcon}. Icon bitmaps are sized by their ink
     * instead of their em square, so they get their own key space.
     *
     * @param inkPixels size of the larger side of the glyph's ink box in pixels ({@code 1..1023})
     */
    static long iconKey(int source, int glyphId, int inkPixels) {
        return key(source, glyphId, 0, inkPixels, 0) | ICON_KEY_FLAG;
    }

    @Nullable
    Glyph find(long key) {
        return this.glyphs.get(key);
    }

    /**
     * Uploads a rasterized glyph and remembers it under the given key.
     */
    @NotNull
    Glyph add(long key, @NotNull SmoothGlyphRasterizer.GlyphBitmap bitmap) {
        if (bitmap.isEmpty()) {
            this.glyphs.put(key, Glyph.EMPTY);
            return Glyph.EMPTY;
        }
        Page page = null;
        long position = SmoothAtlasPacker.NO_SPACE;
        for (Page candidate : this.pages) {
            position = candidate.packer.allocate(bitmap.width(), bitmap.height());
            if (position != SmoothAtlasPacker.NO_SPACE) {
                page = candidate;
                break;
            }
        }
        if (page == null) {
            page = this.obtainEmptyPage();
            position = page.packer.allocate(bitmap.width(), bitmap.height());
            if (position == SmoothAtlasPacker.NO_SPACE) {
                // Bitmaps never get larger than a page (raster sizes are capped), so this only happens for invalid input.
                this.glyphs.put(key, Glyph.EMPTY);
                return Glyph.EMPTY;
            }
        }
        int x = SmoothAtlasPacker.x(position);
        int y = SmoothAtlasPacker.y(position);
        this.upload(page, bitmap, x, y);
        float size = page.size;
        Glyph glyph = new Glyph(page, x / size, y / size, (x + bitmap.width()) / size, (y + bitmap.height()) / size, bitmap.left(), bitmap.top(), bitmap.width(), bitmap.height(), bitmap.color());
        page.glyphKeys.add(key);
        this.glyphs.put(key, glyph);
        return glyph;
    }

    /**
     * A page for drawing decorations when a text has no glyph that already picked one.
     */
    @NotNull
    Page anyPage() {
        return this.pages.isEmpty() ? this.obtainEmptyPage() : this.pages.get(0);
    }

    @NotNull
    private Page obtainEmptyPage() {
        long now = System.nanoTime();
        if (this.pages.size() >= SOFT_PAGE_LIMIT) {
            Page leastRecentlyUsed = null;
            for (Page page : this.pages) {
                if ((now - page.lastUsedNanos) > PAGE_IDLE_NANOS && (leastRecentlyUsed == null || page.lastUsedNanos < leastRecentlyUsed.lastUsedNanos)) {
                    leastRecentlyUsed = page;
                }
            }
            if (leastRecentlyUsed != null) {
                this.recycle(leastRecentlyUsed);
                return leastRecentlyUsed;
            }
            if (!this.warnedAboutPageLimit) {
                this.warnedAboutPageLimit = true;
                LOGGER.warn("[FANCYMENU] Smooth font '{}' uses more than {} glyph atlas pages at once.", this.debugName, SOFT_PAGE_LIMIT);
            }
        }
        int size = Math.min(this.pageSize, SmoothGlyphTexture.maxSize());
        Page page = new Page(new SmoothGlyphTexture("fancymenu_smooth_font_" + this.debugName + "_" + this.pages.size(), size), size, now);
        this.reserveWhiteBlock(page);
        this.pages.add(page);
        return page;
    }

    private void recycle(@NotNull Page page) {
        for (int i = 0; i < page.glyphKeys.size(); i++) {
            this.glyphs.remove(page.glyphKeys.getLong(i));
        }
        page.glyphKeys.clear();
        page.packer.reset();
        page.texture.clear();
        this.reserveWhiteBlock(page);
    }

    private void reserveWhiteBlock(@NotNull Page page) {
        long position = page.packer.allocate(WHITE_BLOCK_SIZE, WHITE_BLOCK_SIZE);
        int x = SmoothAtlasPacker.x(position);
        int y = SmoothAtlasPacker.y(position);
        ByteBuffer buffer = this.staging(WHITE_BLOCK_SIZE * WHITE_BLOCK_SIZE * 4);
        for (int i = 0; i < WHITE_BLOCK_SIZE * WHITE_BLOCK_SIZE; i++) {
            buffer.putInt(i * 4, 0xFFFFFFFF);
        }
        page.texture.upload(buffer, x, y, WHITE_BLOCK_SIZE, WHITE_BLOCK_SIZE);
        page.whiteU = (x + (WHITE_BLOCK_SIZE / 2.0F)) / page.size;
        page.whiteV = (y + (WHITE_BLOCK_SIZE / 2.0F)) / page.size;
    }

    private void upload(@NotNull Page page, @NotNull SmoothGlyphRasterizer.GlyphBitmap bitmap, int x, int y) {
        int pixelCount = bitmap.width() * bitmap.height();
        ByteBuffer buffer = this.staging(pixelCount * 4);
        byte[] coverage = bitmap.coverage();
        int[] argb = bitmap.argb();
        // The buffer is little-endian, so an int written as 0xAABBGGRR lands in memory as the bytes R, G, B, A.
        if (coverage != null) {
            for (int i = 0; i < pixelCount; i++) {
                buffer.putInt(i * 4, ((coverage[i] & 0xFF) << 24) | 0x00FFFFFF);
            }
        } else if (argb != null) {
            for (int i = 0; i < pixelCount; i++) {
                int color = argb[i];
                buffer.putInt(i * 4, (color & 0xFF00FF00) | ((color >> 16) & 0xFF) | ((color & 0xFF) << 16));
            }
        }
        page.texture.upload(buffer, x, y, bitmap.width(), bitmap.height());
    }

    /**
     * Returns the reusable direct upload buffer, limited to {@code bytes}.
     */
    @NotNull
    private ByteBuffer staging(int bytes) {
        ByteBuffer buffer = this.staging;
        if (buffer == null || buffer.capacity() < bytes) {
            if (buffer != null) {
                MemoryUtil.memFree(buffer);
            }
            buffer = MemoryUtil.memAlloc(Math.max(bytes, 64 * 64 * 4)).order(ByteOrder.LITTLE_ENDIAN);
            this.staging = buffer;
        }
        buffer.clear().limit(bytes);
        return buffer;
    }

    @Override
    public void close() {
        for (Page page : this.pages) {
            page.texture.close();
        }
        this.pages.clear();
        this.glyphs.clear();
        if (this.staging != null) {
            MemoryUtil.memFree(this.staging);
            this.staging = null;
        }
    }

    static final class Page {

        final SmoothGlyphTexture texture;
        final int size;
        final SmoothAtlasPacker packer;
        final LongArrayList glyphKeys = new LongArrayList();
        long lastUsedNanos;
        float whiteU;
        float whiteV;

        private Page(@NotNull SmoothGlyphTexture texture, int size, long now) {
            this.texture = texture;
            this.size = size;
            this.packer = new SmoothAtlasPacker(size, size);
            this.lastUsedNanos = now;
        }

    }

    /**
     * A glyph in the atlas: its texture coordinates and its pixel placement relative to the pen position on the baseline.
     * The empty glyph (whitespace) has no page.
     */
    record Glyph(@Nullable Page page, float u0, float v0, float u1, float v1, int left, int top, int width, int height, boolean color) {

        static final Glyph EMPTY = new Glyph(null, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0, 0, 0, false);

    }

}
