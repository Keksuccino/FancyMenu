package de.keksuccino.fancymenu.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ResourceLoadProgressGui;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MainMenuScreen.class)
public class MixinTitleScreen {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cancelTitleScreenRenderingInLoadingScreenFancyMenu(MatrixStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
        if (MenuCustomization.isMenuCustomizable((Screen)((Object)this)) && (Minecraft.getInstance().getOverlay() != null) && (Minecraft.getInstance().getOverlay() instanceof ResourceLoadProgressGui)) {
            info.cancel();
        }
    }

}
