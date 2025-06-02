package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.mixin.LateMixinHandler;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MixinMain {

    @Inject(method = "main", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;beginInitialization()V", shift = At.Shift.AFTER, remap = false), remap = false)
    private static void after_beginInitialization_in_main_FancyMenu(String[] args, CallbackInfo info) {

        LateMixinHandler.initLateMixins();

    }

}
