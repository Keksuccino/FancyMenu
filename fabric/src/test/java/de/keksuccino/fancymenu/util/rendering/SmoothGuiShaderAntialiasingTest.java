package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothGuiShaderAntialiasingTest {

    private static final Pattern ONE_PIXEL_DERIVATIVE = Pattern.compile("max\\(fwidth\\([^\\n]+\\) \\* 0\\.5, 0\\.0001\\)");
    private static final Pattern UNHALVED_DERIVATIVE_ASSIGNMENT = Pattern.compile("float (?:aa|fw|angleAa|innerAa) = fwidth\\(");

    @ParameterizedTest
    @MethodSource("smoothShaders")
    void edgeTransitionsUseOnePixelDerivatives(String path, int expectedDerivativeCount, String requiredExpression) throws IOException {
        String source = readResource(path);
        Matcher matcher = ONE_PIXEL_DERIVATIVE.matcher(source);
        int derivativeCount = 0;
        while (matcher.find()) {
            derivativeCount++;
        }

        assertEquals(expectedDerivativeCount, derivativeCount, path);
        assertFalse(UNHALVED_DERIVATIVE_ASSIGNMENT.matcher(source).find(), path);
        assertTrue(source.contains(requiredExpression), path);
    }

    private static Stream<Arguments> smoothShaders() {
        return Stream.of(
                Arguments.of("assets/minecraft/shaders/program/fancymenu_gui_smooth_circle.fsh", 2, "float angleAa = max(fwidth(angle) * 0.5, 0.0001);"),
                Arguments.of("assets/minecraft/shaders/program/fancymenu_gui_smooth_image_circle.fsh", 1, "float fw = max(fwidth(d) * 0.5, 0.0001);"),
                Arguments.of("assets/minecraft/shaders/program/fancymenu_gui_smooth_image_rect.fsh", 1, "float aa = max(fwidth(dist) * 0.5, 0.0001);"),
                Arguments.of("assets/minecraft/shaders/program/fancymenu_gui_smooth_rect.fsh", 2, "float innerAa = max(fwidth(innerDist) * 0.5, 0.0001);"),
                Arguments.of("assets/minecraft/shaders/program/fancymenu_gui_blur.fsh", 2, "float aa = max(fwidth(dist) * 0.5, 0.0001);"),
                Arguments.of("assets/minecraft/shaders/core/fancymenu_gui_smooth_rect_local.fsh", 2, "float innerAa = max(fwidth(innerDist) * 0.5, 0.0001);")
        );
    }

    private static String readResource(String path) throws IOException {
        ClassLoader classLoader = SmoothGuiShaderAntialiasingTest.class.getClassLoader();
        try (InputStream stream = Objects.requireNonNull(classLoader.getResourceAsStream(path), path)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
