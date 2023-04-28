package de.keksuccino.fancymenu.mixin.mixins.client;

import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinFabricExample {

    @Shadow @Final private static Logger LOGGER;

    @Inject(at = @At("HEAD"), method = "init()V")
    private void init(CallbackInfo info) {
        
        LOGGER.info("This line is printed by an example mod mixin from Fabric!");

    }

}