package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.platform.Services;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameNarrator.class)
public class MixinGameNarrator {

    @Unique private static boolean called_FancyMenu = false;

    /**
     * @reason FancyMenu needs to initialize its customization engine as early as possible, but AFTER all other mods got initialized, so we run ScreenCustomization#init() in the GameNarrator constructor, which gets called before the loading screen overlay gets set for the first time.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void after_construct_FancyMenu(Minecraft $$0, CallbackInfo info) {
        if (!called_FancyMenu) {
            called_FancyMenu = true;
            if (Services.PLATFORM.isOnClient()) {
                ScreenCustomization.init();
            }
        }
    }

}
