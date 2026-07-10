package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiTextureCoverRendererTest {

    private static final float EPSILON = 0.000001F;
    private static final String SHADER_ROOT = "assets/minecraft/shaders/core/";
    private static final String COVER_JSON = "fancymenu_gui_texture_cover.json";
    private static final String COVER_FRAGMENT = "fancymenu_gui_texture_cover.fsh";
    private static final String ALPHA_VERTEX = "fancymenu_gui_alpha_texture.vsh";

    @Test
    void equalAspectUsesTheFullTexture() {
        GuiTextureCoverRenderer.CoverRegion region = requireRegion(0, 0, 320, 180, 1920, 1080);

        assertUv(region, 0.0F, 1.0F, 0.0F, 1.0F);
    }

    @Test
    void wideTextureIsCroppedSymmetricallyOnTheHorizontalAxis() {
        GuiTextureCoverRenderer.CoverRegion region = requireRegion(0, 0, 100, 100, 200, 100);

        assertUv(region, 0.25F, 0.75F, 0.0F, 1.0F);
    }

    @Test
    void tallTextureIsCroppedSymmetricallyOnTheVerticalAxis() {
        GuiTextureCoverRenderer.CoverRegion region = requireRegion(0, 0, 100, 100, 100, 200);

        assertUv(region, 0.0F, 1.0F, 0.25F, 0.75F);
    }

    @Test
    void nonzeroDestinationRemainsExactlyBounded() {
        GuiTextureCoverRenderer.CoverRegion region = requireRegion(17, -9, 320, 180, 100, 100);

        assertAll(() -> assertEquals(17.0F, region.minX(), EPSILON), () -> assertEquals(337.0F, region.maxX(), EPSILON), () -> assertEquals(-9.0F, region.minY(), EPSILON), () -> assertEquals(171.0F, region.maxY(), EPSILON));
    }

    @Test
    void reportedGuiDimensionsStayBoundedAndCentered() {
        GuiTextureCoverRenderer.CoverRegion region = requireRegion(0, 0, 1277, 694, 2560, 1440);

        assertAll(() -> assertEquals(0.0F, region.minX(), EPSILON), () -> assertEquals(1277.0F, region.maxX(), EPSILON), () -> assertEquals(0.0F, region.minY(), EPSILON), () -> assertEquals(694.0F, region.maxY(), EPSILON), () -> assertUv(region, 0.0F, 1.0F, 0.016923345F, 0.98307663F));
    }

    @Test
    void fullScreenCoverDoesNotInheritLegacyHeaderOffset() {
        GuiTextureCoverRenderer.CoverRegion fullScreen = requireRegion(0, 0, 1277, 694, 2560, 1440);
        GuiTextureCoverRenderer.CoverRegion boundedBody = requireRegion(0, 24, 1277, 694, 2560, 1440);

        assertAll(() -> assertEquals(0.0F, fullScreen.minY(), EPSILON), () -> assertEquals(694.0F, fullScreen.maxY(), EPSILON), () -> assertEquals(24.0F, boundedBody.minY(), EPSILON), () -> assertEquals(718.0F, boundedBody.maxY(), EPSILON), () -> assertUv(fullScreen, boundedBody.minU(), boundedBody.maxU(), boundedBody.minV(), boundedBody.maxV()));
    }

    @Test
    void uniformGuiScalingDoesNotChangeTheCrop() {
        GuiTextureCoverRenderer.CoverRegion base = requireRegion(0, 0, 320, 180, 100, 100);
        GuiTextureCoverRenderer.CoverRegion scaled = requireRegion(0, 0, 960, 540, 100, 100);

        assertUv(scaled, base.minU(), base.maxU(), base.minV(), base.maxV());
    }

    @Test
    void viewportAspectChangesCropTheExpectedAxis() {
        GuiTextureCoverRenderer.CoverRegion wideArea = requireRegion(0, 0, 200, 100, 100, 100);
        GuiTextureCoverRenderer.CoverRegion tallArea = requireRegion(0, 0, 100, 200, 100, 100);

        assertAll(() -> assertUv(wideArea, 0.0F, 1.0F, 0.25F, 0.75F), () -> assertUv(tallArea, 0.25F, 0.75F, 0.0F, 1.0F));
    }

    @Test
    void nonPositiveDimensionsAreRejected() {
        List<GuiTextureCoverRenderer.CoverRegion> invalid = Arrays.asList(nullableRegion(0, 10, 10, 10), nullableRegion(10, 0, 10, 10), nullableRegion(-1, 10, 10, 10), nullableRegion(10, -1, 10, 10), nullableRegion(10, 10, 0, 10), nullableRegion(10, 10, 10, 0), nullableRegion(10, 10, -1, 10), nullableRegion(10, 10, 10, -1));

        assertAll(invalid.stream().map(region -> () -> assertNull(region)));
    }

    @Test
    void onePixelAndExtremeDimensionsStayFiniteAndNormalized() {
        GuiTextureCoverRenderer.CoverRegion onePixel = requireRegion(Integer.MIN_VALUE, Integer.MAX_VALUE, 1, 1, Integer.MAX_VALUE, 1);
        GuiTextureCoverRenderer.CoverRegion extreme = requireRegion(Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 1);

        assertAll(() -> assertFiniteAndNormalized(onePixel), () -> assertFiniteAndNormalized(extreme), () -> assertTrue(extreme.maxX() > 0.0F), () -> assertTrue(extreme.maxY() < 0.0F || extreme.maxY() == 0.0F));
    }

    @Test
    void everyCropIsCenteredAndNormalized() {
        List<GuiTextureCoverRenderer.CoverRegion> regions = List.of(requireRegion(0, 0, 853, 480, 4096, 1024), requireRegion(0, 0, 480, 853, 1024, 4096), requireRegion(0, 0, 641, 359, 1920, 1080));

        assertAll(regions.stream().map(region -> () -> assertAll(() -> assertEquals(region.minU(), 1.0F - region.maxU(), EPSILON), () -> assertEquals(region.minV(), 1.0F - region.maxV(), EPSILON), () -> assertFiniteAndNormalized(region))));
    }

    @Test
    void shaderResourcesLinkTheCoverFragmentToTheSharedVertexFormat() throws IOException {
        String json = loadResource(COVER_JSON);

        assertAll(() -> assertNotNull(getResource(COVER_FRAGMENT)), () -> assertNotNull(getResource(ALPHA_VERTEX)), () -> assertTrue(json.contains("\"vertex\": \"fancymenu_gui_alpha_texture\"")), () -> assertTrue(json.contains("\"fragment\": \"fancymenu_gui_texture_cover\"")), () -> assertTrue(json.contains("\"Position\"")), () -> assertTrue(json.contains("\"UV0\"")), () -> assertTrue(json.contains("\"Color\"")), () -> assertTrue(json.contains("\"ColorModulator\"")));
    }

    @Test
    void fragmentClampsBeforeSamplingAndPreservesTintMultiplication() throws IOException {
        String fragment = loadResource(COVER_FRAGMENT);
        int textureSize = fragment.indexOf("textureSize(Sampler0, 0)");
        int clamp = fragment.indexOf("sampleUv = clamp(texCoord0, halfTexel, vec2(1.0) - halfTexel)");
        int sample = fragment.indexOf("texture(Sampler0, sampleUv)");
        int tint = fragment.indexOf("texColor * vertexColor * ColorModulator");

        assertAll(() -> assertTrue(textureSize >= 0), () -> assertTrue(clamp > textureSize), () -> assertTrue(sample > clamp), () -> assertTrue(tint > sample), () -> assertFalse(fragment.contains("glTexParameter")));
    }

    private static GuiTextureCoverRenderer.CoverRegion requireRegion(int x, int y, int width, int height, int textureWidth, int textureHeight) {
        GuiTextureCoverRenderer.CoverRegion region = GuiTextureCoverRenderer.calculateRegion(x, y, width, height, textureWidth, textureHeight);
        assertNotNull(region);
        return region;
    }

    private static GuiTextureCoverRenderer.CoverRegion nullableRegion(int width, int height, int textureWidth, int textureHeight) {
        return GuiTextureCoverRenderer.calculateRegion(0, 0, width, height, textureWidth, textureHeight);
    }

    private static void assertUv(GuiTextureCoverRenderer.CoverRegion region, float minU, float maxU, float minV, float maxV) {
        assertAll(() -> assertEquals(minU, region.minU(), EPSILON), () -> assertEquals(maxU, region.maxU(), EPSILON), () -> assertEquals(minV, region.minV(), EPSILON), () -> assertEquals(maxV, region.maxV(), EPSILON));
    }

    private static void assertFiniteAndNormalized(GuiTextureCoverRenderer.CoverRegion region) {
        assertAll(() -> assertTrue(Float.isFinite(region.minX())), () -> assertTrue(Float.isFinite(region.maxX())), () -> assertTrue(Float.isFinite(region.minY())), () -> assertTrue(Float.isFinite(region.maxY())), () -> assertTrue(region.minU() >= 0.0F && region.minU() <= 1.0F), () -> assertTrue(region.maxU() >= 0.0F && region.maxU() <= 1.0F), () -> assertTrue(region.minV() >= 0.0F && region.minV() <= 1.0F), () -> assertTrue(region.maxV() >= 0.0F && region.maxV() <= 1.0F), () -> assertTrue(region.minU() <= region.maxU()), () -> assertTrue(region.minV() <= region.maxV()));
    }

    private static java.net.URL getResource(String name) {
        return GuiTextureCoverRendererTest.class.getClassLoader().getResource(SHADER_ROOT + name);
    }

    private static String loadResource(String name) throws IOException {
        InputStream input = GuiTextureCoverRendererTest.class.getClassLoader().getResourceAsStream(SHADER_ROOT + name);
        assertNotNull(input, name);
        try (input) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
