package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.global.SeamlessWorldLoadingHandler;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SelectWorldScreen.class)
public class MixinSelectWorldScreen {

    @Inject(method = "init", at = @At("HEAD"))
    private void before_init_FancyMenu(CallbackInfo info) {
        SeamlessWorldLoadingHandler.preLoadRecentWorldScreenshots();
    }

}
