package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
public class MixinOnlineServerEntry {

    @Inject(at = @At("HEAD"), method = "extractIcon", cancellable = true)
    private void head_extractIcon_FancyMenu(GuiGraphicsExtractor graphics, int rowLeft, int rowTop, Identifier location, CallbackInfo info) {
        if (!FancyMenu.getOptions().showMultiplayerScreenServerIcons.getValue()) {
            info.cancel();
        }
    }

    @WrapWithCondition(method = "extractContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
    private boolean wrap_fill_in_extractContent_FancyMenu(GuiGraphicsExtractor instance, int x0, int y0, int x1, int y1, int col) {
        return FancyMenu.getOptions().showMultiplayerScreenServerIcons.getValue();
    }

}
