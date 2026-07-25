package de.keksuccino.fancymenu.util.rendering.glsl;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.BlendOp;
import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlslGpuPipelineCacheTest {

    @Test
    void preservesLegacySeparateColorAndAlphaBlendFactors() {
        BlendFunction blend = GlslGpuPipelineCache.LEGACY_IMAGE_BLEND;

        assertAll(() -> assertEquals(BlendFactor.SRC_ALPHA, blend.color().sourceFactor()), () -> assertEquals(BlendFactor.ONE_MINUS_SRC_ALPHA, blend.color().destFactor()), () -> assertEquals(BlendOp.ADD, blend.color().op()), () -> assertEquals(BlendFactor.ONE, blend.alpha().sourceFactor()), () -> assertEquals(BlendFactor.ZERO, blend.alpha().destFactor()), () -> assertEquals(BlendOp.ADD, blend.alpha().op()), () -> assertNotEquals(BlendFunction.OVERLAY, blend), () -> assertNotEquals(BlendFunction.TRANSLUCENT, blend));
    }

    @Test
    void reusesOnlyExactShaderFormatAndBlendCacheKeys() {
        GlslShaderSourceTransformer.FragmentVariant variant = variant("void main() { gl_FragColor = vec4(1.0); }");

        GlslGpuPipelineCache.PipelineBundle first = GlslGpuPipelineCache.getOrCreate(variant, GpuFormat.RGBA8_UNORM, false);
        GlslGpuPipelineCache.PipelineBundle repeated = GlslGpuPipelineCache.getOrCreate(variant, GpuFormat.RGBA8_UNORM, false);
        GlslGpuPipelineCache.PipelineBundle blended = GlslGpuPipelineCache.getOrCreate(variant, GpuFormat.RGBA8_UNORM, true);
        GlslGpuPipelineCache.PipelineBundle floatingPoint = GlslGpuPipelineCache.getOrCreate(variant, GpuFormat.RGBA16_FLOAT, false);

        assertAll(() -> assertSame(first, repeated), () -> assertNotSame(first, blended), () -> assertNotSame(first, floatingPoint), () -> assertNotEquals(first.pipelineIdentity(), blended.pipelineIdentity()), () -> assertNotEquals(first.pipelineIdentity(), floatingPoint.pipelineIdentity()));
    }

    @Test
    void declaresOnlyReferencedBuiltInAndCustomSamplersInThePipelineLayout() {
        String source = """
                uniform sampler2D customNoise;
                // iChannel0 is intentionally inactive.
                void main() {
                    gl_FragColor = texture(customNoise, fmUv_FancyMenu) + texture(iChannel2, fmUv_FancyMenu);
                }
                """;
        GlslShaderSourceTransformer.FragmentVariant variant = variant(source);

        GlslGpuPipelineCache.PipelineBundle bundle = GlslGpuPipelineCache.getOrCreate(variant, GpuFormat.RGBA16_FLOAT, false);

        assertAll(() -> assertEquals(List.of("iChannel2", "customNoise"), variant.activeSamplerNames()), () -> assertEquals(List.of("iChannel2", "customNoise"), bundle.pipeline().getBindGroupLayouts().getFirst().getSamplers()), () -> assertEquals(List.of(GlslShaderSourceTransformer.UNIFORM_BLOCK_NAME), bundle.pipeline().getBindGroupLayouts().getFirst().getUniforms().stream().map(uniform -> uniform.name()).toList()));
    }

    @Test
    void buildsExactUnblendedTriangleStateAndRoutesOnlyMatchingStageSources() {
        GlslShaderSourceTransformer.FragmentVariant variant = variant("void main() { gl_FragColor = vec4(0.25); }");
        GlslGpuPipelineCache.PipelineBundle bundle = GlslGpuPipelineCache.getOrCreate(variant, GpuFormat.RGBA8_UNORM, false);
        Identifier expectedVertexId = Identifier.fromNamespaceAndPath("fancymenu", "runtime_glsl/vertex/" + variant.identity());
        Identifier expectedFragmentId = Identifier.fromNamespaceAndPath("fancymenu", "runtime_glsl/fragment/" + variant.identity());
        Identifier unrelatedId = Identifier.fromNamespaceAndPath("fancymenu", "runtime_glsl/fragment/unrelated");
        ColorTargetState targetState = bundle.pipeline().getColorTargetState();

        assertNotNull(targetState);
        assertAll(() -> assertEquals(PrimitiveTopology.TRIANGLES, bundle.pipeline().getPrimitiveTopology()), () -> assertFalse(bundle.pipeline().isCull()), () -> assertEquals(GpuFormat.RGBA8_UNORM, targetState.format()), () -> assertEquals(ColorTargetState.WRITE_ALL, targetState.writeMask()), () -> assertTrue(targetState.blendFunction().isEmpty()), () -> assertEquals(expectedVertexId, bundle.pipeline().getVertexShader()), () -> assertEquals(expectedFragmentId, bundle.pipeline().getFragmentShader()), () -> assertEquals(variant.vertexSource(), bundle.shaderSource().get(expectedVertexId, ShaderType.VERTEX)), () -> assertEquals(variant.source(), bundle.shaderSource().get(expectedFragmentId, ShaderType.FRAGMENT)), () -> assertNull(bundle.shaderSource().get(expectedVertexId, ShaderType.FRAGMENT)), () -> assertNull(bundle.shaderSource().get(expectedFragmentId, ShaderType.VERTEX)), () -> assertNull(bundle.shaderSource().get(unrelatedId, ShaderType.FRAGMENT)));
    }

    private static GlslShaderSourceTransformer.FragmentVariant variant(String source) {
        return GlslShaderSourceTransformer.buildFragmentVariants(source, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.VULKAN, GlslShaderSourceTransformer.PassKind.IMAGE).stream().filter(variant -> variant.label().equals("direct_glfragcolor_compat")).findFirst().orElseThrow();
    }

}
