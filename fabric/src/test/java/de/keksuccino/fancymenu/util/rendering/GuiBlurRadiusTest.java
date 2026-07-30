package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiBlurRadiusTest {

    @ParameterizedTest
    @MethodSource("radiusCases")
    void sanitizesEveryRadiusBoundary(float input, float expected) {
        assertEquals(expected, GuiBlurRadius.sanitize(input));
    }

    @Test
    void finalShaderBoundaryCapsPostTransformAndFramebufferScaling() {
        assertEquals(8.0F, GuiBlurRadius.resolveShaderRadius(4.0F, 2.0D));
        assertEquals(GuiBlurRadius.MAX_RADIUS, GuiBlurRadius.resolveShaderRadius(8.0F, 2.0D));
        assertEquals(GuiBlurRadius.MAX_RADIUS, GuiBlurRadius.resolveShaderRadius(GuiBlurRadius.MAX_RADIUS, 4.0D));
        assertEquals(GuiBlurRadius.MAX_RADIUS, GuiBlurRadius.resolveShaderRadius(Float.MAX_VALUE, 1.0D));
        assertEquals(GuiBlurRadius.MAX_RADIUS, GuiBlurRadius.resolveShaderRadius(Float.MAX_VALUE, 2.0D));
        assertEquals(GuiBlurRadius.MAX_RADIUS, GuiBlurRadius.resolveShaderRadius(4.0F, Double.MAX_VALUE));
        assertEquals(GuiBlurRadius.MAX_RADIUS, GuiBlurRadius.resolveShaderRadius(Float.MAX_VALUE, Double.MAX_VALUE));
    }

    @Test
    void finalShaderBoundaryRejectsInvalidRadiusOrScale() {
        assertEquals(0.0F, GuiBlurRadius.resolveShaderRadius(Float.NaN, 1.0D));
        assertEquals(0.0F, GuiBlurRadius.resolveShaderRadius(Float.POSITIVE_INFINITY, 1.0D));
        assertEquals(0.0F, GuiBlurRadius.resolveShaderRadius(Float.NEGATIVE_INFINITY, 1.0D));
        assertEquals(0.0F, GuiBlurRadius.resolveShaderRadius(4.0F, Double.NaN));
        assertEquals(0.0F, GuiBlurRadius.resolveShaderRadius(4.0F, Double.POSITIVE_INFINITY));
        assertEquals(0.0F, GuiBlurRadius.resolveShaderRadius(4.0F, 0.0D));
        assertEquals(0.0F, GuiBlurRadius.resolveShaderRadius(4.0F, -1.0D));
    }

    @Test
    void fragmentShaderRetainsTheIndependentWorkLimit() throws IOException {
        String shader = readResource("/assets/minecraft/shaders/program/fancymenu_box_blur.fsh");

        assertTrue(shader.contains("clamp(round(Radius * RadiusMultiplier), 0.0, 16.0)"));
        assertTrue(shader.contains("synchronized with GuiBlurRadius.MAX_RADIUS"));
        assertFalse(shader.contains("float actualRadius = round(Radius * RadiusMultiplier)"));
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

    private static String readResource(String path) throws IOException {
        try (InputStream stream = GuiBlurRadiusTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing test resource: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

}
