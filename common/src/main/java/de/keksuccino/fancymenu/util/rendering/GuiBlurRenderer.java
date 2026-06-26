package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

public final class GuiBlurRenderer {

    private static final float SHAPE_TYPE_ROUNDED_RECT = 0.0F;
    private static final float SHAPE_TYPE_SUPERELLIPSE = 1.0F;
    private static final String BLUR_CONFIG_UNIFORM_FANCYMENU = "BlurConfig";
    private static final String GUI_BLUR_CONFIG_UNIFORM_FANCYMENU = "GuiBlurConfig";
    private static final String SAMPLER_INFO_UNIFORM_FANCYMENU = "SamplerInfo";
    private static final String IN_SAMPLER_FANCYMENU = "InSampler";
    private static final String ORIGINAL_SAMPLER_FANCYMENU = "OriginalSampler";
    private static final String BLUR_SAMPLER_FANCYMENU = "BlurSampler";
    private static final int BLUR_CONFIG_UBO_SIZE_FANCYMENU = 12;
    private static final int GUI_BLUR_CONFIG_UBO_SIZE_FANCYMENU = 80;
    private static final int SINGLE_INPUT_SAMPLER_INFO_UBO_SIZE_FANCYMENU = 16;
    private static final int DOUBLE_INPUT_SAMPLER_INFO_UBO_SIZE_FANCYMENU = 24;
    private static final float[] BLUR_RADIUS_MULTIPLIERS_FANCYMENU = new float[]{1.0F, 1.0F, 0.5F, 0.5F, 0.25F, 0.25F};
    private static final BindGroupLayout BOX_BLUR_BIND_GROUP_LAYOUT_FANCYMENU = BindGroupLayout.builder()
            .withSampler(IN_SAMPLER_FANCYMENU)
            .withUniform(SAMPLER_INFO_UNIFORM_FANCYMENU, UniformType.UNIFORM_BUFFER)
            .withUniform(BLUR_CONFIG_UNIFORM_FANCYMENU, UniformType.UNIFORM_BUFFER)
            .build();
    private static final BindGroupLayout SCREEN_COPY_BIND_GROUP_LAYOUT_FANCYMENU = BindGroupLayout.builder()
            .withSampler(IN_SAMPLER_FANCYMENU)
            .withUniform(SAMPLER_INFO_UNIFORM_FANCYMENU, UniformType.UNIFORM_BUFFER)
            .build();
    private static final BindGroupLayout GUI_BLUR_BIND_GROUP_LAYOUT_FANCYMENU = BindGroupLayout.builder()
            .withSampler(ORIGINAL_SAMPLER_FANCYMENU)
            .withSampler(BLUR_SAMPLER_FANCYMENU)
            .withUniform(SAMPLER_INFO_UNIFORM_FANCYMENU, UniformType.UNIFORM_BUFFER)
            .withUniform(GUI_BLUR_CONFIG_UNIFORM_FANCYMENU, UniformType.UNIFORM_BUFFER)
            .build();
    private static final RenderPipeline BOX_BLUR_PIPELINE_FANCYMENU = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(Identifier.withDefaultNamespace("pipeline/fancymenu_box_blur"))
            .withVertexShader("core/screenquad")
            .withFragmentShader("post/fancymenu_box_blur")
            .withBindGroupLayout(BOX_BLUR_BIND_GROUP_LAYOUT_FANCYMENU)
            .build();
    private static final RenderPipeline SCREEN_COPY_PIPELINE_FANCYMENU = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(Identifier.withDefaultNamespace("pipeline/fancymenu_copy_screen"))
            .withVertexShader("core/screenquad")
            .withFragmentShader("post/fancymenu_copy_screen")
            .withBindGroupLayout(SCREEN_COPY_BIND_GROUP_LAYOUT_FANCYMENU)
            .build();
    private static final RenderPipeline GUI_BLUR_PIPELINE_FANCYMENU = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(Identifier.withDefaultNamespace("pipeline/fancymenu_gui_blur"))
            .withVertexShader("core/screenquad")
            .withFragmentShader("post/fancymenu_gui_blur")
            .withBindGroupLayout(GUI_BLUR_BIND_GROUP_LAYOUT_FANCYMENU)
            .build();
    private static final GpuBuffer[] BLUR_CONFIG_BUFFERS_FANCYMENU = new GpuBuffer[BLUR_RADIUS_MULTIPLIERS_FANCYMENU.length];

    private static TextureTarget blurOriginalTarget_FancyMenu;
    private static TextureTarget blurSwapTarget_FancyMenu;
    private static TextureTarget blurBlurredTarget_FancyMenu;
    private static GpuBuffer boxBlurSamplerInfoBuffer_FancyMenu;
    private static GpuBuffer guiBlurSamplerInfoBuffer_FancyMenu;
    private static GpuBuffer guiBlurConfigBuffer_FancyMenu;
    private static int blurTargetWidth_FancyMenu;
    private static int blurTargetHeight_FancyMenu;

