package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
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
import de.keksuccino.fancymenu.util.rendering.GuiRenderPhaseAction;
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
import java.util.function.Supplier;

@Mixin(GuiRenderer.class)
public abstract class MixinGuiRenderer {

    @Shadow @Final private MultiBufferSource.BufferSource bufferSource;
    @Shadow @Final private GuiRenderState renderState;
    @Shadow @Final private List<?> draws;
    @Shadow @Final private List<?> meshesToDraw;
    @Shadow @Nullable private ScreenRectangle previousScissorArea;
    @Shadow @Nullable private RenderPipeline previousPipeline;
    @Shadow @Nullable private TextureSetup previousTextureSetup;
    @Shadow @Nullable private BufferBuilder bufferBuilder;
    @Shadow private int firstDrawIndexAfterBlur;

    @Unique private final Projection preciseGuiProjection_FancyMenu = new Projection();
    @Unique private final List<RenderPhaseAction_FancyMenu> renderPhaseActions_FancyMenu = new ArrayList<>();
    @Unique private FancyMenuPanoramaPictureInPictureRenderer panoramaPictureInPictureRenderer_FancyMenu;
    @Unique private GuiRenderState.TraverseRange activeTraverseRange_FancyMenu = GuiRenderState.TraverseRange.ALL;
    @Unique private int nextRenderPhaseActionOrder_FancyMenu;

