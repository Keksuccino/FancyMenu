package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.RenderRotationUtil;
import de.keksuccino.fancymenu.util.rendering.RenderScaleUtil;
import de.keksuccino.fancymenu.util.rendering.RenderTranslationUtil;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.customization.layout.editor.widget.LayoutEditorWidgetRenderContext;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(GuiGraphics.class)
public class MixinGuiGraphics {

    @Unique private boolean adjustingScissor_FancyMenu;

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

    @Inject(method = "enableScissor", at = @At("HEAD"), cancellable = true)
    private void before_enableScissor_FancyMenu(int x1, int y1, int x2, int y2, CallbackInfo info) {
        if (this.adjustingScissor_FancyMenu) {
            return;
        }
        boolean pipActive = PiPWindowHandler.INSTANCE.isScreenRenderActive();
        boolean widgetActive = LayoutEditorWidgetRenderContext.isBodyRenderActive();
        if (!pipActive && !widgetActive) {
            return;
        }
        double scale = 1.0;
        double offsetX = 0.0;
        double offsetY = 0.0;
        if (pipActive) {
            scale = PiPWindowHandler.INSTANCE.getActiveScreenRenderScaleFactor();
            offsetX = PiPWindowHandler.INSTANCE.getActiveScreenRenderOffsetX();
            offsetY = PiPWindowHandler.INSTANCE.getActiveScreenRenderOffsetY();
        }
        if (widgetActive) {
            double widgetScale = LayoutEditorWidgetRenderContext.getActiveBodyRenderScaleFactor();
            double widgetOffsetX = LayoutEditorWidgetRenderContext.getActiveBodyRenderOffsetX();
            double widgetOffsetY = LayoutEditorWidgetRenderContext.getActiveBodyRenderOffsetY();
            if (pipActive) {
                offsetX = offsetX + (widgetOffsetX * scale);
                offsetY = offsetY + (widgetOffsetY * scale);
                scale = scale * widgetScale;
            } else {
                offsetX = widgetOffsetX;
                offsetY = widgetOffsetY;
                scale = widgetScale;
            }
        }
        int minX = (int) Math.floor(x1 * scale + offsetX);
        int minY = (int) Math.floor(y1 * scale + offsetY);
        int width = (int) Math.ceil((x2 - x1) * scale);
        int height = (int) Math.ceil((y2 - y1) * scale);
        this.adjustingScissor_FancyMenu = true;
        try {
            ((GuiGraphics)(Object)this).enableScissor(minX, minY, minX + width, minY + height);
        } finally {
            this.adjustingScissor_FancyMenu = false;
        }
        info.cancel();
    }

}
