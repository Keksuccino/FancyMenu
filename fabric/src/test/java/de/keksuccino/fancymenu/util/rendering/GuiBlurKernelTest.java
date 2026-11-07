package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiBlurKernelTest {

    @ParameterizedTest
    @ValueSource(floats = {0.0F, -3.0F, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
    void invalidOrZeroRadiusHasNoBlur(float radius) {
        GuiBlurKernel.Kernel kernel = GuiBlurKernel.resolve(radius);

        assertFalse(kernel.hasBlur());
        assertEquals(0, kernel.taps());
    }

    @Test
    void stagesAddUpToTheTargetVarianceAcrossTheWholeRadiusRange() {
        for (float radius = 0.25F; radius <= GuiBlurRadius.MAX_FRAMEBUFFER_RADIUS; radius += 0.25F) {
            GuiBlurKernel.Kernel kernel = GuiBlurKernel.resolve(radius);
            double target = GuiBlurKernel.targetVariance(radius);
            double chain = GuiBlurKernel.chainVariance(kernel.level());
            double gaussian = (double) kernel.sigma() * kernel.sigma() * GuiBlurKernel.levelArea(kernel.level());
            // Below one downsample step's blur, the mixed-in strength carries the rest of the variance.
            double resolved = (kernel.strength() < 1.0F) ? chain * kernel.strength() : chain + gaussian;
            double tolerance = Math.max(1.0E-3D, target * 1.0E-3D) + (kernel.taps() == 0 && kernel.strength() >= 1.0F ? 0.0025D * GuiBlurKernel.levelArea(kernel.level()) : 0.0D);
            float currentRadius = radius;
            assertEquals(target, resolved, tolerance, () -> "Wrong blur variance for radius " + currentRadius + ": " + kernel);
        }
    }

    @Test
    void blurStrengthGrowsMonotonicallyWithTheRadius() {
        double previous = 0.0D;
        for (float radius = 0.1F; radius <= GuiBlurRadius.MAX_FRAMEBUFFER_RADIUS; radius += 0.1F) {
            GuiBlurKernel.Kernel kernel = GuiBlurKernel.resolve(radius);
            double variance = (kernel.strength() < 1.0F) ? GuiBlurKernel.chainVariance(kernel.level()) * kernel.strength() : GuiBlurKernel.chainVariance(kernel.level()) + (double) kernel.sigma() * kernel.sigma() * GuiBlurKernel.levelArea(kernel.level());
            assertTrue(variance + 1.0E-2D >= previous, "Blur got weaker at radius " + radius);
            previous = variance;
        }
    }

    @Test
    void levelsStayInRangeAndTapsStayBounded() {
        int previousLevel = 1;
        for (float radius = 0.5F; radius <= GuiBlurRadius.MAX_FRAMEBUFFER_RADIUS; radius += 0.5F) {
            GuiBlurKernel.Kernel kernel = GuiBlurKernel.resolve(radius);
            assertTrue(kernel.level() >= 1 && kernel.level() <= GuiBlurKernel.MAX_LEVELS);
            assertTrue(kernel.level() >= previousLevel, "Level count dropped at radius " + radius);
            assertTrue(kernel.taps() >= 0 && kernel.taps() <= GuiBlurKernel.MAX_TAPS);
            if (kernel.level() > 1) {
                // Deeper levels only get picked while their Gaussian keeps at least one texel of sigma.
                assertTrue(kernel.sigma() >= 1.0F - 1.0E-4F);
            }
            previousLevel = kernel.level();
        }
    }

    @Test
    void deepestLevelOfTheWorkLimitDoesNotNeedTheTapLimit() {
        GuiBlurKernel.Kernel kernel = GuiBlurKernel.resolve(GuiBlurRadius.MAX_FRAMEBUFFER_RADIUS);

        assertTrue(kernel.level() < GuiBlurKernel.MAX_LEVELS);
        assertTrue(kernel.taps() < GuiBlurKernel.MAX_TAPS);
    }

    @Test
    void tinyRadiiBlendTheSmallestBlurInsteadOfSkippingIt() {
        GuiBlurKernel.Kernel kernel = GuiBlurKernel.resolve(0.5F);

        assertTrue(kernel.hasBlur());
        assertEquals(1, kernel.level());
        assertTrue(kernel.strength() > 0.0F && kernel.strength() < 1.0F);
    }

}
