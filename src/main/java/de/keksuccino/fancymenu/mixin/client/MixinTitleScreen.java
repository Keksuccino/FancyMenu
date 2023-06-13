package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cancelTitleScreenRenderingInLoadingScreenFancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
        if (MenuCustomization.isMenuCustomizable((Screen)((Object)this)) && (Minecraft.getInstance().getOverlay() != null) && (Minecraft.getInstance().getOverlay() instanceof LoadingOverlay)) {
            info.cancel();
        }
    }

}
