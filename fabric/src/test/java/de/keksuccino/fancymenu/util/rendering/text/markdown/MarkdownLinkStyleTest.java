package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownUnderlineMode;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownLinkStyleTest {

    private static final int CHAR_WIDTH = 6;

    @ParameterizedTest
    @ValueSource(strings = {"https://example.invalid", "click:some_event", "hover:some_event"})
    void classicLookUnderlinesLinksInTheirOwnColorWhileTheMouseIsOverThem(String target) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("a [first second](" + target + ") b", 400);
        renderer.tick();
        MarkdownTextFragment first = renderer.fragmentContaining("first");

        assertFalse(first.getRenderComponent().getStyle().isUnderlined());
        assertNull(MarkdownLinkPainter.getUnderlineColor(first));

        renderer.fragmentContaining("second").setHovered(true);

        // The underline is part of the text, so the font draws it
        assertTrue(first.getRenderComponent().getStyle().isUnderlined());
        assertEquals(0x0771FC, first.getRenderComponent().getStyle().getColor().getValue());
        assertNull(MarkdownLinkPainter.getUnderlineColor(first));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.invalid", "click:some_event", "hover:some_event"})
    void pixelLookAlwaysUnderlinesLinksInADimmerColorAndLightsThemUpUnderTheMouse(String target) {
        MarkdownTestRenderer renderer = pixelRenderer("a [first second](" + target + ") b");
        renderer.tick();
        MarkdownTextFragment first = renderer.fragmentContaining("first");
        Component component = first.getRenderComponent();

        assertEquals(0x7FD8FF, component.getStyle().getColor().getValue());
        assertFalse(component.getStyle().isUnderlined());
        assertSame(renderer.getStyle().linkUnderlineColor, MarkdownLinkPainter.getUnderlineColor(first));
        assertEquals(0xFF3E7C9E, renderer.getStyle().linkUnderlineColor.argb());

        renderer.fragmentContaining("second").setHovered(true);

        assertNotSame(component, first.getRenderComponent());
        assertEquals(0xFFFFFF, first.getRenderComponent().getStyle().getColor().getValue());
        assertFalse(first.getRenderComponent().getStyle().isUnderlined());
        assertSame(renderer.getStyle().linkHoverUnderlineColor, MarkdownLinkPainter.getUnderlineColor(first));
        assertEquals(0xFF7FD8FF, renderer.getStyle().linkHoverUnderlineColor.argb());
    }

    @Test
    void underlineModeDecidesWhenLinksAreUnderlined() {
        MarkdownTestRenderer renderer = pixelRenderer("[link](https://example.invalid)");
        renderer.tick();
        MarkdownTextFragment link = renderer.fragmentContaining("link");

        renderer.getStyle().linkUnderline.set(MarkdownUnderlineMode.NEVER);
        assertNull(MarkdownLinkPainter.getUnderlineColor(link));
        link.setHovered(true);
        assertNull(MarkdownLinkPainter.getUnderlineColor(link));
        link.setHovered(false);

        renderer.getStyle().linkUnderline.set(MarkdownUnderlineMode.ON_HOVER);
        assertNull(MarkdownLinkPainter.getUnderlineColor(link));
        link.setHovered(true);
        assertSame(renderer.getStyle().linkHoverUnderlineColor, MarkdownLinkPainter.getUnderlineColor(link));

        // Without a hover color of its own, the underline keeps its color under the mouse
        renderer.getStyle().linkHoverUnderlineColor.set(null);
        assertSame(renderer.getStyle().linkUnderlineColor, MarkdownLinkPainter.getUnderlineColor(link));

        // Without any color of its own, the underline is part of the text again
        renderer.getStyle().linkUnderlineColor.set(null);
        assertNull(MarkdownLinkPainter.getUnderlineColor(link));
        assertTrue(link.getRenderComponent().getStyle().isUnderlined());
        renderer.getStyle().linkUnderline.set(MarkdownUnderlineMode.NEVER);
        assertFalse(link.getRenderComponent().getStyle().isUnderlined());
    }

    @Test
    void underlineEndsWhereTheTextOfTheLinkEnds() {
        MarkdownTestRenderer renderer = pixelRenderer("a [first second](https://example.invalid) b");
        renderer.tick();
        MarkdownTextFragment first = renderer.fragmentContaining("first");
        MarkdownTextFragment second = renderer.fragmentContaining("second");
        assertEquals("first ", first.text);
        assertEquals("second ", second.text);

        // The Minecraft font ends every letter with an empty column, which is no part of the underline. The space between the words of a link is.
        assertEquals((6 * CHAR_WIDTH) - 1, MarkdownLinkPainter.getUnderlineWidth(first));
        assertEquals((6 * CHAR_WIDTH) - 1, MarkdownLinkPainter.getUnderlineWidth(second));

        renderer.getStyle().trimGlyphSpacing.set(false);
        renderer.setTextBaseScale(2.0F);
        renderer.tick();

        assertEquals(6 * CHAR_WIDTH * 2, MarkdownLinkPainter.getUnderlineWidth(renderer.fragmentContaining("second")));
    }

    @Test
    void underlineEndsBehindTheLastLetterOfALineTheLinkWrapsAt() {
        // The line has room for "a first" and the space behind it, but not for the second word of the link
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("a [first second](https://example.invalid) b", 80);
        renderer.setParentRenderScale(3.0F);
        renderer.getStyle().applyLook(MarkdownLook.PIXEL);
        renderer.setAutoLineBreakingEnabled(true);
        renderer.tick();
        MarkdownTextFragment first = renderer.fragmentContaining("first");
        MarkdownTextFragment second = renderer.fragmentContaining("second");

        assertTrue(first.autoLineBreakAfter);
        assertNotSame(first.parentLine, second.parentLine);
        assertEquals((5 * CHAR_WIDTH) - 1, MarkdownLinkPainter.getUnderlineWidth(first));
        assertEquals((6 * CHAR_WIDTH) - 1, MarkdownLinkPainter.getUnderlineWidth(second));
    }

    @Test
    void hoverColorIsOptional() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("[link](https://example.invalid)", 400);
        renderer.tick();
        MarkdownTextFragment link = renderer.fragmentContaining("link");

        renderer.getStyle().linkHoverColor.set(DrawableColor.of(1, 2, 3));
        assertEquals(0x0771FC, link.getRenderComponent().getStyle().getColor().getValue());
        link.setHovered(true);
        assertEquals(0x010203, link.getRenderComponent().getStyle().getColor().getValue());
    }

    private static MarkdownTestRenderer pixelRenderer(String text) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, 400);
        renderer.setParentRenderScale(3.0F);
        renderer.getStyle().applyLook(MarkdownLook.PIXEL);
        return renderer;
    }

}
