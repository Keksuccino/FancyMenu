package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlainTextButton;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlainTextButton.class)
public class MixinPlainTextButton {

    @Inject(method = "extractContents", at = @At("HEAD"))
    private void before_extractContents_FancyMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
        // Fix for some opacity glitches with the Copyright button in the Title screen
        RenderingUtils.setShaderColor(graphics, 1.0f, 1.0f, 1.0f, 1.0f);
    }
}
