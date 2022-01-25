//TODO übernehmen
package de.keksuccino.fancymenu.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.gui.GuiComponent;
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
    private void onDrawIcon(PoseStack p_99890_, int p_99891_, int p_99892_, ResourceLocation p_99893_, CallbackInfo info) {
        if (!FancyMenu.config.getOrDefault("show_server_icons", true)) {
            info.cancel();
        }
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiComponent;fill(Lcom/mojang/blaze3d/vertex/PoseStack;IIIII)V"), method = "render")
    private void onFillInRender(PoseStack p_93173_, int p_93174_, int p_93175_, int p_93176_, int p_93177_, int p_93178_) {
        if (FancyMenu.config.getOrDefault("show_server_icons", true)) {
            GuiComponent.fill(p_93173_, p_93174_, p_93175_, p_93176_, p_93177_, p_93178_);
        }
    }

}
