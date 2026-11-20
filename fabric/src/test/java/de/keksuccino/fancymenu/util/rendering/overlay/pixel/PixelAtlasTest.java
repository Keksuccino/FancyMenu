package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every test builds its own sprites, because baked variants live on the sprite and belong to one atlas.
 */
class PixelAtlasTest {

    private static final PixelPalette RED = PixelPalette.builder().rgb('x', 0xFF0000).build();
    private static final PixelPalette BLUE = PixelPalette.builder().rgb('x', 0x0000FF).build();

    @Test
    void bakesTheSpriteIntoThePageWithATransparentGutter() {
        RecordingPixelTexture.Factory factory = new RecordingPixelTexture.Factory();
        PixelAtlas atlas = new PixelAtlas(factory);
        PixelSprite sprite = PixelSprite.of("x.", "xx");

        PixelAtlas.Entry entry = atlas.resolve(sprite, RED);

        assertNotNull(entry);
        RecordingPixelTexture page = factory.last();
        assertEquals(PixelAtlas.PAGE_SIZE, page.width);
        int[] expected = sprite.bake(RED);
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) assertEquals(expected[y * 2 + x], page.pixel(entry.u + x, entry.v + y));
        }
        // The gutter around the sprite stays transparent
        for (int x = -1; x <= 2; x++) {
            assertEquals(0, page.pixel(entry.u + x, entry.v - 1));
            assertEquals(0, page.pixel(entry.u + x, entry.v + 2));
        }
        assertEquals(1, page.uploads.size());
        assertEquals(entry.v - PixelAtlas.GUTTER, page.uploads.get(0)[0]);
        assertEquals(entry.v + 2 + PixelAtlas.GUTTER, page.uploads.get(0)[1]);
    }

    @Test
    void drawingAgainReusesTheBakedVariant() {
        RecordingPixelTexture.Factory factory = new RecordingPixelTexture.Factory();
        PixelAtlas atlas = new PixelAtlas(factory);
        PixelSprite sprite = PixelSprite.of("xx");

        PixelAtlas.Entry first = atlas.resolve(sprite, RED);
        PixelAtlas.Entry again = atlas.resolve(sprite, RED);
        PixelAtlas.Entry equalPalette = atlas.resolve(sprite, PixelPalette.builder().rgb('x', 0xFF0000).build());

        assertSame(first, again);
        assertSame(first, equalPalette);
        assertEquals(1, factory.last().uploads.size());
        assertEquals(1, atlas.getEntryCount());
    }

    @Test
    void everyPaletteGetsItsOwnVariantOnTheSamePage() {
        RecordingPixelTexture.Factory factory = new RecordingPixelTexture.Factory();
        PixelAtlas atlas = new PixelAtlas(factory);
        PixelSprite sprite = PixelSprite.of("x");

        PixelAtlas.Entry red = atlas.resolve(sprite, RED);
        PixelAtlas.Entry blue = atlas.resolve(sprite, BLUE);

        assertNotNull(red);
        assertNotNull(blue);
        assertNotSame(red, blue);
        assertSame(red.page, blue.page);
        assertEquals(1, factory.created.size());
        assertEquals(0xFF0000FF, factory.last().pixel(blue.u, blue.v));
        assertEquals(0xFFFF0000, factory.last().pixel(red.u, red.v));
    }

    @Test
    void invisibleSpritesNeedNoPage() {
        RecordingPixelTexture.Factory factory = new RecordingPixelTexture.Factory();
        PixelAtlas atlas = new PixelAtlas(factory);

        assertNull(atlas.resolve(PixelSprite.of("..", ".."), RED));
        assertNull(atlas.resolve(PixelSprite.of("y"), RED));
        assertTrue(factory.created.isEmpty());
    }

    @Test
    void spritesBiggerThanAPageGetTheirOwnPage() {
        RecordingPixelTexture.Factory factory = new RecordingPixelTexture.Factory();
        PixelAtlas atlas = new PixelAtlas(factory);
        char[] codes = new char[600 * 2];
        Arrays.fill(codes, 'x');

        PixelAtlas.Entry entry = atlas.resolve(PixelSprite.fromCodes(600, 2, codes), RED);

        assertNotNull(entry);
        assertEquals(602, factory.last().width);
    }

    @Test
    void unusedVariantsGetEvictedAndTheirPagesReleased() {
        RecordingPixelTexture.Factory factory = new RecordingPixelTexture.Factory();
        PixelAtlas atlas = new PixelAtlas(factory);
        PixelSprite kept = PixelSprite.of("x");
        PixelSprite dropped = PixelSprite.of("xx");
        atlas.resolve(kept, RED);
        atlas.resolve(dropped, RED);

        // A fresh variant counts as used once, so it takes one check more than the idle limit
        for (int run = 0; run <= PixelAtlas.IDLE_RUNS_BEFORE_EVICTION; run++) {
            atlas.resolve(kept, RED);
            atlas.maintain();
        }

        assertEquals(1, atlas.getEntryCount());
        assertEquals(0, dropped.variantCount);
        assertEquals(1, kept.variantCount);
        assertFalse(factory.last().closed);

        for (int run = 0; run <= PixelAtlas.IDLE_RUNS_BEFORE_EVICTION; run++) atlas.maintain();

        assertEquals(0, atlas.getEntryCount());
        assertEquals(0, atlas.getPageCount());
        assertTrue(factory.last().closed);
    }

    @Test
    void pagesMostlyFullOfEvictedVariantsGetRebuilt() {
        RecordingPixelTexture.Factory factory = new RecordingPixelTexture.Factory();
        PixelAtlas atlas = new PixelAtlas(factory);
        // Two pages of big sprites that get evicted, and one small sprite that stays in use
        char[] codes = new char[500 * 300];
        Arrays.fill(codes, 'x');
        PixelSprite big = PixelSprite.fromCodes(500, 300, codes);
        PixelSprite small = PixelSprite.of("x");
        atlas.resolve(big, RED);
        atlas.resolve(big, BLUE);
        atlas.resolve(small, RED);
        assertEquals(2, atlas.getPageCount());

        for (int run = 0; run <= PixelAtlas.IDLE_RUNS_BEFORE_EVICTION; run++) {
            atlas.resolve(small, RED);
            atlas.maintain();
        }

        assertEquals(0, atlas.getPageCount());
        assertTrue(factory.created.stream().allMatch(texture -> texture.closed));
        PixelAtlas.Entry rebaked = atlas.resolve(small, RED);
        assertNotNull(rebaked);
        assertEquals(1, atlas.getPageCount());
        assertEquals(0xFFFF0000, factory.last().pixel(rebaked.u, rebaked.v));
    }

    @Test
    void aPageReleasedFromOutsideGetsBakedAgain() {
        RecordingPixelTexture.Factory factory = new RecordingPixelTexture.Factory();
        PixelAtlas atlas = new PixelAtlas(factory);
        PixelSprite sprite = PixelSprite.of("x");
        atlas.resolve(sprite, RED);
        RecordingPixelTexture firstPage = factory.last();

        firstPage.close();
        PixelAtlas.Entry entry = atlas.resolve(sprite, RED);

        assertNotNull(entry);
        assertEquals(2, factory.created.size());
        assertNotSame(firstPage, entry.page.texture);
        assertEquals(0xFFFF0000, factory.last().pixel(entry.u, entry.v));
    }

    @Test
    void packerFillsShelvesAndReportsAFullPage() {
        PixelAtlasPacker packer = new PixelAtlasPacker(10, 6);
        int first = packer.place(4, 3);
        int second = packer.place(4, 2);
        int third = packer.place(4, 3);
        assertEquals(0, PixelAtlasPacker.unpackX(first));
        assertEquals(0, PixelAtlasPacker.unpackY(first));
        assertEquals(4, PixelAtlasPacker.unpackX(second));
        assertEquals(0, PixelAtlasPacker.unpackY(second));
        // Doesn't fit next to the others anymore, so it starts the next shelf below the tallest block
        assertEquals(0, PixelAtlasPacker.unpackX(third));
        assertEquals(3, PixelAtlasPacker.unpackY(third));
        assertEquals(PixelAtlasPacker.NO_ROOM, packer.place(4, 4));
        assertEquals(PixelAtlasPacker.NO_ROOM, packer.place(11, 1));
    }

}