    @Shadow
    private void recordMesh(BufferBuilder bufferBuilder, RenderPipeline pipeline, TextureSetup textureSetup, @Nullable ScreenRectangle scissorArea) {
        throw new AssertionError();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void after_init_FancyMenu(CallbackInfo info) {
        this.panoramaPictureInPictureRenderer_FancyMenu = new FancyMenuPanoramaPictureInPictureRenderer(this.bufferSource);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void before_render_FancyMenu(GpuBufferSlice fogBuffer, CallbackInfo info) {
        this.clearRenderPhaseActions_FancyMenu();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void after_render_FancyMenu(GpuBufferSlice fogBuffer, CallbackInfo info) {
        this.clearRenderPhaseActions_FancyMenu();
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
        if (!(elementState instanceof GuiRenderPhaseAction renderPhaseAction)) {
            return;
        }

        if (this.bufferBuilder != null) {
            this.recordMesh(this.bufferBuilder, this.previousPipeline, this.previousTextureSetup, this.previousScissorArea);
        }

        this.bufferBuilder = null;
        this.previousPipeline = null;
        this.previousTextureSetup = null;
        this.previousScissorArea = null;
        // In 26.1.2 recordDraws creates one draw per mesh, so the current mesh count is the future draw boundary.
        this.renderPhaseActions_FancyMenu.add(new RenderPhaseAction_FancyMenu(this.meshesToDraw.size(), this.nextRenderPhaseActionOrder_FancyMenu++, this.activeTraverseRange_FancyMenu, renderPhaseAction));
        info.cancel();
    }

    /** @reason Preserve render-phase actions even when vanilla skips an empty before/after draw range. */
    @WrapMethod(method = "draw")
    private void wrap_draw_FancyMenu(GpuBufferSlice fogBuffer, Operation<Void> original) {
        boolean vanillaCallsBeforeRange = !this.draws.isEmpty() && this.firstDrawIndexAfterBlur > 0;
        boolean vanillaProcessesBlur = !this.draws.isEmpty() && this.draws.size() > this.firstDrawIndexAfterBlur;
        if (!vanillaCallsBeforeRange) {
            this.executePendingRenderPhaseActions_FancyMenu(GuiRenderState.TraverseRange.BEFORE_BLUR);
        }

        original.call(fogBuffer);

        if (this.hasPendingRenderPhaseActions_FancyMenu(GuiRenderState.TraverseRange.AFTER_BLUR)) {
            if (!vanillaProcessesBlur) {
                Minecraft minecraft = Minecraft.getInstance();
                RenderTarget mainRenderTarget = minecraft.getMainRenderTarget();
                RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mainRenderTarget.getDepthTexture(), 1.0);
                minecraft.gameRenderer.processBlurEffect();
            }
            this.executePendingRenderPhaseActions_FancyMenu(GuiRenderState.TraverseRange.AFTER_BLUR);
        }
    }

    /** @reason Execute ordered actions between loader-native draw slices without duplicating loader-specific draw behavior. */
    @WrapMethod(method = "executeDrawRange")
    private void wrap_executeDrawRange_FancyMenu(Supplier<String> label, RenderTarget mainRenderTarget, GpuBufferSlice fogBuffer, GpuBufferSlice dynamicTransforms, GpuBuffer indexBuffer, VertexFormat.IndexType indexType, int startIndex, int endIndex, Operation<Void> original) {
        GuiRenderState.TraverseRange executeRange = this.resolveExecuteRange_FancyMenu(startIndex);
        List<RenderPhaseAction_FancyMenu> actions = this.renderPhaseActions_FancyMenu.stream()
                .filter(action -> action.range_FancyMenu() == executeRange)
                .filter(action -> action.drawIndex_FancyMenu() >= startIndex && action.drawIndex_FancyMenu() <= endIndex)
                .sorted(Comparator.comparingInt(RenderPhaseAction_FancyMenu::drawIndex_FancyMenu).thenComparingInt(RenderPhaseAction_FancyMenu::order_FancyMenu))
                .toList();
        if (actions.isEmpty()) {
            original.call(label, mainRenderTarget, fogBuffer, dynamicTransforms, indexBuffer, indexType, startIndex, endIndex);
            return;
        }

        int currentIndex = startIndex;
        for (RenderPhaseAction_FancyMenu action : actions) {
            int actionIndex = Math.max(startIndex, Math.min(endIndex, action.drawIndex_FancyMenu()));
            if (currentIndex < actionIndex) {
                original.call(label, mainRenderTarget, fogBuffer, dynamicTransforms, indexBuffer, indexType, currentIndex, actionIndex);
            }
            action.renderPhaseAction_FancyMenu().executeRender_FancyMenu();
            this.renderPhaseActions_FancyMenu.remove(action);
            currentIndex = actionIndex;
        }

        if (currentIndex < endIndex) {
            original.call(label, mainRenderTarget, fogBuffer, dynamicTransforms, indexBuffer, indexType, currentIndex, endIndex);
        }
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
    private void clearRenderPhaseActions_FancyMenu() {
        this.renderPhaseActions_FancyMenu.clear();
        this.nextRenderPhaseActionOrder_FancyMenu = 0;
        this.activeTraverseRange_FancyMenu = GuiRenderState.TraverseRange.ALL;
    }

    @Unique
    private GuiRenderState.TraverseRange resolveExecuteRange_FancyMenu(int startIndex) {
        return startIndex >= this.firstDrawIndexAfterBlur ? GuiRenderState.TraverseRange.AFTER_BLUR : GuiRenderState.TraverseRange.BEFORE_BLUR;
    }

    @Unique
    private boolean hasPendingRenderPhaseActions_FancyMenu(GuiRenderState.TraverseRange range) {
        return this.renderPhaseActions_FancyMenu.stream().anyMatch(action -> action.range_FancyMenu() == range);
    }

    @Unique
    private void executePendingRenderPhaseActions_FancyMenu(GuiRenderState.TraverseRange range) {
        List<RenderPhaseAction_FancyMenu> actions = this.renderPhaseActions_FancyMenu.stream()
                .filter(action -> action.range_FancyMenu() == range)
                .sorted(Comparator.comparingInt(RenderPhaseAction_FancyMenu::drawIndex_FancyMenu).thenComparingInt(RenderPhaseAction_FancyMenu::order_FancyMenu))
                .toList();
        for (RenderPhaseAction_FancyMenu action : actions) {
            action.renderPhaseAction_FancyMenu().executeRender_FancyMenu();
            this.renderPhaseActions_FancyMenu.remove(action);
        }
    }

    @Unique
    private record RenderPhaseAction_FancyMenu(int drawIndex_FancyMenu, int order_FancyMenu, GuiRenderState.TraverseRange range_FancyMenu, GuiRenderPhaseAction renderPhaseAction_FancyMenu) {
    }

}
