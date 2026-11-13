package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderingUtilsColorTest {

    @Test
    void fullOpacityLeavesAColorExactlyAsItIs() {
        assertEquals(0xFF336699, RenderingUtils.multiplyAlphaInColor(0xFF336699, 1.0F));
        assertEquals(0x80336699, RenderingUtils.multiplyAlphaInColor(0x80336699, 1.0F));
        assertEquals(-1, RenderingUtils.multiplyAlphaInColor(-1, 1.0F));
        // More than fully opaque is fully opaque
        assertEquals(0x80336699, RenderingUtils.multiplyAlphaInColor(0x80336699, 3.0F));
    }

    @Test
    void opacityScalesTheAlphaAColorAlreadyHas() {
        assertEquals(0x7F336699, RenderingUtils.multiplyAlphaInColor(0xFF336699, 0.5F));
        assertEquals(0x40336699, RenderingUtils.multiplyAlphaInColor(0x80336699, 0.5F));
        assertEquals(0x7FFFFFFF, RenderingUtils.multiplyAlphaInColor(-1, 0.5F));
        assertEquals(0x00336699, RenderingUtils.multiplyAlphaInColor(0x00336699, 0.5F));
    }

    @Test
    void noOpacityTakesAllAlphaAndKeepsTheColor() {
        assertEquals(0x00336699, RenderingUtils.multiplyAlphaInColor(0xFF336699, 0.0F));
        // Less than fully transparent is fully transparent
        assertEquals(0x00336699, RenderingUtils.multiplyAlphaInColor(0xFF336699, -2.0F));
    }

}
