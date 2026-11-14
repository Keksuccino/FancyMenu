package de.keksuccino.fancymenu.util.rendering;

import org.joml.Matrix3x2f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiBlurPlanTest {

    private static final int TARGET_WIDTH = 1923;
    private static final int TARGET_HEIGHT = 1081;

    @ParameterizedTest
    @ValueSource(floats = {1.0F, 3.0F, 8.0F, 24.0F, 64.0F, 180.0F, 256.0F})
    void everyStageCoversAllTexelsTheNextStageSamples(float radius) {
        GuiBlurKernel.Kernel kernel = GuiBlurKernel.resolve(radius);
        for (int[] composite : new int[][]{{400, 300, 640, 420}, {0, 0, 37, 1081}, {1900, 1000, 1923, 1081}, {961, 17, 962, 18}}) {
            GuiBlurPlan plan = new GuiBlurPlan();
            assertTrue(plan.prepare(new GuiBlurPlan.Region().set(composite[0], composite[1], composite[2], composite[3]), kernel, TARGET_WIDTH, TARGET_HEIGHT));
            assertCompositeSamplesAreCovered(plan);
            assertGaussianInputsAreCovered(plan);
            assertDownsampleInputsAreCovered(plan);
        }
    }

    @Test
    void regionsStayInsideTheirLevels() {
        GuiBlurPlan plan = new GuiBlurPlan();
        GuiBlurKernel.Kernel kernel = GuiBlurKernel.resolve(96.0F);
        assertTrue(plan.prepare(new GuiBlurPlan.Region().set(-50, -50, TARGET_WIDTH + 50, TARGET_HEIGHT + 50), kernel, TARGET_WIDTH, TARGET_HEIGHT));

        assertRegionInside(plan.composite(), TARGET_WIDTH, TARGET_HEIGHT);
        for (int level = 1; level <= kernel.level(); level++) {
            assertEquals(GuiBlurPlan.levelSize(TARGET_WIDTH, level), plan.levelWidth(level));
            assertRegionInside(plan.downsample(level), plan.levelWidth(level), plan.levelHeight(level));
        }
        assertRegionInside(plan.horizontal(), plan.levelWidth(kernel.level()), plan.levelHeight(kernel.level()));
        assertRegionInside(plan.vertical(), plan.levelWidth(kernel.level()), plan.levelHeight(kernel.level()));
    }

    @Test
    void smallAreasOnlyTouchTexelsNearThem() {
        GuiBlurPlan plan = new GuiBlurPlan();
        GuiBlurKernel.Kernel kernel = GuiBlurKernel.resolve(24.0F);
        assertTrue(plan.prepare(new GuiBlurPlan.Region().set(800, 500, 900, 540), kernel, TARGET_WIDTH, TARGET_HEIGHT));

        // The kernel reaches about 2 sigma-widths past the area, so a small area must not plan a near-full-screen region.
        GuiBlurPlan.Region firstLevel = plan.downsample(1);
        assertTrue(firstLevel.width() * 2 < 100 + 8 * radiusReach(24.0F));
        assertTrue(firstLevel.height() * 2 < 40 + 8 * radiusReach(24.0F));
    }

    @Test
    void emptyCompositeRegionPlansNothing() {
        GuiBlurPlan plan = new GuiBlurPlan();

        assertFalse(plan.prepare(new GuiBlurPlan.Region().set(TARGET_WIDTH + 10, 0, TARGET_WIDTH + 20, 10), GuiBlurKernel.resolve(8.0F), TARGET_WIDTH, TARGET_HEIGHT));
    }

    @Test
    void compositeRegionCoversTheAreaWithAnAntialiasingPixel() {
        GuiBlurPlan.Region region = new GuiBlurPlan.Region();

        assertTrue(GuiBlurPlan.resolveCompositeRegion(region, 10.0F, 20.0F, 30.0F, 40.0F, 1.0F, 0.0F, 0.0F, 1.0F, 2.0D, 400, 300));

        // GUI y runs down, the framebuffer region runs up: 300 - (20 + 40) * 2 = 180.
        assertEquals(19, region.x());
        assertEquals(179, region.y());
        assertEquals(62, region.width());
        assertEquals(82, region.height());
    }

    @Test
    void rotatedCompositeRegionCoversTheRotatedMask() {
        GuiBlurPlan.Region region = new GuiBlurPlan.Region();
        float angle = (float) Math.toRadians(45.0D);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        assertTrue(GuiBlurPlan.resolveCompositeRegion(region, 100.0F, 100.0F, 100.0F, 20.0F, cos, -sin, sin, cos, 1.0D, 1000, 1000));

        // A 100x20 box rotated by 45 degrees spans (100 + 20) * cos(45) around its center (150, 110).
        float halfExtent = 120.0F * cos * 0.5F;
        assertTrue(region.x() <= 150.0F - halfExtent && region.x() + region.width() >= 150.0F + halfExtent);
        assertTrue(region.y() <= 1000.0F - 110.0F - halfExtent && region.y() + region.height() >= 1000.0F - 110.0F + halfExtent);
    }

    @Test
    void tiltedCompositeRegionCoversTheMaskWithoutGrowingPastIt() {
        GuiBlurPlan.Region region = new GuiBlurPlan.Region();
        // A 200x20 box around (500, 500), rotated and tilted like an element
        Matrix3x2f pose = new Matrix3x2f();
        RenderRotationUtil.rotateAndTilt(pose, 500.0F, 500.0F, 315.0F, -60.0F, 30.0F);
        RenderRotationUtil.Rotation2D mask = GuiPoseTransformUtil.resolve(pose).rotation();

        assertTrue(GuiBlurPlan.resolveCompositeRegion(region, 400.0F, 490.0F, 200.0F, 20.0F, mask.m00(), mask.m01(), mask.m10(), mask.m11(), 1.0D, 1000, 1000));

        float extentX = (Math.abs(pose.m00()) * 100.0F) + (Math.abs(pose.m10()) * 10.0F);
        float extentY = (Math.abs(pose.m01()) * 100.0F) + (Math.abs(pose.m11()) * 10.0F);
        assertTrue(region.x() <= 500.0F - extentX && region.x() + region.width() >= 500.0F + extentX);
        assertTrue(region.y() <= 500.0F - extentY && region.y() + region.height() >= 500.0F + extentY);
        // The region joins the untransformed box with the mask on the screen, plus the antialiasing pixels around both
        assertTrue(region.width() <= (2.0F * Math.max(100.0F, extentX)) + 4.0F);
        assertTrue(region.height() <= (2.0F * Math.max(10.0F, extentY)) + 4.0F);
    }

    @Test
    void fractionalGuiScaleStillCoversTheWholeFramebuffer() {
        GuiBlurPlan.Region region = new GuiBlurPlan.Region();

        // 625 GUI pixels at a 1.152 GUI scale are exactly 720 framebuffer pixels, which float math alone could round inward.
        assertTrue(GuiBlurPlan.resolveCompositeRegion(region, 0.0F, 0.0F, 625.0F, 625.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.152D, 720, 720));

        assertEquals(0, region.x());
        assertEquals(0, region.y());
        assertEquals(720, region.width());
        assertEquals(720, region.height());
    }

    @Test
    void guiScissorClipsTheCompositeRegion() {
        GuiBlurPlan.Region region = new GuiBlurPlan.Region();
        assertTrue(GuiBlurPlan.resolveCompositeRegion(region, 0.0F, 0.0F, 200.0F, 150.0F, 1.0F, 0.0F, 0.0F, 1.0F, 2.0D, 400, 300));

        assertTrue(GuiBlurPlan.clipToGuiScissor(region, 50, 25, 100, 75, 2.0D, 400, 300));

        assertEquals(100, region.x());
        assertEquals(150, region.y());
        assertEquals(100, region.width());
        assertEquals(100, region.height());
    }

    @Test
    void guiScissorOutsideTheAreaLeavesNothing() {
        GuiBlurPlan.Region region = new GuiBlurPlan.Region();
        assertTrue(GuiBlurPlan.resolveCompositeRegion(region, 0.0F, 0.0F, 20.0F, 20.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0D, 400, 300));

        assertFalse(GuiBlurPlan.clipToGuiScissor(region, 100, 100, 200, 200, 1.0D, 400, 300));
    }

    @Test
    void areaOutsideTheTargetResolvesToNothing() {
        GuiBlurPlan.Region region = new GuiBlurPlan.Region();

        assertFalse(GuiBlurPlan.resolveCompositeRegion(region, 500.0F, 10.0F, 20.0F, 20.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0D, 400, 300));
    }

    private static void assertCompositeSamplesAreCovered(GuiBlurPlan plan) {
        int scale = plan.kernel().levelScale();
        int level = plan.kernel().level();
        GuiBlurPlan.Region composite = plan.composite();
        for (int x = composite.x(); x < composite.x() + composite.width(); x += Math.max(1, composite.width() / 7)) {
            for (int y = composite.y(); y < composite.y() + composite.height(); y += Math.max(1, composite.height() / 7)) {
                assertSplineFootprintCovered(plan, scale, level, x, y);
            }
            assertSplineFootprintCovered(plan, scale, level, x, composite.y() + composite.height() - 1);
        }
        assertSplineFootprintCovered(plan, scale, level, composite.x() + composite.width() - 1, composite.y() + composite.height() - 1);
    }

    private static void assertSplineFootprintCovered(GuiBlurPlan plan, int scale, int level, int pixelX, int pixelY) {
        // Mirrors sampleBlur() in fancymenu_blur_composite.fsh: texels base - 1 to base + 2 around the pixel center.
        int baseX = (int) Math.floor((pixelX + 0.5D) / scale - 0.5D);
        int baseY = (int) Math.floor((pixelY + 0.5D) / scale - 0.5D);
        for (int dx = -1; dx <= 2; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                int texelX = clamp(baseX + dx, plan.levelWidth(level));
                int texelY = clamp(baseY + dy, plan.levelHeight(level));
                assertTrue(plan.vertical().contains(texelX, texelY), () -> "Composite samples texel " + texelX + "," + texelY + " outside of the blurred region");
            }
        }
    }

    private static void assertGaussianInputsAreCovered(GuiBlurPlan plan) {
        int level = plan.kernel().level();
        int reach = (plan.kernel().taps() > 0) ? plan.kernel().taps() + 1 : 0;
        GuiBlurPlan.Region vertical = plan.vertical();
        GuiBlurPlan.Region horizontal = plan.horizontal();
        for (int x : edges(vertical.x(), vertical.width())) {
            for (int y : edges(vertical.y(), vertical.height())) {
                for (int offset : new int[]{-reach, reach}) {
                    int texelY = clamp(y + offset, plan.levelHeight(level));
                    assertTrue(horizontal.contains(x, texelY), "Vertical pass samples outside of the horizontal pass");
                }
            }
        }
        for (int x : edges(horizontal.x(), horizontal.width())) {
            for (int y : edges(horizontal.y(), horizontal.height())) {
                for (int offset : new int[]{-reach, reach}) {
                    int texelX = clamp(x + offset, plan.levelWidth(level));
                    assertTrue(plan.downsample(level).contains(texelX, y), "Horizontal pass samples outside of the downsampled region");
                }
            }
        }
    }

    private static void assertDownsampleInputsAreCovered(GuiBlurPlan plan) {
        for (int level = plan.kernel().level(); level >= 2; level--) {
            GuiBlurPlan.Region output = plan.downsample(level);
            GuiBlurPlan.Region input = plan.downsample(level - 1);
            for (int x : edges(output.x(), output.width())) {
                for (int y : edges(output.y(), output.height())) {
                    // Mirrors fancymenu_blur_downsample.fsh: input texels 2x - 1 to 2x + 2.
                    for (int dx : new int[]{-1, 2}) {
                        for (int dy : new int[]{-1, 2}) {
                            int texelX = clamp(2 * x + dx, plan.levelWidth(level - 1));
                            int texelY = clamp(2 * y + dy, plan.levelHeight(level - 1));
                            assertTrue(input.contains(texelX, texelY), "Downsample samples outside of the level above it");
                        }
                    }
                }
            }
        }
    }

    private static int[] edges(int start, int size) {
        return new int[]{start, start + size - 1};
    }

    /** Clamp-to-edge sampling of a texel index. */
    private static int clamp(int texel, int size) {
        return Math.max(0, Math.min(size - 1, texel));
    }

    private static int radiusReach(float radius) {
        return (int) Math.ceil(Math.sqrt(GuiBlurKernel.targetVariance(radius)) * 3.0D);
    }

    private static void assertRegionInside(GuiBlurPlan.Region region, int width, int height) {
        assertTrue(region.x() >= 0 && region.y() >= 0 && region.x() + region.width() <= width && region.y() + region.height() <= height);
    }

}
