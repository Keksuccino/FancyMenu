package de.keksuccino.fancymenu.customization.decorationoverlay.overlays;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Only checks the canvas side of the layer: drawing straight to the screen needs the game's graphics.
 */
class ParticleLayerTest {

    private static PixelGraphics graphics(int width, int height) {
        // The layer only asks for the screen size while it draws into its canvas, so no game graphics are needed
        return new PixelGraphics().begin(null, width, height);
    }

    @Test
    void manyQuadsGoIntoTheCanvasAndFewGoBackToPlainQuads() {
        ParticleLayer layer = new ParticleLayer(new PixelCanvas("test"));
        PixelGraphics graphics = graphics(64, 32);
        layer.begin(graphics, ParticleLayer.CANVAS_ABOVE + 1);
        assertTrue(layer.isCanvasMode());
        // Hysteresis: a count between both limits keeps the current way
        layer.begin(graphics, ParticleLayer.DIRECT_BELOW + 1);
        assertTrue(layer.isCanvasMode());
        layer.begin(graphics, ParticleLayer.DIRECT_BELOW - 1);
        assertFalse(layer.isCanvasMode());
        layer.begin(graphics, ParticleLayer.CANVAS_ABOVE);
        assertFalse(layer.isCanvasMode());
    }

    @Test
    void theCanvasStartsEveryFrameEmptyAndBlendsInDrawingOrder() {
        PixelCanvas canvas = new PixelCanvas("test");
        ParticleLayer layer = new ParticleLayer(canvas);
        PixelGraphics graphics = graphics(64, 32);
        PixelSprite sprite = PixelSprite.of("ab");
        PixelPalette palette = PixelPalette.builder().rgb('a', 0xFF0000).rgb('b', 0x0000FF, 128).build();

        layer.begin(graphics, 1000);
        assertEquals(64, canvas.width());
        layer.sprite(sprite, palette, 10, 5, 2);
        layer.rect(0, 0, 2, 2, 0xFF00FF00);
        assertEquals(0xFFFF0000, canvas.get(11, 6));
        assertEquals(0x800000FF, canvas.get(12, 5));
        assertEquals(0xFF00FF00, canvas.get(1, 1));

        layer.begin(graphics, 1000);
        assertEquals(0, canvas.get(11, 6));
        assertEquals(0, canvas.get(1, 1));
    }

}
