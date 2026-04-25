package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.opengl.GlRenderPass;
import com.mojang.blaze3d.systems.RenderPass;
import de.keksuccino.fancymenu.util.rendering.GuiBlurRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlRenderPass.class)
public class MixinGlRenderPass {

    @Inject(method = "draw(II)V", at = @At("HEAD"))
    private void before_draw_FancyMenu(int firstVertex, int vertexCount, CallbackInfo info) {
        GuiBlurRenderer.PostPassScissor scissor = GuiBlurRenderer.getActivePostPassScissor_FancyMenu();
        if (scissor != null) {
            ((RenderPass) (Object) this).enableScissor(scissor.x(), scissor.y(), scissor.width(), scissor.height());
        }
    }

}
