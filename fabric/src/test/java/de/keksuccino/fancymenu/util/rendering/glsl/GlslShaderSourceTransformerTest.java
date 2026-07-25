package de.keksuccino.fancymenu.util.rendering.glsl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlslShaderSourceTransformerTest {

    @Test
    void preservesUnrelatedDeviceExtensionBranchesForTheActualBackendCompiler() {
        String source = """
                #if defined(GL_ARB_gpu_shader_int64)
                #error Shaderc must not eagerly select this driver-dependent branch
                #define FM_DEVICE_VALUE 1.0
                #else
                #define FM_DEVICE_VALUE 0.0
                #endif
                #define FM_UNUSED_UNIFORM(type, name) \\\\
                    uniform type name
                void main() { gl_FragColor = vec4(FM_DEVICE_VALUE); }
                """;

        GlslShaderSourceTransformer.FragmentVariant variant = variant(source, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");

        assertAll(() -> assertTrue(variant.source().contains("#if defined(GL_ARB_gpu_shader_int64)")), () -> assertTrue(variant.source().contains("#error Shaderc must not eagerly select this driver-dependent branch")), () -> assertTrue(variant.source().contains("#define FM_DEVICE_VALUE 1.0")), () -> assertTrue(variant.source().contains("#define FM_DEVICE_VALUE 0.0")), () -> assertTrue(variant.source().contains("FM_UNUSED_UNIFORM")), () -> assertTrue(variant.source().contains("uniform type name")));
    }

    @Test
    void normalizesBomLineEndingsVersionAndPrecisionDeclarations() {
        String source = "\uFEFF#version 100\r\nprecision mediump float;\rprecision highp int;\r\nvoid main() { gl_FragColor = vec4(1.0); }\r\n";

        String normalized = GlslShaderSourceTransformer.normalizeSource(source);

        assertAll(() -> assertEquals("void main() { gl_FragColor = vec4(1.0); }", normalized), () -> assertFalse(normalized.contains("\r")), () -> assertFalse(normalized.contains("#version")), () -> assertFalse(normalized.contains("precision")));
    }

    @Test
    void leavesVersionAndPrecisionTextInsideCommentsUntouched() {
        String source = "// #version 120\n/* precision mediump float; */\nvoid main() {}";

        String normalized = GlslShaderSourceTransformer.normalizeSource(source);

        assertAll(() -> assertTrue(normalized.contains("// #version 120")), () -> assertTrue(normalized.contains("/* precision mediump float; */")));
    }

    @Test
    void wrapsLegacyShadertoySourceAndKeepsItsSamplerActive() {
        String source = """
                #version 100
                precision mediump float;
                uniform sampler2D iChannel0;
                void mainImage(out vec4 fragColor, in vec2 fragCoord) {
                    fragColor = texture2D(iChannel0, fragCoord / iResolution.xy);
                }
                """;

        GlslShaderSourceTransformer.FragmentVariant variant = onlyVariant(source, GlslShaderRuntime.CompileMode.SHADERTOY, false, GlslShaderSourceTransformer.BackendCoordinates.VULKAN, GlslShaderSourceTransformer.PassKind.IMAGE);

        assertAll(() -> assertEquals("shadertoy", variant.label()), () -> assertTrue(variant.source().startsWith("#version 330\n")), () -> assertFalse(variant.source().contains("#version 100")), () -> assertFalse(variant.source().contains("precision mediump")), () -> assertTrue(variant.source().contains("#define texture2D texture")), () -> assertTrue(variant.source().contains("mainImage(fmColor_FancyMenu, gl_FragCoord.xy - fmAreaOffset);")), () -> assertTrue(variant.source().contains("fmColor_FancyMenu.a * fmOpacity")), () -> assertEquals(1, countOccurrences(variant.source(), "uniform sampler2D iChannel0;")), () -> assertEquals(List.of("iChannel0"), variant.activeSamplerNames()));
    }

    @Test
    void keepsShadertoyBufferAlphaIndependentFromFinalImageOpacity() {
        String source = "void mainImage(out vec4 color, in vec2 coordinate) { color = vec4(coordinate, 0.0, 0.25); }";

        GlslShaderSourceTransformer.FragmentVariant buffer = onlyVariant(source, GlslShaderRuntime.CompileMode.SHADERTOY, false, GlslShaderSourceTransformer.BackendCoordinates.VULKAN, GlslShaderSourceTransformer.PassKind.BUFFER);
        GlslShaderSourceTransformer.FragmentVariant image = onlyVariant(source, GlslShaderRuntime.CompileMode.SHADERTOY, false, GlslShaderSourceTransformer.BackendCoordinates.VULKAN, GlslShaderSourceTransformer.PassKind.IMAGE);

        assertAll(() -> assertTrue(buffer.source().contains("fmOutputColor_FancyMenu = fmColor_FancyMenu;")), () -> assertFalse(buffer.source().contains("fmColor_FancyMenu.a * fmOpacity")), () -> assertTrue(image.source().contains("fmColor_FancyMenu.a * fmOpacity")));
    }

    @Test
    void rewritesLegacyDirectUniformsIntoAnAnonymousBlockWithoutChangingReferences() {
        String source = """
                uniform float fmVarFloat_score;
                void main() {
                    gl_FragColor = texture2D(iChannel0, fmUv_FancyMenu) * fmVarFloat_score;
                }
                """;

        GlslShaderSourceTransformer.FragmentVariant variant = variant(source, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");
        GlslStd140Layout.Member variable = variant.uniformLayout().member("fmVarFloat_score");

        assertNotNull(variable);
        assertAll(() -> assertTrue(variant.source().contains("layout(std140) uniform FancyMenuUniforms {")), () -> assertTrue(variant.source().contains("    float fmVarFloat_score;")), () -> assertFalse(variant.source().contains("uniform float fmVarFloat_score;")), () -> assertTrue(variant.source().contains("texture2D(iChannel0, fmUv_FancyMenu) * fmVarFloat_score")), () -> assertTrue(variant.source().contains("#define gl_FragColor fmOutputColor_FancyMenu")), () -> assertEquals("float", variable.type()), () -> assertEquals(List.of("iChannel0"), variant.activeSamplerNames()));
    }

    @Test
    void prefersModernDirectOutputBeforeTheLegacyCompatibilityFallback() {
        String source = """
                layout(location = 0) out vec4 color;
                void main() {
                    color = vec4(fmUv_FancyMenu, 0.0, 1.0);
                }
                """;

        List<GlslShaderSourceTransformer.FragmentVariant> variants = variants(source, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE);

        assertAll(() -> assertEquals(List.of("direct_no_compat", "direct_glfragcolor_compat"), variants.stream().map(GlslShaderSourceTransformer.FragmentVariant::label).toList()), () -> assertFalse(variants.getFirst().source().contains("#define gl_FragColor")), () -> assertFalse(variants.getFirst().source().contains("out vec4 fmOutputColor_FancyMenu;")), () -> assertTrue(variants.getFirst().source().contains("layout(location = 0) out vec4 color;")));
    }

    @Test
    void ignoresOutputAndEntryPointTextInsideComments() {
        String source = """
                // gl_FragColor = vec4(1.0);
                /* void mainImage(out vec4 color, in vec2 coordinate) {} */
                layout(location = 0) out vec4 color;
                void main() {
                    color = vec4(1.0);
                }
                """;

        List<GlslShaderSourceTransformer.FragmentVariant> variants = variants(source, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE);

        assertEquals("direct_no_compat", variants.getFirst().label());
    }

    @Test
    void enforcesExplicitModeEntryPointsWhileAutoAcceptsEitherDialect() {
        String direct = "void main() { gl_FragColor = vec4(1.0); }";
        String shadertoy = "void mainImage(out vec4 color, in vec2 coordinate) { color = vec4(coordinate, 0.0, 1.0); }";

        assertAll(() -> assertTrue(variants(direct, GlslShaderRuntime.CompileMode.SHADERTOY, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE).isEmpty()), () -> assertTrue(variants(shadertoy, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE).isEmpty()), () -> assertEquals(List.of("direct_glfragcolor_compat", "direct_no_compat"), variants(direct, GlslShaderRuntime.CompileMode.AUTO, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE).stream().map(GlslShaderSourceTransformer.FragmentVariant::label).toList()), () -> assertEquals("shadertoy", variants(shadertoy, GlslShaderRuntime.CompileMode.AUTO, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE).getFirst().label()));
    }

    @Test
    void autoOrdersBothDialectsByTheShadertoyCompatibilityPreference() {
        String source = """
                void mainImage(out vec4 color, in vec2 coordinate) {
                    color = vec4(coordinate, 0.0, 1.0);
                }
                void main() {
                    gl_FragColor = vec4(fmUv_FancyMenu, 0.0, 1.0);
                }
                """;

        List<GlslShaderSourceTransformer.FragmentVariant> preferredShadertoy = variants(source, GlslShaderRuntime.CompileMode.AUTO, true, GlslShaderSourceTransformer.BackendCoordinates.VULKAN, GlslShaderSourceTransformer.PassKind.IMAGE);
        List<GlslShaderSourceTransformer.FragmentVariant> preferredDirect = variants(source, GlslShaderRuntime.CompileMode.AUTO, false, GlslShaderSourceTransformer.BackendCoordinates.VULKAN, GlslShaderSourceTransformer.PassKind.IMAGE);

        assertAll(() -> assertEquals(List.of("shadertoy", "direct_glfragcolor_compat", "direct_no_compat"), preferredShadertoy.stream().map(GlslShaderSourceTransformer.FragmentVariant::label).toList()), () -> assertEquals(List.of("direct_glfragcolor_compat", "direct_no_compat", "shadertoy"), preferredDirect.stream().map(GlslShaderSourceTransformer.FragmentVariant::label).toList()), () -> assertFalse(preferredShadertoy.getFirst().source().contains("gl_FragColor =")), () -> assertEquals(1, countOccurrences(preferredShadertoy.getFirst().source(), "void main()")));
    }

    @Test
    void blanksOnlyTheTopLevelDirectMainWithNestedBracesAfterPreprocessing() {
        String source = """
                float helperBefore_FancyMenu(float value) {
                    return value * 2.0;
                }
                float helperAfter_FancyMenu(float value);
                void main();
                void mainImage(out vec4 color, in vec2 coordinate) {
                    color = vec4(helperBefore_FancyMenu(coordinate.x), coordinate.y, helperAfter_FancyMenu(1.0), 1.0);
                }
                void main() {
                    float directOnlyAccumulator_FancyMenu = 0.0;
                    if (directOnlyAccumulator_FancyMenu == 0.0) {
                        /* Misleading function delimiters inside a comment: } { */
                        for (int index = 0; index < 2; index++) {
                            directOnlyAccumulator_FancyMenu += float(index);
                        }
                    }
                    // Another misleading closing brace: }
                    gl_FragColor = vec4(directOnlyAccumulator_FancyMenu);
                }
                float helperAfter_FancyMenu(float value) {
                    return value + 1.0;
                }
                """;

        GlslShaderSourceTransformer.FragmentVariant variant = onlyVariant(source, GlslShaderRuntime.CompileMode.SHADERTOY, false, GlslShaderSourceTransformer.BackendCoordinates.VULKAN, GlslShaderSourceTransformer.PassKind.IMAGE);
        String transformed = variant.source();

        assertAll(() -> assertTrue(transformed.contains("void main();")), () -> assertTrue(transformed.contains("void mainImage(out vec4 color, in vec2 coordinate)")), () -> assertTrue(transformed.contains("float helperBefore_FancyMenu(float value)")), () -> assertTrue(transformed.contains("float helperAfter_FancyMenu(float value)")), () -> assertFalse(transformed.contains("directOnlyAccumulator_FancyMenu")), () -> assertFalse(transformed.contains("gl_FragColor =")), () -> assertEquals(1, countOccurrences(transformed, "void main() {")));
    }

    @Test
    void rejectsSourcesWhoseOnlyEntryPointsAreCommentedOut() {
        String source = "// void main() {}\n/* void mainImage(out vec4 color, in vec2 coordinate) {} */";

        assertTrue(variants(source, GlslShaderRuntime.CompileMode.AUTO, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE).isEmpty());
    }

    @Test
    void parsesEmptyCommentsLayoutQualifiersMultilineDeclarationsAndMultipleDeclarators() {
        String source = """
                /**/
                /* uniform float ignored; void mainImage() {} */
                layout(
                    row_major
                ) uniform mat2 fmVarMatrix;
                uniform
                    highp float
                    fmVarFloat_score,
                    fmVarFloat_history[2]
                ;
                void
                main
                () {
                    float value = fmVarFloat_score + fmVarFloat_history[1] + fmVarMatrix[0][0];
                    gl_FragColor = vec4(value);
                }
                """;

        GlslShaderSourceTransformer.FragmentVariant variant = variant(source, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.BUFFER, "direct_glfragcolor_compat");
        GlslStd140Layout.Member history = variant.uniformLayout().member("fmVarFloat_history");

        assertNotNull(history);
        assertAll(() -> assertTrue(variant.source().contains("/**/")), () -> assertTrue(variant.source().contains("/* uniform float ignored; void mainImage() {} */")), () -> assertFalse(variant.source().contains("row_major")), () -> assertTrue(variant.source().contains("mat2 fmVarMatrix;")), () -> assertTrue(variant.source().contains("float fmVarFloat_score;")), () -> assertTrue(variant.source().contains("float fmVarFloat_history[2];")), () -> assertEquals(2, history.arrayLength()), () -> assertEquals(16, history.arrayStride()), () -> assertTrue(variant.source().contains("fmVarFloat_score + fmVarFloat_history[1] + fmVarMatrix[0][0]")));
    }

    @Test
    void keepsSamplerDeclarationsLooseAndDetectsOnlyActuallyReferencedSamplers() {
        String source = """
                uniform sampler2D noise;
                // iChannel2 and iChannel3 are documentation only.
                void main() {
                    gl_FragColor = texture(noise, fmUv_FancyMenu) + texture(iChannel1, fmUv_FancyMenu);
                }
                """;

        GlslShaderSourceTransformer.FragmentVariant variant = variant(source, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.VULKAN, GlslShaderSourceTransformer.PassKind.BUFFER, "direct_glfragcolor_compat");

        assertAll(() -> assertTrue(variant.source().contains("uniform sampler2D noise;")), () -> assertNull(variant.uniformLayout().member("noise")), () -> assertEquals(List.of("iChannel1", "noise"), variant.activeSamplerNames()), () -> assertEquals(1, countOccurrences(variant.source(), "uniform sampler2D iChannel1;")));
    }

    @Test
    void preservesBuiltInDeclarationsOnlyOnceAndValidatesStd140ArrayLayout() {
        String source = """
                uniform vec3 iResolution;
                uniform float iTime;
                uniform float iChannelTime[4];
                uniform vec3 iChannelResolution[4];
                void main() {
                    gl_FragColor = vec4(iResolution.xy, iTime + iChannelTime[0] + iChannelResolution[0].x, 1.0);
                }
                """;

        GlslShaderSourceTransformer.FragmentVariant variant = variant(source, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.BUFFER, "direct_glfragcolor_compat");
        GlslStd140Layout.Member resolution = variant.uniformLayout().member("iResolution");
        GlslStd140Layout.Member time = variant.uniformLayout().member("iTime");
        GlslStd140Layout.Member channelTime = variant.uniformLayout().member("iChannelTime");
        GlslStd140Layout.Member channelResolution = variant.uniformLayout().member("iChannelResolution");

        assertNotNull(resolution);
        assertNotNull(time);
        assertNotNull(channelTime);
        assertNotNull(channelResolution);
        assertAll(() -> assertEquals(1, countOccurrences(variant.source(), "vec3 iResolution;")), () -> assertEquals(1, countOccurrences(variant.source(), "float iTime;")), () -> assertEquals(resolution.offset() + 12, time.offset()), () -> assertEquals(4, channelTime.arrayLength()), () -> assertEquals(16, channelTime.arrayStride()), () -> assertEquals(64, channelTime.size()), () -> assertEquals(4, channelResolution.arrayLength()), () -> assertEquals(16, channelResolution.arrayStride()), () -> assertEquals(64, channelResolution.size()));
    }

    @Test
    void preprocessesMacroArrayLengthsBeforeUniformTransformationForEveryBackend() {
        String source = """
                #define SAMPLE_COUNT 3
                uniform float samples[SAMPLE_COUNT];
                void main() {
                    gl_FragColor = vec4(samples[2]);
                }
                """;

        for (GlslShaderSourceTransformer.BackendCoordinates backend : GlslShaderSourceTransformer.BackendCoordinates.values()) {
            GlslShaderSourceTransformer.FragmentVariant variant = variant(source, GlslShaderRuntime.CompileMode.DIRECT, false, backend, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");
            GlslStd140Layout.Member samples = variant.uniformLayout().member("samples");

            assertNotNull(samples);
            assertAll(() -> assertEquals(3, samples.arrayLength()), () -> assertEquals(16, samples.arrayStride()), () -> assertTrue(variant.source().contains("float samples[3];")), () -> assertFalse(variant.source().contains("SAMPLE_COUNT")));
        }
    }

    @Test
    void preprocessesMacroGeneratedUniformDeclarationsForEveryBackend() {
        String source = """
                #define FM_UNIFORM(type, name) uniform type name
                FM_UNIFORM(vec2, macroOffset);
                void main() {
                    gl_FragColor = vec4(macroOffset, 0.0, 1.0);
                }
                """;

        for (GlslShaderSourceTransformer.BackendCoordinates backend : GlslShaderSourceTransformer.BackendCoordinates.values()) {
            GlslShaderSourceTransformer.FragmentVariant variant = variant(source, GlslShaderRuntime.CompileMode.DIRECT, false, backend, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");
            GlslStd140Layout.Member macroOffset = variant.uniformLayout().member("macroOffset");

            assertNotNull(macroOffset);
            assertAll(() -> assertEquals("vec2", macroOffset.type()), () -> assertTrue(variant.source().contains("vec2 macroOffset;")), () -> assertFalse(variant.source().contains("FM_UNIFORM")));
        }
    }

    @Test
    void preprocessesMacroGeneratedRequiredEntryPoints() {
        String source = """
                #define FM_DIRECT_ENTRY main
                void FM_DIRECT_ENTRY() {
                    gl_FragColor = vec4(1.0);
                }
                """;

        for (GlslShaderSourceTransformer.BackendCoordinates backend : GlslShaderSourceTransformer.BackendCoordinates.values()) {
            GlslShaderSourceTransformer.FragmentVariant variant = variant(source, GlslShaderRuntime.CompileMode.DIRECT, false, backend, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");

            assertAll(() -> assertTrue(variant.source().contains("void main()")), () -> assertFalse(variant.source().contains("FM_DIRECT_ENTRY")));
        }
    }

    @Test
    void preprocessesOnlyTheActiveBackendConditionalUniformBranch() {
        String source = """
                #if defined(VULKAN) && defined(gl_VertexID)
                uniform float backendValue;
                #else
                uniform vec2 backendValue;
                #endif
                void main() {
                    gl_FragColor = vec4(backendValue);
                }
                """;

        GlslShaderSourceTransformer.FragmentVariant openGl = variant(source, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");
        GlslShaderSourceTransformer.FragmentVariant vulkan = variant(source, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.VULKAN, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");

        assertAll(() -> assertEquals("vec2", openGl.uniformLayout().member("backendValue").type()), () -> assertEquals("float", vulkan.uniformLayout().member("backendValue").type()), () -> assertEquals(1, countOccurrences(openGl.source(), "backendValue;")), () -> assertEquals(1, countOccurrences(vulkan.source(), "backendValue;")), () -> assertFalse(openGl.source().contains("#if")), () -> assertFalse(vulkan.source().contains("#if")));
    }

    @Test
    void preprocessesAConditionalUniformEvenWhenOnlyOneBranchDeclaresIt() {
        String source = """
                #if defined(VULKAN) && defined(gl_VertexID)
                uniform float vulkanOnlyValue;
                #endif
                void main() {
                #if defined(VULKAN) && defined(gl_VertexID)
                    gl_FragColor = vec4(vulkanOnlyValue);
                #else
                    gl_FragColor = vec4(1.0);
                #endif
                }
                """;

        GlslShaderSourceTransformer.FragmentVariant openGl = variant(source, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");
        GlslShaderSourceTransformer.FragmentVariant vulkan = variant(source, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.VULKAN, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");

        assertAll(() -> assertNull(openGl.uniformLayout().member("vulkanOnlyValue")), () -> assertNotNull(vulkan.uniformLayout().member("vulkanOnlyValue")), () -> assertFalse(openGl.source().contains("#if")), () -> assertFalse(vulkan.source().contains("#if")));
    }

    @Test
    void preservesShadercPreprocessorDiagnosticsWithBackendContext() {
        GlslShaderSourceTransformer.ShaderTransformException failure = assertThrows(GlslShaderSourceTransformer.ShaderTransformException.class, () -> variants("#if\nvoid main() {}\n#endif", GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE));

        assertAll(() -> assertDiagnostic(failure, "OpenGL shader preprocessing failed"), () -> assertDiagnostic(failure, "fancymenu_runtime_fragment_preprocess.glsl"));
    }

    @Test
    void emitsStableUnambiguousContentIdentitiesThatTrackEveryPipelineInput() {
        String canonical = "void main() { gl_FragColor = vec4(1.0); }";
        String equivalentLegacy = "\uFEFF#version 120\r\nprecision mediump float;\r\nvoid main() { gl_FragColor = vec4(1.0); }\r\n";
        GlslShaderSourceTransformer.FragmentVariant base = variant(canonical, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");
        GlslShaderSourceTransformer.FragmentVariant repeated = variant(canonical, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");
        GlslShaderSourceTransformer.FragmentVariant normalizedEquivalent = variant(equivalentLegacy, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");
        GlslShaderSourceTransformer.FragmentVariant changedSource = variant("void main() { gl_FragColor = vec4(0.0); }", GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");
        GlslShaderSourceTransformer.FragmentVariant changedMode = variant(canonical, GlslShaderRuntime.CompileMode.AUTO, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");
        GlslShaderSourceTransformer.FragmentVariant changedCompatibility = variant(canonical, GlslShaderRuntime.CompileMode.DIRECT, true, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");
        GlslShaderSourceTransformer.FragmentVariant changedBackend = variant(canonical, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.VULKAN, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");
        GlslShaderSourceTransformer.FragmentVariant changedPass = variant(canonical, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.BUFFER, "direct_glfragcolor_compat");

        assertAll(() -> assertTrue(base.identity().matches("[0-9a-f]{64}")), () -> assertEquals(base.identity(), repeated.identity()), () -> assertEquals(base.identity(), normalizedEquivalent.identity()), () -> assertNotEquals(base.identity(), changedSource.identity()), () -> assertNotEquals(base.identity(), changedMode.identity()), () -> assertNotEquals(base.identity(), changedCompatibility.identity()), () -> assertNotEquals(base.identity(), changedBackend.identity()), () -> assertNotEquals(base.identity(), changedPass.identity()), () -> assertNotEquals(GlslShaderSourceTransformer.contentIdentity("ab", "c"), GlslShaderSourceTransformer.contentIdentity("a", "bc")));
    }

    @Test
    void describesAndGeneratesOpenGlImageCoordinatesAsAbsoluteBottomLeft() {
        GlslShaderSourceTransformer.FragmentVariant variant = coordinateVariant(GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE);
        GlslShaderSourceTransformer.CoordinateConvention convention = variant.coordinateConvention();

        assertAll(() -> assertEquals(GlslShaderSourceTransformer.BackendCoordinates.OPENGL, convention.backend()), () -> assertEquals(GlslShaderSourceTransformer.PassKind.IMAGE, convention.passKind()), () -> assertTrue(convention.rawFragCoordUsesBottomOrigin()), () -> assertTrue(convention.uvZeroStoredAtTextureVZero()), () -> assertTrue(convention.imageFragCoordIsAbsolute()), () -> assertTrue(variant.source().contains("fmGlFragCoord_FancyMenu() { return gl_FragCoord; }")), () -> assertTrue(variant.vertexSource().contains("fmLogicalPixel = fmRenderArea_FancyMenu.xy + fmUv * fmRenderArea_FancyMenu.zw;")), () -> assertTrue(variant.vertexSource().contains("fmPixel = fmLogicalPixel;")), () -> assertTrue(variant.vertexSource().contains("fmUv_FancyMenu = (fmLogicalPixel - fmAreaOffset) / fmAreaSize;")));
    }

    @Test
    void normalizesVulkanImageCoordinatesToAbsoluteBottomLeftAndFlipsImageUvGeometry() {
        GlslShaderSourceTransformer.FragmentVariant variant = coordinateVariant(GlslShaderSourceTransformer.BackendCoordinates.VULKAN, GlslShaderSourceTransformer.PassKind.IMAGE);
        GlslShaderSourceTransformer.CoordinateConvention convention = variant.coordinateConvention();

        assertAll(() -> assertTrue(convention.rawFragCoordUsesBottomOrigin()), () -> assertTrue(convention.uvZeroStoredAtTextureVZero()), () -> assertTrue(convention.imageFragCoordIsAbsolute()), () -> assertTrue(variant.source().contains("fmRenderTargetSize_FancyMenu.y - gl_FragCoord.y")), () -> assertTrue(variant.vertexSource().contains("fmRenderTargetSize_FancyMenu.y - fmLogicalPixel.y")), () -> assertTrue(variant.vertexSource().contains("fmUv_FancyMenu = (fmLogicalPixel - fmAreaOffset) / fmAreaSize;")));
    }

    @Test
    void keepsBufferCoordinatesTargetLocalWithTheBackendStorageConvention() {
        GlslShaderSourceTransformer.FragmentVariant openGl = coordinateVariant(GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.BUFFER);
        GlslShaderSourceTransformer.FragmentVariant vulkan = coordinateVariant(GlslShaderSourceTransformer.BackendCoordinates.VULKAN, GlslShaderSourceTransformer.PassKind.BUFFER);

        assertAll(() -> assertTrue(openGl.coordinateConvention().rawFragCoordUsesBottomOrigin()), () -> assertFalse(openGl.coordinateConvention().imageFragCoordIsAbsolute()), () -> assertFalse(vulkan.coordinateConvention().rawFragCoordUsesBottomOrigin()), () -> assertFalse(vulkan.coordinateConvention().imageFragCoordIsAbsolute()), () -> assertTrue(openGl.coordinateConvention().uvZeroStoredAtTextureVZero()), () -> assertTrue(vulkan.coordinateConvention().uvZeroStoredAtTextureVZero()), () -> assertTrue(openGl.source().contains("fmGlFragCoord_FancyMenu() { return gl_FragCoord; }")), () -> assertTrue(vulkan.source().contains("fmGlFragCoord_FancyMenu() { return gl_FragCoord; }")), () -> assertTrue(vulkan.vertexSource().contains("fmLogicalPixel = fmRenderArea_FancyMenu.xy + fmUv * fmRenderArea_FancyMenu.zw;")), () -> assertTrue(vulkan.vertexSource().contains("fmPixel = fmLogicalPixel;")));
    }

    @Test
    void generatesAnExactSixVertexUnitQuadAndPreservesFrontFacingAcrossTheVulkanImageFlip() {
        String normalWinding = "const vec2 fmVertices[6] = vec2[6](vec2(0.0, 0.0), vec2(1.0, 0.0), vec2(1.0, 1.0), vec2(0.0, 0.0), vec2(1.0, 1.0), vec2(0.0, 1.0));";
        String reversedWinding = "const vec2 fmVertices[6] = vec2[6](vec2(0.0, 0.0), vec2(1.0, 1.0), vec2(1.0, 0.0), vec2(0.0, 0.0), vec2(0.0, 1.0), vec2(1.0, 1.0));";

        for (GlslShaderSourceTransformer.BackendCoordinates backend : GlslShaderSourceTransformer.BackendCoordinates.values()) {
            for (GlslShaderSourceTransformer.PassKind passKind : GlslShaderSourceTransformer.PassKind.values()) {
                String vertexSource = coordinateVariant(backend, passKind).vertexSource();
                String expectedVertices = backend == GlslShaderSourceTransformer.BackendCoordinates.VULKAN && passKind == GlslShaderSourceTransformer.PassKind.IMAGE ? reversedWinding : normalWinding;
                assertAll(() -> assertTrue(vertexSource.contains(expectedVertices)), () -> assertTrue(vertexSource.contains("vec2 fmUv = fmVertices[gl_VertexID];")), () -> assertTrue(vertexSource.contains("fmUv_FancyMenu = (fmLogicalPixel - fmAreaOffset) / fmAreaSize;")));
            }
        }
    }

    @Test
    void givesShadertoyAreaLocalCoordinatesAfterBackendNormalization() {
        String source = "void mainImage(out vec4 color, in vec2 coordinate) { color = vec4(coordinate, 0.0, 1.0); }";

        GlslShaderSourceTransformer.FragmentVariant openGl = onlyVariant(source, GlslShaderRuntime.CompileMode.SHADERTOY, false, GlslShaderSourceTransformer.BackendCoordinates.OPENGL, GlslShaderSourceTransformer.PassKind.IMAGE);
        GlslShaderSourceTransformer.FragmentVariant vulkan = onlyVariant(source, GlslShaderRuntime.CompileMode.SHADERTOY, false, GlslShaderSourceTransformer.BackendCoordinates.VULKAN, GlslShaderSourceTransformer.PassKind.IMAGE);

        assertAll(() -> assertTrue(openGl.source().contains("mainImage(fmColor_FancyMenu, gl_FragCoord.xy - fmAreaOffset);")), () -> assertTrue(vulkan.source().contains("mainImage(fmColor_FancyMenu, gl_FragCoord.xy - fmAreaOffset);")), () -> assertTrue(vulkan.source().contains("#define gl_FragCoord fmGlFragCoord_FancyMenu()")));
    }

    @Test
    void diagnosesReservedAndMismatchedBuiltInUniforms() {
        GlslShaderSourceTransformer.ShaderTransformException reserved = transformFailure("uniform vec4 fmRenderArea_FancyMenu;\nvoid main() {}");
        GlslShaderSourceTransformer.ShaderTransformException reservedSampler = transformFailure("uniform float iChannel0;\nvoid main() {}");
        GlslShaderSourceTransformer.ShaderTransformException wrongType = transformFailure("uniform vec4 iTime;\nvoid main() {}");
        GlslShaderSourceTransformer.ShaderTransformException wrongArray = transformFailure("uniform float iChannelTime[3];\nvoid main() {}");

        assertAll(() -> assertDiagnostic(reserved, "reserved"), () -> assertDiagnostic(reserved, "fmRenderArea_FancyMenu"), () -> assertDiagnostic(reservedSampler, "reserved sampler"), () -> assertDiagnostic(reservedSampler, "iChannel0"), () -> assertDiagnostic(reservedSampler, "sampler2D"), () -> assertDiagnostic(wrongType, "iTime"), () -> assertDiagnostic(wrongType, "float iTime;"), () -> assertDiagnostic(wrongArray, "iChannelTime"), () -> assertDiagnostic(wrongArray, "[4]"));
    }

    @Test
    void diagnosesDuplicateOpaqueAndUnsupportedUniforms() {
        GlslShaderSourceTransformer.ShaderTransformException duplicateLoose = transformFailure("uniform float value;\nuniform int value;\nvoid main() {}");
        GlslShaderSourceTransformer.ShaderTransformException duplicateSampler = transformFailure("uniform sampler2D noise;\nuniform sampler2D noise;\nvoid main() {}");
        GlslShaderSourceTransformer.ShaderTransformException opaque = transformFailure("uniform image2D pixels;\nvoid main() {}");
        GlslShaderSourceTransformer.ShaderTransformException unsupported = transformFailure("uniform double preciseValue;\nvoid main() {}");

        assertAll(() -> assertDiagnostic(duplicateLoose, "Duplicate loose uniform"), () -> assertDiagnostic(duplicateSampler, "Duplicate sampler"), () -> assertDiagnostic(opaque, "Opaque uniform type 'image2D'"), () -> assertDiagnostic(unsupported, "double"));
    }

    @Test
    void diagnosesUniformBlocksSamplerBoundariesAndInvalidArrays() {
        GlslShaderSourceTransformer.ShaderTransformException block = transformFailure("uniform LegacyBlock { float value; } legacy;\nvoid main() {}");
        GlslShaderSourceTransformer.ShaderTransformException samplerArray = transformFailure("uniform sampler2D textures[2];\nvoid main() {}");
        GlslShaderSourceTransformer.ShaderTransformException samplerType = transformFailure("uniform samplerCube environment;\nvoid main() {}");
        GlslShaderSourceTransformer.ShaderTransformException symbolicArray = transformFailure("uniform float values[COUNT];\nvoid main() {}");
        GlslShaderSourceTransformer.ShaderTransformException zeroArray = transformFailure("uniform float values[0];\nvoid main() {}");
        GlslShaderSourceTransformer.ShaderTransformException unterminated = transformFailure("uniform float value");

        assertAll(() -> assertDiagnostic(block, "Legacy uniform block 'LegacyBlock'"), () -> assertDiagnostic(samplerArray, "Sampler array 'textures'"), () -> assertDiagnostic(samplerType, "only sampler2D"), () -> assertDiagnostic(symbolicArray, "positive integer literal"), () -> assertDiagnostic(zeroArray, "positive integer literal"), () -> assertDiagnostic(unterminated, "Unterminated uniform declaration"), () -> assertThrows(UnsupportedOperationException.class, () -> block.diagnostics().clear()));
    }

    @Test
    void enforcesTheCrossBackendUniformBlockLimitBeforePipelineCompilation() {
        String baselineSource = "void main() { gl_FragColor = vec4(1.0); }";
        int builtInSize = variant(baselineSource, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.VULKAN, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat").uniformLayout().size();
        int exactUserArrayLength = (GlslStd140Layout.MAX_UNIFORM_BLOCK_SIZE - builtInSize) / 16;
        String exactSource = "uniform float values[" + exactUserArrayLength + "];\nvoid main() { gl_FragColor = vec4(values[0]); }";
        String oneStrideOverSource = "uniform float values[" + (exactUserArrayLength + 1) + "];\nvoid main() { gl_FragColor = vec4(values[0]); }";
        String pathologicalSource = "uniform mat4 values[" + Integer.MAX_VALUE + "];\nvoid main() { gl_FragColor = values[0][0]; }";

        assertEquals(0, (GlslStd140Layout.MAX_UNIFORM_BLOCK_SIZE - builtInSize) % 16);
        for (GlslShaderSourceTransformer.BackendCoordinates backend : GlslShaderSourceTransformer.BackendCoordinates.values()) {
            GlslShaderSourceTransformer.FragmentVariant exact = variant(exactSource, GlslShaderRuntime.CompileMode.DIRECT, false, backend, GlslShaderSourceTransformer.PassKind.IMAGE, "direct_glfragcolor_compat");
            GlslShaderSourceTransformer.ShaderTransformException oneStrideOver = transformFailure(oneStrideOverSource, backend);
            GlslShaderSourceTransformer.ShaderTransformException pathological = transformFailure(pathologicalSource, backend);

            assertAll(() -> assertEquals(GlslStd140Layout.MAX_UNIFORM_BLOCK_SIZE, exact.uniformLayout().size()), () -> assertDiagnostic(oneStrideOver, "cross-backend limit"), () -> assertDiagnostic(oneStrideOver, Integer.toString(GlslStd140Layout.MAX_UNIFORM_BLOCK_SIZE)), () -> assertDiagnostic(pathological, "cross-backend limit"), () -> assertDiagnostic(pathological, "values"));
        }
    }

    private static GlslShaderSourceTransformer.FragmentVariant coordinateVariant(GlslShaderSourceTransformer.BackendCoordinates backend, GlslShaderSourceTransformer.PassKind passKind) {
        String source = "void main() { gl_FragColor = vec4(gl_FragCoord.xy, fmUv_FancyMenu); }";
        return variant(source, GlslShaderRuntime.CompileMode.DIRECT, false, backend, passKind, "direct_glfragcolor_compat");
    }

    private static GlslShaderSourceTransformer.ShaderTransformException transformFailure(String source) {
        return transformFailure(source, GlslShaderSourceTransformer.BackendCoordinates.VULKAN);
    }

    private static GlslShaderSourceTransformer.ShaderTransformException transformFailure(String source, GlslShaderSourceTransformer.BackendCoordinates backend) {
        return assertThrows(GlslShaderSourceTransformer.ShaderTransformException.class, () -> variants(source, GlslShaderRuntime.CompileMode.DIRECT, false, backend, GlslShaderSourceTransformer.PassKind.IMAGE));
    }

    private static void assertDiagnostic(GlslShaderSourceTransformer.ShaderTransformException exception, String expectedText) {
        assertTrue(exception.diagnostics().stream().anyMatch(diagnostic -> diagnostic.contains(expectedText)), () -> "Expected diagnostic containing '" + expectedText + "' but got " + exception.diagnostics());
    }

    private static GlslShaderSourceTransformer.FragmentVariant onlyVariant(String source, GlslShaderRuntime.CompileMode mode, boolean forceCompatibility, GlslShaderSourceTransformer.BackendCoordinates backend, GlslShaderSourceTransformer.PassKind passKind) {
        List<GlslShaderSourceTransformer.FragmentVariant> variants = variants(source, mode, forceCompatibility, backend, passKind);
        assertEquals(1, variants.size());
        return variants.getFirst();
    }

    private static GlslShaderSourceTransformer.FragmentVariant variant(String source, GlslShaderRuntime.CompileMode mode, boolean forceCompatibility, GlslShaderSourceTransformer.BackendCoordinates backend, GlslShaderSourceTransformer.PassKind passKind, String label) {
        return variants(source, mode, forceCompatibility, backend, passKind).stream().filter(variant -> variant.label().equals(label)).findFirst().orElseThrow(() -> new AssertionError("Missing fragment variant '" + label + "'."));
    }

    private static List<GlslShaderSourceTransformer.FragmentVariant> variants(String source, GlslShaderRuntime.CompileMode mode, boolean forceCompatibility, GlslShaderSourceTransformer.BackendCoordinates backend, GlslShaderSourceTransformer.PassKind passKind) {
        return GlslShaderSourceTransformer.buildFragmentVariants(source, mode, forceCompatibility, backend, passKind);
    }

    private static int countOccurrences(String source, String text) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(text, index)) >= 0) {
            count++;
            index += text.length();
        }
        return count;
    }

}
