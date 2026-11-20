package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The shared runtime atlas that holds every {@link PixelSprite} in every {@link PixelPalette} it gets drawn with.<br>
 * A sprite gets baked into a page the first time it is drawn with a palette, so all sprites of all overlays end up on a few
 * nearest-filtered textures and the GUI renderer can batch them. Baked sprites keep a transparent one-pixel gutter, so
 * neighbors never bleed into each other.<br><br>
 *
 * Variants nobody drew for a few seconds get evicted (for example the old colors after the user picked new ones).
 * When much of the pages is space of evicted variants, all pages get released and the variants still in use get baked again
 * when they are drawn next. All of this runs on the render thread; the maintenance runs on the client tick, between frames.
 */
public final class PixelAtlas {

    static final int PAGE_SIZE = 512;
    /** Transparent pixels around every baked sprite */
    static final int GUTTER = 1;
    /** 5 checks of 2 seconds: a variant gets evicted after it was not drawn for about 10 seconds. */
    static final int IDLE_RUNS_BEFORE_EVICTION = 5;

    private static final PixelAtlas INSTANCE = new PixelAtlas(PixelTextures.FACTORY);

    private final PixelTexture.Factory textureFactory;
    private final List<Page> pages = new ArrayList<>();
    private final List<Entry> entries = new ArrayList<>();
    private int[] bakeBuffer = new int[256];

    /**
     * There is one atlas for the whole game, see {@link #get()}. Baked variants live on the sprites themselves,
     * so a separate atlas (like in tests) must only ever see sprites no other atlas drew.
     */
    PixelAtlas(@NotNull PixelTexture.Factory textureFactory) {
        this.textureFactory = textureFactory;
    }

    @NotNull
    public static PixelAtlas get() {
        return INSTANCE;
    }

    /**
     * Returns the baked variant of the sprite in the palette, baking it first if needed.
     * Returns NULL if the sprite has no visible pixel in this palette, then there is nothing to draw.
     */
    @Nullable
    Entry resolve(@NotNull PixelSprite sprite, @NotNull PixelPalette palette) {
        Entry entry = findVariant(sprite, palette);
        if (entry == null) {
            entry = new Entry(sprite, palette);
            addVariant(sprite, entry);
            this.entries.add(entry);
        }
        entry.used = true;
        if (entry.empty) return null;
        if ((entry.page == null) || entry.page.texture.isClosed()) {
            if (entry.page != null) this.dropPage(entry.page);
            this.place(entry);
            if (entry.empty) return null;
        }
        return entry;
    }

    @Nullable
    private static Entry findVariant(@NotNull PixelSprite sprite, @NotNull PixelPalette palette) {
        Entry[] variants = sprite.variants;
        int count = sprite.variantCount;
        // Identity first: overlays almost always draw with the same palette instances
        for (int i = 0; i < count; i++) {
            if (variants[i].palette == palette) return variants[i];
        }
        for (int i = 0; i < count; i++) {
            if (variants[i].palette.equals(palette)) return variants[i];
        }
        return null;
    }

    private static void addVariant(@NotNull PixelSprite sprite, @NotNull Entry entry) {
        if (sprite.variantCount == sprite.variants.length) {
            Entry[] grown = new Entry[Math.max(2, sprite.variants.length * 2)];
            System.arraycopy(sprite.variants, 0, grown, 0, sprite.variantCount);
            sprite.variants = grown;
        }
        sprite.variants[sprite.variantCount++] = entry;
    }

    private static void removeVariant(@NotNull PixelSprite sprite, @NotNull Entry entry) {
        Entry[] variants = sprite.variants;
        for (int i = 0; i < sprite.variantCount; i++) {
            if (variants[i] == entry) {
                System.arraycopy(variants, i + 1, variants, i, sprite.variantCount - i - 1);
                variants[--sprite.variantCount] = null;
                return;
            }
        }
    }

