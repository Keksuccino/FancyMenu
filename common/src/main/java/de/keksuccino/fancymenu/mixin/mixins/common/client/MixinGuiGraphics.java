package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.RenderRotationUtil;
import de.keksuccino.fancymenu.util.rendering.RenderScaleUtil;
import de.keksuccino.fancymenu.util.rendering.RenderTranslationUtil;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(GuiGraphics.class)
public class MixinGuiGraphics {

    @Inject(method = "<init>(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V", at = @At("TAIL"))
    private void after_init_FancyMenu(Minecraft minecraft, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, CallbackInfo info) {
        RenderScaleUtil.resetActiveRenderScale_FancyMenu();
        RenderTranslationUtil.resetActiveRenderTranslation_FancyMenu();
        RenderRotationUtil.resetActiveRenderRotation_FancyMenu();
    }

    @Inject(method = "renderTooltipInternal", at = @At("HEAD"), cancellable = true)
    private void head_renderTooltipInternal_FancyMenu(Font $$0, List<ClientTooltipComponent> $$1, int $$2, int $$3, ClientTooltipPositioner $$4, CallbackInfo info) {
        if (RenderingUtils.isTooltipRenderingBlocked()) info.cancel();
    }

    @ModifyArgs(method = "enableScissor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/navigation/ScreenRectangle;<init>(IIII)V"))
    private void modify_enableScissor_FancyMenu(Args args) {
        if (!PiPWindowHandler.INSTANCE.isScreenRenderActive()) {
            return;
        }
        int offsetX = PiPWindowHandler.INSTANCE.getActiveScreenRenderOffsetX();
        int offsetY = PiPWindowHandler.INSTANCE.getActiveScreenRenderOffsetY();
        double scale = PiPWindowHandler.INSTANCE.getActiveScreenRenderScaleFactor();
        int minX = (int) Math.floor(((int) args.get(0)) * scale + offsetX);
        int minY = (int) Math.floor(((int) args.get(1)) * scale + offsetY);
        int width = (int) Math.ceil(((int) args.get(2)) * scale);
        int height = (int) Math.ceil(((int) args.get(3)) * scale);
        args.set(0, minX);
        args.set(1, minY);
        args.set(2, width);
        args.set(3, height);
    }

    @ModifyArgs(method = "containsPointInScissor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics$ScissorStack;containsPoint(II)Z"))
    private void modify_containsPointInScissor_FancyMenu(Args args) {
        if (!PiPWindowHandler.INSTANCE.isScreenRenderActive()) {
            return;
        }
        int offsetX = PiPWindowHandler.INSTANCE.getActiveScreenRenderOffsetX();
        int offsetY = PiPWindowHandler.INSTANCE.getActiveScreenRenderOffsetY();
        double scale = PiPWindowHandler.INSTANCE.getActiveScreenRenderScaleFactor();
        args.set(0, (int) Math.round(((int) args.get(0)) * scale + offsetX));
        args.set(1, (int) Math.round(((int) args.get(1)) * scale + offsetY));
    }

}
