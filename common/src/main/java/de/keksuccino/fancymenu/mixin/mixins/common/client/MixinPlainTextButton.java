package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.PlainTextButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlainTextButton.class)
public class MixinPlainTextButton {

    @Inject(method = "renderButton", at = @At("HEAD"))
    private void before_renderButton_FancyMenu(PoseStack pose, int mouseX, int mouseY, float partial, CallbackInfo info) {
        // Fix for some opacity glitches with the Copyright button in the Title screen
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
