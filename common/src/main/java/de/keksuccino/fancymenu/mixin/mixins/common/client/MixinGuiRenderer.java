package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderPass;
import de.keksuccino.fancymenu.customization.panorama.FancyMenuPanoramaPictureInPictureRenderer;
import de.keksuccino.fancymenu.customization.panorama.FancyMenuPanoramaRenderState;
import de.keksuccino.fancymenu.util.window.FancyWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiRenderer.class)
public class MixinGuiRenderer {

    @Unique private final Projection preciseGuiProjection_FancyMenu = new Projection();
    @Unique private FancyMenuPanoramaPictureInPictureRenderer panoramaPictureInPictureRenderer_FancyMenu;

    @Shadow @Final private MultiBufferSource.BufferSource bufferSource;
    @Shadow @Final private GuiRenderState renderState;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void after_init_FancyMenu(GuiRenderState renderState, MultiBufferSource.BufferSource bufferSource, SubmitNodeCollector submitNodeCollector, FeatureRenderDispatcher featureRenderDispatcher, List<PictureInPictureRenderer<?>> pictureInPictureRenderers, CallbackInfo info) {
        this.panoramaPictureInPictureRenderer_FancyMenu = new FancyMenuPanoramaPictureInPictureRenderer(this.bufferSource);
    }

    /** @reason Prepare FancyMenu panoramas as their own GUI picture-in-picture render state. */
    @Inject(method = "preparePictureInPictureState", at = @At("HEAD"), cancellable = true)
    private void before_preparePictureInPictureState_FancyMenu(PictureInPictureRenderState picturesInPictureState, int guiScale, CallbackInfo info) {
        if ((picturesInPictureState instanceof FancyMenuPanoramaRenderState panoramaRenderState) && (this.panoramaPictureInPictureRenderer_FancyMenu != null)) {
            this.panoramaPictureInPictureRenderer_FancyMenu.prepare(panoramaRenderState, this.renderState, guiScale);
            info.cancel();
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

}