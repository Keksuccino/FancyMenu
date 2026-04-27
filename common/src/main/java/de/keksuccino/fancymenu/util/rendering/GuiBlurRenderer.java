package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGameRenderer;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGuiGraphicsExtractor;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinPostChain;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinPostPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.MemoryStack;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GuiBlurRenderer {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final float SHAPE_TYPE_ROUNDED_RECT = 0.0F;
    private static final float SHAPE_TYPE_SUPERELLIPSE = 1.0F;
    private static final Identifier GUI_BLUR_POST_CHAIN = Identifier.withDefaultNamespace("fancymenu_gui_blur");
    private static final String BLUR_CONFIG_UNIFORM_FANCYMENU = "BlurConfig";
    private static final String GUI_BLUR_CONFIG_UNIFORM_FANCYMENU = "GuiBlurConfig";
    private static final int BLUR_CONFIG_UBO_SIZE_FANCYMENU = 12;
    private static final int GUI_BLUR_CONFIG_UBO_SIZE_FANCYMENU = 80;
    private static final float[] BLUR_RADIUS_MULTIPLIERS_FANCYMENU = new float[]{1.0F, 1.0F, 0.5F, 0.5F, 0.25F, 0.25F};
    private static final ThreadLocal<PostPassScissor> ACTIVE_POST_PASS_SCISSOR_FANCYMENU = new ThreadLocal<>();

    private static boolean blurPostChainFailed;

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
        PostChain postChain = getOrCreatePostChain(minecraft);
        if (postChain == null) {
            return;
        }
        int targetWidth = minecraft.getWindow().getWidth();
        int targetHeight = minecraft.getWindow().getHeight();
        if (targetWidth <= 0 || targetHeight <= 0) {
            return;
        }
        ensurePostChainSize(postChain, targetWidth, targetHeight);

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
        PostPassScissor scissor = toPostPassScissor(resolveScissorBounds(area, margin, scissorRotation), guiScale, targetWidth, targetHeight);
        if (scissor.isEmpty()) {
            return;
        }
        applyUniforms(postChain, scaledX, scaledY, scaledWidth, scaledHeight, blurRadius, scaledRadii, area.shapeType, area.roundness, maskRotation, tint);

        // The final post pass writes a masked mix back into the main target, so no extra blit is needed here.
        ACTIVE_POST_PASS_SCISSOR_FANCYMENU.set(scissor);
        try {
            postChain.process(minecraft.getMainRenderTarget(), getResourceAllocator(minecraft));
        } finally {
            ACTIVE_POST_PASS_SCISSOR_FANCYMENU.remove();
        }
    }

    private static GraphicsResourceAllocator getResourceAllocator(Minecraft minecraft) {
        return ((IMixinGameRenderer) minecraft.gameRenderer).get_resourcePool_FancyMenu();
    }

    public static PostPassScissor getActivePostPassScissor_FancyMenu() {
        return ACTIVE_POST_PASS_SCISSOR_FANCYMENU.get();
    }

    private static PostChain getOrCreatePostChain(Minecraft minecraft) {
        PostChain postChain = minecraft.getShaderManager().getPostChain(GUI_BLUR_POST_CHAIN, LevelTargetBundle.MAIN_TARGETS);
        if (postChain == null) {
            if (!blurPostChainFailed) {
                blurPostChainFailed = true;
                LOGGER.error("[FANCYMENU] Failed to load GUI blur shader!");
            }
            return null;
        }
        blurPostChainFailed = false;
        return postChain;
    }

    private static void ensurePostChainSize(PostChain postChain, int width, int height) {
        // 1.21.11 frame graph post chains size their internal targets from the input target each process call.
    }

    private static void applyUniforms(PostChain postChain, float x, float y, float width, float height, float blurRadius, CornerRadii cornerRadii, float shapeType, float roundness, RenderRotationUtil.Rotation2D rotation, DrawableColor.FloatColor tint) {
        List<PostPass> passes = ((IMixinPostChain) postChain).getPasses_FancyMenu();
        int blurIndex = 0;
        for (PostPass pass : passes) {
            Map<String, GpuBuffer> customUniforms = ((IMixinPostPass) pass).get_customUniforms_FancyMenu();
            if (customUniforms.containsKey(BLUR_CONFIG_UNIFORM_FANCYMENU) && blurIndex < BLUR_RADIUS_MULTIPLIERS_FANCYMENU.length) {
                float directionX = (blurIndex & 1) == 0 ? 1.0F : 0.0F;
                float directionY = (blurIndex & 1) == 0 ? 0.0F : 1.0F;
                updateBlurConfigUniform(customUniforms, directionX, directionY, blurRadius * BLUR_RADIUS_MULTIPLIERS_FANCYMENU[blurIndex]);
                blurIndex++;
            }

            if (customUniforms.containsKey(GUI_BLUR_CONFIG_UNIFORM_FANCYMENU)) {
                updateGuiBlurConfigUniform(customUniforms, x, y, width, height, cornerRadii, shapeType, roundness, rotation, tint);
            }
        }
    }

    private static void updateBlurConfigUniform(Map<String, GpuBuffer> customUniforms, float directionX, float directionY, float radius) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, BLUR_CONFIG_UBO_SIZE_FANCYMENU)
                    .putVec2(directionX, directionY)
                    .putFloat(radius)
                    .get();
            updateUniformBuffer(customUniforms, BLUR_CONFIG_UNIFORM_FANCYMENU, data);
        }
    }

    private static void updateGuiBlurConfigUniform(Map<String, GpuBuffer> customUniforms, float x, float y, float width, float height, CornerRadii cornerRadii, float shapeType, float roundness, RenderRotationUtil.Rotation2D rotation, DrawableColor.FloatColor tint) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, GUI_BLUR_CONFIG_UBO_SIZE_FANCYMENU)
                    .putVec4(x, y, width, height)
                    .putVec4(cornerRadii.topLeft(), cornerRadii.topRight(), cornerRadii.bottomRight(), cornerRadii.bottomLeft())
                    .putVec4(rotation.m00(), rotation.m01(), rotation.m10(), rotation.m11())
                    .putVec4(tint.red(), tint.green(), tint.blue(), tint.alpha())
                    .putVec4(shapeType, roundness, 0.0F, 0.0F)
                    .get();
            updateUniformBuffer(customUniforms, GUI_BLUR_CONFIG_UNIFORM_FANCYMENU, data);
        }
    }

    private static void updateUniformBuffer(Map<String, GpuBuffer> customUniforms, String uniformName, ByteBuffer data) {
        GpuBuffer currentBuffer = customUniforms.get(uniformName);
        if (currentBuffer == null) {
            return;
        }

        if (currentBuffer.size() != data.remaining() || (currentBuffer.usage() & GpuBuffer.USAGE_COPY_DST) == 0) {
            GpuBuffer newBuffer = RenderSystem.getDevice().createBuffer(() -> "FancyMenu GUI blur " + uniformName, GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, data);
            customUniforms.put(uniformName, newBuffer);
            currentBuffer.close();
            return;
        }

        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(currentBuffer.slice(), data);
    }

    private static PostPassScissor toPostPassScissor(ScissorBounds bounds, float guiScale, int targetWidth, int targetHeight) {
        float minX = bounds.minX() * guiScale;
        float maxX = bounds.maxX() * guiScale;
        float minY = targetHeight - bounds.maxY() * guiScale;
        float maxY = targetHeight - bounds.minY() * guiScale;

        int x0 = clampToInt(floorToInt(minX), 0, targetWidth);
        int y0 = clampToInt(floorToInt(minY), 0, targetHeight);
        int x1 = clampToInt(ceilToInt(maxX), 0, targetWidth);
        int y1 = clampToInt(ceilToInt(maxY), 0, targetHeight);
        return new PostPassScissor(x0, y0, Math.max(0, x1 - x0), Math.max(0, y1 - y0));
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

    public record PostPassScissor(int x, int y, int width, int height) {

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
