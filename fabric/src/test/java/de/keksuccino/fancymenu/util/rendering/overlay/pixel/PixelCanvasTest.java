package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PixelCanvasTest {

    private static final int RED = 0xFFFF0000;

    @Test
    void anEmptyCanvasHasNothingToDraw() {
        RecordingPixelTexture.Factory factory = new RecordingPixelTexture.Factory();
        PixelCanvas canvas = new PixelCanvas("test", factory);
        assertNull(canvas.flush());
        assertTrue(factory.created.isEmpty());
    }

    @Test
    void aBlankCanvasNeedsNoUpload() {
        RecordingPixelTexture.Factory factory = new RecordingPixelTexture.Factory();
        PixelCanvas canvas = new PixelCanvas("test", factory);
        canvas.resize(8, 4);
        canvas.clear();

        RecordingPixelTexture texture = (RecordingPixelTexture)canvas.flush();

        assertEquals(1, factory.created.size());
        assertEquals(8, texture.width);
        assertTrue(texture.uploads.isEmpty());
    }

    @Test
    void uploadsOnlyTheChangedRowsAndOnlyOnce() {
        RecordingPixelTexture.Factory factory = new RecordingPixelTexture.Factory();
        PixelCanvas canvas = new PixelCanvas("test", factory);
        canvas.resize(8, 6);
        canvas.flush();
        RecordingPixelTexture texture = factory.last();

        canvas.set(2, 1, RED);
        canvas.fillRect(5, 3, 2, 2, RED);
        assertTrue(canvas.isDirty());
        assertSame(texture, canvas.flush());

        assertEquals(1, texture.writes.size());
        // The union of both changes: x 2..7, y 1..5
        assertArrayEquals(new int[]{2, 1, 5, 4}, texture.writes.get(0));
        assertArrayEquals(new int[]{1, 5}, texture.uploads.get(0));
        assertEquals(RED, texture.pixel(2, 1));
        assertEquals(RED, texture.pixel(6, 4));
        assertFalse(canvas.isDirty());

        canvas.flush();
        assertEquals(1, texture.uploads.size());
    }

    @Test
    void directWritesNeedMarkDirty() {
        RecordingPixelTexture.Factory factory = new RecordingPixelTexture.Factory();
        PixelCanvas canvas = new PixelCanvas("test", factory);
        canvas.resize(4, 4);
        canvas.flush();

        canvas.pixels()[2 * 4 + 1] = RED;
        assertFalse(canvas.isDirty());
        canvas.markDirty(1, 2, 1, 1);
        canvas.flush();

        assertEquals(RED, factory.last().pixel(1, 2));
        // The direct write counts as content, so clearing has to upload transparent pixels again
        canvas.clear();
        assertTrue(canvas.isDirty());
        canvas.flush();
        assertEquals(0, factory.last().pixel(1, 2));
    }

    @Test
    void clearingTwiceUploadsOnce() {
        RecordingPixelTexture.Factory factory = new RecordingPixelTexture.Factory();
        PixelCanvas canvas = new PixelCanvas("test", factory);
        canvas.resize(4, 4);
        canvas.fill(RED);
        canvas.flush();
        canvas.clear();
        canvas.flush();
        canvas.clear();
        canvas.flush();

        assertEquals(2, factory.last().uploads.size());
    }

    @Test
    void resizeClearsAndUploadsToANewTexture() {
        RecordingPixelTexture.Factory factory = new RecordingPixelTexture.Factory();
        PixelCanvas canvas = new PixelCanvas("test", factory);
        canvas.resize(4, 4);
        canvas.fill(RED);
        canvas.flush();
        RecordingPixelTexture old = factory.last();

        assertFalse(canvas.resize(4, 4));
        assertTrue(canvas.resize(6, 3));

        assertTrue(old.closed);
        assertEquals(0, canvas.get(0, 0));
        assertEquals(6 * 3, canvas.pixels().length);
        RecordingPixelTexture resized = (RecordingPixelTexture)canvas.flush();
        assertNotSame(old, resized);
        assertEquals(6, resized.width);
        assertEquals(3, resized.height);
    }

    @Test
    void aTextureReleasedFromOutsideGetsEverythingUploadedAgain() {
        RecordingPixelTexture.Factory factory = new RecordingPixelTexture.Factory();
        PixelCanvas canvas = new PixelCanvas("test", factory);
        canvas.resize(4, 2);
        canvas.set(3, 1, RED);
        canvas.flush();
        factory.last().close();

        RecordingPixelTexture again = (RecordingPixelTexture)canvas.flush();

        assertEquals(2, factory.created.size());
        assertArrayEquals(new int[]{0, 0, 4, 2}, again.writes.get(0));
        assertEquals(RED, again.pixel(3, 1));
    }

    @Test
    void writesOutsideOfTheCanvasAreIgnored() {
        RecordingPixelTexture.Factory factory = new RecordingPixelTexture.Factory();
        PixelCanvas canvas = new PixelCanvas("test", factory);
        canvas.resize(4, 4);
        canvas.set(-1, 0, RED);
        canvas.set(4, 2, RED);
        canvas.fillRect(10, 10, 5, 5, RED);
        assertFalse(canvas.isDirty());
        canvas.fillRect(-2, -2, 3, 3, RED);
        assertEquals(RED, canvas.get(0, 0));
        assertEquals(0, canvas.get(1, 1));
    }

    @Test
    void stampedSpritesMatchTheBakedSprite() {
        PixelPalette palette = PixelPalette.builder().rgb('a', 0x102030).rgb('b', 0x405060).build();
        PixelSprite sprite = PixelSprite.of("ab.", ".ba");
        PixelCanvas canvas = new PixelCanvas("test", new RecordingPixelTexture.Factory());
        canvas.resize(20, 10);

        canvas.stamp(sprite, palette, 4, 2, true, true, 3);

        int[] baked = sprite.bake(palette, true, true, 3);
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 9; x++) assertEquals(baked[y * 9 + x], canvas.get(4 + x, 2 + y), "pixel " + x + "," + y);
        }
        assertEquals(0, canvas.get(3, 2));
        assertEquals(0, canvas.get(13, 2));
    }

    @Test
    void translucentColorsBlendOverThePixel() {
        PixelCanvas canvas = new PixelCanvas("test", new RecordingPixelTexture.Factory());
        canvas.resize(2, 1);
        canvas.blend(0, 0, 0x80FF0000);
        assertEquals(0x80FF0000, canvas.get(0, 0));
        canvas.set(1, 0, 0xFF000000);
        canvas.blend(1, 0, 0x80FFFFFF);
        int mixed = canvas.get(1, 0);
        assertEquals(0xFF, mixed >>> 24);
        assertEquals(0x80, mixed & 0xFF, 1);
    }

}
