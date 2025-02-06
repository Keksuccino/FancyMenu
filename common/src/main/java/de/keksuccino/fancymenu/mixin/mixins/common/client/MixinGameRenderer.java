package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Shadow @Final private RenderBuffers renderBuffers;

    @Inject(method = "render", at = @At("HEAD"))
    private void before_render_FancyMenu(float $$0, long $$1, boolean $$2, CallbackInfo info) {

        ScreenCustomization.onPreGameRenderTick();

    }

    /**
     * @reason This basically ports the 1.20.1 GuiGraphics to >=1.19.2.
     */
    @WrapOperation(method = "render", at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;", ordinal = 0), slice = @Slice(from = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Lighting;setupFor3DItems()V")))
    private PoseStack wrap_new_PoseStack_FancyMenu(Operation<PoseStack> original) {
        PoseStack pose = original.call();
        GuiGraphics.updateGraphicsAndGet(pose, this.renderBuffers.bufferSource());
        return pose;
    }

}
