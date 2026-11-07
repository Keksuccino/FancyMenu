package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * GPU side of FancyMenu's GUI blur: a downsample chain, a separable Gaussian on its deepest level, and a composite pass that
 * upsamples that level with a cubic B-spline and blends the tinted result into the main target. {@link GuiBlurKernel} picks the
 * stages and {@link GuiBlurPlan} the texel regions they touch.
 *
 * <p>Levels use RGBA8 textures, the only color format of this Minecraft version; the dithered composite hides their 8-bit steps.
 * They get allocated on first use and released again after {@link #IDLE_RELEASE_NANOS} without blur areas.</p>
 */
final class GuiBlurPasses {

    private static final long IDLE_RELEASE_NANOS = TimeUnit.SECONDS.toNanos(10L);
    private static final String PASS_INFO_UNIFORM = "BlurPassInfo";
    private static final String COMPOSITE_INFO_UNIFORM = "BlurCompositeInfo";
    private static final String IN_SAMPLER = "InSampler";
    private static final String BLUR_SAMPLER = "BlurSampler";
    private static final int PASS_INFO_SIZE = 32;
    private static final int COMPOSITE_INFO_SIZE = 128;
    private static final Supplier<String> DOWNSAMPLE_LABEL = () -> "FancyMenu GUI blur downsample";
    private static final Supplier<String> HORIZONTAL_LABEL = () -> "FancyMenu GUI blur horizontal";
    private static final Supplier<String> VERTICAL_LABEL = () -> "FancyMenu GUI blur vertical";
    private static final Supplier<String> COMPOSITE_LABEL = () -> "FancyMenu GUI blur composite";
    private static final Supplier<String> PASS_INFO_BUFFER_LABEL = () -> "FancyMenu GUI blur pass info";
    private static final Supplier<String> COMPOSITE_INFO_BUFFER_LABEL = () -> "FancyMenu GUI blur composite info";
    // Level passes replace the texels of one attachment.
    private static final RenderPipeline.Snippet LEVEL_PIPELINE_SNIPPET = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withoutBlend()
            .withColorWrite(true, true)
            .withVertexShader("core/screenquad")
            .withSampler(IN_SAMPLER)
            .withUniform(PASS_INFO_UNIFORM, UniformType.UNIFORM_BUFFER)
            .buildSnippet();
    private static final RenderPipeline DOWNSAMPLE_PIPELINE = RenderPipeline.builder(LEVEL_PIPELINE_SNIPPET)
            .withLocation(Identifier.withDefaultNamespace("pipeline/fancymenu_blur_downsample"))
            .withFragmentShader("post/fancymenu_blur_downsample")
            .build();
    private static final RenderPipeline GAUSSIAN_PIPELINE = RenderPipeline.builder(LEVEL_PIPELINE_SNIPPET)
            .withLocation(Identifier.withDefaultNamespace("pipeline/fancymenu_blur_gaussian"))
            .withFragmentShader("post/fancymenu_blur_gaussian")
            .withShaderDefine("FANCYMENU_BLUR_MAX_TAPS", GuiBlurKernel.MAX_TAPS)
            .build();
    // The composite outputs premultiplied color and keeps the main target's alpha untouched.
    private static final RenderPipeline COMPOSITE_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(Identifier.withDefaultNamespace("pipeline/fancymenu_blur_composite"))
            .withVertexShader("core/screenquad")
            .withFragmentShader("post/fancymenu_blur_composite")
            .withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ZERO, DestFactor.ONE))
            .withColorWrite(true, false)
            .withSampler(BLUR_SAMPLER)
            .withUniform(COMPOSITE_INFO_UNIFORM, UniformType.UNIFORM_BUFFER)
            .build();

    private static final TextureTarget[] LEVEL_TARGETS = new TextureTarget[GuiBlurKernel.MAX_LEVELS + 1];
    private static final TextureTarget[] SCRATCH_TARGETS = new TextureTarget[GuiBlurKernel.MAX_LEVELS + 1];
    private static final GpuBuffer[] DOWNSAMPLE_INFO_BUFFERS = new GpuBuffer[GuiBlurKernel.MAX_LEVELS + 1];
    private static GpuBuffer horizontalInfoBuffer;
    private static GpuBuffer verticalInfoBuffer;
    private static GpuBuffer compositeInfoBuffer;
    private static TextureTarget placeholderTarget;
    private static int levelsTargetWidth;
    private static int levelsTargetHeight;
    // The Gaussian buffers only get rewritten when the kernel changes.
    private static int gaussianLevel = -1;
    private static float gaussianSigma = -1.0F;
    private static int gaussianTaps = -1;
    private static long lastUseNanos;

    private GuiBlurPasses() {
    }

    /**
     * Blurs the main target's texels around the plan's composite region and blends the tinted result back into it.
     *
     * @param sampleBlur false if the composite only needs the tints, e.g. because they are fully opaque or there is no blur radius.
     */
    static void render(@NotNull RenderTarget mainTarget, @NotNull GuiBlurPlan plan, boolean sampleBlur, @NotNull CompositeShape shape) {
        GpuTextureView mainView = mainTarget.getColorTextureView();
        if (mainView == null) {
            return;
        }
        lastUseNanos = System.nanoTime();
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        GuiBlurKernel.Kernel kernel = plan.kernel();
        GpuTextureView blurView;
        if (sampleBlur) {
            int level = kernel.level();
            ensureLevels(mainTarget.width, mainTarget.height, level);
            updateGaussianInfo(plan);
            GpuTextureView source = mainView;
            for (int i = 1; i <= level; i++) {
                GpuTextureView output = LEVEL_TARGETS[i].getColorTextureView();
                renderLevelPass(encoder, DOWNSAMPLE_LABEL, DOWNSAMPLE_PIPELINE, output, source, sampler, DOWNSAMPLE_INFO_BUFFERS[i], plan.downsample(i));
                source = output;
            }
            blurView = LEVEL_TARGETS[level].getColorTextureView();
            if (kernel.taps() > 0) {
                GpuTextureView scratchView = SCRATCH_TARGETS[level].getColorTextureView();
                renderLevelPass(encoder, HORIZONTAL_LABEL, GAUSSIAN_PIPELINE, scratchView, blurView, sampler, horizontalInfoBuffer, plan.horizontal());
                renderLevelPass(encoder, VERTICAL_LABEL, GAUSSIAN_PIPELINE, blurView, scratchView, sampler, verticalInfoBuffer, plan.vertical());
            }
        } else {
            blurView = getPlaceholderView();
        }

        writeCompositeInfo(plan, sampleBlur, mainTarget.width, mainTarget.height, shape);
        GuiBlurPlan.Region region = plan.composite();
        try (RenderPass renderPass = encoder.createRenderPass(COMPOSITE_LABEL, mainView, OptionalInt.empty())) {
            renderPass.setPipeline(COMPOSITE_PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform(COMPOSITE_INFO_UNIFORM, compositeInfoBuffer);
            renderPass.bindTexture(BLUR_SAMPLER, blurView, sampler);
            renderPass.enableScissor(region.x(), region.y(), region.width(), region.height());
            renderPass.draw(0, 3);
        }
    }

    /**
     * Releases all blur textures and buffers once no blur area got rendered for a while. Call this once per frame.
     */
    static void releaseIfIdle() {
        if (lastUseNanos != 0L && System.nanoTime() - lastUseNanos > IDLE_RELEASE_NANOS) {
            close();
        }
    }

    static void close() {
        closeLevels();
        closeTarget(placeholderTarget);
        placeholderTarget = null;
        horizontalInfoBuffer = closeBuffer(horizontalInfoBuffer);
        verticalInfoBuffer = closeBuffer(verticalInfoBuffer);
        compositeInfoBuffer = closeBuffer(compositeInfoBuffer);
        lastUseNanos = 0L;
    }

    private static void renderLevelPass(@NotNull CommandEncoder encoder, @NotNull Supplier<String> label, @NotNull RenderPipeline pipeline, @NotNull GpuTextureView output, @NotNull GpuTextureView input, @NotNull GpuSampler sampler, @NotNull GpuBuffer passInfo, @NotNull GuiBlurPlan.Region region) {
        if (region.isEmpty()) {
            return;
        }
        try (RenderPass renderPass = encoder.createRenderPass(label, output, OptionalInt.empty())) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform(PASS_INFO_UNIFORM, passInfo);
            renderPass.bindTexture(IN_SAMPLER, input, sampler);
            renderPass.enableScissor(region.x(), region.y(), region.width(), region.height());
            renderPass.draw(0, 3);
        }
    }

    private static void ensureLevels(int targetWidth, int targetHeight, int level) {
        if (targetWidth != levelsTargetWidth || targetHeight != levelsTargetHeight) {
            closeLevels();
            levelsTargetWidth = targetWidth;
            levelsTargetHeight = targetHeight;
        }
        for (int i = 1; i <= level; i++) {
            if (LEVEL_TARGETS[i] != null) {
                continue;
            }
            int width = GuiBlurPlan.levelSize(targetWidth, i);
            int height = GuiBlurPlan.levelSize(targetHeight, i);
            LEVEL_TARGETS[i] = createLevelTarget("FancyMenu GUI blur level " + i, width, height);
            int sourceWidth = (i == 1) ? targetWidth : GuiBlurPlan.levelSize(targetWidth, i - 1);
            int sourceHeight = (i == 1) ? targetHeight : GuiBlurPlan.levelSize(targetHeight, i - 1);
            DOWNSAMPLE_INFO_BUFFERS[i] = writePassInfo(DOWNSAMPLE_INFO_BUFFERS[i], width, height, sourceWidth, sourceHeight, 0.0F, 0.0F, 0.0F, 0);
        }
        if (SCRATCH_TARGETS[level] == null) {
            SCRATCH_TARGETS[level] = createLevelTarget("FancyMenu GUI blur scratch " + level, GuiBlurPlan.levelSize(targetWidth, level), GuiBlurPlan.levelSize(targetHeight, level));
        }
    }

    private static TextureTarget createLevelTarget(@NotNull String label, int width, int height) {
        TextureTarget target = new TextureTarget(label, width, height, false);
        // Freshly allocated texels are undefined and may hold NaN bit patterns, which zero-weight bilinear taps would still spread.
        if (target.getColorTexture() != null) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(target.getColorTexture(), 0);
        }
        return target;
    }

    private static void updateGaussianInfo(@NotNull GuiBlurPlan plan) {
        GuiBlurKernel.Kernel kernel = plan.kernel();
        int level = kernel.level();
        if (kernel.taps() <= 0 || (level == gaussianLevel && kernel.sigma() == gaussianSigma && kernel.taps() == gaussianTaps && horizontalInfoBuffer != null && verticalInfoBuffer != null)) {
            return;
        }
        int width = plan.levelWidth(level);
        int height = plan.levelHeight(level);
        horizontalInfoBuffer = writePassInfo(horizontalInfoBuffer, width, height, width, height, 1.0F, 0.0F, kernel.sigma(), kernel.taps());
        verticalInfoBuffer = writePassInfo(verticalInfoBuffer, width, height, width, height, 0.0F, 1.0F, kernel.sigma(), kernel.taps());
        gaussianLevel = level;
        gaussianSigma = kernel.sigma();
        gaussianTaps = kernel.taps();
    }

    private static GpuBuffer writePassInfo(@Nullable GpuBuffer buffer, int outputWidth, int outputHeight, int inputWidth, int inputHeight, float directionX, float directionY, float sigma, int taps) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, PASS_INFO_SIZE)
                    .putVec4(outputWidth, outputHeight, inputWidth, inputHeight)
                    .putVec4(directionX, directionY, sigma, taps)
                    .get();
            return writeUniformBuffer(buffer, PASS_INFO_BUFFER_LABEL, data);
        }
    }

    private static void writeCompositeInfo(@NotNull GuiBlurPlan plan, boolean sampleBlur, int targetWidth, int targetHeight, @NotNull CompositeShape shape) {
        GuiBlurKernel.Kernel kernel = plan.kernel();
        int level = kernel.level();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, COMPOSITE_INFO_SIZE)
                    .putVec4(shape.x(), shape.y(), shape.width(), shape.height())
                    .putVec4(shape.bottomLeftRadius(), shape.bottomRightRadius(), shape.topRightRadius(), shape.topLeftRadius())
                    .putVec4(shape.rotation().m00(), shape.rotation().m01(), shape.rotation().m10(), shape.rotation().m11())
                    .putVec4(shape.tint().red(), shape.tint().green(), shape.tint().blue(), shape.tint().alpha())
                    .putVec4(shape.headerTint().red(), shape.headerTint().green(), shape.headerTint().blue(), shape.headerTint().alpha())
                    .putVec4(shape.shapeType(), shape.roundness(), shape.headerHeight(), shape.opacity())
                    .putVec4(sampleBlur ? plan.levelWidth(level) : 1.0F, sampleBlur ? plan.levelHeight(level) : 1.0F, kernel.levelScale(), kernel.strength())
                    .putVec4(targetWidth, targetHeight, sampleBlur ? 1.0F : 0.0F, 0.0F)
                    .get();
            compositeInfoBuffer = writeUniformBuffer(compositeInfoBuffer, COMPOSITE_INFO_BUFFER_LABEL, data);
        }
    }

    private static GpuBuffer writeUniformBuffer(@Nullable GpuBuffer buffer, @NotNull Supplier<String> label, @NotNull ByteBuffer data) {
        if (buffer == null || buffer.isClosed() || buffer.size() != data.remaining()) {
            closeBuffer(buffer);
            return RenderSystem.getDevice().createBuffer(label, GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, data);
        }
        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), data);
        return buffer;
    }

    /**
     * Texture the composite binds when it doesn't sample a blur, since every bound sampler needs a valid texture.
     */
    private static GpuTextureView getPlaceholderView() {
        if (placeholderTarget == null) {
            placeholderTarget = createLevelTarget("FancyMenu GUI blur placeholder", 1, 1);
        }
        return placeholderTarget.getColorTextureView();
    }

    private static void closeLevels() {
        for (int i = 0; i < LEVEL_TARGETS.length; i++) {
            closeTarget(LEVEL_TARGETS[i]);
            closeTarget(SCRATCH_TARGETS[i]);
            LEVEL_TARGETS[i] = null;
            SCRATCH_TARGETS[i] = null;
            DOWNSAMPLE_INFO_BUFFERS[i] = closeBuffer(DOWNSAMPLE_INFO_BUFFERS[i]);
        }
        levelsTargetWidth = 0;
        levelsTargetHeight = 0;
        gaussianLevel = -1;
    }

    private static void closeTarget(@Nullable TextureTarget target) {
        if (target != null) {
            target.destroyBuffers();
        }
    }

    @Nullable
    private static GpuBuffer closeBuffer(@Nullable GpuBuffer buffer) {
        if (buffer != null && !buffer.isClosed()) {
            buffer.close();
        }
        return null;
    }

    /**
     * Framebuffer-space mask and tints of one composite.
     *
     * @param x Bottom-left corner in framebuffer pixels.
     * @param rotation Maps framebuffer offsets from the mask's center into the mask's local space.
     * @param headerHeight Height of the band at the mask's top that uses {@code headerTint} instead of {@code tint}, 0 for none.
     */
    record CompositeShape(float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, @NotNull RenderRotationUtil.Rotation2D rotation, @NotNull DrawableColor.FloatColor tint, @NotNull DrawableColor.FloatColor headerTint, float shapeType, float roundness, float headerHeight, float opacity) {
    }

}
