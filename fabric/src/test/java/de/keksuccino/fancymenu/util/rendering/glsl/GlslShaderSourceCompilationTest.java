package de.keksuccino.fancymenu.util.rendering.glsl;

import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.vulkan.glsl.GlslCompiler;
import com.mojang.blaze3d.vulkan.glsl.IntermediaryShaderModule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GlslShaderSourceCompilationTest {

    @Test
    void compilesTransformedLegacyDirectVertexAndFragmentForVulkan12() {
        String source = """
                #version 120
                precision mediump float;
                uniform vec4 iMouse;
                uniform vec2 fmVarVec2_offset;
                uniform sampler2D noise;
                void main() {
                    vec2 uv = fmUv_FancyMenu + fmVarVec2_offset;
                    vec2 pixel = gl_FragCoord.xy / iResolution.xy;
                    gl_FragColor = texture2D(noise, uv) + vec4(pixel, iMouse.x, 0.0);
                }
                """;
        GlslShaderSourceTransformer.FragmentVariant variant = variant(source, GlslShaderRuntime.CompileMode.DIRECT, GlslShaderSourceTransformer.PassKind.BUFFER, "direct_glfragcolor_compat");

        GlslShaderValidator.ValidationResult validation = validateOrSkip(variant);

        assertTrue(validation.valid(), () -> "Transformed direct shader failed Vulkan 1.2 compilation: " + validation.diagnostics());
    }

    @Test
    void compilesRealProceduralTitleScreenShadertoyForVulkan12() {
        GlslShaderSourceTransformer.FragmentVariant variant = variant(TitleScreenShadertoyFixture.SOURCE, GlslShaderRuntime.CompileMode.SHADERTOY, GlslShaderSourceTransformer.PassKind.IMAGE, "shadertoy");

        GlslShaderValidator.ValidationResult validation = validateOrSkip(variant);

        assertTrue(validation.valid(), () -> "Real title-screen Shadertoy source failed Vulkan 1.2 compilation: " + validation.diagnostics());
    }

    @Test
    void reportsShadercStageDiagnosticsForInvalidTransformedSource() {
        String source = "void main() { gl_FragColor = vec4(fmMissingIdentifier); }";
        GlslShaderSourceTransformer.FragmentVariant variant = variant(source, GlslShaderRuntime.CompileMode.DIRECT, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");

        GlslShaderValidator.ValidationResult validation = validateOrSkip(variant);

        assertFalse(validation.valid());
        assertTrue(validation.diagnostics().stream().anyMatch(diagnostic -> diagnostic.contains("fragment shader") && diagnostic.contains("fmMissingIdentifier")), () -> "Expected named fragment-stage diagnostic but got " + validation.diagnostics());
    }

    @Test
    void compilesPreferredAndFallbackAutoVariantsWhenSourceDefinesBothDialects() {
        String source = """
                void mainImage(out vec4 color, in vec2 coordinate) {
                    color = vec4(coordinate / iResolution.xy, 0.0, 1.0);
                }
                void main() {
                    gl_FragColor = vec4(fmUv_FancyMenu, 0.0, 1.0);
                }
                """;
        List<GlslShaderSourceTransformer.FragmentVariant> preferredShadertoy = GlslShaderSourceTransformer.buildFragmentVariants(source, GlslShaderRuntime.CompileMode.AUTO, true, GlslShaderSourceTransformer.BackendCoordinates.VULKAN, GlslShaderSourceTransformer.PassKind.IMAGE);
        List<GlslShaderSourceTransformer.FragmentVariant> preferredDirect = GlslShaderSourceTransformer.buildFragmentVariants(source, GlslShaderRuntime.CompileMode.AUTO, false, GlslShaderSourceTransformer.BackendCoordinates.VULKAN, GlslShaderSourceTransformer.PassKind.IMAGE);

        List<GlslShaderSourceTransformer.FragmentVariant> variantsToCompile = List.of(preferredShadertoy.getFirst(), preferredDirect.getFirst(), preferredDirect.getLast());
        assertEquals(List.of("shadertoy", "direct_glfragcolor_compat", "shadertoy"), variantsToCompile.stream().map(GlslShaderSourceTransformer.FragmentVariant::label).toList());
        for (GlslShaderSourceTransformer.FragmentVariant variant : variantsToCompile) {
            GlslShaderValidator.ValidationResult validation = validateOrSkip(variant);
            assertTrue(validation.valid(), () -> "AUTO variant '" + variant.label() + "' failed Vulkan 1.2 compilation: " + validation.diagnostics());
        }
    }

    @Test
    void minecraftCompilerReflectionMatchesPrunedActiveSamplerNamesExactly() throws Exception {
        String source = """
                uniform sampler2D iChannel0;
                uniform sampler2D iChannel2;
                uniform sampler2D detailNoise;
                uniform sampler2D unusedNoise;
                void main() {
                    gl_FragColor = texture(iChannel2, fmUv_FancyMenu) + texture(detailNoise, fmUv_FancyMenu);
                }
                """;
        GlslShaderSourceTransformer.FragmentVariant variant = variant(source, GlslShaderRuntime.CompileMode.DIRECT, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");

        assertEquals(List.of("iChannel2", "detailNoise"), variant.activeSamplerNames());
        try (GlslCompiler compiler = new GlslCompiler(); IntermediaryShaderModule module = compiler.createIntermediary("fancymenu_sampler_pruning.fsh", variant.source(), ShaderType.FRAGMENT)) {
            List<?> reflectedSamplers = module.samplers();
            assertEquals(variant.activeSamplerNames(), reflectedSamplers.stream().map(GlslShaderSourceCompilationTest::readReflectedName).toList());
        } catch (UnsatisfiedLinkError error) {
            assumeTrue(false, () -> "LWJGL shader compiler or SPIR-V reflection native library is unavailable: " + error.getMessage());
        } catch (ExceptionInInitializerError error) {
            if (hasUnsatisfiedLinkCause(error)) {
                assumeTrue(false, () -> "LWJGL shader compiler or SPIR-V reflection native library is unavailable: " + error.getCause());
            }
            throw error;
        }
    }

    @Test
    void minecraftCompilerCompilesAndReflectsPreprocessedMacroConditionalUniformsForEveryBackend() throws Exception {
        String source = """
                #define ARRAY_LENGTH 2
                #define FM_UNIFORM(type, name) uniform type name[ARRAY_LENGTH]
                #if defined(VULKAN) && defined(gl_VertexID)
                FM_UNIFORM(float, backendValues);
                #define FM_COLOR(value) vec4(value)
                #else
                FM_UNIFORM(vec2, backendValues);
                #define FM_COLOR(value) vec4(value, 0.0, 1.0)
                #endif
                void main() {
                    gl_FragColor = FM_COLOR(backendValues[1]);
                }
                """;

        try (GlslCompiler compiler = new GlslCompiler()) {
            for (GlslShaderSourceTransformer.BackendCoordinates backend : GlslShaderSourceTransformer.BackendCoordinates.values()) {
                GlslShaderSourceTransformer.FragmentVariant variant = variant(source, GlslShaderRuntime.CompileMode.DIRECT, backend, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");
                GlslStd140Layout.Member backendValues = variant.uniformLayout().member("backendValues");
                String expectedType = backend == GlslShaderSourceTransformer.BackendCoordinates.VULKAN ? "float" : "vec2";

                assertEquals(expectedType, backendValues.type());
                assertEquals(2, backendValues.arrayLength());
                try (IntermediaryShaderModule module = compiler.createIntermediary("fancymenu_preprocessed_" + backend.name() + ".fsh", variant.source(), ShaderType.FRAGMENT)) {
                    List<?> reflectedUniformBuffers = module.uniformBuffers();
                    assertEquals(List.of(GlslShaderSourceTransformer.UNIFORM_BLOCK_NAME), reflectedUniformBuffers.stream().map(GlslShaderSourceCompilationTest::readReflectedName).toList());
                }
            }
        } catch (UnsatisfiedLinkError error) {
            assumeTrue(false, () -> "LWJGL shader compiler or SPIR-V reflection native library is unavailable: " + error.getMessage());
        } catch (ExceptionInInitializerError error) {
            if (hasUnsatisfiedLinkCause(error)) {
                assumeTrue(false, () -> "LWJGL shader compiler or SPIR-V reflection native library is unavailable: " + error.getCause());
            }
            throw error;
        }
    }

    private static String readReflectedName(Object resource) {
        try {
            var nameAccessor = resource.getClass().getDeclaredMethod("name");
            nameAccessor.setAccessible(true);
            return (String) nameAccessor.invoke(resource);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Minecraft's reflected shader resource no longer exposes its expected name accessor.", error);
        }
    }

    private static GlslShaderSourceTransformer.FragmentVariant variant(String source, GlslShaderRuntime.CompileMode mode, GlslShaderSourceTransformer.PassKind passKind, String label) {
        return variant(source, mode, GlslShaderSourceTransformer.BackendCoordinates.VULKAN, passKind, label);
    }

    private static GlslShaderSourceTransformer.FragmentVariant variant(String source, GlslShaderRuntime.CompileMode mode, GlslShaderSourceTransformer.BackendCoordinates backend, GlslShaderSourceTransformer.PassKind passKind, String label) {
        List<GlslShaderSourceTransformer.FragmentVariant> variants = GlslShaderSourceTransformer.buildFragmentVariants(source, mode, false, backend, passKind);
        return variants.stream().filter(variant -> variant.label().equals(label)).findFirst().orElseThrow(() -> new AssertionError("Missing fragment variant '" + label + "'."));
    }

    private static GlslShaderValidator.ValidationResult validateOrSkip(GlslShaderSourceTransformer.FragmentVariant variant) {
        try {
            return GlslShaderValidator.validate(variant.vertexSource(), variant.source());
        } catch (UnsatisfiedLinkError error) {
            assumeTrue(false, () -> "LWJGL Shaderc native library is unavailable: " + error.getMessage());
            throw new AssertionError("Unreachable after native-library assumption", error);
        } catch (ExceptionInInitializerError error) {
            if (hasUnsatisfiedLinkCause(error)) {
                assumeTrue(false, () -> "LWJGL Shaderc native library is unavailable: " + error.getCause());
                throw new AssertionError("Unreachable after native-library assumption", error);
            }
            throw error;
        }
    }

    private static boolean hasUnsatisfiedLinkCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof UnsatisfiedLinkError) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

}
