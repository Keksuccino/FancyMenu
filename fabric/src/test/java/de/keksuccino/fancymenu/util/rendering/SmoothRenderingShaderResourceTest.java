package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothRenderingShaderResourceTest {

    @Test
    void smoothCircleUsesHalfDerivativeForShapeAndArcEdges() throws IOException {
        String shader = readResource("assets/minecraft/shaders/core/fancymenu_gui_smooth_circle.fsh");

        assertTrue(shader.contains("max(fwidth(d) * 0.5, 0.0001)"));
        assertTrue(shader.contains("max(fwidth(angle) * 0.5, 0.0001)"));
        assertFalse(shader.contains("max(fwidth(d), 0.0001)"));
        assertFalse(shader.contains("max(fwidth(angle), 0.0001)"));
    }

    @Test
    void smoothImageCircleUsesHalfDerivativeForShapeEdge() throws IOException {
        String shader = readResource("assets/minecraft/shaders/core/fancymenu_gui_smooth_image_circle.fsh");

        assertTrue(shader.contains("max(fwidth(d) * 0.5, 0.0001)"));
        assertFalse(shader.contains("max(fwidth(d), 0.0001)"));
    }

    @Test
    void smoothImageRectangleUsesHalfDerivativeForShapeEdge() throws IOException {
        String shader = readResource("assets/minecraft/shaders/core/fancymenu_gui_smooth_image_rect.fsh");

        assertTrue(shader.contains("max(fwidth(dist) * 0.5, 0.0001)"));
        assertFalse(shader.contains("max(fwidth(dist), 0.0001)"));
    }

    @Test
    void smoothRectangleUsesIndependentHalfDerivativesForOuterAndInnerEdges() throws IOException {
        String shader = readResource("assets/minecraft/shaders/core/fancymenu_gui_smooth_rect.fsh");

        assertEquals(1, occurrences(shader, "max(fwidth(dist) * 0.5, 0.0001)"));
        assertEquals(1, occurrences(shader, "max(fwidth(innerDist) * 0.5, 0.0001)"));
        assertTrue(shader.contains("smoothstep(-innerAa, innerAa, innerDist)"));
        assertFalse(shader.contains("smoothstep(-aa, aa, innerDist)"));
    }

    @Test
    void blurMaskUsesHalfDerivativeForRectangleAndSuperellipseEdges() throws IOException {
        String shader = readResource("assets/minecraft/shaders/post/fancymenu_gui_blur.fsh");

        assertTrue(shader.contains("max(fwidth(d) * 0.5, 0.0001)"));
        assertTrue(shader.contains("max(fwidth(dist) * 0.5, 0.0001)"));
        assertFalse(shader.contains("max(fwidth(d), 0.0001)"));
        assertFalse(shader.contains("max(fwidth(dist), 0.0001)"));
    }

    private String readResource(String path) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "Missing packaged shader: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
