package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.RenderRotationUtil;
import de.keksuccino.fancymenu.util.rendering.RenderScaleUtil;
import de.keksuccino.fancymenu.util.rendering.RenderTranslationUtil;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.customization.layout.editor.widget.LayoutEditorWidgetRenderContext;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(GuiGraphicsExtractor.class)
public class MixinGuiGraphicsExtractor {

    @Inject(method = "<init>(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;II)V", at = @At("TAIL"))
    private void after_init_FancyMenu(Minecraft minecraft, GuiRenderState guiRenderState, int mouseX, int mouseY, CallbackInfo info) {
        RenderScaleUtil.resetActiveRenderScale_FancyMenu();
        RenderTranslationUtil.resetActiveRenderTranslation_FancyMenu();
        RenderRotationUtil.resetActiveRenderRotation_FancyMenu();
    }

    @Inject(method = "setTooltipForNextFrameInternal", at = @At("HEAD"), cancellable = true)
    private void head_setTooltipForNextFrameInternal_FancyMenu(Font font, List<ClientTooltipComponent> lines, int xo, int yo, ClientTooltipPositioner positioner, @Nullable Identifier style, boolean replaceExisting, CallbackInfo info) {
        if (RenderingUtils.isTooltipRenderingBlocked()) info.cancel();
    }

    @Inject(method = "tooltip", at = @At("HEAD"), cancellable = true)
    private void head_tooltip_FancyMenu(Font font, List<ClientTooltipComponent> lines, int xo, int yo, ClientTooltipPositioner positioner, @Nullable Identifier style, CallbackInfo info) {
        if (RenderingUtils.isTooltipRenderingBlocked()) info.cancel();
    }

    @ModifyArg(
        method = "innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSampler;IIIIFFFFI)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/BlitRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2f;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;)V"),
        index = 11
    )
    private int modify_innerBlitColor_FancyMenu(int color) {
        return RenderingUtils.applyShaderColor(color);
    }

    @ModifyArg(
        method = "innerTiledBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSampler;IIIIIIFFFFI)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/TiledBlitRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2f;IIIIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;)V"),
        index = 13
    )
    private int modify_innerTiledBlitColor_FancyMenu(int color) {
        return RenderingUtils.applyShaderColor(color);
    }

    @ModifyArgs(method = "containsPointInScissor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor$ScissorStack;containsPoint(II)Z"))
    private void modify_containsPointInScissor_FancyMenu(Args args) {
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
