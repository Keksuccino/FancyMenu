package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.Objects;

public final class GuiBlurRenderer {

    private static final Logger LOGGER = LogManager.getLogger("FancyMenu-GuiBlur");
    private static final GuiBlurRenderer INSTANCE = new GuiBlurRenderer();

    private TextureTarget pingTarget;
    private TextureTarget pongTarget;
    private EffectInstance boxBlurEffect;
    private ShaderInstance compositeShader;
    private boolean compositeManagedExternally;
    private boolean resourcesFailed;

    private GuiBlurRenderer() {
        MinecraftResourceReloadObserver.addReloadListener(action -> {
            if (action == MinecraftResourceReloadObserver.ReloadAction.STARTING) {
                RenderCall call = this::resetResources;
                if (RenderSystem.isOnRenderThread()) {
                    call.execute();
                } else {
                    RenderSystem.recordRenderCall(call);
                }
            }
        });
    }

    public static GuiBlurRenderer getInstance() {
        return INSTANCE;
    }

    public void apply(@NotNull GuiGraphics graphics, float x, float y, float width, float height, int tintArgb, float intensity, float cornerRadiusGui, boolean rounded) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }

        graphics.flush();
        RenderSystem.assertOnRenderThread();

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        if (mainTarget == null) {
            fallbackFill(graphics, x, y, width, height, tintArgb);
            return;
        }

        if (!ensureResources(minecraft)) {
            fallbackFill(graphics, x, y, width, height, tintArgb);
            return;
        }

        double guiScale = minecraft.getWindow().getGuiScale();
        int windowWidth = mainTarget.viewWidth;
        int windowHeight = mainTarget.viewHeight;

        int areaWidthPx = Math.max(1, Mth.ceil(width * guiScale));
        int areaHeightPx = Math.max(1, Mth.ceil(height * guiScale));
        int areaLeftPx = Mth.floor(x * guiScale);
        int areaTopPx = Mth.floor(y * guiScale);
        int areaRightPx = areaLeftPx + areaWidthPx;
        int areaBottomPx = windowHeight - (areaTopPx + areaHeightPx);

        if (areaRightPx <= 0 || areaBottomPx + areaHeightPx <= 0 || areaLeftPx >= windowWidth || areaBottomPx >= windowHeight) {
            return;
        }

        float radius = Mth.clamp(intensity, 0.0F, GameRenderer.MAX_BLUR_RADIUS);
        if (radius < 0.05F) {
            fallbackFill(graphics, x, y, width, height, tintArgb);
            return;
        }

        int padding = Math.max(2, Mth.ceil(radius) + 2);
        int copyLeft = Mth.clamp(areaLeftPx - padding, 0, windowWidth);
        int copyRight = Mth.clamp(areaRightPx + padding, 0, windowWidth);
        int copyBottom = Mth.clamp(areaBottomPx - padding, 0, windowHeight);
        int copyTop = Mth.clamp(areaBottomPx + areaHeightPx + padding, 0, windowHeight);

        int copyWidth = copyRight - copyLeft;
        int copyHeight = copyTop - copyBottom;
        if (copyWidth <= 0 || copyHeight <= 0) {
            fallbackFill(graphics, x, y, width, height, tintArgb);
            return;
        }

        TextureTarget firstTarget = ensureTarget(copyWidth, copyHeight, true);
        TextureTarget secondTarget = ensureTarget(copyWidth, copyHeight, false);
        if (firstTarget == null || secondTarget == null) {
            fallbackFill(graphics, x, y, width, height, tintArgb);
            return;
        }

        blitRegion(mainTarget, firstTarget, copyLeft, copyBottom, copyRight, copyTop);

        TextureTarget blurred = runBlurPasses(minecraft, firstTarget, secondTarget, radius);

        restoreMainTarget(mainTarget);

        if (blurred == null) {
            fallbackFill(graphics, x, y, width, height, tintArgb);
            return;
        }

        float uMin = copyWidth == 0 ? 0.0F : (float) Mth.clamp(areaLeftPx - copyLeft, 0, copyWidth) / (float) copyWidth;
        float uMax = copyWidth == 0 ? 1.0F : (float) Mth.clamp(areaRightPx - copyLeft, 0, copyWidth) / (float) copyWidth;
        float vMin = copyHeight == 0 ? 0.0F : (float) Mth.clamp(areaBottomPx - copyBottom, 0, copyHeight) / (float) copyHeight;
        float vMax = copyHeight == 0 ? 1.0F : (float) Mth.clamp(areaBottomPx + areaHeightPx - copyBottom, 0, copyHeight) / (float) copyHeight;

        float tintR = FastColor.ARGB32.red(tintArgb) / 255.0F;
        float tintG = FastColor.ARGB32.green(tintArgb) / 255.0F;
        float tintB = FastColor.ARGB32.blue(tintArgb) / 255.0F;
        float tintA = FastColor.ARGB32.alpha(tintArgb) / 255.0F;

        float cornerRadiusPx = rounded ? cornerRadiusGui * (float) guiScale : 0.0F;
        float maxRadiusPx = Math.min(areaWidthPx, areaHeightPx) * 0.5F;
        cornerRadiusPx = rounded ? Mth.clamp(cornerRadiusPx, 0.0F, maxRadiusPx) : 0.0F;
        boolean useRounded = rounded && cornerRadiusPx > 0.0F;
        float smoothRadiusPx = useRounded ? Math.max(1.0F, (float) guiScale * 0.75F) : 0.0F;

        renderCompositeQuad(
            graphics,
            blurred,
            x,
            y,
            width,
            height,
            areaLeftPx,
            areaBottomPx,
            areaWidthPx,
            areaHeightPx,
            uMin,
            vMin,
            uMax,
            vMax,
            tintR,
            tintG,
            tintB,
            tintA,
            cornerRadiusPx,
            smoothRadiusPx,
            useRounded,
            tintArgb);
    }

    private void fallbackFill(GuiGraphics graphics, float x, float y, float width, float height, int tintArgb) {
        int minX = Mth.floor(x);
        int minY = Mth.floor(y);
        int maxX = Mth.floor(x + width);
        int maxY = Mth.floor(y + height);
        graphics.fill(minX, minY, maxX, maxY, tintArgb);
    }

    private boolean ensureResources(Minecraft minecraft) {
        if (resourcesFailed) {
            return false;
        }

        ResourceProvider provider = minecraft.getResourceManager();
        try {
            if (boxBlurEffect == null) {
                boxBlurEffect = new EffectInstance(provider, "box_blur");
            }
            if (compositeShader == null) {
                compositeShader = new ShaderInstance(provider, "fancymenu_gui_blur_composite", DefaultVertexFormat.POSITION_TEX);
                compositeManagedExternally = false;
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to initialize GUI blur shader resources", exception);
            resetResources();
            resourcesFailed = true;
            return false;
        }

        resourcesFailed = false;
        return true;
    }

    private @Nullable TextureTarget ensureTarget(int width, int height, boolean primary) {
        TextureTarget target = primary ? pingTarget : pongTarget;
        try {
            if (target == null) {
                target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
                target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                if (primary) {
                    pingTarget = target;
                } else {
                    pongTarget = target;
                }
            } else if (target.width != width || target.height != height) {
                target.resize(width, height, Minecraft.ON_OSX);
                target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            }
            return target;
        } catch (RuntimeException error) {
            LOGGER.error("Unable to allocate blur framebuffer of size {}x{}", width, height, error);
            resourcesFailed = true;
            if (primary) {
                pingTarget = null;
            } else {
                pongTarget = null;
            }
            return null;
        }
    }

    private void blitRegion(RenderTarget mainTarget, TextureTarget destination, int srcLeft, int srcBottom, int srcRight, int srcTop) {
        GlStateManager._glBindFramebuffer(36008, mainTarget.frameBufferId);
        GlStateManager._glBindFramebuffer(36009, destination.frameBufferId);
        GlStateManager._glBlitFrameBuffer(srcLeft, srcBottom, srcRight, srcTop, 0, 0, destination.width, destination.height, 16384, 9729);
        GlStateManager._glBindFramebuffer(36008, 0);
        GlStateManager._glBindFramebuffer(36009, 0);
    }

    private void restoreMainTarget(RenderTarget mainTarget) {
        mainTarget.bindWrite(false);
        RenderSystem.viewport(0, 0, mainTarget.viewWidth, mainTarget.viewHeight);
        RenderSystem.depthFunc(515);
    }

    private @Nullable TextureTarget runBlurPasses(Minecraft minecraft, TextureTarget source, TextureTarget temp, float radius) {
        if (boxBlurEffect == null) {
            return null;
        }

        configureBlurEffect(minecraft, source, temp, new Matrix4f().setOrtho(0.0F, temp.width, 0.0F, temp.height, -1.0F, 1.0F), radius, 1.0F, 0.0F);
        temp.clear(false);
        temp.bindWrite(false);
        RenderSystem.viewport(0, 0, temp.width, temp.height);
        RenderSystem.depthFunc(519);
        boxBlurEffect.apply();
        drawFullscreenQuad(temp.width, temp.height);
        boxBlurEffect.clear();
        temp.unbindWrite();

        configureBlurEffect(minecraft, temp, source, new Matrix4f().setOrtho(0.0F, source.width, 0.0F, source.height, -1.0F, 1.0F), radius, 0.0F, 1.0F);
        source.clear(false);
        source.bindWrite(false);
        RenderSystem.viewport(0, 0, source.width, source.height);
        RenderSystem.depthFunc(519);
        boxBlurEffect.apply();
        drawFullscreenQuad(source.width, source.height);
        boxBlurEffect.clear();
        source.unbindWrite();
        temp.unbindRead();

        RenderSystem.depthFunc(515);
        return source;
    }

    private void configureBlurEffect(Minecraft minecraft, TextureTarget input, TextureTarget output, Matrix4f projection, float radius, float dirX, float dirY) {
        if (boxBlurEffect == null) {
            return;
        }

        boxBlurEffect.setSampler("DiffuseSampler", input::getColorTextureId);
        boxBlurEffect.safeGetUniform("ProjMat").set(projection);
        boxBlurEffect.safeGetUniform("InSize").set((float) input.width, (float) input.height);
        boxBlurEffect.safeGetUniform("OutSize").set((float) output.width, (float) output.height);
        boxBlurEffect.safeGetUniform("BlurDir").set(dirX, dirY);
        boxBlurEffect.safeGetUniform("Radius").set(radius);
        boxBlurEffect.safeGetUniform("RadiusMultiplier").set(1.0F);
        boxBlurEffect.safeGetUniform("Time").set(0.0F);
        boxBlurEffect.safeGetUniform("ScreenSize").set((float) minecraft.getWindow().getWidth(), (float) minecraft.getWindow().getHeight());
    }

    private void drawFullscreenQuad(int width, int height) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.addVertex(0.0F, 0.0F, 0.0F);
        builder.addVertex((float) width, 0.0F, 0.0F);
        builder.addVertex((float) width, (float) height, 0.0F);
        builder.addVertex(0.0F, (float) height, 0.0F);
        BufferUploader.draw(builder.buildOrThrow());
    }

    private void renderCompositeQuad(
        GuiGraphics graphics,
        TextureTarget texture,
        float x,
        float y,
        float width,
        float height,
        int areaLeftPx,
        int areaBottomPx,
        int areaWidthPx,
        int areaHeightPx,
        float uMin,
        float vMin,
        float uMax,
        float vMax,
        float tintR,
        float tintG,
        float tintB,
        float tintA,
        float cornerRadiusPx,
        float smoothRadiusPx,
        boolean useRounded,
        int fallbackTint) {
        if (compositeShader == null) {
            fallbackFill(graphics, x, y, width, height, fallbackTint);
            return;
        }

        compositeShader.setSampler("BlurSampler", texture);
        compositeShader.safeGetUniform("ModelViewMat").set(graphics.pose().last().pose());
        compositeShader.safeGetUniform("ProjMat").set(RenderSystem.getProjectionMatrix());
        compositeShader.safeGetUniform("ColorTint").set(tintR, tintG, tintB, tintA);
        compositeShader.safeGetUniform("UVMin").set(uMin, vMin);
        compositeShader.safeGetUniform("UVMax").set(uMax, vMax);
        compositeShader.safeGetUniform("AreaOrigin").set((float) areaLeftPx, (float) areaBottomPx);
        compositeShader.safeGetUniform("AreaSize").set((float) areaWidthPx, (float) areaHeightPx);
        compositeShader.safeGetUniform("CornerRadius").set(cornerRadiusPx);
        compositeShader.safeGetUniform("SmoothRadius").set(smoothRadiusPx);
        compositeShader.safeGetUniform("Rounded").set(useRounded ? 1 : 0);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        compositeShader.apply();

        Matrix4f poseMatrix = graphics.pose().last().pose();
        float minX = x;
        float minY = y;
        float maxX = x + width;
        float maxY = y + height;
        float z = 0.0F;

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(poseMatrix, minX, maxY, z).setUv(0.0F, 1.0F);
        builder.addVertex(poseMatrix, maxX, maxY, z).setUv(1.0F, 1.0F);
        builder.addVertex(poseMatrix, maxX, minY, z).setUv(1.0F, 0.0F);
        builder.addVertex(poseMatrix, minX, minY, z).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(Objects.requireNonNull(builder.build()));

        compositeShader.clear();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void resetResources() {
        RenderSystem.assertOnRenderThread();

        if (boxBlurEffect != null) {
            boxBlurEffect.close();
            boxBlurEffect = null;
        }

        if (pingTarget != null) {
            pingTarget.destroyBuffers();
            pingTarget = null;
        }

        if (pongTarget != null) {
            pongTarget.destroyBuffers();
            pongTarget = null;
        }

        if (compositeShader != null) {
            if (!compositeManagedExternally) {
                compositeShader.close();
            }
            compositeShader = null;
            compositeManagedExternally = false;
        }

        resourcesFailed = false;
    }

    public void setCompositeShader(@NotNull ShaderInstance shader, boolean managedExternally) {
        RenderCall apply = () -> {
            if (compositeShader != null && !compositeManagedExternally) {
                compositeShader.close();
            }
            compositeShader = shader;
            compositeManagedExternally = managedExternally;
            resourcesFailed = false;
        };

        if (RenderSystem.isOnRenderThread()) {
            apply.execute();
        } else {
            RenderSystem.recordRenderCall(apply);
        }
    }
}
