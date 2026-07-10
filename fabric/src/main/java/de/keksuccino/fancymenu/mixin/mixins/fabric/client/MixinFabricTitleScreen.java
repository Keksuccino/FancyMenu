package de.keksuccino.fancymenu.mixin.mixins.fabric.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.mixin.interfaces.TitleScreenBrandingController;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TitleScreen.class)
public class MixinFabricTitleScreen {

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;drawString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
    private boolean cancelFabricVanillaBrandingRendering_FancyMenu(PoseStack pose, Font font, String text, int x, int y, int color) {
        ((TitleScreenBrandingController)this).fancymenu$setBrandingText(text);
        return false;
    }

}
