package de.keksuccino.fancymenu.util.rendering;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import de.keksuccino.fancymenu.customization.panorama.LocalTexturePanoramaRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.lwjgl.util.shaderc.ShadercIncludeResult;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class EarlyShaderSourceTest {

    private static final Identifier SMOOTH_RECT_SHADER = Identifier.withDefaultNamespace("core/fancymenu_gui_smooth_rect");
    private static final Identifier VANILLA_SHADER = Identifier.withDefaultNamespace("core/gui");
    private static final Identifier FANCYMENU_INCLUDE = Identifier.fromNamespaceAndPath("fancymenu", "fancymenu_rounded_box.glsl");
    private static final Identifier VANILLA_INCLUDE = Identifier.withDefaultNamespace("globals.glsl");
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("#include\\s*<([^>]+)>");

    @Test
    void readsFancyMenuShadersFromTheResourcesInsteadOfTheVanillaSource() {
        RecordingVanillaSource vanilla = new RecordingVanillaSource();
        Identifier location = Identifier.withDefaultNamespace("shaders/core/fancymenu_gui_smooth_rect.vsh");
        EarlyShaderSource source = new EarlyShaderSource(vanilla, () -> resources(Map.of(location, "fancymenu vertex")));

        assertEquals("fancymenu vertex", source.getShader(SMOOTH_RECT_SHADER, ShaderType.VERTEX));
        assertTrue(vanilla.requestedShaders.isEmpty());
    }

    @Test
    void delegatesVanillaShadersAndIncludesToTheVanillaSource() {
        RecordingVanillaSource vanilla = new RecordingVanillaSource();
        EarlyShaderSource source = new EarlyShaderSource(vanilla, () -> {
            throw new AssertionError("Vanilla shaders must not be read from the client resources");
        });

        String vanillaShader = source.getShader(VANILLA_SHADER, ShaderType.FRAGMENT);
        assertNotNull(vanillaShader);
        assertTrue(vanillaShader.startsWith("#version"));
        assertNull(source.getInclude(VANILLA_INCLUDE));
        assertEquals(List.of(VANILLA_SHADER), vanilla.requestedShaders);
        assertEquals(List.of(VANILLA_INCLUDE), vanilla.requestedIncludes);
    }

    @Test
    void returnsNullForMissingFancyMenuShadersAndIncludes() {
        RecordingVanillaSource vanilla = new RecordingVanillaSource();
        EarlyShaderSource source = new EarlyShaderSource(vanilla, () -> ResourceProvider.EMPTY);

        assertNull(source.getShader(SMOOTH_RECT_SHADER, ShaderType.FRAGMENT));
        assertNull(source.getInclude(FANCYMENU_INCLUDE));
        assertTrue(vanilla.requestedShaders.isEmpty());
        assertTrue(vanilla.requestedIncludes.isEmpty());
    }

    @Test
    void cachesFancyMenuIncludesUntilClosedAndClosesTheVanillaSource() {
        RecordingVanillaSource vanilla = new RecordingVanillaSource();
        Identifier location = Identifier.fromNamespaceAndPath("fancymenu", "shaders/include/fancymenu_rounded_box.glsl");
        int[] reads = new int[1];
        EarlyShaderSource source = new EarlyShaderSource(vanilla, () -> id -> {
            if (!id.equals(location)) return Optional.empty();
            reads[0]++;
            return Optional.of(resource("float roundedBox();"));
        });

        ShaderSource.CachedIncludeSource include = getIncludeOrSkip(source, FANCYMENU_INCLUDE);
        assertNotNull(include);
        assertSame(include, source.getInclude(FANCYMENU_INCLUDE));
        assertEquals(1, reads[0]);
        ShadercIncludeResult result = ShadercIncludeResult.create(include.includeResultPtr());
        assertEquals(FANCYMENU_INCLUDE.toString(), result.source_nameString());
        assertEquals("float roundedBox();", result.contentString());

        source.close();
        assertTrue(vanilla.closed);
    }

    @Test
    void onlyTreatsFancyMenuShadersAndIncludesAsFancyMenuShaders() {
        assertTrue(EarlyShaderSource.isFancyMenuShader(SMOOTH_RECT_SHADER));
        assertTrue(EarlyShaderSource.isFancyMenuShader(Identifier.withDefaultNamespace("post/fancymenu_box_blur")));
        assertTrue(EarlyShaderSource.isFancyMenuShader(FANCYMENU_INCLUDE));
        assertFalse(EarlyShaderSource.isFancyMenuShader(VANILLA_SHADER));
        assertFalse(EarlyShaderSource.isFancyMenuShader(VANILLA_INCLUDE));
        assertFalse(EarlyShaderSource.isFancyMenuShader(Identifier.withDefaultNamespace("fancymenu_dir/gui")));
    }

    @ParameterizedTest
    @MethodSource("fancyMenuPipelines")
    void resolvesAllShadersOfFancyMenuPipelinesFromTheShippedResources(RenderPipeline pipeline) {
        // Vanilla's fallback source only sees the vanilla pack, so every FancyMenu shader has to bypass it.
        RecordingVanillaSource vanilla = new RecordingVanillaSource();
        EarlyShaderSource source = new EarlyShaderSource(vanilla, ClasspathResources::new);
        List<String> fancyMenuSources = new ArrayList<>();
        assertEquals(ShaderType.values().length, pipeline.getShaders().size());
        for (Map.Entry<ShaderType, Identifier> entry : pipeline.getShaders().entrySet()) {
            ShaderType type = entry.getKey();
            Identifier shader = entry.getValue();
            String shaderSource = source.getShader(shader, type);
            assertNotNull(shaderSource, () -> "Missing " + type + " shader " + shader + " of " + pipeline.getLocation());
            if (EarlyShaderSource.isFancyMenuShader(shader)) fancyMenuSources.add(shaderSource);
        }
        assertFalse(fancyMenuSources.isEmpty());
        assertTrue(vanilla.requestedShaders.stream().noneMatch(EarlyShaderSource::isFancyMenuShader));

        for (String shaderSource : fancyMenuSources) {
            Matcher matcher = INCLUDE_PATTERN.matcher(shaderSource);
            while (matcher.find()) {
                Identifier include = Identifier.parse(matcher.group(1));
                if (EarlyShaderSource.isFancyMenuShader(include)) {
                    assertNotNull(getIncludeOrSkip(source, include), () -> "Missing include " + include + " of " + pipeline.getLocation());
                }
            }
        }
        source.close();
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

    private static ShaderSource.CachedIncludeSource getIncludeOrSkip(EarlyShaderSource source, Identifier include) {
        try {
            return source.getInclude(include);
        } catch (UnsatisfiedLinkError error) {
            assumeTrue(false, () -> "LWJGL native library is unavailable: " + error.getMessage());
            throw new AssertionError("Unreachable after native-library assumption", error);
        }
    }

    private static ResourceProvider resources(Map<Identifier, String> files) {
        return id -> Optional.ofNullable(files.get(id)).map(EarlyShaderSourceTest::resource);
    }

    private static Resource resource(String contents) {
        return new Resource(null, () -> new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)));
    }

    /** Resolves resources like the client resource manager does for the FancyMenu and vanilla jars on the test classpath. */
    private static final class ClasspathResources implements ResourceProvider {

        @Override
        public Optional<Resource> getResource(Identifier location) {
            URL url = EarlyShaderSourceTest.class.getResource("/assets/" + location.getNamespace() + "/" + location.getPath());
            return Optional.ofNullable(url).map(found -> new Resource(null, found::openStream));
        }

    }

    private static final class RecordingVanillaSource implements ShaderSource {

        private final List<Identifier> requestedShaders = new ArrayList<>();
        private final List<Identifier> requestedIncludes = new ArrayList<>();
        private final ClasspathResources resources = new ClasspathResources();
        private boolean closed;

        @Override
        public String getShader(Identifier id, ShaderType type) {
            this.requestedShaders.add(id);
            return this.resources.getResource(type.idConverter().idToFile(id)).map(found -> {
                try {
                    return found.readAllAsString();
                } catch (Exception ex) {
                    throw new AssertionError(ex);
                }
            }).orElse("vanilla " + id);
        }

        @Override
        public CachedIncludeSource getInclude(Identifier id) {
            this.requestedIncludes.add(id);
            return null;
        }

        @Override
        public void close() {
            this.closed = true;
        }

    }

}
