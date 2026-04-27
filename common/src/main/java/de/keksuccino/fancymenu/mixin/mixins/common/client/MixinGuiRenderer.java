package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.keksuccino.fancymenu.customization.panorama.FancyMenuPanoramaPictureInPictureRenderer;
import de.keksuccino.fancymenu.customization.panorama.FancyMenuPanoramaRenderState;
import de.keksuccino.fancymenu.util.rendering.GuiBlurRenderer;
import de.keksuccino.fancymenu.util.window.FancyWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

@Mixin(GuiRenderer.class)
public abstract class MixinGuiRenderer {

    @Unique private final Projection preciseGuiProjection_FancyMenu = new Projection();
    @Unique private final List<RenderPhaseBlurAction_FancyMenu> renderPhaseBlurActions_FancyMenu = new ArrayList<>();
    @Unique private FancyMenuPanoramaPictureInPictureRenderer panoramaPictureInPictureRenderer_FancyMenu;
    @Unique private GuiRenderState.TraverseRange activeTraverseRange_FancyMenu = GuiRenderState.TraverseRange.ALL;
    @Unique private int nextRenderPhaseBlurActionOrder_FancyMenu;

    @Shadow @Final private MultiBufferSource.BufferSource bufferSource;
    @Shadow @Final private GuiRenderState renderState;
    @Shadow @Final private List<?> draws;
    @Shadow @Final private List<?> meshesToDraw;
    @Shadow @Nullable private ScreenRectangle previousScissorArea;
    @Shadow @Nullable private RenderPipeline previousPipeline;
    @Shadow @Nullable private TextureSetup previousTextureSetup;
    @Shadow @Nullable private BufferBuilder bufferBuilder;
    @Shadow private int firstDrawIndexAfterBlur;

    @Shadow
    private void recordMesh(BufferBuilder bufferBuilder, RenderPipeline pipeline, TextureSetup textureSetup, @Nullable ScreenRectangle scissorArea) {
        throw new AssertionError();
    }

