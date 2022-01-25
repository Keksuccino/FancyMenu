package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerListEntryNormal.class)
public class MixinOnlineServerEntry {

    @Inject(at = @At("HEAD"), method = "drawTextureAt", cancellable = true)
    private void onDrawIcon(int p_99891_, int p_99892_, ResourceLocation p_99893_, CallbackInfo info) {
        if (!FancyMenu.config.getOrDefault("show_server_icons", true)) {
            info.cancel();
        }
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;drawRect(IIIII)V"), method = "drawEntry")
    private void onFillInRender(int i, int j, int left, int top, int right) {
        if (FancyMenu.config.getOrDefault("show_server_icons", true)) {
            Gui.drawRect(i, j, left, top, right);
        }
    }

}