    private void place(@NotNull Entry entry) {
        PixelSprite sprite = entry.sprite;
        int w = sprite.width();
        int h = sprite.height();
        int blockWidth = w + GUTTER * 2;
        int blockHeight = h + GUTTER * 2;
        int pixelCount = blockWidth * blockHeight;
        if (this.bakeBuffer.length < pixelCount) this.bakeBuffer = new int[Math.max(pixelCount, this.bakeBuffer.length * 2)];
        int[] buffer = this.bakeBuffer;
        // The gutter stays transparent
        Arrays.fill(buffer, 0, pixelCount, 0);
        boolean visible = sprite.bakeInto(entry.palette, buffer, GUTTER * blockWidth + GUTTER, blockWidth);
        if (!visible) {
            entry.empty = true;
            entry.page = null;
            return;
        }
        Page page = null;
        int packed = PixelAtlasPacker.NO_ROOM;
        for (int i = this.pages.size() - 1; i >= 0; i--) {
            Page candidate = this.pages.get(i);
            if (candidate.texture.isClosed()) continue;
            packed = candidate.packer.place(blockWidth, blockHeight);
            if (packed != PixelAtlasPacker.NO_ROOM) {
                page = candidate;
                break;
            }
        }
        if (page == null) {
            // Sprites bigger than a normal page get a page of their own
            int pageWidth = Math.max(PAGE_SIZE, blockWidth);
            int pageHeight = Math.max(PAGE_SIZE, blockHeight);
            page = new Page(this.textureFactory.create("atlas", pageWidth, pageHeight), pageWidth, pageHeight);
            this.pages.add(page);
            packed = page.packer.place(blockWidth, blockHeight);
        }
        int blockX = PixelAtlasPacker.unpackX(packed);
        int blockY = PixelAtlasPacker.unpackY(packed);
        page.texture.write(buffer, 0, blockWidth, blockX, blockY, blockWidth, blockHeight);
        page.texture.upload(blockY, blockY + blockHeight);
        page.usedArea += pixelCount;
        entry.page = page;
        entry.u = blockX + GUTTER;
        entry.v = blockY + GUTTER;
        entry.area = pixelCount;
    }

    private void dropPage(@NotNull Page page) {
        page.texture.close();
        this.pages.remove(page);
        for (Entry entry : this.entries) {
            if (entry.page == page) entry.page = null;
        }
    }

    /**
     * Evicts variants nobody drew for a while and rebuilds the pages when most of their space belongs to evicted variants.
     * Gets called every few seconds by {@link PixelTextures}, between frames.
     */
    void maintain() {
        long liveArea = 0L;
        for (int i = this.entries.size() - 1; i >= 0; i--) {
            Entry entry = this.entries.get(i);
            if (entry.used) {
                entry.used = false;
                entry.idleRuns = 0;
            } else if (++entry.idleRuns >= IDLE_RUNS_BEFORE_EVICTION) {
                this.entries.remove(i);
                removeVariant(entry.sprite, entry);
                entry.page = null;
                continue;
            }
            if (entry.page != null) liveArea += entry.area;
        }
        long pageArea = 0L;
        for (Page page : this.pages) pageArea += (long)page.width * page.height;
        boolean noLiveSprites = (liveArea == 0L) && !this.pages.isEmpty();
        // A page whose shelves are full can't take new sprites anymore, so a lot of evicted space means new pages instead of reused ones
        boolean mostlyEvicted = (this.pages.size() > 1) && (liveArea * 2L < pageArea) && (this.getEvictedArea() * 2L > pageArea);
        if (noLiveSprites || mostlyEvicted) this.releasePages();
    }

    private long getEvictedArea() {
        long used = 0L;
        long live = 0L;
        for (Page page : this.pages) used += page.usedArea;
        for (Entry entry : this.entries) {
            if (entry.page != null) live += entry.area;
        }
        return used - live;
    }

    /**
     * Releases all pages. Variants still in use get baked into new pages the next time they are drawn.
     */
    void releasePages() {
        for (Page page : this.pages) page.texture.close();
        this.pages.clear();
        for (Entry entry : this.entries) entry.page = null;
    }

    int getPageCount() {
        return this.pages.size();
    }

    int getEntryCount() {
        return this.entries.size();
    }

    static final class Entry {

        final PixelSprite sprite;
        final PixelPalette palette;
        @Nullable
        Page page;
        int u;
        int v;
        /** Area of the baked block on its page, gutter included */
        int area;
        boolean empty = false;
        boolean used = true;
        int idleRuns = 0;

        private Entry(@NotNull PixelSprite sprite, @NotNull PixelPalette palette) {
            this.sprite = sprite;
            this.palette = palette;
        }

    }

    static final class Page {

        final PixelTexture texture;
        final int width;
        final int height;
        final PixelAtlasPacker packer;
        /** Area of all blocks ever placed on this page, evicted ones included */
        long usedArea = 0L;

        private Page(@NotNull PixelTexture texture, int width, int height) {
            this.texture = texture;
            this.width = width;
            this.height = height;
            this.packer = new PixelAtlasPacker(width, height);
        }

    }

}
