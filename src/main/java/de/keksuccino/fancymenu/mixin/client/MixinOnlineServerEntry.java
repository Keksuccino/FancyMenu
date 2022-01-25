package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public class MixinOnlineServerEntry {

    @Inject(at = @At("HEAD"), method = "draw", cancellable = true)
    private void onDrawIcon(MatrixStack p_99890_, int p_99891_, int p_99892_, Identifier p_99893_, CallbackInfo info) {
        if (!FancyMenu.config.getOrDefault("show_server_icons", true)) {
            info.cancel();
        }
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawableHelper;fill(Lnet/minecraft/client/util/math/MatrixStack;IIIII)V"), method = "render")
    private void onFillInRender(MatrixStack p_93173_, int p_93174_, int p_93175_, int p_93176_, int p_93177_, int p_93178_) {
        if (FancyMenu.config.getOrDefault("show_server_icons", true)) {
            DrawableHelper.fill(p_93173_, p_93174_, p_93175_, p_93176_, p_93177_, p_93178_);
        }
    }

}
