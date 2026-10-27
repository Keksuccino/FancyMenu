package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import de.keksuccino.fancymenu.customization.panorama.LocalTexturePanoramaRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EarlyShaderSourceTest {

    private static final Identifier SHADER = Identifier.withDefaultNamespace("core/fancymenu_gui_smooth_rect");
    private static final Identifier SHADER_FILE = Identifier.withDefaultNamespace("shaders/core/fancymenu_gui_smooth_rect.fsh");

    @Test
    void resolvesAbsoluteAndRelativeImportsOnceFromTheResources() {
        EarlyShaderSource source = new EarlyShaderSource(() -> resources(Map.of(
                SHADER_FILE, "#version 330\n#moj_import <fancymenu:rounded_box.glsl>\n#moj_import \"helper.glsl\"\n#moj_import <fancymenu:rounded_box.glsl>\nvoid main() {}\n",
                Identifier.fromNamespaceAndPath("fancymenu", "shaders/include/rounded_box.glsl"), "float roundedBox() { return 1.0; }\n",
                Identifier.withDefaultNamespace("shaders/core/helper.glsl"), "float helper() { return 2.0; }\n"
        )));

        String resolved = source.get(SHADER, ShaderType.FRAGMENT);

        assertNotNull(resolved);
        assertEquals(1, occurrences(resolved, "float roundedBox()"));
        assertEquals(1, occurrences(resolved, "float helper()"));
        assertFalse(resolved.contains("#moj_import <fancymenu:rounded_box.glsl>\n"));
        assertTrue(resolved.contains("void main() {}"));
    }

    @Test
    void returnsNullForMissingShaders() {
        EarlyShaderSource source = new EarlyShaderSource(() -> ResourceProvider.EMPTY);

        assertNull(source.get(SHADER, ShaderType.VERTEX));
    }

    @Test
    void turnsMissingImportsIntoShaderErrorsInsteadOfFailing() {
        EarlyShaderSource source = new EarlyShaderSource(() -> resources(Map.of(SHADER_FILE, "#version 330\n#moj_import <fancymenu:missing.glsl>\nvoid main() {}\n")));

        String resolved = source.get(SHADER, ShaderType.FRAGMENT);

        assertNotNull(resolved);
        assertTrue(resolved.contains("#error"));
    }

    @ParameterizedTest
    @MethodSource("fancyMenuPipelines")
    void resolvesAllShadersOfFancyMenuPipelinesFromTheShippedResources(RenderPipeline pipeline) {
        EarlyShaderSource source = new EarlyShaderSource(ClasspathResources::new);
        for (ShaderType type : ShaderType.values()) {
            Identifier shader = (type == ShaderType.VERTEX) ? pipeline.getVertexShader() : pipeline.getFragmentShader();
            String resolved = source.get(shader, type);
            assertNotNull(resolved, () -> "Missing " + type + " shader " + shader + " of " + pipeline.getLocation());
            assertFalse(resolved.contains("#error"), () -> "Unresolved import in " + type + " shader " + shader + " of " + pipeline.getLocation());
        }
    }

    private static List<RenderPipeline> fancyMenuPipelines() throws IllegalAccessException {
        List<RenderPipeline> pipelines = new ArrayList<>();
        for (Class<?> renderer : List.of(SmoothRectangleRenderer.class, SmoothCircleRenderer.class, SmoothImageRectangleRenderer.class, SmoothImageCircleRenderer.class, LocalTexturePanoramaRenderer.class, GuiBlurRenderer.class)) {
            for (Field field : renderer.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.getType() == RenderPipeline.class) {
                    field.setAccessible(true);
                    pipelines.add((RenderPipeline) field.get(null));
                }
            }
        }
        assertEquals(8, pipelines.size());
        return pipelines;
    }

    private static int occurrences(String text, String part) {
        int count = 0;
        for (int index = text.indexOf(part); index >= 0; index = text.indexOf(part, index + part.length())) {
            count++;
        }
        return count;
    }

    private static ResourceProvider resources(Map<Identifier, String> files) {
        return id -> Optional.ofNullable(files.get(id)).map(contents -> new Resource(null, () -> new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8))));
    }

    /** Resolves resources like the client resource manager does for the FancyMenu and vanilla jars on the test classpath. */
    private static final class ClasspathResources implements ResourceProvider {

        @Override
        public Optional<Resource> getResource(Identifier location) {
            URL url = EarlyShaderSourceTest.class.getResource("/assets/" + location.getNamespace() + "/" + location.getPath());
            return Optional.ofNullable(url).map(found -> new Resource(null, found::openStream));
        }

    }

}
