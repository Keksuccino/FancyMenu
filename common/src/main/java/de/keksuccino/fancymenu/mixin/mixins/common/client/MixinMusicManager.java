package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.elements.musiccontroller.MusicControllerHandler;
import de.keksuccino.fancymenu.customization.global.GlobalCustomizationHandler;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
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

    @Shadow @Nullable private SoundInstance currentMusic;

    @Unique @Nullable private SoundInstance pendingStoppedMusic_FancyMenu;
    @Unique @Nullable private String currentTrackResourceLocation_FancyMenu;
    @Unique @Nullable private String currentTrackEventLocation_FancyMenu;

    @Shadow public abstract void stopPlaying();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void cancel_tick_FancyMenu(CallbackInfo info) {
        if (Minecraft.getInstance().level == null) {
            if (!MusicControllerHandler.shouldPlayMenuMusic()) info.cancel();
        } else {
            if (!MusicControllerHandler.shouldPlayWorldMusic()) info.cancel();
        }
    }

    @Inject(method = "startPlaying", at = @At("HEAD"), cancellable = true)
    private void cancel_startPlaying_FancyMenu(Music music, CallbackInfo info) {
        if (Minecraft.getInstance().level == null) {
            if (!FancyMenu.getOptions().playVanillaMenuMusic.getValue() || GlobalCustomizationHandler.hasCustomMenuMusicTracks()) {
                this.stopPlaying();
                info.cancel();
            }
        }
    }

    @Inject(method = "startPlaying", at = @At("RETURN"))
    private void after_startPlaying_FancyMenu(Music music, CallbackInfo info) {
        if (!this.isMusicListenerTrackingNeeded_FancyMenu()) {
            this.clearMusicListenerState_FancyMenu();
            return;
        }
        if ((this.currentMusic != null) && (this.currentMusic.getSound() != SoundManager.EMPTY_SOUND)) {
            this.fireMusicTrackStarted_FancyMenu(this.currentMusic);
        } else {
            this.currentTrackResourceLocation_FancyMenu = null;
            this.currentTrackEventLocation_FancyMenu = null;
        }
    }

    @Inject(method = "stopPlaying()V", at = @At("HEAD"))
    private void before_stopPlaying_FancyMenu(CallbackInfo info) {
        this.pendingStoppedMusic_FancyMenu = this.isMusicListenerTrackingNeeded_FancyMenu() ? this.currentMusic : null;
    }

    @Inject(method = "stopPlaying()V", at = @At("RETURN"))
    private void after_stopPlaying_FancyMenu(CallbackInfo info) {
        if (this.pendingStoppedMusic_FancyMenu != null) {
            this.fireMusicTrackStopped_FancyMenu(this.pendingStoppedMusic_FancyMenu);
        }
        this.pendingStoppedMusic_FancyMenu = null;
    }

    @WrapOperation(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/sounds/MusicManager;currentMusic:Lnet/minecraft/client/resources/sounds/SoundInstance;", opcode = Opcodes.PUTFIELD))
    private void wrap_setCurrentMusic_FancyMenu(MusicManager instance, SoundInstance value, Operation<Void> operation) {
        SoundInstance previous = this.currentMusic;
        operation.call(instance, value);
        if (!this.isMusicListenerTrackingNeeded_FancyMenu()) {
            this.clearMusicListenerState_FancyMenu();
            return;
        }
        if ((previous != null) && (value == null)) {
            this.fireMusicTrackStopped_FancyMenu(previous);
        } else if (value != null) {
            this.currentTrackResourceLocation_FancyMenu = this.extractTrackResourceLocation_FancyMenu(value);
            this.currentTrackEventLocation_FancyMenu = this.extractEventResourceLocation_FancyMenu(value);
        }
    }

    @Unique
    private void fireMusicTrackStarted_FancyMenu(@Nullable SoundInstance soundInstance) {
        if (!this.isMusicListenerTrackingNeeded_FancyMenu()) {
            this.clearMusicListenerState_FancyMenu();
            return;
        }
        String eventLocation = this.extractEventResourceLocation_FancyMenu(soundInstance);
        String trackLocation = this.extractTrackResourceLocation_FancyMenu(soundInstance);
        this.currentTrackResourceLocation_FancyMenu = trackLocation;
        this.currentTrackEventLocation_FancyMenu = eventLocation;
        if (Listeners.ON_MUSIC_TRACK_STARTED.hasInstancesListening() && ((trackLocation != null) || (eventLocation != null))) {
            Listeners.ON_MUSIC_TRACK_STARTED.onMusicTrackStarted(trackLocation, eventLocation);
        }
    }

    @Unique
    private void fireMusicTrackStopped_FancyMenu(@Nullable SoundInstance soundInstance) {
        if (!this.isMusicListenerTrackingNeeded_FancyMenu()) {
            this.clearMusicListenerState_FancyMenu();
            return;
        }
        if (!Listeners.ON_MUSIC_TRACK_STOPPED.hasInstancesListening()) {
            this.clearMusicListenerState_FancyMenu();
            return;
        }
        String eventLocation = this.extractEventResourceLocation_FancyMenu(soundInstance);
        if (eventLocation == null) {
            eventLocation = this.currentTrackEventLocation_FancyMenu;
        }
        String trackLocation = this.extractTrackResourceLocation_FancyMenu(soundInstance);
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
    private String extractTrackResourceLocation_FancyMenu(@Nullable SoundInstance soundInstance) {
        if (soundInstance == null) {
            return null;
        }
        Sound sound = soundInstance.getSound();
        if (sound != null && sound != SoundManager.EMPTY_SOUND && sound != SoundManager.INTENTIONALLY_EMPTY_SOUND) {
            Identifier resolvedPath = sound.getPath();
            if (resolvedPath != null) {
                return resolvedPath.toString();
            }
            Identifier resolvedLocation = sound.getLocation();
            if (resolvedLocation != null) {
                return resolvedLocation.toString();
            }
        }
        Identifier fallback = soundInstance.getIdentifier();
        return (fallback != null) ? fallback.toString() : null;
    }

    @Unique
    @Nullable
    private String extractEventResourceLocation_FancyMenu(@Nullable SoundInstance soundInstance) {
        if (soundInstance == null) {
            return null;
        }
        Identifier location = soundInstance.getIdentifier();
        return (location != null) ? location.toString() : null;
    }

    @Unique
    private boolean isMusicListenerTrackingNeeded_FancyMenu() {
        return Listeners.ON_MUSIC_TRACK_STARTED.hasInstancesListening() || Listeners.ON_MUSIC_TRACK_STOPPED.hasInstancesListening();
    }

    @Unique
    private void clearMusicListenerState_FancyMenu() {
        this.pendingStoppedMusic_FancyMenu = null;
        this.currentTrackResourceLocation_FancyMenu = null;
        this.currentTrackEventLocation_FancyMenu = null;
    }
}
