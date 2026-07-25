package de.keksuccino.fancymenu.util.rendering.glsl;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Content-addressed dynamic pipelines shared by all GLSL elements, menu backgrounds, and decoration overlays. */
final class GlslGpuPipelineCache {

    static final BlendFunction LEGACY_IMAGE_BLEND = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ONE, BlendFactor.ZERO);

    private static final Map<PipelineKey, PipelineBundle> PIPELINES = new HashMap<>();
    private static long generation;

    static {
        MinecraftResourceReloadObserver.addReloadListener(action -> {
            if (action == MinecraftResourceReloadObserver.ReloadAction.STARTING) {
                synchronized (PIPELINES) {
                    PIPELINES.clear();
                    generation++;
                }
            }
        });
    }

    private GlslGpuPipelineCache() {
    }

    static long generation() {
        synchronized (PIPELINES) {
            return generation;
        }
    }

    @NotNull
    static PipelineBundle getOrCreate(@NotNull GlslShaderSourceTransformer.FragmentVariant variant, @NotNull GpuFormat targetFormat, boolean blend) {
        PipelineKey key = new PipelineKey(variant.identity(), targetFormat, blend);
        synchronized (PIPELINES) {
            return PIPELINES.computeIfAbsent(key, ignored -> createPipeline(variant, targetFormat, blend));
        }
    }

    @NotNull
    private static PipelineBundle createPipeline(@NotNull GlslShaderSourceTransformer.FragmentVariant variant, @NotNull GpuFormat targetFormat, boolean blend) {
        String pipelineIdentity = GlslShaderSourceTransformer.contentIdentity(variant.identity(), targetFormat.name(), Boolean.toString(blend));
        Identifier vertexId = Identifier.fromNamespaceAndPath("fancymenu", "runtime_glsl/vertex/" + variant.identity());
        Identifier fragmentId = Identifier.fromNamespaceAndPath("fancymenu", "runtime_glsl/fragment/" + variant.identity());
        Identifier pipelineId = Identifier.fromNamespaceAndPath("fancymenu", "runtime_glsl/pipeline/" + pipelineIdentity);

        BindGroupLayout.Builder bindGroupBuilder = BindGroupLayout.builder().withUniform(GlslShaderSourceTransformer.UNIFORM_BLOCK_NAME, UniformType.UNIFORM_BUFFER);
        for (String samplerName : variant.activeSamplerNames()) {
            bindGroupBuilder.withSampler(samplerName);
        }

        ColorTargetState colorTargetState = new ColorTargetState(blend ? Optional.of(LEGACY_IMAGE_BLEND) : Optional.empty(), targetFormat, ColorTargetState.WRITE_ALL);
        RenderPipeline pipeline = RenderPipeline.builder()
                .withLocation(pipelineId)
                .withVertexShader(vertexId)
                .withFragmentShader(fragmentId)
                .withBindGroupLayout(bindGroupBuilder.build())
                .withColorTargetState(colorTargetState)
                .withCull(false)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .build();
        ShaderSource shaderSource = (id, type) -> resolveSource(id, type, vertexId, fragmentId, variant);
        return new PipelineBundle(pipeline, shaderSource, variant, pipelineIdentity);
    }

    private static String resolveSource(@NotNull Identifier id, @NotNull ShaderType type, @NotNull Identifier vertexId, @NotNull Identifier fragmentId, @NotNull GlslShaderSourceTransformer.FragmentVariant variant) {
        if (type == ShaderType.VERTEX && id.equals(vertexId)) {
            return variant.vertexSource();
        }
        if (type == ShaderType.FRAGMENT && id.equals(fragmentId)) {
            return variant.source();
        }
        return null;
    }

    record PipelineBundle(@NotNull RenderPipeline pipeline, @NotNull ShaderSource shaderSource, @NotNull GlslShaderSourceTransformer.FragmentVariant variant, @NotNull String pipelineIdentity) {

        @NotNull
        CompilationResult precompile() {
            GpuDevice device = RenderSystem.getDevice();
            try {
                // Resource reload clears Minecraft's backend cache while this content object can remain live. Supplying the
                // exact source callback on every lookup prevents a post-reload miss from falling back to ShaderManager.
                CompiledRenderPipeline compiled = device.precompilePipeline(this.pipeline, this.shaderSource);
                if (compiled.isValid()) {
                    return new CompilationResult(true, List.of());
                }
            } catch (Exception ex) {
                List<String> diagnostics = new ArrayList<>();
                diagnostics.add("Pipeline setup failed: " + safeMessage(ex));
                appendBackendMessages(device, diagnostics);
                return new CompilationResult(false, List.copyOf(diagnostics));
            }

            GlslShaderValidator.ValidationResult validation = GlslShaderValidator.validate(this.variant.vertexSource(), this.variant.source());
            List<String> diagnostics = new ArrayList<>(validation.diagnostics());
            if (diagnostics.isEmpty()) {
                diagnostics.add("The backend rejected the reflected or linked pipeline. Consult the game log for the backend's link diagnostics.");
            }
            appendBackendMessages(device, diagnostics);
            diagnostics.add("Pipeline content ID: " + this.pipelineIdentity);
            return new CompilationResult(false, List.copyOf(diagnostics));
        }

        private static void appendBackendMessages(@NotNull GpuDevice device, @NotNull List<String> diagnostics) {
            try {
                for (String message : device.getLastDebugMessages()) {
                    if (message != null && !message.isBlank() && !diagnostics.contains(message)) {
                        diagnostics.add("Backend: " + message.strip());
                    }
                }
            } catch (Exception ignored) {
            }
        }

        @NotNull
        private static String safeMessage(@NotNull Exception exception) {
            String message = exception.getMessage();
            return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
        }
    }

    record CompilationResult(boolean valid, @NotNull List<String> diagnostics) {
    }

    private record PipelineKey(@NotNull String shaderIdentity, @NotNull GpuFormat targetFormat, boolean blend) {
    }
}
