package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuiBlurRadiusTest {

    @ParameterizedTest
    @MethodSource("radiusCases")
    void sanitizesEveryRadiusBoundary(float input, float expected) {
        assertEquals(expected, GuiBlurRadius.sanitize(input));
    }

    @Test
    void framebufferRadiusScalesWithTheGuiInsteadOfCappingAtTheGuiLimit() {
        assertEquals(8.0F, GuiBlurRadius.resolveFramebufferRadius(4.0F, 2.0D));
        assertEquals(48.0F, GuiBlurRadius.resolveFramebufferRadius(12.0F, 4.0D));
        assertEquals(GuiBlurRadius.MAX_RADIUS * 6.0F, GuiBlurRadius.resolveFramebufferRadius(GuiBlurRadius.MAX_RADIUS, 6.0D));
        assertEquals(GuiBlurRadius.MAX_RADIUS * 4.0F, GuiBlurRadius.resolveFramebufferRadius(GuiBlurRadius.MAX_RADIUS + 1.0F, 4.0D));
    }

    @Test
    void framebufferRadiusSaturatesAtTheWorkLimit() {
        assertEquals(GuiBlurRadius.MAX_FRAMEBUFFER_RADIUS, GuiBlurRadius.resolveFramebufferRadius(GuiBlurRadius.MAX_RADIUS, 100.0D));
        assertEquals(GuiBlurRadius.MAX_FRAMEBUFFER_RADIUS, GuiBlurRadius.resolveFramebufferRadius(Float.MAX_VALUE, 1000.0D));
        assertEquals(GuiBlurRadius.MAX_FRAMEBUFFER_RADIUS, GuiBlurRadius.resolveFramebufferRadius(4.0F, Double.MAX_VALUE));
    }

    @Test
    void framebufferRadiusRejectsInvalidRadiusOrScale() {
        assertEquals(0.0F, GuiBlurRadius.resolveFramebufferRadius(Float.NaN, 1.0D));
        assertEquals(0.0F, GuiBlurRadius.resolveFramebufferRadius(Float.POSITIVE_INFINITY, 1.0D));
        assertEquals(0.0F, GuiBlurRadius.resolveFramebufferRadius(Float.NEGATIVE_INFINITY, 1.0D));
        assertEquals(0.0F, GuiBlurRadius.resolveFramebufferRadius(-2.0F, 1.0D));
        assertEquals(0.0F, GuiBlurRadius.resolveFramebufferRadius(4.0F, Double.NaN));
        assertEquals(0.0F, GuiBlurRadius.resolveFramebufferRadius(4.0F, Double.POSITIVE_INFINITY));
        assertEquals(0.0F, GuiBlurRadius.resolveFramebufferRadius(4.0F, 0.0D));
        assertEquals(0.0F, GuiBlurRadius.resolveFramebufferRadius(4.0F, -1.0D));
    }

    private static Stream<Arguments> radiusCases() {
        return Stream.of(
                Arguments.of(-1.0F, 0.0F),
                Arguments.of(0.0F, 0.0F),
                Arguments.of(4.0F, 4.0F),
                Arguments.of(GuiBlurRadius.MAX_RADIUS, GuiBlurRadius.MAX_RADIUS),
                Arguments.of(GuiBlurRadius.MAX_RADIUS + 1.0F, GuiBlurRadius.MAX_RADIUS),
                Arguments.of(Float.MAX_VALUE, GuiBlurRadius.MAX_RADIUS),
                Arguments.of(Float.NaN, 0.0F),
                Arguments.of(Float.POSITIVE_INFINITY, 0.0F),
                Arguments.of(Float.NEGATIVE_INFINITY, 0.0F)
        );
    }

}
