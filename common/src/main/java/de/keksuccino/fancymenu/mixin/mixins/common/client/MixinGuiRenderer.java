package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.keksuccino.fancymenu.util.rendering.GuiRenderPhaseAction;
import de.keksuccino.fancymenu.util.rendering.GuiRenderPhaseQueue;
import de.keksuccino.fancymenu.util.window.FancyWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Supplier;

@Mixin(GuiRenderer.class)
public class MixinGuiRenderer {

    @Shadow @Final private List<?> draws;
    @Shadow @Final private List<?> meshesToDraw;
    @Shadow private int firstDrawIndexAfterBlur;
    @Shadow private ScreenRectangle previousScissorArea;
    @Shadow private RenderPipeline previousPipeline;
    @Shadow private TextureSetup previousTextureSetup;
    @Shadow private BufferBuilder bufferBuilder;

    @Unique private final GuiRenderPhaseQueue<GuiRenderState.TraverseRange, GuiRenderPhaseAction> renderPhaseActions_FancyMenu = new GuiRenderPhaseQueue<>();
    @Unique private GuiRenderState.TraverseRange activeTraverseRange_FancyMenu = GuiRenderState.TraverseRange.ALL;

    @Shadow
    private void recordMesh(BufferBuilder bufferBuilder, RenderPipeline pipeline, TextureSetup textureSetup, ScreenRectangle scissorArea) {
        throw new AssertionError();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void before_render_FancyMenu(GpuBufferSlice bufferSlice, CallbackInfo info) {
        this.clearRenderPhaseActions_FancyMenu();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void after_render_FancyMenu(GpuBufferSlice bufferSlice, CallbackInfo info) {
        this.clearRenderPhaseActions_FancyMenu();
    }

    @Inject(method = "addElementsToMeshes", at = @At("HEAD"))
    private void before_addElementsToMeshes_FancyMenu(GuiRenderState.TraverseRange traverseRange, CallbackInfo info) {
        this.activeTraverseRange_FancyMenu = traverseRange;
    }

    @Inject(method = "addElementToMesh", at = @At("HEAD"), cancellable = true)
    private void before_addElementToMesh_FancyMenu(GuiElementRenderState elementState, CallbackInfo info) {
        if (!(elementState instanceof GuiRenderPhaseAction renderPhaseAction)) {
            return;
        }

        // A phase action owns the boundary after every vertex submitted before it. Leaving the active builder open
        // would merge later GUI vertices into the earlier draw and make the raw render happen at the wrong layer.
        if (this.bufferBuilder != null) {
            this.recordMesh(this.bufferBuilder, this.previousPipeline, this.previousTextureSetup, this.previousScissorArea);
        }
        this.bufferBuilder = null;
        this.previousPipeline = null;
        this.previousTextureSetup = null;
        this.previousScissorArea = null;
        this.renderPhaseActions_FancyMenu.add(this.meshesToDraw.size(), this.activeTraverseRange_FancyMenu, renderPhaseAction);
        info.cancel();
    }

    @Inject(method = "draw", at = @At("HEAD"))
    private void before_draw_FancyMenu(GpuBufferSlice bufferSlice, CallbackInfo info) {
        if (this.draws.isEmpty()) {
            this.executeActions_FancyMenu(this.renderPhaseActions_FancyMenu.drainAll());
        }
    }

    @Inject(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;processBlurEffect()V"))
    private void before_processBlurEffect_FancyMenu(GpuBufferSlice bufferSlice, CallbackInfo info) {
        this.executeActions_FancyMenu(this.renderPhaseActions_FancyMenu.drainPhase(GuiRenderState.TraverseRange.BEFORE_BLUR));
    }

    @WrapOperation(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V"))
    private void wrap_executeDrawRange_FancyMenu(GuiRenderer instance, Supplier<String> label, RenderTarget renderTarget, GpuBufferSlice fogUniforms, GpuBufferSlice dynamicTransforms, GpuBuffer buffer, VertexFormat.IndexType indexType, int startIndex, int endIndex, Operation<Void> original) {
        GuiRenderState.TraverseRange phase = startIndex < this.firstDrawIndexAfterBlur ? GuiRenderState.TraverseRange.BEFORE_BLUR : GuiRenderState.TraverseRange.AFTER_BLUR;
        List<GuiRenderPhaseQueue.Entry<GuiRenderState.TraverseRange, GuiRenderPhaseAction>> actions = this.renderPhaseActions_FancyMenu.drainRange(phase, startIndex, endIndex);
        if (actions.isEmpty()) {
            original.call(instance, label, renderTarget, fogUniforms, dynamicTransforms, buffer, indexType, startIndex, endIndex);
            return;
        }

        int currentIndex = startIndex;
        for (GuiRenderPhaseQueue.Entry<GuiRenderState.TraverseRange, GuiRenderPhaseAction> action : actions) {
            int actionIndex = Math.max(startIndex, Math.min(endIndex, action.drawIndex()));
            if (currentIndex < actionIndex) {
                original.call(instance, label, renderTarget, fogUniforms, dynamicTransforms, buffer, indexType, currentIndex, actionIndex);
            }
            action.action().executeRender_FancyMenu();
            currentIndex = actionIndex;
        }
        if (currentIndex < endIndex) {
            original.call(instance, label, renderTarget, fogUniforms, dynamicTransforms, buffer, indexType, currentIndex, endIndex);
        }
    }

    @Inject(method = "draw", at = @At("TAIL"))
    private void after_draw_FancyMenu(GpuBufferSlice bufferSlice, CallbackInfo info) {
        // Empty vanilla ranges do not call executeDrawRange. Drain them here so action-only strata are not lost.
        this.executeActions_FancyMenu(this.renderPhaseActions_FancyMenu.drainAll());
    }

    @WrapOperation(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/CachedOrthoProjectionMatrixBuffer;getBuffer(FF)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
    private GpuBufferSlice wrap_getBuffer_FancyMenu(CachedOrthoProjectionMatrixBuffer instance, float f1, float f2, Operation<GpuBufferSlice> original) {
        Window w = Minecraft.getInstance().getWindow();
        FancyWindow fancyWindow = ((FancyWindow)(Object)w);
        double precise = fancyWindow.getPreciseGuiScale_FancyMenu();
        if (precise > 0) {
            return original.call(instance, (float)w.getWidth() / (float)precise, (float)w.getHeight() / (float)precise);
        }
        return original.call(instance, f1, f2);
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

    @Unique
    private void executeActions_FancyMenu(List<GuiRenderPhaseQueue.Entry<GuiRenderState.TraverseRange, GuiRenderPhaseAction>> actions) {
        actions.forEach(action -> action.action().executeRender_FancyMenu());
    }

    @Unique
    private void clearRenderPhaseActions_FancyMenu() {
        this.renderPhaseActions_FancyMenu.clear();
        this.activeTraverseRange_FancyMenu = GuiRenderState.TraverseRange.ALL;
    }

}
