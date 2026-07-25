package de.keksuccino.fancymenu.util.rendering.glsl;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGuiGraphicsExtractor;
import de.keksuccino.fancymenu.util.rendering.GuiRenderPhaseAction;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Backend-neutral runtime for FancyMenu's Image and Buffer A-D GLSL passes. */
public class GlslShaderRuntime {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int CHANNEL_COUNT = 4;
    private static final int BUFFER_PASS_COUNT = 4;
    private static final int TOTAL_PASS_COUNT = BUFFER_PASS_COUNT + 1;
    private static final int IMAGE_PASS_INDEX = BUFFER_PASS_COUNT;

    @Nullable
    private String lastCompileError;
    private boolean sourceMissing;
    private final ProgramState[] passPrograms_FancyMenu = new ProgramState[TOTAL_PASS_COUNT];
    private final GlslPingPongTarget[] bufferTargets_FancyMenu = new GlslPingPongTarget[BUFFER_PASS_COUNT];
    private final GlslFrameUniformState frameUniformState_FancyMenu = new GlslFrameUniformState();
    @Nullable
    private TextureTarget fallbackTextureTarget_FancyMenu;

    public GlslShaderRuntime() {
        for (int i = 0; i < TOTAL_PASS_COUNT; i++) {
            this.passPrograms_FancyMenu[i] = new ProgramState();
        }
        for (int i = 0; i < BUFFER_PASS_COUNT; i++) {
            this.bufferTargets_FancyMenu[i] = new GlslPingPongTarget("FancyMenu GLSL Buffer " + (char) ('A' + i));
        }
    }

    public enum CompileMode {
        AUTO,
        DIRECT,
        SHADERTOY
    }

    public enum ChannelInput {
        NONE("none", -1),
        RESOURCE0("resource0", -1),
        RESOURCE1("resource1", -1),
        RESOURCE2("resource2", -1),
        RESOURCE3("resource3", -1),
        BUFFER_A("buffer_a", 0),
        BUFFER_B("buffer_b", 1),
        BUFFER_C("buffer_c", 2),
        BUFFER_D("buffer_d", 3);

        @NotNull
        private final String serializedName;
        private final int bufferPassIndex;

        ChannelInput(@NotNull String serializedName, int bufferPassIndex) {
            this.serializedName = serializedName;
            this.bufferPassIndex = bufferPassIndex;
        }

        @NotNull
        public String serializedName() {
            return this.serializedName;
        }

        public int bufferPassIndex() {
            return this.bufferPassIndex;
        }

        @NotNull
        public static ChannelInput fromSerialized(@Nullable String serialized, @NotNull ChannelInput fallback) {
            if (serialized == null || serialized.isBlank()) {
                return fallback;
            }
            for (ChannelInput value : values()) {
                if (value.serializedName.equalsIgnoreCase(serialized.trim())) {
                    return value;
                }
            }
            return fallback;
        }
    }

    public record ChannelRouting(@NotNull ChannelInput channel0, @NotNull ChannelInput channel1, @NotNull ChannelInput channel2, @NotNull ChannelInput channel3) {

        @NotNull
        public ChannelInput channelForIndex(int index) {
            return switch (index) {
                case 0 -> this.channel0;
                case 1 -> this.channel1;
                case 2 -> this.channel2;
                case 3 -> this.channel3;
                default -> ChannelInput.NONE;
            };
        }

        @NotNull
        public static ChannelRouting defaultResources() {
            return new ChannelRouting(ChannelInput.RESOURCE0, ChannelInput.RESOURCE1, ChannelInput.RESOURCE2, ChannelInput.RESOURCE3);
        }

        @NotNull
        public static ChannelRouting defaultNone() {
            return new ChannelRouting(ChannelInput.NONE, ChannelInput.NONE, ChannelInput.NONE, ChannelInput.NONE);
        }
    }

    public record RenderSettings(@NotNull CompileMode compileMode, boolean forceShadertoyCompatibility, float timeScale, boolean freezeTime, boolean enableBlend, boolean useInput, boolean mousePositionRequiresHold, float opacity, @Nullable ResourceSupplier<ITexture> channel0, @Nullable ResourceSupplier<ITexture> channel1, @Nullable ResourceSupplier<ITexture> channel2, @Nullable ResourceSupplier<ITexture> channel3, @Nullable String bufferASource, @Nullable String bufferBSource, @Nullable String bufferCSource, @Nullable String bufferDSource, @NotNull ChannelRouting imageRouting, @NotNull ChannelRouting bufferARouting, @NotNull ChannelRouting bufferBRouting, @NotNull ChannelRouting bufferCRouting, @NotNull ChannelRouting bufferDRouting) {