    @Shadow
    private void enableScissor(ScreenRectangle rectangle, RenderPass renderPass) {
        throw new AssertionError();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void after_init_FancyMenu(CallbackInfo info) {
        this.panoramaPictureInPictureRenderer_FancyMenu = new FancyMenuPanoramaPictureInPictureRenderer(this.bufferSource);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void before_render_FancyMenu(GpuBufferSlice fogBuffer, CallbackInfo info) {
        this.clearRenderPhaseBlurActions_FancyMenu();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void after_render_FancyMenu(GpuBufferSlice fogBuffer, CallbackInfo info) {
        this.clearRenderPhaseBlurActions_FancyMenu();
    }

	/** @reason Prepare FancyMenu panoramas after vanilla picture-in-picture extraction on both loaders. */
	@Inject(method = "preparePictureInPicture", at = @At("RETURN"))
	private void after_preparePictureInPicture_FancyMenu(CallbackInfo info) {
		if (this.panoramaPictureInPictureRenderer_FancyMenu == null) {
			return;
		}

		int guiScale = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.guiScale;
		this.renderState.forEachPictureInPicture(picturesInPictureState -> {
			if (picturesInPictureState instanceof FancyMenuPanoramaRenderState panoramaRenderState) {
				this.panoramaPictureInPictureRenderer_FancyMenu.prepare(panoramaRenderState, this.renderState, guiScale);
			}
		});
	}

    @Inject(method = "addElementsToMeshes", at = @At("HEAD"))
    private void before_addElementsToMeshes_FancyMenu(GuiRenderState.TraverseRange range, CallbackInfo info) {
        this.activeTraverseRange_FancyMenu = range;
    }

    @Inject(method = "addElementsToMeshes", at = @At("RETURN"))
    private void after_addElementsToMeshes_FancyMenu(GuiRenderState.TraverseRange range, CallbackInfo info) {
        this.activeTraverseRange_FancyMenu = GuiRenderState.TraverseRange.ALL;
    }

    @Inject(method = "addElementToMesh", at = @At("HEAD"), cancellable = true)
    private void before_addElementToMesh_FancyMenu(GuiElementRenderState elementState, CallbackInfo info) {
        if (!(elementState instanceof GuiBlurRenderer.GuiBlurRenderState blurRenderState)) {
            return;
        }

        if (this.bufferBuilder != null) {
            this.recordMesh(this.bufferBuilder, this.previousPipeline, this.previousTextureSetup, this.previousScissorArea);
        }

        this.bufferBuilder = null;
        this.previousPipeline = null;
        this.previousTextureSetup = null;
        this.previousScissorArea = null;
        this.renderPhaseBlurActions_FancyMenu.add(new RenderPhaseBlurAction_FancyMenu(
                this.meshesToDraw.size(),
                this.nextRenderPhaseBlurActionOrder_FancyMenu++,
                this.activeTraverseRange_FancyMenu,
                blurRenderState.queuedBlurArea()
        ));
        info.cancel();
    }

    @Inject(method = "draw", at = @At("HEAD"))
    private void before_draw_FancyMenu(GpuBufferSlice fogBuffer, CallbackInfo info) {
        if (!this.draws.isEmpty() || this.renderPhaseBlurActions_FancyMenu.isEmpty()) {
            return;
        }

        this.renderPhaseBlurActions_FancyMenu.stream()
                .sorted(Comparator.comparingInt(RenderPhaseBlurAction_FancyMenu::order_FancyMenu))
                .forEach(action -> GuiBlurRenderer.executeQueuedBlurArea_FancyMenu(action.queuedBlurArea_FancyMenu()));
    }

    @Inject(method = "executeDrawRange", at = @At("HEAD"), cancellable = true)
    private void before_executeDrawRange_FancyMenu(Supplier<String> label, RenderTarget mainRenderTarget, GpuBufferSlice fogBuffer, GpuBufferSlice dynamicTransforms, GpuBuffer indexBuffer, VertexFormat.IndexType indexType, int startIndex, int endIndex, CallbackInfo info) {
        GuiRenderState.TraverseRange executeRange = this.resolveExecuteRange_FancyMenu(startIndex);
        List<RenderPhaseBlurAction_FancyMenu> actions = this.renderPhaseBlurActions_FancyMenu.stream()
                .filter(action -> action.range_FancyMenu() == executeRange)
                .filter(action -> action.drawIndex_FancyMenu() >= startIndex && action.drawIndex_FancyMenu() <= endIndex)
                .sorted(Comparator.comparingInt(RenderPhaseBlurAction_FancyMenu::drawIndex_FancyMenu).thenComparingInt(RenderPhaseBlurAction_FancyMenu::order_FancyMenu))
                .toList();
        if (actions.isEmpty()) {
            return;
        }

        info.cancel();
        int currentIndex = startIndex;
        for (RenderPhaseBlurAction_FancyMenu action : actions) {
            int actionIndex = Math.max(startIndex, Math.min(endIndex, action.drawIndex_FancyMenu()));
            this.executePlainDrawRange_FancyMenu(label, mainRenderTarget, fogBuffer, dynamicTransforms, indexBuffer, indexType, currentIndex, actionIndex);
            GuiBlurRenderer.executeQueuedBlurArea_FancyMenu(action.queuedBlurArea_FancyMenu());
            currentIndex = actionIndex;
        }

        this.executePlainDrawRange_FancyMenu(label, mainRenderTarget, fogBuffer, dynamicTransforms, indexBuffer, indexType, currentIndex, endIndex);
    }

    @WrapOperation(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lnet/minecraft/client/renderer/Projection;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
    private GpuBufferSlice wrap_getBuffer_FancyMenu(ProjectionMatrixBuffer instance, Projection projection, Operation<GpuBufferSlice> original) {
        Window w = Minecraft.getInstance().getWindow();
        FancyWindow fancyWindow = ((FancyWindow)(Object)w);
        double precise = fancyWindow.getPreciseGuiScale_FancyMenu();
        if (precise > 0) {
            this.preciseGuiProjection_FancyMenu.setupOrtho(projection.zNear(), projection.zFar(), (float)w.getWidth() / (float)precise, (float)w.getHeight() / (float)precise, projection.invertY());
            return original.call(instance, this.preciseGuiProjection_FancyMenu);
        }
        return original.call(instance, projection);
    }

    @Inject(method = "enableScissor", at = @At("HEAD"), cancellable = true)
    private void before_enableScissor_FancyMenu(ScreenRectangle screenRectangle, RenderPass renderPass, CallbackInfo info) {
        Window w = Minecraft.getInstance().getWindow();
        FancyWindow fancyWindow = ((FancyWindow)(Object)w);
        double precise = fancyWindow.getPreciseGuiScale_FancyMenu();
        if (precise > 0) {
            info.cancel();
            int h = w.getHeight();
            double d1 = ((double)screenRectangle.left() * precise);
            double d2 = ((double)h - screenRectangle.bottom() * precise);
            double d3 = ((double)screenRectangle.width() * precise);
            double d4 = ((double)screenRectangle.height() * precise);
            renderPass.enableScissor((int)d1, (int)d2, Math.max(0, (int)d3), Math.max(0, (int)d4));
        }
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void after_close_FancyMenu(CallbackInfo info) {
        if (this.panoramaPictureInPictureRenderer_FancyMenu != null) {
            this.panoramaPictureInPictureRenderer_FancyMenu.close();
            this.panoramaPictureInPictureRenderer_FancyMenu = null;
        }
    }

    @Unique
    private void clearRenderPhaseBlurActions_FancyMenu() {
        this.renderPhaseBlurActions_FancyMenu.clear();
        this.nextRenderPhaseBlurActionOrder_FancyMenu = 0;
        this.activeTraverseRange_FancyMenu = GuiRenderState.TraverseRange.ALL;
    }

    @Unique
    private GuiRenderState.TraverseRange resolveExecuteRange_FancyMenu(int startIndex) {
        return startIndex >= this.firstDrawIndexAfterBlur ? GuiRenderState.TraverseRange.AFTER_BLUR : GuiRenderState.TraverseRange.BEFORE_BLUR;
    }

    @Unique
    private void executePlainDrawRange_FancyMenu(Supplier<String> label, RenderTarget mainRenderTarget, GpuBufferSlice fogBuffer, GpuBufferSlice dynamicTransforms, GpuBuffer indexBuffer, VertexFormat.IndexType indexType, int startIndex, int endIndex) {
        if (startIndex >= endIndex) {
            return;
        }

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        label,
                        mainRenderTarget.getColorTextureView(),
                        OptionalInt.empty(),
                        mainRenderTarget.useDepth ? mainRenderTarget.getDepthTextureView() : null,
                        OptionalDouble.empty()
                )) {
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("Fog", fogBuffer);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);

            for (int i = startIndex; i < endIndex; i++) {
                this.executeDraw_FancyMenu(this.draws.get(i), renderPass, indexBuffer, indexType);
            }
        }
    }

    @Unique
    private void executeDraw_FancyMenu(Object drawObject, RenderPass renderPass, GpuBuffer indexBuffer, VertexFormat.IndexType indexType) {
        IMixinGuiRendererDraw draw = (IMixinGuiRendererDraw) drawObject;
        TextureSetup textureSetup = draw.get_textureSetup_FancyMenu();
        renderPass.setPipeline(draw.get_pipeline_FancyMenu());
        renderPass.setVertexBuffer(0, draw.get_vertexBuffer_FancyMenu());

        ScreenRectangle scissorArea = draw.get_scissorArea_FancyMenu();
        if (scissorArea != null) {
            this.enableScissor(scissorArea, renderPass);
        } else {
            renderPass.disableScissor();
        }

        if (textureSetup.texure0() != null) {
            renderPass.bindTexture("Sampler0", textureSetup.texure0(), textureSetup.sampler0());
        }

        if (textureSetup.texure1() != null) {
            renderPass.bindTexture("Sampler1", textureSetup.texure1(), textureSetup.sampler1());
        }

        if (textureSetup.texure2() != null) {
            renderPass.bindTexture("Sampler2", textureSetup.texure2(), textureSetup.sampler2());
        }

        renderPass.setIndexBuffer(indexBuffer, indexType);
        renderPass.drawIndexed(draw.get_baseVertex_FancyMenu(), 0, draw.get_indexCount_FancyMenu(), 1);
    }

    @Unique
    private record RenderPhaseBlurAction_FancyMenu(int drawIndex_FancyMenu, int order_FancyMenu, GuiRenderState.TraverseRange range_FancyMenu, GuiBlurRenderer.QueuedBlurArea queuedBlurArea_FancyMenu) {
    }

}
