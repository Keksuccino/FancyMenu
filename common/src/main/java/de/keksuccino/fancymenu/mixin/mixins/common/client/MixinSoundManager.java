package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.global.GlobalCustomizationHandler;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
    private void afterInitFancyMenu(Options options, CallbackInfo info) {
        if (this.worldSoundEventBridge_FancyMenu == null) {
            this.worldSoundEventBridge_FancyMenu = (sound, accessor, range) -> Listeners.ON_WORLD_SOUND_TRIGGERED.onWorldSoundTriggered(sound, accessor.getSubtitle(), range);
            this.soundEngine.addEventListener(this.worldSoundEventBridge_FancyMenu);
        }
    }

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void head_play_FancyMenu(SoundInstance sound, CallbackInfo info) {
        if (sound instanceof SimpleSoundInstance i) {
            SoundEvent event = SoundEvents.UI_BUTTON_CLICK.value();
            if ((event != null) && (i.getLocation() == event.getLocation())) {
                IAudio globalClickSound = GlobalCustomizationHandler.getCustomButtonClickSound();
                if (globalClickSound != null) {
                    globalClickSound.setSoundChannel(SoundSource.MASTER);
                    globalClickSound.stop();
                    globalClickSound.play();
                    info.cancel();
                }
            }
        }
    }

}