        @Nullable
        public ResourceSupplier<ITexture> channelSupplier(int index) {
            return switch (index) {
                case 0 -> this.channel0;
                case 1 -> this.channel1;
                case 2 -> this.channel2;
                case 3 -> this.channel3;
                default -> null;
            };
        }

        @Nullable
        public String bufferSource(int bufferPassIndex) {
            return switch (bufferPassIndex) {
                case 0 -> this.bufferASource;
                case 1 -> this.bufferBSource;
                case 2 -> this.bufferCSource;
                case 3 -> this.bufferDSource;
                default -> null;
            };
        }

        @NotNull
        public ChannelRouting routingForPass(int passIndex) {
            return switch (passIndex) {
                case 0 -> this.bufferARouting;
                case 1 -> this.bufferBRouting;
                case 2 -> this.bufferCRouting;
                case 3 -> this.bufferDRouting;
                case IMAGE_PASS_INDEX -> this.imageRouting;
                default -> ChannelRouting.defaultNone();
            };
        }
    }

    public boolean render(@NotNull GuiGraphicsExtractor graphics, int areaX, int areaY, int areaWidth, int areaHeight, float partialTick, @Nullable String fragmentSource, @NotNull RenderSettings settings) {
        if (!GlslOwnedResourceLifecycle.hasRenderableArea(areaWidth, areaHeight)) {
            // Destruction must remain ordered with extracted GUI actions. Closing here could invalidate resources owned
            // by an earlier action for this runtime before the render phase has had a chance to execute that action.
            // The one-pixel bound makes the vertexless action a valid GuiRenderState entry; it emits no visible geometry.
            ScreenRectangle releaseBounds = new ScreenRectangle(areaX, areaY, 1, 1).transformMaxBounds(graphics.pose());
            var renderState = ((IMixinGuiGraphicsExtractor) graphics).get_guiRenderState_FancyMenu();
            renderState.nextStratum();
            renderState.addGuiElement(new GlslResourceReleaseRenderState(this, releaseBounds));
            renderState.nextStratum();
            return false;
        }

        this.sourceMissing = fragmentSource == null || fragmentSource.isBlank();
        if (this.sourceMissing) {
            this.lastCompileError = null;
        }

        ScreenRectangle bounds = new ScreenRectangle(areaX, areaY, areaWidth, areaHeight).transformAxisAligned(graphics.pose());
        var renderState = ((IMixinGuiGraphicsExtractor) graphics).get_guiRenderState_FancyMenu();
        renderState.nextStratum();
        renderState.addGuiElement(new GlslRenderState(this, areaX, areaY, areaWidth, areaHeight, partialTick, fragmentSource, settings, bounds));
        renderState.nextStratum();
        RenderingUtils.resetShaderColor(graphics);
        return !this.sourceMissing;
    }

