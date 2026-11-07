package de.keksuccino.fancymenu.util.rendering;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiBlurShaderResourceTest {

    private static final String CORE_SHADERS = "/assets/minecraft/shaders/core/";
    private static final Pattern UNIFORM = Pattern.compile("^uniform\\s+(\\w+)\\s+(\\w+);", Pattern.MULTILINE);

    @Test
    void gaussianTapLimitMatchesTheKernelLimit() throws IOException {
        Matcher matcher = Pattern.compile("#define FANCYMENU_BLUR_MAX_TAPS (\\d+)").matcher(readResource(CORE_SHADERS + "fancymenu_blur_gaussian.fsh"));

        assertTrue(matcher.find());
        assertEquals(GuiBlurKernel.MAX_TAPS, Integer.parseInt(matcher.group(1)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fancymenu_blur_downsample", "fancymenu_blur_gaussian", "fancymenu_blur_composite"})
    void shaderDefinitionDeclaresEveryUniformAndSamplerOfItsShaders(String name) throws IOException {
        JsonObject definition = JsonParser.parseString(readResource(CORE_SHADERS + name + ".json")).getAsJsonObject();
        String vertex = readResource(CORE_SHADERS + definition.get("vertex").getAsString() + ".vsh");
        String fragment = readResource(CORE_SHADERS + definition.get("fragment").getAsString() + ".fsh");
        Set<String> samplers = names(definition, "samplers");
        Set<String> uniforms = names(definition, "uniforms");

        for (String source : new String[]{vertex, fragment}) {
            Matcher matcher = UNIFORM.matcher(source);
            while (matcher.find()) {
                String type = matcher.group(1);
                String uniform = matcher.group(2);
                Set<String> declared = type.startsWith("sampler") ? samplers : uniforms;
                assertTrue(declared.contains(uniform), () -> name + ".json doesn't declare " + type + " " + uniform);
            }
        }
    }

    private static Set<String> names(JsonObject definition, String key) {
        Set<String> names = new HashSet<>();
        for (JsonElement element : definition.getAsJsonArray(key)) {
            names.add(element.getAsJsonObject().get("name").getAsString());
        }
        return names;
    }

    private static String readResource(String path) throws IOException {
        try (InputStream stream = GuiBlurShaderResourceTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing shader resource: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

}
