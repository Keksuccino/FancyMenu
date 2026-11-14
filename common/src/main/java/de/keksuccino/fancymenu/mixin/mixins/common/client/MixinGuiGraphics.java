package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.util.rendering.RenderRotationUtil;
import de.keksuccino.fancymenu.util.rendering.RenderScaleUtil;
import de.keksuccino.fancymenu.util.rendering.RenderTranslationUtil;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.scissor.ScissorShape;
import de.keksuccino.fancymenu.util.rendering.scissor.ScissorShapes;
import de.keksuccino.fancymenu.customization.layout.editor.widget.LayoutEditorWidgetRenderContext;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerDrawScale;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(GuiGraphics.class)
public class MixinGuiGraphics {

    @Inject(method = "<init>(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/render/state/GuiRenderState;II)V", at = @At("TAIL"))
    private void after_init_FancyMenu(Minecraft minecraft, GuiRenderState guiRenderState, int mouseX, int mouseY, CallbackInfo info) {
        RenderScaleUtil.resetActiveRenderScale_FancyMenu();
        RenderTranslationUtil.resetActiveRenderTranslation_FancyMenu();
        RenderRotationUtil.resetActiveRenderRotation_FancyMenu();
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void head_renderTooltip_FancyMenu(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, Identifier background, CallbackInfo info) {
        if (RenderingUtils.isTooltipRenderingBlocked()) info.cancel();
    }

    @ModifyArgs(method = "innerBlit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;submitBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSampler;IIIIFFFFI)V"))
    private void modify_innerBlitColor_FancyMenu(Args args) {
        args.set(11, RenderingUtils.applyShaderColor((int) args.get(11)));
    }

    /**
     * @reason Vanilla transforms only two corners of the scissor, which gives a wrong rectangle under a rotated pose,
     * and a rectangle with a negative size, which hides everything, under a flipping pose like a rotation by 180 degrees.<br>
     * Under a rotated pose, the GPU scissor gets the bounds of the rotated rectangle, and the rotated rectangle itself becomes the shape of the scissor,
     * which cuts everything rendered in it to the rotated rectangle (see {@link ScissorShapes}).
     */
    @WrapOperation(method = "enableScissor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/navigation/ScreenRectangle;transformAxisAligned(Lorg/joml/Matrix3x2fc;)Lnet/minecraft/client/gui/navigation/ScreenRectangle;"))
    private ScreenRectangle wrap_transformAxisAligned_in_enableScissor_FancyMenu(ScreenRectangle rectangle, Matrix3x2fc pose, Operation<ScreenRectangle> original) {
        ScissorShape shape = ScissorShapes.getTransformedShape(rectangle, pose);
        if (shape == null) {
            // The bounds of all four corners are right for every axis-aligned pose, flipping ones included
            return ((pose.m00() < 0.0F) || (pose.m11() < 0.0F)) ? rectangle.transformMaxBounds(pose) : original.call(rectangle, pose);
        }
        ScreenRectangle bounds = rectangle.transformMaxBounds(pose);
        ScissorShapes.setShape(bounds, shape);
        return bounds;
    }

    @ModifyArgs(method = "containsPointInScissor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics$ScissorStack;containsPoint(II)Z"))
    private void modify_containsPointInScissor_FancyMenu(Args args) {
        // A child of a container that scales what it draws checks the mouse it has in its own unscaled space, and what the GUI gets cut to is on the screen
        if (ElementContainerDrawScale.isActive()) {
            args.set(0, (int) Math.floor(ElementContainerDrawScale.toScreenX((int) args.get(0))));
            args.set(1, (int) Math.floor(ElementContainerDrawScale.toScreenY((int) args.get(1))));
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
        args.set(0, (int) Math.round(((int) args.get(0)) * scale + offsetX));
        args.set(1, (int) Math.round(((int) args.get(1)) * scale + offsetY));
    }

}
