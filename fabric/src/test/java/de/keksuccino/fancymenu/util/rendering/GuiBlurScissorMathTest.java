package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiBlurScissorMathTest {

    @Test
    void roundsFractionalBoundsOutward() {
        GuiBlurScissorMath.LogicalBounds bounds = GuiBlurScissorMath.resolve(10.0F, 20.0F, 30.0F, 40.0F, 0.0F, 800.0D / 309.0D, 600, RenderRotationUtil.Rotation2D.identity());

        assertEquals(new GuiBlurScissorMath.LogicalBounds(9, 19, 41, 61), bounds);
    }

    @Test
    void includesRotatedAreaExtents() {
        RenderRotationUtil.Rotation2D quarterTurn = new RenderRotationUtil.Rotation2D(0.0F, -1.0F, 1.0F, 0.0F);

        assertEquals(new GuiBlurScissorMath.LogicalBounds(99, 89, 131, 121), GuiBlurScissorMath.resolve(100.0F, 100.0F, 30.0F, 10.0F, 0.0F, 2.0D, 600, quarterTurn));
    }

    @Test
    void invalidRotationFallsBackToIdentity() {
        RenderRotationUtil.Rotation2D invalid = new RenderRotationUtil.Rotation2D(Float.NaN, 0.0F, 0.0F, 1.0F);
        GuiBlurScissorMath.LogicalBounds expected = GuiBlurScissorMath.resolve(10.0F, 20.0F, 30.0F, 40.0F, 3.0F, 1.5D, 600, RenderRotationUtil.Rotation2D.identity());

        assertEquals(expected, GuiBlurScissorMath.resolve(10.0F, 20.0F, 30.0F, 40.0F, 3.0F, 1.5D, 600, invalid));
    }

    @Test
    void fullscreenBoundsStillCoverFramebufferAfterGuiScissorConversion() {
        int targetWidth = 800;
        int targetHeight = 450;
        double guiScale = 800.0D / 309.0D;
        int guiWidth = (int)Math.ceil(targetWidth / guiScale);
        int guiHeight = (int)Math.ceil(targetHeight / guiScale);
        GuiBlurScissorMath.LogicalBounds bounds = GuiBlurScissorMath.resolve(0.0F, 0.0F, guiWidth, guiHeight, 0.0F, guiScale, targetHeight, RenderRotationUtil.Rotation2D.identity());
        int framebufferX = (int)(bounds.minX() * guiScale);
        int framebufferY = (int)(targetHeight - bounds.maxY() * guiScale);
        int framebufferWidth = (int)((bounds.maxX() - bounds.minX()) * guiScale);
        int framebufferHeight = (int)((bounds.maxY() - bounds.minY()) * guiScale);

        assertTrue(framebufferX <= 0);
        assertTrue(framebufferY <= 0);
        assertTrue(framebufferX + framebufferWidth >= targetWidth);
        assertTrue(framebufferY + framebufferHeight >= targetHeight);
    }
}
