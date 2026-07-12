package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothShapeShaderResourceTest {

    private static final Map<String, List<String>> EXPECTED_DERIVATIVE_RAMPS = Map.of(
            "/assets/minecraft/shaders/program/fancymenu_gui_smooth_circle.fsh", List.of("max(fwidth(d) * 0.5, 0.0001)", "max(fwidth(angle) * 0.5, 0.0001)"),
            "/assets/minecraft/shaders/program/fancymenu_gui_smooth_image_circle.fsh", List.of("max(fwidth(d) * 0.5, 0.0001)"),
            "/assets/minecraft/shaders/program/fancymenu_gui_smooth_image_rect.fsh", List.of("max(fwidth(dist) * 0.5, 0.0001)"),
            "/assets/minecraft/shaders/program/fancymenu_gui_smooth_rect.fsh", List.of("max(fwidth(dist) * 0.5, 0.0001)", "max(fwidth(innerDist) * 0.5, 0.0001)"),
            "/assets/minecraft/shaders/core/fancymenu_gui_smooth_rect_local.fsh", List.of("max(fwidth(dist) * 0.5, 0.0001)", "max(fwidth(innerDist) * 0.5, 0.0001)"),
            "/assets/minecraft/shaders/program/fancymenu_gui_blur.fsh", List.of("max(fwidth(d) * 0.5, 0.0001)", "max(fwidth(dist) * 0.5, 0.0001)")
    );

    @Test
    void smoothShapeEdgesUseOnePixelDerivativeRamps() throws IOException {
        for (Map.Entry<String, List<String>> entry : EXPECTED_DERIVATIVE_RAMPS.entrySet()) {
            String shader = readResource(entry.getKey());
            for (String derivativeRamp : entry.getValue()) {
                assertTrue(shader.contains(derivativeRamp), () -> entry.getKey() + " is missing derivative ramp " + derivativeRamp);
            }
        }
    }

    @Test
    void rectangleBordersCalculateTheirInnerEdgeDerivativeIndependently() throws IOException {
        for (String resource : List.of("/assets/minecraft/shaders/program/fancymenu_gui_smooth_rect.fsh", "/assets/minecraft/shaders/core/fancymenu_gui_smooth_rect_local.fsh")) {
            String shader = readResource(resource);

            assertTrue(shader.contains("float innerAa = max(fwidth(innerDist) * 0.5, 0.0001);"), () -> resource + " must calculate antialiasing from the inner edge");
            assertTrue(shader.contains("smoothstep(-innerAa, innerAa, innerDist)"), () -> resource + " must apply the inner edge derivative");
            assertFalse(shader.contains("smoothstep(-aa, aa, innerDist)"), () -> resource + " must not reuse the outer edge derivative for its border hole");
        }
    }

    private static String readResource(String resource) throws IOException {
        try (InputStream stream = SmoothShapeShaderResourceTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, () -> "Missing shader test resource " + resource);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
