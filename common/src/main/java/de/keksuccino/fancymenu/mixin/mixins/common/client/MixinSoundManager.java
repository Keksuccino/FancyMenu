package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public abstract class MixinSoundManager {

    @Shadow @Final
    private SoundEngine soundEngine;

    @Unique
    private SoundEventListener worldSoundEventBridge_FancyMenu;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void afterInitFancyMenu(ResourceManager resourceManager, Options options, CallbackInfo ci) {
        if (this.worldSoundEventBridge_FancyMenu == null) {
            this.worldSoundEventBridge_FancyMenu = (sound, accessor) -> {
                Sound resolvedSound = sound.getSound();
                float attenuationDistance = resolvedSound != null ? (float)resolvedSound.getAttenuationDistance() : 0.0F;
                float audibleRange = Math.max(sound.getVolume(), 1.0F) * attenuationDistance;
                Listeners.ON_WORLD_SOUND_TRIGGERED.onWorldSoundTriggered(sound, accessor.getSubtitle(), audibleRange);
            };
            this.soundEngine.addEventListener(this.worldSoundEventBridge_FancyMenu);
        }
    }

}

