package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlainTextButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlainTextButton.class)
public class MixinPlainTextButton {

    @Inject(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I"))
    private void before_drawString_in_renderWidget_FancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
        // Fix for some opacity glitches with the Copyright button in the Title screen
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
