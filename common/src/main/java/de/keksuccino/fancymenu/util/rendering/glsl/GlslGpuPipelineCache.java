package de.keksuccino.fancymenu.util.rendering.glsl;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.blaze3d.pipeline.PipelineCache;
import org.jetbrains.annotations.Nullable;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.device.GpuDevice;
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
                clear();
            }
        });
    }

    private GlslGpuPipelineCache() {
    }

    static void clear() {
        synchronized (PIPELINES) {
            PIPELINES.values().forEach(PipelineBundle::close);
            PIPELINES.clear();
            generation++;
        }
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
            bindGroupBuilder.withUniform(samplerName, UniformType.COMBINED_IMAGE_SAMPLER);
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
        ShaderSource shaderSource = new ShaderSource() {

            @Override
            public @Nullable String getShader(Identifier id, ShaderType type) {
                return resolveSource(id, type, vertexId, fragmentId, variant);
            }

            @Override
            public @Nullable CachedIncludeSource getInclude(Identifier id) {
                return null;
            }

            @Override
            public void close() {
            }

        };
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

    static final class PipelineBundle implements AutoCloseable {

        private final RenderPipeline pipeline;
        private final ShaderSource shaderSource;
        private final GlslShaderSourceTransformer.FragmentVariant variant;
        private final String pipelineIdentity;
        @Nullable private PipelineCache cache;

        private PipelineBundle(RenderPipeline pipeline, ShaderSource shaderSource, GlslShaderSourceTransformer.FragmentVariant variant, String pipelineIdentity) {
            this.pipeline = pipeline;
            this.shaderSource = shaderSource;
            this.variant = variant;
            this.pipelineIdentity = pipelineIdentity;
        }

        RenderPipeline pipeline() {
            return this.pipeline;
        }

        GlslShaderSourceTransformer.FragmentVariant variant() {
            return this.variant;
        }

        ShaderSource shaderSource() {
            return this.shaderSource;
        }

        String pipelineIdentity() {
            return this.pipelineIdentity;
        }

        private PipelineCache cache() {
            if (this.cache == null) this.cache = new PipelineCache(RenderSystem.getDevice(), this.shaderSource);
            return this.cache;
        }

        @Override
        public void close() {
            if (this.cache != null) {
                this.cache.close();
                this.cache = null;
            }
        }

        @NotNull
        CompiledRenderPipeline compiledPipeline() {
            return java.util.Objects.requireNonNull(this.cache().get(this.pipeline), "GLSL pipeline failed to compile");
        }

        @NotNull
        CompilationResult precompile() {
            GpuDevice device = RenderSystem.getDevice();
            try {
                // Dynamic sources have their own cache; resource reload closes it along with the compiled GPU pipelines.
                CompiledRenderPipeline compiled = this.cache().get(this.pipeline);
                if (compiled != null && !compiled.isClosed()) {
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