    private GuiBlurRenderer() {
    }

    /**
     * Queues a blur area that samples the current framebuffer contents when the GUI render state reaches it.
     *
     * @param graphics The GuiGraphicsExtractor for the current render pass.
     * @param x The X position in GUI pixels (top-left origin). Recommended range: 0 to screen width.
     * @param y The Y position in GUI pixels (top-left origin). Recommended range: 0 to screen height.
     * @param width The width in GUI pixels. Recommended range: 1 to screen width.
     * @param height The height in GUI pixels. Recommended range: 1 to screen height.
     * @param blurRadius The blur intensity in GUI pixels. Recommended range: 0 to 16 (4 is a good default).
     * @param cornerRadius The rounded corner radius in GUI pixels. Recommended range: 0 to min(width, height) / 2 (6 is a good default).
     * @param tint The tint color that is mixed into the blurred area. Use alpha to control strength (0.15 is a good default).
     * @param partial Partial tick for the frame; pass the current render partial.
     *
     * Example defaults: x=0, y=0, width=200, height=100, blurRadius=4, cornerRadius=6, tint=DrawableColor.of(0, 0, 0, 38)
     */
    public static void renderBlurArea(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        renderBlurAreaInternal(graphics, x, y, width, height, blurRadius, CornerRadii.uniform(cornerRadius), tint, partial);
    }

