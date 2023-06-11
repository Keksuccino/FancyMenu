package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
public class MixinOnlineServerEntry {

    @Inject(at = @At("HEAD"), method = "drawIcon", cancellable = true)
    private void onDrawIcon(GuiGraphics p_281338_, int p_283001_, int p_282834_, ResourceLocation p_282534_, CallbackInfo info) {
        if (!FancyMenu.config.getOrDefault("show_server_icons", true)) {
            info.cancel();
        }
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"), method = "render")
    private void onFillInRender(GuiGraphics instance, int p_282988_, int p_282861_, int p_281278_, int p_281710_, int p_281470_) {
        if (FancyMenu.config.getOrDefault("show_server_icons", true)) {
            instance.fill(p_282988_, p_282861_, p_281278_, p_281710_, p_281470_);
        }
    }

}
