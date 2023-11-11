package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.resources.audio.MinecraftVolumeObserver;
import net.minecraft.client.Options;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public class MixinSoundEngine {

    @Shadow private boolean loaded;
    @Shadow @Final private Options options;

    @Inject(method = "updateCategoryVolume", at = @At("RETURN"))
    private void beforeUpdateVolumeCategoryFancyMenu(SoundSource source, float vol, CallbackInfo info) {
        if (this.loaded) {
            MinecraftVolumeObserver.getListenerMap(source).forEach((aLong, floatConsumer) -> floatConsumer.accept(this.options.getSoundSourceVolume(source)));
        }
    }

}