    private boolean renderNow(int areaX, int areaY, int areaWidth, int areaHeight, float partialTick, @Nullable String fragmentSource, @NotNull RenderSettings settings) {
        RenderSystem.assertOnRenderThread();
        boolean imageSourceActive = hasShaderSource(fragmentSource);
        boolean[] activeBufferPasses = resolveActiveBufferPasses(settings);
        // Reconcile source-owned resources before draw-related early returns. An off-screen pass must not keep large
        // feedback targets alive after its source was disabled, and active passes must retain their frame history.
        GlslPassResourceLifecycle.releaseUnusedBufferPasses(imageSourceActive, activeBufferPasses, this::releaseBufferPassResources);
        if (!imageSourceActive) {
            this.deactivateProgram(this.passPrograms_FancyMenu[IMAGE_PASS_INDEX]);
            this.passPrograms_FancyMenu[IMAGE_PASS_INDEX].passFrames.deactivate();
            this.sourceMissing = true;
            this.lastCompileError = getPassName(IMAGE_PASS_INDEX) + ": Shader source is missing.";
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        RenderTarget mainTarget = minecraft.gameRenderer.mainRenderTarget();
        GpuTextureView mainColorView = mainTarget.getColorTextureView();
        if (mainColorView == null || mainColorView.isClosed()) {
            this.lastCompileError = "Render error: the main GPU color target is unavailable.";
            return false;
        }

        double guiScale = window.getGuiScale();
        int screenWidthPx = mainTarget.width;
        int screenHeightPx = mainTarget.height;
        int areaXPx = Mth.floor(areaX * guiScale);
        int areaYPxTop = Mth.floor(areaY * guiScale);
        int areaWidthPx = Math.max(1, Mth.floor(areaWidth * guiScale));
        int areaHeightPx = Math.max(1, Mth.floor(areaHeight * guiScale));
        int areaYPxBottom = screenHeightPx - areaYPxTop - areaHeightPx;
        int viewportX = Math.max(0, areaXPx);
        int viewportYBottom = Math.max(0, areaYPxBottom);
        int viewportRight = Math.min(screenWidthPx, areaXPx + areaWidthPx);
        int viewportTop = Math.min(screenHeightPx, areaYPxBottom + areaHeightPx);
        int viewportWidth = Math.max(0, viewportRight - viewportX);
        int viewportHeight = Math.max(0, viewportTop - viewportYBottom);
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            return false;
        }

        GlslShaderSourceTransformer.BackendCoordinates backendCoordinates = RenderingUtils.isVulkanActive() ? GlslShaderSourceTransformer.BackendCoordinates.VULKAN : GlslShaderSourceTransformer.BackendCoordinates.OPENGL;
        this.sourceMissing = false;
        this.lastCompileError = null;

        for (int passIndex = 0; passIndex < BUFFER_PASS_COUNT; passIndex++) {
            if (!activeBufferPasses[passIndex]) {
                continue;
            }
            if (!this.ensureProgramForPass(passIndex, settings.bufferSource(passIndex), settings, false, GpuFormat.RGBA16_FLOAT, backendCoordinates)) {
                return false;
            }
        }
        if (!this.ensureProgramForPass(IMAGE_PASS_INDEX, fragmentSource, settings, true, mainColorView.texture().getFormat(), backendCoordinates)) {
            return false;
        }

        try {
            for (int passIndex = 0; passIndex < BUFFER_PASS_COUNT; passIndex++) {
                if (activeBufferPasses[passIndex]) {
                    ProgramState program = this.passPrograms_FancyMenu[passIndex];
                    GlslPingPongTarget target = this.bufferTargets_FancyMenu[passIndex];
                    String historyIdentity = buildPassHistoryIdentity(program, settings.routingForPass(passIndex));
                    if (program.passFrames.historyIdentityChanged(historyIdentity)) {
                        target.close();
                    }
                    boolean storageRecreated = target.ensureSize(areaWidthPx, areaHeightPx);
                    program.passFrames.activate(historyIdentity, storageRecreated);
                }
            }
            ProgramState imageProgram = this.passPrograms_FancyMenu[IMAGE_PASS_INDEX];
            imageProgram.passFrames.activate(buildPassHistoryIdentity(imageProgram, settings.routingForPass(IMAGE_PASS_INDEX)), false);
            ChannelTextureState fallback = this.resolveFallbackTextureState();
            ChannelTextureState[] externalChannels = this.resolveExternalChannelTextureStates(minecraft, settings, fallback);
            GlslFrameUniformState.FrameSnapshot frameSnapshot = this.frameUniformState_FancyMenu.capture(minecraft, window, settings, partialTick, areaX, areaY, areaWidth, areaHeight, areaWidthPx, areaHeightPx, screenWidthPx, screenHeightPx);
            List<PreparedPass> preparedPasses = this.preparePasses(settings, activeBufferPasses, externalChannels, fallback, frameSnapshot, mainColorView, areaXPx, areaYPxTop, areaYPxBottom, areaWidthPx, areaHeightPx, viewportX, viewportYBottom, viewportWidth, viewportHeight, screenWidthPx, screenHeightPx);
            this.executePasses(preparedPasses, activeBufferPasses);
            this.lastCompileError = null;
            return true;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed rendering backend-neutral GLSL shader.", ex);
            this.lastCompileError = "Render error: " + safeMessage(ex);
            return false;
        }
    }

