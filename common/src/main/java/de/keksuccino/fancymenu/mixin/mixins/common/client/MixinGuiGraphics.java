package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.RenderRotationUtil;
import de.keksuccino.fancymenu.util.rendering.RenderScaleUtil;
import de.keksuccino.fancymenu.util.rendering.RenderTranslationUtil;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.scissor.ScissorShape;
import de.keksuccino.fancymenu.util.rendering.scissor.ScissorShapes;
import de.keksuccino.fancymenu.customization.layout.editor.widget.LayoutEditorWidgetRenderContext;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(GuiGraphics.class)
public class MixinGuiGraphics {

    @Unique private boolean adjustingScissor_FancyMenu;
    /** The shape of the scissor {@code enableScissor} is about to push, handed from its arguments to the rectangle it creates. */
    @Unique @Nullable
    private ScissorShape pendingScissorShape_FancyMenu;

    @Inject(method = "<init>(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V", at = @At("TAIL"))
    private void after_init_FancyMenu(Minecraft minecraft, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, CallbackInfo info) {
        RenderScaleUtil.resetActiveRenderScale_FancyMenu();
        RenderTranslationUtil.resetActiveRenderTranslation_FancyMenu();
        RenderRotationUtil.resetActiveRenderRotation_FancyMenu();
        ScissorShapes.activate(null);
    }

    @Inject(method = "renderTooltipInternal", at = @At("HEAD"), cancellable = true)
    private void head_renderTooltipInternal_FancyMenu(Font $$0, List<ClientTooltipComponent> $$1, int $$2, int $$3, ClientTooltipPositioner $$4, CallbackInfo info) {
        if (RenderingUtils.isTooltipRenderingBlocked()) info.cancel();
    }

    /**
     * @reason Vanilla ignores the pose for scissors, which gives the wrong area under a rotated or tilted pose. There, the GPU scissor gets the bounds of the
     * transformed rectangle (see {@code before_enableScissor_FancyMenu}), and the transformed rectangle itself becomes the shape of the scissor,
     * which cuts everything rendered in it to the transformed rectangle (see {@link ScissorShapes}).
     */
    @ModifyExpressionValue(method = "enableScissor", at = @At(value = "NEW", target = "(IIII)Lnet/minecraft/client/gui/navigation/ScreenRectangle;"))
    private ScreenRectangle wrap_new_ScreenRectangle_in_enableScissor_FancyMenu(ScreenRectangle rectangle) {
        if (this.pendingScissorShape_FancyMenu != null) {
            ScissorShapes.setShape(rectangle, this.pendingScissorShape_FancyMenu);
            this.pendingScissorShape_FancyMenu = null;
        }
        return rectangle;
    }

    /** @reason Everything rendered from now on gets cut to the shape of the applied scissor, see {@link ScissorShapes}. */
    @Inject(method = "applyScissor", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableScissor(IIII)V"))
    private void before_enableScissor_in_applyScissor_FancyMenu(ScreenRectangle rectangle, CallbackInfo info) {
        ScissorShapes.activate(rectangle);
    }

    @Inject(method = "applyScissor", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;disableScissor()V"))
    private void before_disableScissor_in_applyScissor_FancyMenu(ScreenRectangle rectangle, CallbackInfo info) {
        ScissorShapes.activate(null);
    }

    @Inject(method = "enableScissor", at = @At("HEAD"), cancellable = true)
    private void before_enableScissor_FancyMenu(int x1, int y1, int x2, int y2, CallbackInfo info) {
        if (this.adjustingScissor_FancyMenu) {
            return;
        }
        // Under a rotated or tilted pose, the pose (which includes the scaling of PiP windows and editor widgets) decides where the scissor is
        if (ScissorShapes.isPoseRotated()) {
            Matrix4f pose = ((GuiGraphics)(Object)this).pose().last().pose();
            this.pendingScissorShape_FancyMenu = ScissorShapes.getTransformedShape(x1, y1, x2, y2, pose);
            int[] bounds = ScissorShapes.getTransformedBounds(x1, y1, x2, y2, pose);
            this.adjustingScissor_FancyMenu = true;
            try {
                ((GuiGraphics)(Object)this).enableScissor(bounds[0], bounds[1], bounds[0] + bounds[2], bounds[1] + bounds[3]);
            } finally {
                this.adjustingScissor_FancyMenu = false;
                this.pendingScissorShape_FancyMenu = null;
            }
            info.cancel();
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
