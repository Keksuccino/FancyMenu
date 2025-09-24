package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.elements.musiccontroller.MusicControllerHandler;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicManager.class)
public abstract class MixinMusicManager {

    @Unique
    @Nullable
    private SoundInstance pendingStoppedMusic_FancyMenu;

    @Unique
    @Nullable
    private String currentTrackResourceLocation_FancyMenu;

    @Unique
    @Nullable
    private String currentTrackEventLocation_FancyMenu;

    @Unique
    private void fireMusicTrackStartedFancyMenu(@Nullable SoundInstance soundInstance) {
        String eventLocation = this.extractEventResourceLocationFancyMenu(soundInstance);
        String trackLocation = this.extractTrackResourceLocationFancyMenu(soundInstance);
        this.currentTrackResourceLocation_FancyMenu = trackLocation;
        this.currentTrackEventLocation_FancyMenu = eventLocation;
        if ((trackLocation != null) || (eventLocation != null)) {
            Listeners.ON_MUSIC_TRACK_STARTED.onMusicTrackStarted(trackLocation, eventLocation);
        }
    }

    @Unique
    private void fireMusicTrackStoppedFancyMenu(@Nullable SoundInstance soundInstance) {
        String eventLocation = this.extractEventResourceLocationFancyMenu(soundInstance);
        if (eventLocation == null) {
            eventLocation = this.currentTrackEventLocation_FancyMenu;
        }
        String trackLocation = this.extractTrackResourceLocationFancyMenu(soundInstance);
        if (trackLocation == null) {
            trackLocation = this.currentTrackResourceLocation_FancyMenu;
        }
        this.currentTrackResourceLocation_FancyMenu = null;
        this.currentTrackEventLocation_FancyMenu = null;
        if ((trackLocation != null) || (eventLocation != null)) {
            Listeners.ON_MUSIC_TRACK_STOPPED.onMusicTrackStopped(trackLocation, eventLocation);
        }
    }

    @Unique
    @Nullable
    private String extractTrackResourceLocationFancyMenu(@Nullable SoundInstance soundInstance) {
        if (soundInstance == null) {
            return null;
        }
        Sound sound = soundInstance.getSound();
        if (sound != null && sound != SoundManager.EMPTY_SOUND && sound != SoundManager.INTENTIONALLY_EMPTY_SOUND) {
            ResourceLocation resolvedPath = sound.getPath();
            if (resolvedPath != null) {
                return resolvedPath.toString();
            }
            ResourceLocation resolvedLocation = sound.getLocation();
            if (resolvedLocation != null) {
                return resolvedLocation.toString();
            }
        }
        ResourceLocation fallback = soundInstance.getLocation();
        return (fallback != null) ? fallback.toString() : null;
    }

    @Unique
    @Nullable
    private String extractEventResourceLocationFancyMenu(@Nullable SoundInstance soundInstance) {
        if (soundInstance == null) {
            return null;
        }
        ResourceLocation location = soundInstance.getLocation();
        return (location != null) ? location.toString() : null;
    }

    @Shadow
    public abstract void stopPlaying();

    @Shadow
    @Nullable
    private SoundInstance currentMusic;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void cancelTickIfMusicDisabledViaController_FancyMenu(CallbackInfo info) {
        if (Minecraft.getInstance().level == null) {
            if (!MusicControllerHandler.shouldPlayMenuMusic()) info.cancel();
        } else {
            if (!MusicControllerHandler.shouldPlayWorldMusic()) info.cancel();
        }
    }

    @Inject(method = "startPlaying", at = @At("HEAD"), cancellable = true)
    private void stopMusicIfDisabledInConfigFancyMenu(Music music, CallbackInfo info) {
        if ((Minecraft.getInstance().level == null) && !FancyMenu.getOptions().playVanillaMenuMusic.getValue()) {
            this.stopPlaying();
            info.cancel();
        }
    }

    @Inject(method = "startPlaying", at = @At("RETURN"))
    private void after_startPlayingFancyMenu(Music music, CallbackInfo info) {
        if ((this.currentMusic != null) && (this.currentMusic.getSound() != SoundManager.EMPTY_SOUND)) {
            this.fireMusicTrackStartedFancyMenu(this.currentMusic);
        } else {
            this.currentTrackResourceLocation_FancyMenu = null;
            this.currentTrackEventLocation_FancyMenu = null;
        }
    }

    @Inject(method = "stopPlaying()V", at = @At("HEAD"))
    private void before_stopPlayingFancyMenu(CallbackInfo info) {
        this.pendingStoppedMusic_FancyMenu = this.currentMusic;
    }

    @Inject(method = "stopPlaying()V", at = @At("RETURN"))
    private void after_stopPlayingFancyMenu(CallbackInfo info) {
        if (this.pendingStoppedMusic_FancyMenu != null) {
            this.fireMusicTrackStoppedFancyMenu(this.pendingStoppedMusic_FancyMenu);
        }
        this.pendingStoppedMusic_FancyMenu = null;
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/sounds/MusicManager;currentMusic:Lnet/minecraft/client/resources/sounds/SoundInstance;", opcode = Opcodes.PUTFIELD)
    )
    private void wrap_setCurrentMusicFancyMenu(MusicManager instance, SoundInstance value, Operation<Void> operation) {
        SoundInstance previous = this.currentMusic;
        operation.call(instance, value);
        if ((previous != null) && (value == null)) {
            this.fireMusicTrackStoppedFancyMenu(previous);
        } else if (value != null) {
            this.currentTrackResourceLocation_FancyMenu = this.extractTrackResourceLocationFancyMenu(value);
            this.currentTrackEventLocation_FancyMenu = this.extractEventResourceLocationFancyMenu(value);
        }
    }
}
