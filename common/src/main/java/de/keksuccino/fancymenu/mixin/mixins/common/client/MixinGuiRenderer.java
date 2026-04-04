package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderPass;
import de.keksuccino.fancymenu.util.window.FancyWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public class MixinGuiRenderer {

    @Unique
    private final Projection preciseGuiProjectionFancyMenu = new Projection();

    @WrapOperation(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lnet/minecraft/client/renderer/Projection;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
    private GpuBufferSlice wrap_getBuffer_FancyMenu(ProjectionMatrixBuffer instance, Projection projection, Operation<GpuBufferSlice> original) {
        Window w = Minecraft.getInstance().getWindow();
        FancyWindow fancyWindow = ((FancyWindow)(Object)w);
        double precise = fancyWindow.getPreciseGuiScale_FancyMenu();
        if (precise > 0) {
            this.preciseGuiProjectionFancyMenu.setupOrtho(
                projection.zNear(),
                projection.zFar(),
                (float)w.getWidth() / (float)precise,
                (float)w.getHeight() / (float)precise,
                projection.invertY()
            );
            return original.call(instance, this.preciseGuiProjectionFancyMenu);
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

}
