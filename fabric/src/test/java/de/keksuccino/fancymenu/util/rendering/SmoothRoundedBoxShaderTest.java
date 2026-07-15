package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothRoundedBoxShaderTest {

    private static final String SHARED_COVERAGE_IMPORT = "#moj_import <fancymenu_rounded_box.glsl>";

    @Test
    void sharpCornerCoverageDoesNotDependOnFragmentQuadAlignment() {
        float legacyStraightEdgeAlpha = distanceFieldAlpha(-0.5F, 1.0F);
        float legacyCornerQuadAlpha = distanceFieldAlpha(-0.5F, 2.0F);

        assertEquals(1.0F, legacyStraightEdgeAlpha);
        assertNotEquals(legacyStraightEdgeAlpha, legacyCornerQuadAlpha);
        assertEquals(0.84375F, legacyCornerQuadAlpha);
        assertEquals(1.0F, sharpBoxAlpha(-0.5F, -0.5F, 1.0F, 1.0F));
    }

    @Test
    void sharpBoxCoveragePreservesSubpixelAntialiasing() {
        assertEquals(1.0F, sharpBoxAlpha(-0.5F, -0.5F, 1.0F, 1.0F));
        assertEquals(0.5F, sharpBoxAlpha(0.0F, -0.5F, 1.0F, 1.0F));
        assertEquals(0.0F, sharpBoxAlpha(0.5F, -0.5F, 1.0F, 1.0F));
    }

    @Test
    void pixelAlignedSharpBoxCoverageIsStableAcrossOriginParities() {
        int width = 10;
        int height = 10;
        for (int originX : new int[]{16, 17}) {
            for (int originY : new int[]{12, 13}) {
                int right = originX + width - 1;
                int bottom = originY + height - 1;

                assertEquals(1.0F, sharpBoxAlphaAtPixel(originX, originY, width, height, originX, originY));
                assertEquals(1.0F, sharpBoxAlphaAtPixel(originX, originY, width, height, right, originY));
                assertEquals(1.0F, sharpBoxAlphaAtPixel(originX, originY, width, height, originX, bottom));
                assertEquals(1.0F, sharpBoxAlphaAtPixel(originX, originY, width, height, right, bottom));

                assertEquals(0.0F, sharpBoxAlphaAtPixel(originX, originY, width, height, originX - 1, originY - 1));
                assertEquals(0.0F, sharpBoxAlphaAtPixel(originX, originY, width, height, right + 1, originY - 1));
                assertEquals(0.0F, sharpBoxAlphaAtPixel(originX, originY, width, height, originX - 1, bottom + 1));
                assertEquals(0.0F, sharpBoxAlphaAtPixel(originX, originY, width, height, right + 1, bottom + 1));
            }
        }
    }

    @Test
    void zeroDerivativeFootprintProducesFiniteCoverage() {
        float alpha = sharpBoxAlpha(0.0F, 0.0F, 0.0F, 0.0F);

        assertTrue(Float.isFinite(alpha));
        assertEquals(0.25F, alpha);
    }

    @Test
    void subpixelCornerRadiusTransitionsAwayFromSharpCoverageGradually() {
        assertEquals(0.0F, roundedWeight(0.0F, 1.0F, 1.0F));
        assertEquals(0.15625F, roundedWeight(0.25F, 1.0F, 1.0F));
        assertEquals(0.5F, roundedWeight(0.5F, 1.0F, 1.0F));
        assertEquals(1.0F, roundedWeight(1.0F, 1.0F, 1.0F));

        float legacyCornerQuadAlpha = distanceFieldAlpha(-0.5F, 2.0F);
        float correctedSubpixelRadiusAlpha = mix(1.0F, legacyCornerQuadAlpha, roundedWeight(0.25F, 1.0F, 1.0F));
        assertTrue(correctedSubpixelRadiusAlpha > legacyCornerQuadAlpha);
    }

    @Test
    void everyRectangleShaderUsesStableSharpCornerCoverage() throws IOException {
        String sharedCoverage = readResource("/assets/minecraft/shaders/include/fancymenu_rounded_box.glsl").trim();
        String imageRectangleShader = readResource("/assets/minecraft/shaders/program/fancymenu_gui_smooth_image_rect.fsh");
        String rectangleShader = readResource("/assets/minecraft/shaders/program/fancymenu_gui_smooth_rect.fsh");
        String blurShader = readResource("/assets/minecraft/shaders/program/fancymenu_gui_blur.fsh");
        String localRectangleShader = readResource("/assets/minecraft/shaders/core/fancymenu_gui_smooth_rect_local.fsh");

        assertTrue(sharedCoverage.contains("fwidth(position)"));
        assertTrue(sharedCoverage.contains("smoothstep(0.0, cornerTransitionWidth"));
        assertTrue(sharedCoverage.contains("mix(sharpAlpha, roundedAlpha, roundedWeight)"));

        assertEffectProgramEmbedsSharedCoverage(imageRectangleShader, sharedCoverage, 2);
        assertEffectProgramEmbedsSharedCoverage(rectangleShader, sharedCoverage, 3);
        assertEffectProgramEmbedsSharedCoverage(blurShader, sharedCoverage, 2);

        assertTrue(localRectangleShader.contains(SHARED_COVERAGE_IMPORT));
        assertEquals(2, countOccurrences(localRectangleShader, "fancymenuRoundedBoxAlpha("));
        assertFalse(localRectangleShader.contains("sdRoundedBox("));
    }

    private static void assertEffectProgramEmbedsSharedCoverage(String shader, String sharedCoverage, int expectedCallCount) {
        assertFalse(shader.contains("#moj_import"));
        assertTrue(shader.contains(sharedCoverage));
        assertEquals(expectedCallCount, countOccurrences(shader, "fancymenuRoundedBoxAlpha("));
        assertFalse(shader.contains("sdRoundedBox("));
    }

    private static float distanceFieldAlpha(float distance, float distanceDerivativeWidth) {
        float antialiasWidth = Math.max(distanceDerivativeWidth * 0.5F, 0.0001F);
        return 1.0F - smoothstep(-antialiasWidth, antialiasWidth, distance);
    }

    private static float sharpBoxAlpha(float xEdgeDistance, float yEdgeDistance, float xCoordinateDerivativeWidth, float yCoordinateDerivativeWidth) {
        float xAntialiasWidth = Math.max(xCoordinateDerivativeWidth * 0.5F, 0.0001F);
        float yAntialiasWidth = Math.max(yCoordinateDerivativeWidth * 0.5F, 0.0001F);
        float xAlpha = 1.0F - smoothstep(-xAntialiasWidth, xAntialiasWidth, xEdgeDistance);
        float yAlpha = 1.0F - smoothstep(-yAntialiasWidth, yAntialiasWidth, yEdgeDistance);
        return xAlpha * yAlpha;
    }

    private static float sharpBoxAlphaAtPixel(int originX, int originY, int width, int height, int pixelX, int pixelY) {
        float centerX = originX + width * 0.5F;
        float centerY = originY + height * 0.5F;
        float xEdgeDistance = Math.abs(pixelX + 0.5F - centerX) - width * 0.5F;
        float yEdgeDistance = Math.abs(pixelY + 0.5F - centerY) - height * 0.5F;
        return sharpBoxAlpha(xEdgeDistance, yEdgeDistance, 1.0F, 1.0F);
    }

    private static float roundedWeight(float cornerRadius, float xCoordinateDerivativeWidth, float yCoordinateDerivativeWidth) {
        float transitionWidth = Math.max(Math.max(xCoordinateDerivativeWidth, yCoordinateDerivativeWidth), 0.0001F);
        return smoothstep(0.0F, transitionWidth, Math.max(cornerRadius, 0.0F));
    }

    private static float mix(float first, float second, float amount) {
        return first * (1.0F - amount) + second * amount;
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Math.max(0.0F, Math.min(1.0F, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0F - 2.0F * t);
    }

    private static int countOccurrences(String value, String match) {
        return (value.length() - value.replace(match, "").length()) / match.length();
    }

    private static String readResource(String path) throws IOException {
        try (InputStream stream = SmoothRoundedBoxShaderTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new AssertionError("Missing test resource: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
