package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.realmsclient.client.RealmsClient;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.platform.Services;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//TODO übernehmen (animation update)
@Mixin(targets = "net.minecraft.client.Minecraft$GameLoadCookie", remap = true)
public class MixinGameLoadCookie {

    @Unique private static boolean called_FancyMenu = false;

    /**
     * @reason FancyMenu needs to initialize its customization engine as early as possible, but AFTER all other mods got initialized, so we run ScreenCustomization#init() in the GameLoadCookie constructor, which gets called right before the loading screen overlay gets set for the first time.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void after_construct_FancyMenu(RealmsClient $$0, GameConfig.QuickPlayData $$1, CallbackInfo info) {
        if (!called_FancyMenu) {
            called_FancyMenu = true;
            if (Services.PLATFORM.isOnClient()) {
                ScreenCustomization.init();
            }
        }
    }

}