    @NotNull
    private List<PreparedPass> preparePasses(@NotNull RenderSettings settings, @NotNull boolean[] activeBufferPasses, @NotNull ChannelTextureState[] externalChannels, @NotNull ChannelTextureState fallback, @NotNull GlslFrameUniformState.FrameSnapshot frameSnapshot, @NotNull GpuTextureView mainColorView, int areaXPx, int areaYPxTop, int areaYPxBottom, int areaWidthPx, int areaHeightPx, int viewportX, int viewportYBottom, int viewportWidth, int viewportHeight, int screenWidthPx, int screenHeightPx) {
        List<PreparedPass> prepared = new ArrayList<>();
        for (int passIndex = 0; passIndex < BUFFER_PASS_COUNT; passIndex++) {
            if (!activeBufferPasses[passIndex]) {
                continue;
            }
            ProgramState program = this.passPrograms_FancyMenu[passIndex];
            GlslPingPongTarget target = this.bufferTargets_FancyMenu[passIndex];
            GpuTextureView outputView = target.writeView();
            if (outputView == null || !target.isReady()) {
                throw new IllegalStateException(getPassName(passIndex) + " feedback target is unavailable.");
            }
            ChannelTextureState[] channels = this.resolveRoutedChannelStates(settings.routingForPass(passIndex), externalChannels, activeBufferPasses, fallback, passIndex, false);
            GlslFrameUniformState.PassContext passContext = new GlslFrameUniformState.PassContext(0, 0, 0, 0, 0, areaWidthPx, areaHeightPx, areaWidthPx, areaHeightPx, program.passFrames.currentFrame());
            this.updateUniformBuffer(program, frameSnapshot, passContext, channels);
            prepared.add(new PreparedPass(passIndex, program, outputView, channels));
        }

        ProgramState imageProgram = this.passPrograms_FancyMenu[IMAGE_PASS_INDEX];
        ChannelTextureState[] imageChannels = this.resolveRoutedChannelStates(settings.routingForPass(IMAGE_PASS_INDEX), externalChannels, activeBufferPasses, fallback, IMAGE_PASS_INDEX, true);
        GlslFrameUniformState.PassContext imageContext = new GlslFrameUniformState.PassContext(areaXPx, areaYPxTop, areaYPxBottom, viewportX, viewportYBottom, viewportWidth, viewportHeight, screenWidthPx, screenHeightPx, imageProgram.passFrames.currentFrame());
        this.updateUniformBuffer(imageProgram, frameSnapshot, imageContext, imageChannels);
        prepared.add(new PreparedPass(IMAGE_PASS_INDEX, imageProgram, mainColorView, imageChannels));
        return List.copyOf(prepared);
    }

    private void executePasses(@NotNull List<PreparedPass> preparedPasses, @NotNull boolean[] activeBufferPasses) {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        for (PreparedPass prepared : preparedPasses) {
            ProgramState program = prepared.program();
            if (program.pipeline == null || program.uniformBuffer == null) {
                throw new IllegalStateException(getPassName(prepared.passIndex()) + " was not fully prepared.");
            }
            try (RenderPass renderPass = encoder.createRenderPass(() -> "FancyMenu GLSL " + getPassName(prepared.passIndex()), prepared.outputView(), Optional.empty())) {
                renderPass.setPipeline(program.pipeline.pipeline());
                renderPass.setUniform(GlslShaderSourceTransformer.UNIFORM_BLOCK_NAME, program.uniformBuffer);
                this.bindActiveSamplers(renderPass, program.pipeline.variant().activeSamplerNames(), prepared.channels());
                renderPass.draw(6, 1, 0, 0);
            }
        }
        // Channel preparation points later passes at earlier passes' write views, which is equivalent to immediate
        // A-to-D swaps while allowing every logical swap to commit atomically only after every draw was encoded.
        for (int passIndex = 0; passIndex < BUFFER_PASS_COUNT; passIndex++) {
            if (activeBufferPasses[passIndex]) {
                this.bufferTargets_FancyMenu[passIndex].swap();
                this.passPrograms_FancyMenu[passIndex].passFrames.commitFrame();
            }
        }
        this.passPrograms_FancyMenu[IMAGE_PASS_INDEX].passFrames.commitFrame();
    }

