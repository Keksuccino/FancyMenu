package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothEdgeShaderResourcesTest {

    private static final String SHADER_ROOT = "assets/minecraft/shaders/";
    private static final String SMOOTH_CIRCLE = "program/fancymenu_gui_smooth_circle.fsh";
    private static final String SMOOTH_IMAGE_CIRCLE = "program/fancymenu_gui_smooth_image_circle.fsh";
    private static final String SMOOTH_IMAGE_RECT = "program/fancymenu_gui_smooth_image_rect.fsh";
    private static final String SMOOTH_RECT = "program/fancymenu_gui_smooth_rect.fsh";
    private static final String SMOOTH_RECT_LOCAL = "core/fancymenu_gui_smooth_rect_local.fsh";
    private static final String GUI_BLUR = "program/fancymenu_gui_blur.fsh";
    private static final List<String> MAPPED_SHADERS = List.of(SMOOTH_CIRCLE, SMOOTH_IMAGE_CIRCLE, SMOOTH_IMAGE_RECT, SMOOTH_RECT, SMOOTH_RECT_LOCAL, GUI_BLUR);
    private static final List<String> IMAGE_SHADERS = List.of(SMOOTH_IMAGE_CIRCLE, SMOOTH_IMAGE_RECT);

    @Test
    void allMappedShaderResourcesExist() {
        assertAll(MAPPED_SHADERS.stream().map(path -> () -> assertNotNull(SmoothEdgeShaderResourcesTest.class.getClassLoader().getResource(SHADER_ROOT + path), path)));
    }

    @Test
    void shapeAndArcTransitionsUseHalfDerivativeWidthWithFloor() throws IOException {
        assertEquals(1, occurrences(loadShader(SMOOTH_CIRCLE), "max(fwidth(d) * 0.5, 0.0001)"));
        assertEquals(1, occurrences(loadShader(SMOOTH_CIRCLE), "max(fwidth(angle) * 0.5, 0.0001)"));
        assertEquals(1, occurrences(loadShader(SMOOTH_IMAGE_CIRCLE), "max(fwidth(d) * 0.5, 0.0001)"));
        assertEquals(1, occurrences(loadShader(GUI_BLUR), "max(fwidth(d) * 0.5, 0.0001)"));
    }

    @Test
    void rectangleBordersUseSharedCoverageForOuterAndInnerMasks() throws IOException {
        for (String path : List.of(SMOOTH_RECT, SMOOTH_RECT_LOCAL)) {
            String shader = loadShader(path);
            assertAll(path, () -> assertTrue(shader.contains("float alpha = fancymenuRoundedBoxAlpha(p, halfSize, CornerRadii);")), () -> assertTrue(shader.contains("float innerAlpha = fancymenuRoundedBoxAlpha(p, innerHalfSize, innerRadii);")));
        }
    }

    @Test
    void imageShadersMapReversedRegionsBeforeClampingToTexelCenters() throws IOException {
        for (String path : IMAGE_SHADERS) {
            String shader = loadShader(path);
            int mapping = shader.indexOf("uv = mix(UvMin, UvMax, uv);");
            int clamp = shader.indexOf("uv = clampUvToRegionTexelCenters(uv);");
            int sample = shader.indexOf("texture(ImageSampler, uv)");
            assertAll(path, () -> assertTrue(mapping >= 0), () -> assertTrue(clamp > mapping), () -> assertTrue(sample > clamp));
        }
    }

    @Test
    void imageShadersClampFullAndSubTexelRegionsWithoutTextureStateMutation() throws IOException {
        for (String path : IMAGE_SHADERS) {
            String shader = loadShader(path);
            assertAll(path, () -> assertTrue(shader.contains("textureSize(ImageSampler, 0)")), () -> assertTrue(shader.contains("vec2 regionMin = min(UvMin, UvMax);")), () -> assertTrue(shader.contains("vec2 regionMax = max(UvMin, UvMax);")), () -> assertTrue(shader.contains("vec2 regionCenter = clamp((regionMin + regionMax) * 0.5, textureMin, textureMax);")), () -> assertTrue(shader.contains("vec2 sampleMin = min(max(regionMin + halfTexel, textureMin), regionCenter);")), () -> assertTrue(shader.contains("vec2 sampleMax = max(min(regionMax - halfTexel, textureMax), regionCenter);")));
        }
    }

    @Test
    void reversedRegionProducesTheSameNormalizedSampleBounds() {
        SampleBounds forward = resolveSampleBounds(0.25F, 0.75F, 16);
        SampleBounds reversed = resolveSampleBounds(0.75F, 0.25F, 16);

        assertEquals(forward, reversed);
    }

    @Test
    void regionNarrowerThanOneTexelCollapsesToItsCenter() {
        SampleBounds bounds = resolveSampleBounds(0.40F, 0.45F, 4);

        assertEquals(0.425F, bounds.min(), 0.0001F);
        assertEquals(0.425F, bounds.max(), 0.0001F);
    }

    @Test
    void fullRegionClampsToOuterTexelCenters() {
        SampleBounds bounds = resolveSampleBounds(0.0F, 1.0F, 4);

        assertEquals(0.125F, bounds.min(), 0.0001F);
        assertEquals(0.875F, bounds.max(), 0.0001F);
    }

    private static String loadShader(String path) throws IOException {
        InputStream input = SmoothEdgeShaderResourcesTest.class.getClassLoader().getResourceAsStream(SHADER_ROOT + path);
        assertNotNull(input, path);
        try (input) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static SampleBounds resolveSampleBounds(float first, float second, int textureSize) {
        float regionMin = Math.min(first, second);
        float regionMax = Math.max(first, second);
        float halfTexel = 0.5F / Math.max(1, textureSize);
        float textureMin = halfTexel;
        float textureMax = 1.0F - halfTexel;
        float regionCenter = clamp((regionMin + regionMax) * 0.5F, textureMin, textureMax);
        float sampleMin = Math.min(Math.max(regionMin + halfTexel, textureMin), regionCenter);
        float sampleMax = Math.max(Math.min(regionMax - halfTexel, textureMax), regionCenter);
        return new SampleBounds(sampleMin, sampleMax);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private record SampleBounds(float min, float max) {
    }

}
