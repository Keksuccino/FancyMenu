package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownRenderColorTest {

    @Test
    void opaqueColorGetsTheOpacityOfTheTextAsItsAlpha() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer();
        DrawableColor color = DrawableColor.of(10, 20, 30);

        assertEquals(0xFF0A141E, renderer.getRenderColor(color));
        renderer.setTextOpacity(0.5F);
        assertEquals(0x7F0A141E, renderer.getRenderColor(color));
        renderer.setTextOpacity(0.0F);
        assertEquals(0x000A141E, renderer.getRenderColor(color));
    }

    @Test
    void translucentColorKeepsItsOwnAlpha() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer();
        DrawableColor color = DrawableColor.of(10, 20, 30, 128);

        assertEquals(0x800A141E, renderer.getRenderColor(color));
        renderer.setTextOpacity(0.5F);
        assertEquals(0x400A141E, renderer.getRenderColor(color));
    }

    @Test
    void fullyTransparentColorStaysInvisibleAtEveryOpacity() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer();

        assertEquals(0, renderer.getRenderColor(DrawableColor.FULLY_TRANSPARENT) >>> 24);
        renderer.setTextOpacity(0.5F);
        assertEquals(0, renderer.getRenderColor(DrawableColor.FULLY_TRANSPARENT) >>> 24);
    }

}
