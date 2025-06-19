package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
public class MixinOnlineServerEntry {

    @Inject(at = @At("HEAD"), method = "drawIcon", cancellable = true)
    private void onDrawIconFancyMenu(GuiGraphics $$0, int $$1, int $$2, ResourceLocation $$3, CallbackInfo info) {
        if (!FancyMenu.getOptions().showMultiplayerScreenServerIcons.getValue()) {
            info.cancel();
        }
    }

    @WrapWithCondition(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"), method = "render")
    private boolean onFillInRenderFancyMenu(GuiGraphics instance, int $$0, int $$1, int $$2, int $$3, int $$4) {
        return FancyMenu.getOptions().showMultiplayerScreenServerIcons.getValue();
    }

}