    private void bindActiveSamplers(@NotNull RenderPass renderPass, @NotNull List<String> samplerNames, @NotNull ChannelTextureState[] channels) {
        for (String samplerName : samplerNames) {
            // Unknown sampler2D uniforms historically defaulted to OpenGL texture unit zero. Explicitly map them to
            // routed channel zero so that compatibility remains deterministic and Vulkan never sees an unbound descriptor.
            ChannelTextureState channel = channels[resolveSamplerChannelIndex(samplerName)];
            renderPass.bindTexture(samplerName, channel.view(), channel.sampler());
        }
    }

    private void updateUniformBuffer(@NotNull ProgramState program, @NotNull GlslFrameUniformState.FrameSnapshot frameSnapshot, @NotNull GlslFrameUniformState.PassContext passContext, @NotNull ChannelTextureState[] channels) {
        if (program.pipeline == null) {
            throw new IllegalStateException("Cannot update uniforms without a pipeline.");
        }
        GlslStd140Layout layout = program.pipeline.variant().uniformLayout();
        ByteBuffer data = prepareStagingBuffer(program, layout.size());
        GlslFrameUniformState.ChannelResolution[] resolutions = new GlslFrameUniformState.ChannelResolution[CHANNEL_COUNT];
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            resolutions[i] = new GlslFrameUniformState.ChannelResolution(channels[i].resolutionX(), channels[i].resolutionY());
        }
        frameSnapshot.write(layout, data, passContext, resolutions);
        data.position(0);
        data.limit(layout.size());
        if (program.uniformBuffer == null || program.uniformBuffer.isClosed() || program.uniformBuffer.size() != layout.size()) {
            GpuBuffer replacement = RenderSystem.getDevice().createBuffer(() -> "FancyMenu GLSL " + program.pipeline.pipelineIdentity() + " uniforms", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, data);
            closeBuffer(program.uniformBuffer);
            program.uniformBuffer = replacement;
        } else {
            // Minecraft's command encoders copy/stage this data during the call (the same lifetime contract used by
            // Std140Builder's stack buffers), so the native staging block can safely be reused by the next frame.
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(program.uniformBuffer.slice(), data);
        }
    }

    @NotNull
    private static ByteBuffer prepareStagingBuffer(@NotNull ProgramState program, int size) {
        if (program.stagingBuffer == null || program.stagingBuffer.capacity() != size) {
            freeStagingBuffer(program);
            program.stagingBuffer = MemoryUtil.memAlloc(size);
        }
        program.stagingBuffer.position(0);
        program.stagingBuffer.limit(size);
        MemoryUtil.memSet(MemoryUtil.memAddress(program.stagingBuffer), 0, size);
        return program.stagingBuffer;
    }

    private boolean ensureProgramForPass(int passIndex, @Nullable String source, @NotNull RenderSettings settings, boolean requiredSource, @NotNull GpuFormat targetFormat, @NotNull GlslShaderSourceTransformer.BackendCoordinates backendCoordinates) {
        ProgramState state = this.passPrograms_FancyMenu[passIndex];
        String passName = getPassName(passIndex);
        if (source == null || source.isBlank()) {
            if (requiredSource) {
                this.sourceMissing = true;
                this.lastCompileError = passName + ": Shader source is missing.";
                return false;
            }
            this.deactivateProgram(state);
            return true;
        }

        if (passIndex == IMAGE_PASS_INDEX) {
            this.sourceMissing = false;
        }
        boolean blend = passIndex == IMAGE_PASS_INDEX && settings.enableBlend();
        GlslShaderSourceTransformer.PassKind passKind = passIndex == IMAGE_PASS_INDEX ? GlslShaderSourceTransformer.PassKind.IMAGE : GlslShaderSourceTransformer.PassKind.BUFFER;
        String programKey = GlslShaderSourceTransformer.contentIdentity(source, settings.compileMode().name(), Boolean.toString(settings.forceShadertoyCompatibility()), backendCoordinates.name(), passKind.name(), targetFormat.name(), Boolean.toString(blend));
        long cacheGeneration = GlslGpuPipelineCache.generation();
        if (state.cacheGeneration != cacheGeneration) {
            state.pipeline = null;
            state.currentProgramKey = null;
            state.lastFailedProgramKey = null;
            state.lastCompileError = null;
            state.cacheGeneration = cacheGeneration;
        }

        if (programKey.equals(state.currentProgramKey) && state.pipeline != null) {
            GlslGpuPipelineCache.CompilationResult compilation = state.pipeline.precompile();
            if (compilation.valid()) {
                return true;
            }
            return this.failProgram(passName, programKey, state, compilation.diagnostics());
        }
        if (programKey.equals(state.lastFailedProgramKey)) {
            this.lastCompileError = passName + ":\n" + state.lastCompileError;
            return false;
        }

        List<GlslShaderSourceTransformer.FragmentVariant> variants;
        try {
            variants = GlslShaderSourceTransformer.buildFragmentVariants(source, settings.compileMode(), settings.forceShadertoyCompatibility(), backendCoordinates, passKind);
        } catch (GlslShaderSourceTransformer.ShaderTransformException ex) {
            return this.failProgram(passName, programKey, state, ex.diagnostics());
        } catch (Exception ex) {
            return this.failProgram(passName, programKey, state, List.of("Shader transformation failed: " + safeMessage(ex)));
        }
        if (variants.isEmpty()) {
            return this.failProgram(passName, programKey, state, List.of("Shader source does not contain the entry point required by " + settings.compileMode().name() + " mode."));
        }

        List<String> diagnostics = new ArrayList<>();
        for (GlslShaderSourceTransformer.FragmentVariant variant : variants) {
            GlslGpuPipelineCache.PipelineBundle pipeline = GlslGpuPipelineCache.getOrCreate(variant, targetFormat, blend);
            GlslGpuPipelineCache.CompilationResult compilation = pipeline.precompile();
            if (compilation.valid()) {
                state.pipeline = pipeline;
                state.currentProgramKey = programKey;
                state.lastFailedProgramKey = null;
                state.lastCompileError = null;
                return true;
            }
            diagnostics.add("[" + variant.label() + "] " + String.join("\n", compilation.diagnostics()));
            LOGGER.warn("[FANCYMENU] GLSL {} variant '{}' failed for content ID {}: {}", passName, variant.label(), variant.identity(), String.join(" | ", compilation.diagnostics()));
        }
        return this.failProgram(passName, programKey, state, diagnostics);
    }

    private boolean failProgram(@NotNull String passName, @NotNull String programKey, @NotNull ProgramState state, @NotNull List<String> diagnostics) {
        state.pipeline = null;
        state.currentProgramKey = null;
        state.lastFailedProgramKey = programKey;
        state.lastCompileError = diagnostics.isEmpty() ? "Unknown pipeline compilation failure." : String.join("\n", diagnostics);
        this.lastCompileError = passName + ":\n" + state.lastCompileError;
        return false;
    }

    private void deactivateProgram(@NotNull ProgramState state) {
        state.pipeline = null;
        state.currentProgramKey = null;
        state.lastFailedProgramKey = null;
        state.lastCompileError = null;
        closeBuffer(state.uniformBuffer);
        state.uniformBuffer = null;
        freeStagingBuffer(state);
    }

    private void releaseBufferPassResources(int passIndex) {
        this.deactivateProgram(this.passPrograms_FancyMenu[passIndex]);
        this.passPrograms_FancyMenu[passIndex].passFrames.deactivate();
        this.bufferTargets_FancyMenu[passIndex].close();
    }

    @NotNull
    private static String buildPassHistoryIdentity(@NotNull ProgramState program, @NotNull ChannelRouting routing) {
        if (program.currentProgramKey == null) {
            throw new IllegalStateException("Cannot identify a GLSL pass lifecycle without a compiled program.");
        }
        return GlslShaderSourceTransformer.contentIdentity(program.currentProgramKey, routing.channel0().serializedName(), routing.channel1().serializedName(), routing.channel2().serializedName(), routing.channel3().serializedName());
    }

    @NotNull
    private ChannelTextureState[] resolveExternalChannelTextureStates(@NotNull Minecraft minecraft, @NotNull RenderSettings settings, @NotNull ChannelTextureState fallback) {
        ChannelTextureState[] states = new ChannelTextureState[CHANNEL_COUNT];
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            states[i] = this.resolveChannelTextureState(minecraft, settings.channelSupplier(i), fallback);
        }
        return states;
    }

    @NotNull
    private ChannelTextureState resolveChannelTextureState(@NotNull Minecraft minecraft, @Nullable ResourceSupplier<ITexture> supplier, @NotNull ChannelTextureState fallback) {
        if (supplier == null) {
            return fallback.withResolution(0.0F, 0.0F);
        }
        ITexture texture = supplier.get();
        if (texture == null || !texture.isReady() || texture.getResourceLocation() == null) {
            return fallback.withResolution(0.0F, 0.0F);
        }
        try {
            AbstractTexture minecraftTexture = minecraft.getTextureManager().getTexture(texture.getResourceLocation());
            GpuTextureView view = minecraftTexture.getTextureView();
            if (view.isClosed()) {
                return fallback.withResolution(0.0F, 0.0F);
            }
            return new ChannelTextureState(view, minecraftTexture.getSampler(), Math.max(0.0F, texture.getWidth()), Math.max(0.0F, texture.getHeight()));
        } catch (Exception ignored) {
            return fallback.withResolution(0.0F, 0.0F);
        }
    }

    @NotNull
    private ChannelTextureState[] resolveRoutedChannelStates(@NotNull ChannelRouting routing, @NotNull ChannelTextureState[] externalChannels, @NotNull boolean[] activeBufferPasses, @NotNull ChannelTextureState fallback, int renderingPassIndex, boolean imagePass) {
        ChannelTextureState[] states = new ChannelTextureState[CHANNEL_COUNT];
        for (int channelIndex = 0; channelIndex < CHANNEL_COUNT; channelIndex++) {
            ChannelInput input = routing.channelForIndex(channelIndex);
            states[channelIndex] = switch (input) {
                case RESOURCE0 -> externalChannels[0];
                case RESOURCE1 -> externalChannels[1];
                case RESOURCE2 -> externalChannels[2];
                case RESOURCE3 -> externalChannels[3];
                case BUFFER_A, BUFFER_B, BUFFER_C, BUFFER_D -> this.resolveBufferChannelState(input.bufferPassIndex(), activeBufferPasses, fallback, renderingPassIndex, imagePass);
                case NONE -> fallback.withResolution(0.0F, 0.0F);
            };
        }
        return states;
    }

    @NotNull
    private ChannelTextureState resolveBufferChannelState(int referencedBufferIndex, @NotNull boolean[] activeBufferPasses, @NotNull ChannelTextureState fallback, int renderingPassIndex, boolean imagePass) {
        GlslPassGraph.BufferVersion version = GlslPassGraph.resolveBufferVersion(renderingPassIndex, referencedBufferIndex, imagePass, activeBufferPasses);
        if (version == GlslPassGraph.BufferVersion.FALLBACK) {
            return fallback.withResolution(0.0F, 0.0F);
        }
        GlslPingPongTarget target = this.bufferTargets_FancyMenu[referencedBufferIndex];
        GpuTextureView view = version == GlslPassGraph.BufferVersion.CURRENT_FRAME ? target.writeView() : target.readView();
        if (view == null || view.isClosed()) {
            return fallback.withResolution(0.0F, 0.0F);
        }
        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        return new ChannelTextureState(view, sampler, target.width(), target.height());
    }

    @NotNull
    private ChannelTextureState resolveFallbackTextureState() {
        Minecraft minecraft = Minecraft.getInstance();
        Identifier missingLocation = MissingTextureAtlasSprite.getLocation();
        try {
            AbstractTexture missingTexture = minecraft.getTextureManager().getTexture(missingLocation);
            GpuTextureView view = missingTexture.getTextureView();
            if (!view.isClosed()) {
                return new ChannelTextureState(view, missingTexture.getSampler(), 0.0F, 0.0F);
            }
        } catch (Exception ignored) {
        }

        if (this.fallbackTextureTarget_FancyMenu == null || this.fallbackTextureTarget_FancyMenu.getColorTextureView() == null || this.fallbackTextureTarget_FancyMenu.getColorTextureView().isClosed()) {
            this.closeFallbackTarget();
            this.fallbackTextureTarget_FancyMenu = new TextureTarget("FancyMenu GLSL fallback", 1, 1, false, GpuFormat.RGBA8_UNORM);
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.fallbackTextureTarget_FancyMenu.getColorTexture(), new Vector4f(1.0F, 0.0F, 1.0F, 1.0F));
        }
        return new ChannelTextureState(this.fallbackTextureTarget_FancyMenu.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST), 0.0F, 0.0F);
    }

    public boolean isSourceMissing() {
        return this.sourceMissing;
    }

    @Nullable
    public String getLastCompileError() {
        return this.lastCompileError;
    }

    public long getFrameCounter() {
        return this.passPrograms_FancyMenu[IMAGE_PASS_INDEX].passFrames.committedFrameCount();
    }

    public void close() {
        RenderSystem.assertOnRenderThread();
        for (ProgramState program : this.passPrograms_FancyMenu) {
            this.deactivateProgram(program);
            program.passFrames.deactivate();
            program.cacheGeneration = GlslGpuPipelineCache.generation();
        }
        for (GlslPingPongTarget target : this.bufferTargets_FancyMenu) {
            target.close();
        }
        this.closeFallbackTarget();
        this.frameUniformState_FancyMenu.onRuntimeResourcesClosed();
        this.lastCompileError = null;
        this.sourceMissing = false;
    }

    private void closeFallbackTarget() {
        if (this.fallbackTextureTarget_FancyMenu != null) {
            this.fallbackTextureTarget_FancyMenu.destroyBuffers();
            this.fallbackTextureTarget_FancyMenu = null;
        }
    }

    @NotNull
    private static boolean[] resolveActiveBufferPasses(@NotNull RenderSettings settings) {
        boolean[] active = new boolean[BUFFER_PASS_COUNT];
        for (int i = 0; i < BUFFER_PASS_COUNT; i++) {
            active[i] = hasShaderSource(settings.bufferSource(i));
        }
        return active;
    }

    private static boolean hasShaderSource(@Nullable String source) {
        return source != null && !source.isBlank();
    }

    static int resolveSamplerChannelIndex(@NotNull String samplerName) {
        if (samplerName.length() == "iChannel0".length() && samplerName.startsWith("iChannel")) {
            char digit = samplerName.charAt(samplerName.length() - 1);
            if (digit >= '0' && digit <= '3') {
                return digit - '0';
            }
        }
        return 0;
    }

    @NotNull
    private static String getPassName(int passIndex) {
        return switch (passIndex) {
            case 0 -> "Buffer A";
            case 1 -> "Buffer B";
            case 2 -> "Buffer C";
            case 3 -> "Buffer D";
            case IMAGE_PASS_INDEX -> "Image";
            default -> "Pass " + passIndex;
        };
    }

    private static void closeBuffer(@Nullable GpuBuffer buffer) {
        if (buffer != null && !buffer.isClosed()) {
            buffer.close();
        }
    }

    private static void freeStagingBuffer(@NotNull ProgramState state) {
        if (state.stagingBuffer != null) {
            MemoryUtil.memFree(state.stagingBuffer);
            state.stagingBuffer = null;
        }
    }

    @NotNull
    private static String safeMessage(@NotNull Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static final class ProgramState {
        @Nullable
        private GlslGpuPipelineCache.PipelineBundle pipeline;
        @Nullable
        private GpuBuffer uniformBuffer;
        @Nullable
        private ByteBuffer stagingBuffer;
        @Nullable
        private String currentProgramKey;
        @Nullable
        private String lastFailedProgramKey;
        @Nullable
        private String lastCompileError;
        private long cacheGeneration = -1L;
        private final GlslPassFrameState passFrames = new GlslPassFrameState();
    }

    private record ChannelTextureState(@NotNull GpuTextureView view, @NotNull GpuSampler sampler, float resolutionX, float resolutionY) {

        @NotNull
        private ChannelTextureState withResolution(float width, float height) {
            return new ChannelTextureState(this.view, this.sampler, width, height);
        }
    }

    private record PreparedPass(int passIndex, @NotNull ProgramState program, @NotNull GpuTextureView outputView, @NotNull ChannelTextureState[] channels) {
    }

    private record GlslRenderState(@NotNull GlslShaderRuntime runtime, int areaX, int areaY, int areaWidth, int areaHeight, float partialTick, @Nullable String fragmentSource, @NotNull RenderSettings settings, @NotNull ScreenRectangle bounds) implements GuiRenderPhaseAction {

        @Override
        public void executeRender_FancyMenu() {
            this.runtime.renderNow(this.areaX, this.areaY, this.areaWidth, this.areaHeight, this.partialTick, this.fragmentSource, this.settings);
        }
    }

    private record GlslResourceReleaseRenderState(@NotNull GlslShaderRuntime runtime, @NotNull ScreenRectangle bounds) implements GuiRenderPhaseAction {

        @Override
        public void executeRender_FancyMenu() {
            this.runtime.close();
        }
    }
}
