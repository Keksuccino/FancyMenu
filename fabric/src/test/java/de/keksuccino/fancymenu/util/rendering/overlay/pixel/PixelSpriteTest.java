package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PixelSpriteTest {

    private static final int A = 0xFF112233;
    private static final int B = 0x80445566;
    private static final PixelPalette PALETTE = PixelPalette.builder().argb('a', A).rgb('b', 0x445566, 0x80).build();

    // a b .
    // . a a
    private static final PixelSprite SPRITE = PixelSprite.of("ab.", ".aa");

    @Test
    void rowsAndPaletteBakeToArgbPixels() {
        assertEquals(3, SPRITE.width());
        assertEquals(2, SPRITE.height());
        assertArrayEquals(new int[]{A, B, 0, 0, A, A}, SPRITE.bake(PALETTE));
    }

    @Test
    void charactersWithoutColorAreTransparent() {
        PixelSprite sprite = PixelSprite.of("a.z");
        assertArrayEquals(new int[]{A, 0, 0}, sprite.bake(PALETTE));
    }

    @Test
    void flipsMirrorThePixels() {
        assertArrayEquals(new int[]{0, B, A, A, A, 0}, SPRITE.bake(PALETTE, true, false, 1));
        assertArrayEquals(new int[]{0, A, A, A, B, 0}, SPRITE.bake(PALETTE, false, true, 1));
        assertArrayEquals(new int[]{A, A, 0, 0, B, A}, SPRITE.bake(PALETTE, true, true, 1));
    }

    @Test
    void upscaleKeepsEveryArtPixelSquare() {
        int[] scaled = SPRITE.bake(PALETTE, false, false, 2);
        int[] expected = {
                A, A, B, B, 0, 0,
                A, A, B, B, 0, 0,
                0, 0, A, A, A, A,
                0, 0, A, A, A, A};
        assertArrayEquals(expected, scaled);
        int[] scaledFlipped = SPRITE.bake(PALETTE, true, false, 3);
        assertEquals(9 * 6, scaledFlipped.length);
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 9; x++) {
                int artX = 2 - x / 3;
                int artY = y / 3;
                assertEquals(SPRITE.bake(PALETTE)[artY * 3 + artX], scaledFlipped[y * 9 + x], "pixel " + x + "," + y);
            }
        }
    }

    @Test
    void rotatesClockwise() {
        PixelSprite rotated = SPRITE.rotatedClockwise();
        assertEquals(2, rotated.width());
        assertEquals(3, rotated.height());
        // . a
        // a b
        // a .
        assertArrayEquals(new int[]{0, A, A, B, A, 0}, rotated.bake(PALETTE));
    }

    @Test
    void codesBuildTheSameSpriteAsRows() {
        PixelSprite fromCodes = PixelSprite.fromCodes(3, 2, "ab..aa".toCharArray());
        assertArrayEquals(SPRITE.bake(PALETTE), fromCodes.bake(PALETTE));
        assertEquals('b', fromCodes.codeAt(1, 0));
    }

    @Test
    void rejectsBrokenDefinitions() {
        assertThrows(IllegalArgumentException.class, () -> PixelSprite.of("ab", "a"));
        assertThrows(IllegalArgumentException.class, PixelSprite::of);
        assertThrows(IllegalArgumentException.class, () -> PixelSprite.of("aé"));
        assertThrows(IllegalArgumentException.class, () -> PixelSprite.fromCodes(2, 2, new char[3]));
        assertThrows(IllegalArgumentException.class, () -> SPRITE.bake(PALETTE, false, false, 0));
    }

    @Test
    void palettesCompareByTheirColors() {
        PixelPalette same = PixelPalette.builder().argb('a', A).argb('b', B).build();
        assertEquals(PALETTE, same);
        assertEquals(PALETTE.hashCode(), same.hashCode());
        PixelPalette changed = PALETTE.withColor('a', 0xFFFFFFFF);
        assertNotEquals(PALETTE, changed);
        assertEquals(0xFFFFFFFF, changed.color('a'));
        assertEquals(A, PALETTE.color('a'));
        assertEquals(0, PALETTE.color('q'));
        assertEquals(0xFF445566, PixelPalette.builder().rgb('c', 0x12445566).build().color('c'));
    }

}
