package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.global.SeamlessWorldLoadingHandler;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public class MixinJoinMultiplayerScreen {

    @Inject(method = "init", at = @At("HEAD"))
    private void before_init_FancyMenu(CallbackInfo info) {
        SeamlessWorldLoadingHandler.preLoadRecentServerScreenshots();
    }

}
