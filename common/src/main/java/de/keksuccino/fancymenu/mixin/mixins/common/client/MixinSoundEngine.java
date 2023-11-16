package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.resources.ResourceHandlers;
import net.minecraft.client.sounds.SoundEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public class MixinSoundEngine {

    @Unique private static final Logger LOGGER_FANCYMENU = LogManager.getLogger();

    @Inject(method = "reload", at = @At("RETURN"))
    private void afterReloadSoundEngineFancyMenu(CallbackInfo info) {
        //Reload AudioResourceHandler
        LOGGER_FANCYMENU.info("[FANCYMENU] Reloading AudioResourceHandler after Minecraft SoundEngine reload..");
        ResourceHandlers.getAudioHandler().releaseAll();
    }

}
