package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.screen.WidgetifiedScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.TextWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@WidgetifiedScreen
@Mixin(GenericDirtMessageScreen.class)
public class MixinGenericDirtMessageScreen extends Screen {

    protected MixinGenericDirtMessageScreen(Component $$0) {
        super($$0);
    }

    @Override
    protected void init() {

        this.addRenderableWidget(TextWidget.of(this.getTitle(), 0, 70, 200))
                .centerWidget(this)
                .setTextAlignment(TextWidget.TextAlignment.CENTER)
                .setWidgetIdentifierFancyMenu("message");

    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/GenericDirtMessageScreen;drawCenteredString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private boolean wrapDrawCenteredStringInRenderFancyMenu(PoseStack poseStack, Font $$0, Component $$1, int $$2, int $$3, int $$4) {
        return !ScreenCustomization.isCustomizationEnabledForScreen(this);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/GenericDirtMessageScreen;renderDirtBackground(I)V"))
    private void wrapRenderDirtBackgroundInRenderFancyMenu(GenericDirtMessageScreen instance, int vOffset, Operation<Void> original, PoseStack poseStack, int mouseX, int mouseY, float partial) {
        GuiGraphics graphics = GuiGraphics.currentGraphics();
        ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(this);
        if ((layer == null) || !ScreenCustomization.isCustomizationEnabledForScreen(this)) {
            original.call(instance, vOffset);
            return;
        }

        if (layer.shouldReplaceVanillaScreenBackground()) {
            RenderSystem.enableBlend();
            graphics.fill(0, 0, this.width, this.height, 0);
            RenderingUtils.resetShaderColor(graphics);
        } else {
            original.call(instance, vOffset);
        }
        EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(this, graphics, mouseX, mouseY, partial));
    }

}
