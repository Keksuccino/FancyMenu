package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.resource.resources.audio.AudioEngineReloadHandler;
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

    /** @reason Release WaterMedia players while Minecraft's previous OpenAL context is still current. */
    @Inject(method = "reload", at = @At("HEAD"))
    private void before_reload_FancyMenu(CallbackInfo info) {
        AudioEngineReloadHandler.beforeSoundEngineReload();
    }

    @Inject(method = "reload", at = @At("RETURN"))
    private void after_reload_FancyMenu(CallbackInfo info) {
        AudioEngineReloadHandler.afterSoundEngineReload();
        //Reload AudioResourceHandler
        LOGGER_FANCYMENU.info("[FANCYMENU] Reloading AudioResourceHandler after Minecraft SoundEngine reload..");
        ResourceHandlers.getAudioHandler().releaseAll();
    }

}