    public static void renderBlurAreaScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        GuiPoseTransformUtil.TransformedArea area = transform.transformArea(x, y, width, height);
        renderBlurArea(graphics, area.x(), area.y(), area.width(), area.height(), blurRadius * transform.scale(), cornerRadius * transform.scale(), tint, partial);
    }

    /**
     * Renders a blur area with only the top-left and top-right corners rounded.
     */
    public static void renderBlurAreaRoundTopCorners(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        renderBlurAreaInternal(graphics, x, y, width, height, blurRadius, CornerRadii.topOnly(cornerRadius), tint, partial);
    }

    public static void renderBlurAreaRoundTopCornersScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        GuiPoseTransformUtil.TransformedArea area = transform.transformArea(x, y, width, height);
        renderBlurAreaRoundTopCorners(graphics, area.x(), area.y(), area.width(), area.height(), blurRadius * transform.scale(), cornerRadius * transform.scale(), tint, partial);
    }

    /**
     * Renders a blur area with only the bottom-left and bottom-right corners rounded.
     */
    public static void renderBlurAreaRoundBottomCorners(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        renderBlurAreaInternal(graphics, x, y, width, height, blurRadius, CornerRadii.bottomOnly(cornerRadius), tint, partial);
    }

    public static void renderBlurAreaRoundBottomCornersScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        GuiPoseTransformUtil.TransformedArea area = transform.transformArea(x, y, width, height);
        renderBlurAreaRoundBottomCorners(graphics, area.x(), area.y(), area.width(), area.height(), blurRadius * transform.scale(), cornerRadius * transform.scale(), tint, partial);
    }

    /**
     * Renders a blur area with individually specified corner radii (top-left, top-right, bottom-right, bottom-left).
     */
    public static void renderBlurAreaRoundAllCorners(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, @Nonnull DrawableColor tint, float partial) {
        renderBlurAreaInternal(graphics, x, y, width, height, blurRadius, CornerRadii.of(topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius), tint, partial);
    }

    public static void renderBlurAreaRoundAllCornersScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, @Nonnull DrawableColor tint, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        GuiPoseTransformUtil.TransformedArea area = transform.transformArea(x, y, width, height);
        renderBlurAreaRoundAllCorners(
                graphics,
                area.x(),
                area.y(),
                area.width(),
                area.height(),
                blurRadius * transform.scale(),
                topLeftRadius * transform.scale(),
                topRightRadius * transform.scale(),
                bottomRightRadius * transform.scale(),
                bottomLeftRadius * transform.scale(),
                tint,
                partial
        );
    }

    /**
     * Renders a blur area as a smooth superellipse (circle/oval) using the provided bounding rectangle.
     *
     * <p>Roundness examples:
     * <br>- Perfect circle/ellipse: {@code roundness = 2.0}
     * <br>- Squircle-like (boxier): {@code roundness = 4.0}
     * <br>- Diamond-like (pointier): {@code roundness = 1.0}
     */
    public static void renderBlurAreaCircle(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float roundness, @Nonnull DrawableColor tint, float partial) {
        renderBlurAreaInternal(graphics, x, y, width, height, blurRadius, CornerRadii.uniform(0.0F), SHAPE_TYPE_SUPERELLIPSE, roundness, tint, partial);
    }

    public static void renderBlurAreaCircleScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float roundness, @Nonnull DrawableColor tint, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        GuiPoseTransformUtil.TransformedArea area = transform.transformArea(x, y, width, height);
        renderBlurAreaCircle(graphics, area.x(), area.y(), area.width(), area.height(), blurRadius * transform.scale(), roundness, tint, partial);
    }

    /**
     * Renders a blur area using FancyMenu's blur intensity setting (a normalized multiplier, e.g., 0.25–3.0).
     * Callers provide the base radius they would normally use; this helper applies the current intensity
     * and renders the blur so UI code doesn’t need to recompute the radius everywhere.
     */
    public static void renderBlurAreaWithIntensity(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float baseBlurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        float appliedRadius = Math.max(0.0F, baseBlurRadius * Math.max(0.0F, FancyMenu.getOptions().uiBlurIntensity.getValue()));
        renderBlurAreaInternal(graphics, x, y, width, height, appliedRadius, CornerRadii.uniform(cornerRadius), tint, partial);
    }

    public static void renderBlurAreaWithIntensityScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float baseBlurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        GuiPoseTransformUtil.TransformedArea area = transform.transformArea(x, y, width, height);
        renderBlurAreaWithIntensity(graphics, area.x(), area.y(), area.width(), area.height(), baseBlurRadius * transform.scale(), cornerRadius * transform.scale(), tint, partial);
    }

    /**
     * Blur area using FancyMenu's blur intensity with only the top corners rounded.
     */
    public static void renderBlurAreaWithIntensityRoundTopCorners(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float baseBlurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        float appliedRadius = Math.max(0.0F, baseBlurRadius * Math.max(0.0F, FancyMenu.getOptions().uiBlurIntensity.getValue()));
        renderBlurAreaInternal(graphics, x, y, width, height, appliedRadius, CornerRadii.topOnly(cornerRadius), tint, partial);
    }

    public static void renderBlurAreaWithIntensityRoundTopCornersScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float baseBlurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        GuiPoseTransformUtil.TransformedArea area = transform.transformArea(x, y, width, height);
        renderBlurAreaWithIntensityRoundTopCorners(graphics, area.x(), area.y(), area.width(), area.height(), baseBlurRadius * transform.scale(), cornerRadius * transform.scale(), tint, partial);
    }

    /**
     * Blur area using FancyMenu's blur intensity with only the bottom corners rounded.
     */
    public static void renderBlurAreaWithIntensityRoundBottomCorners(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float baseBlurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        float appliedRadius = Math.max(0.0F, baseBlurRadius * Math.max(0.0F, FancyMenu.getOptions().uiBlurIntensity.getValue()));
        renderBlurAreaInternal(graphics, x, y, width, height, appliedRadius, CornerRadii.bottomOnly(cornerRadius), tint, partial);
    }

    public static void renderBlurAreaWithIntensityRoundBottomCornersScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float baseBlurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        GuiPoseTransformUtil.TransformedArea area = transform.transformArea(x, y, width, height);
        renderBlurAreaWithIntensityRoundBottomCorners(graphics, area.x(), area.y(), area.width(), area.height(), baseBlurRadius * transform.scale(), cornerRadius * transform.scale(), tint, partial);
    }

    /**
     * Blur area using FancyMenu's blur intensity with individually specified corner radii (top-left, top-right, bottom-right, bottom-left).
     */
    public static void renderBlurAreaWithIntensityRoundAllCorners(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float baseBlurRadius, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, @Nonnull DrawableColor tint, float partial) {
        float appliedRadius = Math.max(0.0F, baseBlurRadius * Math.max(0.0F, FancyMenu.getOptions().uiBlurIntensity.getValue()));
        renderBlurAreaInternal(graphics, x, y, width, height, appliedRadius, CornerRadii.of(topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius), tint, partial);
    }

    public static void renderBlurAreaWithIntensityRoundAllCornersScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float baseBlurRadius, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, @Nonnull DrawableColor tint, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        GuiPoseTransformUtil.TransformedArea area = transform.transformArea(x, y, width, height);
        renderBlurAreaWithIntensityRoundAllCorners(
                graphics,
                area.x(),
                area.y(),
                area.width(),
                area.height(),
                baseBlurRadius * transform.scale(),
                topLeftRadius * transform.scale(),
                topRightRadius * transform.scale(),
                bottomRightRadius * transform.scale(),
                bottomLeftRadius * transform.scale(),
                tint,
                partial
        );
    }

    private static void renderBlurAreaInternal(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, @Nonnull CornerRadii cornerRadii, @Nonnull DrawableColor tint, float partial) {
        renderBlurAreaInternal(graphics, x, y, width, height, blurRadius, cornerRadii, SHAPE_TYPE_ROUNDED_RECT, 2.0F, tint, partial);
    }

    private static void renderBlurAreaInternal(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, @Nonnull CornerRadii cornerRadii, float shapeType, float roundness, @Nonnull DrawableColor tint, float partial) {
        Objects.requireNonNull(graphics);
        Objects.requireNonNull(cornerRadii);
        Objects.requireNonNull(tint);
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        _renderBlurArea(graphics, partial, new BlurArea(x, y, width, height, blurRadius, cornerRadii, shapeType, clampRoundness(roundness), tint));
    }

    private static void _renderBlurArea(GuiGraphicsExtractor graphics, float partial, BlurArea area) {
        RenderRotationUtil.Rotation2D maskRotation = GuiPoseTransformUtil.resolve(graphics).rotation();
        GuiRenderState renderState = ((IMixinGuiGraphicsExtractor) graphics).get_guiRenderState_FancyMenu();
        QueuedBlurArea queuedBlurArea = new QueuedBlurArea(area, maskRotation);
        renderState.nextStratum();
        renderState.addGuiElement(new GuiBlurRenderState(queuedBlurArea, toScreenBounds(area)));
        renderState.nextStratum();
        RenderingUtils.resetShaderColor(graphics);
    }

    public static void executeQueuedBlurArea_FancyMenu(@Nonnull QueuedBlurArea queuedBlurArea) {
        Objects.requireNonNull(queuedBlurArea);
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget mainTarget = minecraft.gameRenderer.mainRenderTarget();
        int targetWidth = mainTarget.width;
        int targetHeight = mainTarget.height;
        if (targetWidth <= 0 || targetHeight <= 0) {
            return;
        }

        BlurArea area = queuedBlurArea.area_FancyMenu;
        float guiScale = (float) minecraft.getWindow().getGuiScale();
        float scaledWidth = area.width * guiScale;
        float scaledHeight = area.height * guiScale;
        if (scaledWidth <= 0.0F || scaledHeight <= 0.0F) {
            return;
        }

        float scaledX = area.x * guiScale;
        float scaledY = targetHeight - (area.y * guiScale) - scaledHeight;
        float rawBlurRadius = area.blurRadius * guiScale;
        float blurRadius = Float.isFinite(rawBlurRadius) && rawBlurRadius > 0.0F ? rawBlurRadius : 0.0F;
        CornerRadii scaledRadii = area.cornerRadii.scaled(guiScale).clamped(Math.min(scaledWidth, scaledHeight) * 0.5F).flipVertical();

        DrawableColor.FloatColor tint = area.tint.getAsFloats();
        RenderRotationUtil.Rotation2D maskRotation = queuedBlurArea.maskRotation_FancyMenu;
        RenderRotationUtil.Rotation2D scissorRotation = maskRotation;
        float margin = guiScale > 0.0F ? (blurRadius / guiScale) * 4.0F : 0.0F;
        BlurScissor scissor = toBlurScissor(resolveScissorBounds(area, margin, scissorRotation), guiScale, targetWidth, targetHeight);
        if (scissor.isEmpty()) {
            return;
        }

        ensureBlurTargets_FancyMenu(targetWidth, targetHeight);
        updateSamplerInfoUniforms_FancyMenu(targetWidth, targetHeight);
        updateBlurConfigUniforms_FancyMenu(scaledX, scaledY, scaledWidth, scaledHeight, blurRadius, scaledRadii, area.shapeType, area.roundness, maskRotation, tint);
        executeBlurPasses_FancyMenu(mainTarget, scissor);
    }

    public static void close_FancyMenu() {
        closeBlurTargets_FancyMenu();
        closeBuffer_FancyMenu(boxBlurSamplerInfoBuffer_FancyMenu);
        closeBuffer_FancyMenu(guiBlurSamplerInfoBuffer_FancyMenu);
        closeBuffer_FancyMenu(guiBlurConfigBuffer_FancyMenu);
        boxBlurSamplerInfoBuffer_FancyMenu = null;
        guiBlurSamplerInfoBuffer_FancyMenu = null;
        guiBlurConfigBuffer_FancyMenu = null;
        for (int i = 0; i < BLUR_CONFIG_BUFFERS_FANCYMENU.length; i++) {
            closeBuffer_FancyMenu(BLUR_CONFIG_BUFFERS_FANCYMENU[i]);
            BLUR_CONFIG_BUFFERS_FANCYMENU[i] = null;
        }
    }

    private static void ensureBlurTargets_FancyMenu(int width, int height) {
        if (blurOriginalTarget_FancyMenu != null && blurTargetWidth_FancyMenu == width && blurTargetHeight_FancyMenu == height) {
            return;
        }
        closeBlurTargets_FancyMenu();
        blurOriginalTarget_FancyMenu = new TextureTarget("FancyMenu GUI blur original", width, height, false, GpuFormat.RGBA8_UNORM);
        blurSwapTarget_FancyMenu = new TextureTarget("FancyMenu GUI blur swap", width, height, false, GpuFormat.RGBA8_UNORM);
        blurBlurredTarget_FancyMenu = new TextureTarget("FancyMenu GUI blur blurred", width, height, false, GpuFormat.RGBA8_UNORM);
        blurTargetWidth_FancyMenu = width;
        blurTargetHeight_FancyMenu = height;
    }

    private static void closeBlurTargets_FancyMenu() {
        if (blurOriginalTarget_FancyMenu != null) {
            blurOriginalTarget_FancyMenu.destroyBuffers();
            blurOriginalTarget_FancyMenu = null;
        }
        if (blurSwapTarget_FancyMenu != null) {
            blurSwapTarget_FancyMenu.destroyBuffers();
            blurSwapTarget_FancyMenu = null;
        }
        if (blurBlurredTarget_FancyMenu != null) {
            blurBlurredTarget_FancyMenu.destroyBuffers();
            blurBlurredTarget_FancyMenu = null;
        }
        blurTargetWidth_FancyMenu = 0;
        blurTargetHeight_FancyMenu = 0;
    }

    private static void updateSamplerInfoUniforms_FancyMenu(int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, SINGLE_INPUT_SAMPLER_INFO_UBO_SIZE_FANCYMENU)
                    .putVec2(width, height)
                    .putVec2(width, height)
                    .get();
            boxBlurSamplerInfoBuffer_FancyMenu = updateUniformBuffer_FancyMenu(boxBlurSamplerInfoBuffer_FancyMenu, "box blur sampler info", data);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, DOUBLE_INPUT_SAMPLER_INFO_UBO_SIZE_FANCYMENU)
                    .putVec2(width, height)
                    .putVec2(width, height)
                    .putVec2(width, height)
                    .get();
            guiBlurSamplerInfoBuffer_FancyMenu = updateUniformBuffer_FancyMenu(guiBlurSamplerInfoBuffer_FancyMenu, "GUI blur sampler info", data);
        }
    }

    private static void updateBlurConfigUniforms_FancyMenu(float x, float y, float width, float height, float blurRadius, CornerRadii cornerRadii, float shapeType, float roundness, RenderRotationUtil.Rotation2D rotation, DrawableColor.FloatColor tint) {
        for (int i = 0; i < BLUR_RADIUS_MULTIPLIERS_FANCYMENU.length; i++) {
            float directionX = (i & 1) == 0 ? 1.0F : 0.0F;
            float directionY = (i & 1) == 0 ? 0.0F : 1.0F;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer data = Std140Builder.onStack(stack, BLUR_CONFIG_UBO_SIZE_FANCYMENU)
                        .putVec2(directionX, directionY)
                        .putFloat(blurRadius * BLUR_RADIUS_MULTIPLIERS_FANCYMENU[i])
                        .get();
                BLUR_CONFIG_BUFFERS_FANCYMENU[i] = updateUniformBuffer_FancyMenu(BLUR_CONFIG_BUFFERS_FANCYMENU[i], "box blur config " + i, data);
            }
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, GUI_BLUR_CONFIG_UBO_SIZE_FANCYMENU)
                    .putVec4(x, y, width, height)
                    .putVec4(cornerRadii.topLeft(), cornerRadii.topRight(), cornerRadii.bottomRight(), cornerRadii.bottomLeft())
                    .putVec4(rotation.m00(), rotation.m01(), rotation.m10(), rotation.m11())
                    .putVec4(tint.red(), tint.green(), tint.blue(), tint.alpha())
                    .putVec4(shapeType, roundness, 0.0F, 0.0F)
                    .get();
            guiBlurConfigBuffer_FancyMenu = updateUniformBuffer_FancyMenu(guiBlurConfigBuffer_FancyMenu, "GUI blur config", data);
        }
    }

    private static GpuBuffer updateUniformBuffer_FancyMenu(@Nullable GpuBuffer currentBuffer, String uniformName, ByteBuffer data) {
        if (currentBuffer == null || currentBuffer.isClosed() || currentBuffer.size() != data.remaining() || (currentBuffer.usage() & GpuBuffer.USAGE_COPY_DST) == 0) {
            GpuBuffer newBuffer = RenderSystem.getDevice().createBuffer(() -> "FancyMenu GUI blur " + uniformName, GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, data);
            closeBuffer_FancyMenu(currentBuffer);
            return newBuffer;
        }

        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(currentBuffer.slice(), data);
        return currentBuffer;
    }

    private static void executeBlurPasses_FancyMenu(RenderTarget mainTarget, BlurScissor scissor) {
        if (mainTarget.getColorTexture() == null || blurOriginalTarget_FancyMenu == null || blurSwapTarget_FancyMenu == null || blurBlurredTarget_FancyMenu == null || blurOriginalTarget_FancyMenu.getColorTexture() == null) {
            return;
        }
        if (boxBlurSamplerInfoBuffer_FancyMenu == null || guiBlurSamplerInfoBuffer_FancyMenu == null || guiBlurConfigBuffer_FancyMenu == null) {
            return;
        }
        for (GpuBuffer blurConfigBuffer : BLUR_CONFIG_BUFFERS_FANCYMENU) {
            if (blurConfigBuffer == null) {
                return;
            }
        }

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        GpuSampler blurSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        if (RenderingUtils.isVulkanActive()) {
            executeTextureCopy_FancyMenu(commandEncoder, mainTarget, blurOriginalTarget_FancyMenu, scissor);
        } else {
            GpuSampler copySampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
            executeScreenCopyPass_FancyMenu(commandEncoder, mainTarget, blurOriginalTarget_FancyMenu, scissor, copySampler);
        }
        executeBoxBlurPass_FancyMenu(commandEncoder, "FancyMenu GUI blur horizontal 0", blurOriginalTarget_FancyMenu, blurSwapTarget_FancyMenu, BLUR_CONFIG_BUFFERS_FANCYMENU[0], scissor, blurSampler);
        executeBoxBlurPass_FancyMenu(commandEncoder, "FancyMenu GUI blur vertical 0", blurSwapTarget_FancyMenu, blurBlurredTarget_FancyMenu, BLUR_CONFIG_BUFFERS_FANCYMENU[1], scissor, blurSampler);
        executeBoxBlurPass_FancyMenu(commandEncoder, "FancyMenu GUI blur horizontal 1", blurBlurredTarget_FancyMenu, blurSwapTarget_FancyMenu, BLUR_CONFIG_BUFFERS_FANCYMENU[2], scissor, blurSampler);
        executeBoxBlurPass_FancyMenu(commandEncoder, "FancyMenu GUI blur vertical 1", blurSwapTarget_FancyMenu, blurBlurredTarget_FancyMenu, BLUR_CONFIG_BUFFERS_FANCYMENU[3], scissor, blurSampler);
        executeBoxBlurPass_FancyMenu(commandEncoder, "FancyMenu GUI blur horizontal 2", blurBlurredTarget_FancyMenu, blurSwapTarget_FancyMenu, BLUR_CONFIG_BUFFERS_FANCYMENU[4], scissor, blurSampler);
        executeBoxBlurPass_FancyMenu(commandEncoder, "FancyMenu GUI blur vertical 2", blurSwapTarget_FancyMenu, blurBlurredTarget_FancyMenu, BLUR_CONFIG_BUFFERS_FANCYMENU[5], scissor, blurSampler);
        executeGuiBlurPass_FancyMenu(commandEncoder, mainTarget, scissor, blurSampler);
    }

    private static void executeTextureCopy_FancyMenu(CommandEncoder commandEncoder, RenderTarget inputTarget, RenderTarget outputTarget, BlurScissor scissor) {
        if (inputTarget.getColorTexture() == null || outputTarget.getColorTexture() == null) {
            return;
        }

        commandEncoder.copyTextureToTexture(
                inputTarget.getColorTexture(),
                outputTarget.getColorTexture(),
                0,
                scissor.x(),
                scissor.y(),
                scissor.x(),
                scissor.y(),
                scissor.width(),
                scissor.height()
        );
    }

    private static void executeScreenCopyPass_FancyMenu(CommandEncoder commandEncoder, RenderTarget inputTarget, RenderTarget outputTarget, BlurScissor scissor, GpuSampler sampler) {
        if (inputTarget.getColorTextureView() == null || outputTarget.getColorTextureView() == null) {
            return;
        }

        // OpenGL texture copies go through framebuffer blits, so use a render pass for predictable sub-region semantics.
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "FancyMenu GUI blur source copy", outputTarget.getColorTextureView(), Optional.empty())) {
            renderPass.setPipeline(SCREEN_COPY_PIPELINE_FANCYMENU);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform(SAMPLER_INFO_UNIFORM_FANCYMENU, boxBlurSamplerInfoBuffer_FancyMenu);
            renderPass.bindTexture(IN_SAMPLER_FANCYMENU, inputTarget.getColorTextureView(), sampler);
            enableScissor_FancyMenu(renderPass, scissor);
            renderPass.draw(3, 1, 0, 0);
            renderPass.disableScissor();
        }
    }

    private static void executeBoxBlurPass_FancyMenu(CommandEncoder commandEncoder, String label, RenderTarget inputTarget, RenderTarget outputTarget, GpuBuffer blurConfigBuffer, BlurScissor scissor, GpuSampler sampler) {
        if (inputTarget.getColorTextureView() == null || outputTarget.getColorTextureView() == null) {
            return;
        }

        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> label, outputTarget.getColorTextureView(), Optional.empty())) {
            renderPass.setPipeline(BOX_BLUR_PIPELINE_FANCYMENU);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform(SAMPLER_INFO_UNIFORM_FANCYMENU, boxBlurSamplerInfoBuffer_FancyMenu);
            renderPass.setUniform(BLUR_CONFIG_UNIFORM_FANCYMENU, blurConfigBuffer);
            renderPass.bindTexture(IN_SAMPLER_FANCYMENU, inputTarget.getColorTextureView(), sampler);
            enableScissor_FancyMenu(renderPass, scissor);
            renderPass.draw(3, 1, 0, 0);
            renderPass.disableScissor();
        }
    }

    private static void executeGuiBlurPass_FancyMenu(CommandEncoder commandEncoder, RenderTarget mainTarget, BlurScissor scissor, GpuSampler sampler) {
        if (mainTarget.getColorTextureView() == null || blurOriginalTarget_FancyMenu.getColorTextureView() == null || blurBlurredTarget_FancyMenu.getColorTextureView() == null) {
            return;
        }

        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "FancyMenu GUI blur composite", mainTarget.getColorTextureView(), Optional.empty(), mainTarget.useDepth ? mainTarget.getDepthTextureView() : null, OptionalDouble.empty())) {
            renderPass.setPipeline(GUI_BLUR_PIPELINE_FANCYMENU);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform(SAMPLER_INFO_UNIFORM_FANCYMENU, guiBlurSamplerInfoBuffer_FancyMenu);
            renderPass.setUniform(GUI_BLUR_CONFIG_UNIFORM_FANCYMENU, guiBlurConfigBuffer_FancyMenu);
            renderPass.bindTexture(ORIGINAL_SAMPLER_FANCYMENU, blurOriginalTarget_FancyMenu.getColorTextureView(), sampler);
            renderPass.bindTexture(BLUR_SAMPLER_FANCYMENU, blurBlurredTarget_FancyMenu.getColorTextureView(), sampler);
            enableScissor_FancyMenu(renderPass, scissor);
            renderPass.draw(3, 1, 0, 0);
            renderPass.disableScissor();
        }
    }

    private static void enableScissor_FancyMenu(RenderPass renderPass, BlurScissor scissor) {
        renderPass.enableScissor(scissor.x(), scissor.y(), scissor.width(), scissor.height());
    }

    private static void closeBuffer_FancyMenu(@Nullable GpuBuffer buffer) {
        if (buffer != null && !buffer.isClosed()) {
            buffer.close();
        }
    }

    private static BlurScissor toBlurScissor(ScissorBounds bounds, float guiScale, int targetWidth, int targetHeight) {
        float minX = bounds.minX() * guiScale;
        float maxX = bounds.maxX() * guiScale;
        float minY = targetHeight - bounds.maxY() * guiScale;
        float maxY = targetHeight - bounds.minY() * guiScale;

        int x0 = clampToInt(floorToInt(minX), 0, targetWidth);
        int y0 = clampToInt(floorToInt(minY), 0, targetHeight);
        int x1 = clampToInt(ceilToInt(maxX), 0, targetWidth);
        int y1 = clampToInt(ceilToInt(maxY), 0, targetHeight);
        return new BlurScissor(x0, y0, Math.max(0, x1 - x0), Math.max(0, y1 - y0));
    }

    private static ScreenRectangle toScreenBounds(BlurArea area) {
        int x0 = floorToInt(area.x);
        int y0 = floorToInt(area.y);
        int x1 = ceilToInt(area.x + area.width);
        int y1 = ceilToInt(area.y + area.height);
        return new ScreenRectangle(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
    }

    private record BlurArea(float x, float y, float width, float height, float blurRadius, CornerRadii cornerRadii, float shapeType, float roundness, DrawableColor tint) {
    }

    public static final class QueuedBlurArea {

        private final BlurArea area_FancyMenu;
        private final RenderRotationUtil.Rotation2D maskRotation_FancyMenu;

        private QueuedBlurArea(BlurArea area, RenderRotationUtil.Rotation2D maskRotation) {
            this.area_FancyMenu = area;
            this.maskRotation_FancyMenu = maskRotation;
        }

    }

    public record GuiBlurRenderState(QueuedBlurArea queuedBlurArea, ScreenRectangle bounds) implements GuiElementRenderState {

        @Override
        public void buildVertices(VertexConsumer vertexConsumer) {
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }

        @Override
        @Nullable
        public ScreenRectangle scissorArea() {
            return null;
        }

    }

    private record BlurScissor(int x, int y, int width, int height) {

        private boolean isEmpty() {
            return width <= 0 || height <= 0;
        }

    }

    private record CornerRadii(float topLeft, float topRight, float bottomRight, float bottomLeft) {

        private static CornerRadii uniform(float radius) {
            return new CornerRadii(radius, radius, radius, radius);
        }

        private static CornerRadii topOnly(float radius) {
            return new CornerRadii(radius, radius, 0.0F, 0.0F);
        }

        private static CornerRadii bottomOnly(float radius) {
            return new CornerRadii(0.0F, 0.0F, radius, radius);
        }

        private static CornerRadii of(float topLeft, float topRight, float bottomRight, float bottomLeft) {
            return new CornerRadii(topLeft, topRight, bottomRight, bottomLeft);
        }

        private CornerRadii scaled(float factor) {
            return new CornerRadii(topLeft * factor, topRight * factor, bottomRight * factor, bottomLeft * factor);
        }

        private CornerRadii clamped(float maxRadius) {
            float clampedMax = Math.max(0.0F, maxRadius);
            return new CornerRadii(clampCorner(topLeft, clampedMax), clampCorner(topRight, clampedMax), clampCorner(bottomRight, clampedMax), clampCorner(bottomLeft, clampedMax));
        }

        private CornerRadii flipVertical() {
            return new CornerRadii(bottomLeft, bottomRight, topRight, topLeft);
        }

        private static float clampCorner(float value, float max) {
            if (value <= 0.0F) {
                return 0.0F;
            }
            return value > max ? max : value;
        }
    }

    private static float clampRoundness(float roundness) {
        if (!Float.isFinite(roundness)) {
            return 2.0F;
        }
        return Math.max(0.1F, roundness);
    }

    private static ScissorBounds resolveScissorBounds(BlurArea area, float margin, RenderRotationUtil.Rotation2D rotation) {
        float baseMinX = area.x - margin;
        float baseMinY = area.y - margin;
        float baseMaxX = area.x + area.width + margin;
        float baseMaxY = area.y + area.height + margin;

        if (rotation == null || isRotationIdentity(rotation)) {
            return new ScissorBounds(baseMinX, baseMinY, baseMaxX, baseMaxY);
        }

        float m00 = rotation.m00();
        float m01 = rotation.m01();
        float m10 = rotation.m10();
        float m11 = rotation.m11();
        if (!isFinite(m00) || !isFinite(m01) || !isFinite(m10) || !isFinite(m11)) {
            return new ScissorBounds(baseMinX, baseMinY, baseMaxX, baseMaxY);
        }

        float halfWidth = area.width * 0.5F;
        float halfHeight = area.height * 0.5F;
        float boundHalfWidth = Math.abs(m00) * halfWidth + Math.abs(m01) * halfHeight;
        float boundHalfHeight = Math.abs(m10) * halfWidth + Math.abs(m11) * halfHeight;
        if (!Float.isFinite(boundHalfWidth) || !Float.isFinite(boundHalfHeight)) {
            return new ScissorBounds(baseMinX, baseMinY, baseMaxX, baseMaxY);
        }

        float centerX = area.x + halfWidth;
        float centerY = area.y + halfHeight;

        float rotatedMinX = centerX - boundHalfWidth - margin;
        float rotatedMaxX = centerX + boundHalfWidth + margin;
        float rotatedMinY = centerY - boundHalfHeight - margin;
        float rotatedMaxY = centerY + boundHalfHeight + margin;

        return new ScissorBounds(
                Math.min(baseMinX, rotatedMinX),
                Math.min(baseMinY, rotatedMinY),
                Math.max(baseMaxX, rotatedMaxX),
                Math.max(baseMaxY, rotatedMaxY)
        );
    }

    private static boolean isRotationIdentity(RenderRotationUtil.Rotation2D rotation) {
        return nearlyEqual(rotation.m00(), 1.0F)
                && nearlyEqual(rotation.m11(), 1.0F)
                && nearlyEqual(rotation.m01(), 0.0F)
                && nearlyEqual(rotation.m10(), 0.0F);
    }

    private static boolean nearlyEqual(float a, float b) {
        return Math.abs(a - b) <= 1.0E-4F;
    }

    private static boolean isFinite(float value) {
        return Float.isFinite(value);
    }

    private static int clampToInt(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    private static int floorToInt(float value) {
        return (int) Math.floor(value);
    }

    private static int ceilToInt(float value) {
        return (int) Math.ceil(value);
    }

    private record ScissorBounds(float minX, float minY, float maxX, float maxY) {
    }

}
