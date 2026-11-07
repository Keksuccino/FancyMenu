package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * GPU side of FancyMenu's GUI blur: a downsample chain, a separable Gaussian on its deepest level, and a composite pass that
 * upsamples that level with a cubic B-spline and blends the tinted result into the main target. {@link GuiBlurKernel} picks the
 * stages and {@link GuiBlurPlan} the texel regions they touch.
 *
 * <p>All levels use half-float textures, so the blur keeps sub-8-bit precision until the dithered composite. They get allocated
 * on first use and released again after {@link #IDLE_RELEASE_NANOS} without blur areas.</p>
 */
final class GuiBlurPasses {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final long IDLE_RELEASE_NANOS = TimeUnit.SECONDS.toNanos(10L);
    private static final String DOWNSAMPLE_SHADER_NAME = "fancymenu_blur_downsample";
    private static final String GAUSSIAN_SHADER_NAME = "fancymenu_blur_gaussian";
    private static final String COMPOSITE_SHADER_NAME = "fancymenu_blur_composite";
    private static final String IN_SAMPLER = "InSampler";
    private static final String BLUR_SAMPLER = "BlurSampler";
    private static final Supplier<ShaderInstance> DOWNSAMPLE_SHADER = () -> GuiBlurPasses.downsampleShader;
    private static final Supplier<ShaderInstance> GAUSSIAN_SHADER = () -> GuiBlurPasses.gaussianShader;
    private static final Supplier<ShaderInstance> COMPOSITE_SHADER = () -> GuiBlurPasses.compositeShader;
    private static final LevelTarget[] LEVEL_TARGETS = new LevelTarget[GuiBlurKernel.MAX_LEVELS + 1];
    private static final LevelTarget[] SCRATCH_TARGETS = new LevelTarget[GuiBlurKernel.MAX_LEVELS + 1];
    private static final int[] SCISSOR_BOX = new int[4];

    private static ShaderInstance downsampleShader;
    private static ShaderInstance gaussianShader;
    private static ShaderInstance compositeShader;
    private static boolean shadersFailed;
    private static boolean reloadListenerRegistered;
    private static boolean scissorActive;
    private static int levelsTargetWidth;
    private static int levelsTargetHeight;
    private static long lastUseNanos;

    private GuiBlurPasses() {
    }

    /**
     * Remembers the GUI's active GL scissor box, so {@link #render} can restore it, and clips {@code region} to it.
     *
     * @return false if the scissor leaves nothing of {@code region}.
     */
    static boolean captureScissor(@NotNull GuiBlurPlan.Region region) {
        // GUI scissors are plain GL state here, and the blur passes need their own scissor boxes in between.
        scissorActive = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        if (!scissorActive) {
            return !region.isEmpty();
        }
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, SCISSOR_BOX);
        region.intersect(SCISSOR_BOX[0], SCISSOR_BOX[1], SCISSOR_BOX[0] + SCISSOR_BOX[2], SCISSOR_BOX[1] + SCISSOR_BOX[3]);
        return !region.isEmpty();
    }

    /**
     * Blurs the main target's texels around the plan's composite region and blends the tinted result back into it.
     * Call {@link #captureScissor} first.
     *
     * @param sampleBlur false if the composite only needs the tints, e.g. because they are fully opaque or there is no blur radius.
     */
    static void render(@NotNull RenderTarget mainTarget, @NotNull GuiBlurPlan plan, boolean sampleBlur, @NotNull CompositeShape shape) {
        if (!ensureShaders()) {
            return;
        }
        lastUseNanos = System.nanoTime();
        GuiBlurKernel.Kernel kernel = plan.kernel();
        // GUI code may leave restricted write masks behind, which would keep stale channels in the level textures.
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.disableBlend();
        int blurTexture = 0;
        if (sampleBlur) {
            int level = kernel.level();
            ensureLevels(mainTarget.width, mainTarget.height, level);
            int previousFilterMode = mainTarget.filterMode;
            mainTarget.setFilterMode(GL11.GL_LINEAR);
            int source = mainTarget.getColorTextureId();
            int sourceWidth = mainTarget.width;
            int sourceHeight = mainTarget.height;
            for (int i = 1; i <= level; i++) {
                LevelTarget output = LEVEL_TARGETS[i];
                downsampleShader.safeGetUniform("BlurSizes").set((float) output.width, (float) output.height, (float) sourceWidth, (float) sourceHeight);
                renderLevelPass(DOWNSAMPLE_SHADER, downsampleShader, output, source, plan.downsample(i));
                source = output.textureId;
                sourceWidth = output.width;
                sourceHeight = output.height;
            }
            mainTarget.setFilterMode(previousFilterMode);
            LevelTarget blurred = LEVEL_TARGETS[level];
            if (kernel.taps() > 0) {
                LevelTarget scratch = SCRATCH_TARGETS[level];
                gaussianShader.safeGetUniform("BlurSizes").set((float) blurred.width, (float) blurred.height, (float) blurred.width, (float) blurred.height);
                gaussianShader.safeGetUniform("BlurKernel").set(1.0F, 0.0F, kernel.sigma(), (float) kernel.taps());
                renderLevelPass(GAUSSIAN_SHADER, gaussianShader, scratch, blurred.textureId, plan.horizontal());
                gaussianShader.safeGetUniform("BlurKernel").set(0.0F, 1.0F, kernel.sigma(), (float) kernel.taps());
                renderLevelPass(GAUSSIAN_SHADER, gaussianShader, blurred, scratch.textureId, plan.vertical());
            }
            blurTexture = blurred.textureId;
        }

        mainTarget.bindWrite(true);
        RenderSystem.enableBlend();
        // Premultiplied color over the framebuffer, keeping its alpha untouched.
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        writeCompositeUniforms(plan, sampleBlur, mainTarget.width, mainTarget.height, shape);
        compositeShader.setSampler(BLUR_SAMPLER, blurTexture);
        GuiBlurPlan.Region region = plan.composite();
        RenderSystem.enableScissor(region.x(), region.y(), region.width(), region.height());
        RenderSystem.setShader(COMPOSITE_SHADER);
        drawTargetQuad();

        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        if (scissorActive) {
            RenderSystem.enableScissor(SCISSOR_BOX[0], SCISSOR_BOX[1], SCISSOR_BOX[2], SCISSOR_BOX[3]);
        } else {
            RenderSystem.disableScissor();
        }
    }

    /**
     * Releases all blur textures once no blur area got rendered for a while. Call this once per frame.
     */
    static void releaseIfIdle() {
        if (lastUseNanos != 0L && System.nanoTime() - lastUseNanos > IDLE_RELEASE_NANOS) {
            closeLevels();
            lastUseNanos = 0L;
        }
    }

    static void close() {
        closeLevels();
        clearShaders();
        lastUseNanos = 0L;
    }

    private static void renderLevelPass(@NotNull Supplier<ShaderInstance> shaderSupplier, @NotNull ShaderInstance shader, @NotNull LevelTarget output, int inputTexture, @NotNull GuiBlurPlan.Region region) {
        if (region.isEmpty()) {
            return;
        }
        output.bindWrite();
        shader.setSampler(IN_SAMPLER, inputTexture);
        RenderSystem.enableScissor(region.x(), region.y(), region.width(), region.height());
        RenderSystem.setShader(shaderSupplier);
        drawTargetQuad();
    }

    private static void writeCompositeUniforms(@NotNull GuiBlurPlan plan, boolean sampleBlur, int targetWidth, int targetHeight, @NotNull CompositeShape shape) {
        GuiBlurKernel.Kernel kernel = plan.kernel();
        int level = kernel.level();
        compositeShader.safeGetUniform("Rect").set(shape.x(), shape.y(), shape.width(), shape.height());
        compositeShader.safeGetUniform("CornerRadii").set(shape.bottomLeftRadius(), shape.bottomRightRadius(), shape.topRightRadius(), shape.topLeftRadius());
        compositeShader.safeGetUniform("Rotation").set(shape.rotation().m00(), shape.rotation().m01(), shape.rotation().m10(), shape.rotation().m11());
        compositeShader.safeGetUniform("Tint").set(shape.tint().red(), shape.tint().green(), shape.tint().blue(), shape.tint().alpha());
        compositeShader.safeGetUniform("HeaderTint").set(shape.headerTint().red(), shape.headerTint().green(), shape.headerTint().blue(), shape.headerTint().alpha());
        compositeShader.safeGetUniform("ShapeInfo").set(shape.shapeType(), shape.roundness(), shape.headerHeight(), shape.opacity());
        compositeShader.safeGetUniform("SampleInfo").set(sampleBlur ? (float) plan.levelWidth(level) : 1.0F, sampleBlur ? (float) plan.levelHeight(level) : 1.0F, (float) kernel.levelScale(), kernel.strength());
        compositeShader.safeGetUniform("TargetInfo").set((float) targetWidth, (float) targetHeight, sampleBlur ? 1.0F : 0.0F, 0.0F);
    }

    /**
     * Covers the whole bound target; the scissor limits the pass to its region.
     */
    private static void drawTargetQuad() {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.addVertex(0.0F, 0.0F, 0.0F);
        builder.addVertex(1.0F, 0.0F, 0.0F);
        builder.addVertex(1.0F, 1.0F, 0.0F);
        builder.addVertex(0.0F, 1.0F, 0.0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static void ensureLevels(int targetWidth, int targetHeight, int level) {
        if (targetWidth != levelsTargetWidth || targetHeight != levelsTargetHeight) {
            closeLevels();
            levelsTargetWidth = targetWidth;
            levelsTargetHeight = targetHeight;
        }
        for (int i = 1; i <= level; i++) {
            if (LEVEL_TARGETS[i] == null) {
                LEVEL_TARGETS[i] = new LevelTarget(GuiBlurPlan.levelSize(targetWidth, i), GuiBlurPlan.levelSize(targetHeight, i));
            }
        }
        if (SCRATCH_TARGETS[level] == null) {
            SCRATCH_TARGETS[level] = new LevelTarget(GuiBlurPlan.levelSize(targetWidth, level), GuiBlurPlan.levelSize(targetHeight, level));
        }
    }

    private static void closeLevels() {
        for (int i = 0; i < LEVEL_TARGETS.length; i++) {
            if (LEVEL_TARGETS[i] != null) {
                LEVEL_TARGETS[i].close();
                LEVEL_TARGETS[i] = null;
            }
            if (SCRATCH_TARGETS[i] != null) {
                SCRATCH_TARGETS[i].close();
                SCRATCH_TARGETS[i] = null;
            }
        }
        levelsTargetWidth = 0;
        levelsTargetHeight = 0;
    }

    private static boolean ensureShaders() {
        ensureReloadListener();
        if (shadersFailed) {
            return false;
        }
        if (downsampleShader == null || gaussianShader == null || compositeShader == null) {
            try {
                downsampleShader = createShader(downsampleShader, DOWNSAMPLE_SHADER_NAME);
                gaussianShader = createShader(gaussianShader, GAUSSIAN_SHADER_NAME);
                compositeShader = createShader(compositeShader, COMPOSITE_SHADER_NAME);
            } catch (Exception ex) {
                shadersFailed = true;
                LOGGER.error("[FANCYMENU] Failed to load GUI blur shaders!", ex);
                return false;
            }
        }
        return true;
    }

    private static ShaderInstance createShader(@Nullable ShaderInstance existing, @NotNull String name) throws Exception {
        return (existing != null) ? existing : new ShaderInstance(Minecraft.getInstance().getResourceManager(), name, DefaultVertexFormat.POSITION);
    }

    private static void ensureReloadListener() {
        if (reloadListenerRegistered) {
            return;
        }
        reloadListenerRegistered = true;
        MinecraftResourceReloadObserver.addReloadListener(action -> {
            if (action == MinecraftResourceReloadObserver.ReloadAction.STARTING) {
                RenderSystem.recordRenderCall(GuiBlurPasses::clearShaders);
            }
        });
    }

    private static void clearShaders() {
        downsampleShader = closeShader(downsampleShader);
        gaussianShader = closeShader(gaussianShader);
        compositeShader = closeShader(compositeShader);
        shadersFailed = false;
    }

    @Nullable
    private static ShaderInstance closeShader(@Nullable ShaderInstance shader) {
        if (shader != null) {
            shader.close();
        }
        return null;
    }

    /**
     * Half-float color texture with its own framebuffer. Vanilla render targets only allocate 8-bit color.
     */
    private static final class LevelTarget {

        private final int width;
        private final int height;
        private final int textureId;
        private final int framebufferId;

        private LevelTarget(int width, int height) {
            this.width = width;
            this.height = height;
            this.textureId = GlStateManager._genTexture();
            GlStateManager._bindTexture(this.textureId);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
            GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, width, height, 0, GL11.GL_RGBA, GL11.GL_FLOAT, null);
            GlStateManager._bindTexture(0);
            this.framebufferId = GlStateManager.glGenFramebuffers();
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebufferId);
            GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, this.textureId, 0);
            int status = GlStateManager.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                LOGGER.error("[FANCYMENU] GUI blur level framebuffer is incomplete: {}", status);
            }
            // Freshly allocated texels are undefined and may hold NaN bit patterns, which zero-weight bilinear taps would still spread.
            RenderSystem.disableScissor();
            GlStateManager._viewport(0, 0, width, height);
            GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GlStateManager._clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
        }

        private void bindWrite() {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebufferId);
            GlStateManager._viewport(0, 0, this.width, this.height);
        }

        private void close() {
            GlStateManager._glDeleteFramebuffers(this.framebufferId);
            GlStateManager._deleteTexture(this.textureId);
        }

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
