package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The expected values come from the helpers of the overlay redesign mockup (JavaScript), so ported overlays behave like the mockup.
 */
class PixelMathTest {

    private static final float EPSILON = 0.000001F;

    @Test
    void randomNumbersFollowTheMockupSequence() {
        PixelRandom random = new PixelRandom(7);
        float[] mockup = {0.0117048F, 0.0619583F, 0.9769076F, 0.6990287F, 0.5214453F};
        for (float expected : mockup) assertEquals(expected, random.next(), 0.0000002F);
    }

    @Test
    void randomRangesStayInsideTheirBounds() {
        PixelRandom random = new PixelRandom(42);
        for (int i = 0; i < 10_000; i++) {
            float value = random.next();
            assertTrue((value >= 0.0F) && (value < 1.0F));
            int whole = random.rangeInt(-2, 3);
            assertTrue((whole >= -2) && (whole <= 3));
            float ranged = random.range(5.0F, 6.0F);
            assertTrue((ranged >= 5.0F) && (ranged < 6.0F));
            assertTrue(random.index(4) < 4);
        }
        assertEquals(7, random.rangeInt(7, 7));
    }

    @Test
    void latticeHashAndValueNoiseMatchTheMockup() {
        assertEquals(0.0F, PixelMath.latticeHash(0, 0), EPSILON);
        assertEquals(0.5081245F, PixelMath.latticeHash(1, 0), EPSILON);
        assertEquals(0.7682746F, PixelMath.latticeHash(0, 1), EPSILON);
        assertEquals(0.9973462F, PixelMath.latticeHash(-3, 5), EPSILON);
        assertEquals(0.0181374F, PixelMath.latticeHash(123, -77), EPSILON);
        assertEquals(0.485063F, PixelMath.valueNoise(0.5F, 0.5F), 0.00001F);
        assertEquals(0.247327F, PixelMath.valueNoise(1.25F, 3.75F), 0.00001F);
        assertEquals(0.905793F, PixelMath.valueNoise(-2.3F, 4.1F), 0.00001F);
        assertEquals(0.225679F, PixelMath.valueNoise(10.0F, 10.0F), 0.00001F);
    }

    @Test
    void triangleWaveAndHashMatchTheMockup() {
        assertEquals(0.0F, PixelMath.tri(0.0F), EPSILON);
        assertEquals(0.5F, PixelMath.tri((float)(Math.PI / 2.0)), EPSILON);
        assertEquals(1.0F, PixelMath.tri((float)Math.PI), EPSILON);
        assertEquals(0.726760F, PixelMath.tri(4.0F), 0.00001F);
        assertEquals(0.318310F, PixelMath.tri(-1.0F), 0.00001F);
        assertEquals(0.0F, PixelMath.hash(0.0F, 0.0F), EPSILON);
        assertEquals(0.020844F, PixelMath.hash(1.0F, 31.0F), 0.00001F);
        assertEquals(0.073054F, PixelMath.hash(17.0F, 5.0F), 0.00001F);
        assertEquals(0.395665F, PixelMath.hash(3.5F, 2.0F), 0.00001F);
    }

    @Test
    void colorHelpers() {
        assertEquals(0xFF808080, PixelMath.mix(0xFF000000, 0xFFFFFFFF, 0.5F));
        assertEquals(0x00FF0000, PixelMath.mix(0x00FF0000, 0xFFFF0000, 0.0F));
        assertEquals(0xFF112233, PixelMath.opaque(0x00112233));
        assertEquals(0x40112233, PixelMath.withAlpha(0xFF112233, 0x40));
        assertEquals(0x80112233, PixelMath.scaleAlpha(0xFF112233, 0.5F));
        assertEquals(-3, PixelMath.floor(-2.5F));
        assertEquals(2, PixelMath.floor(2.9F));
        assertEquals(0, PixelMath.bayer4(0, 0));
        assertEquals(5, PixelMath.bayer4(3, 3));
    }

}
