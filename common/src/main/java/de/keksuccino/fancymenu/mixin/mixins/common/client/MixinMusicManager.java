package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.elements.musiccontroller.MusicControllerHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.MusicInfo;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicManager.class)
public abstract class MixinMusicManager {

    @Shadow public abstract void stopPlaying();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void cancelTickIfMusicDisabledViaController_FancyMenu(CallbackInfo info) {
        if (Minecraft.getInstance().level == null) {
            if (!MusicControllerHandler.shouldPlayMenuMusic()) info.cancel();
        } else {
            if (!MusicControllerHandler.shouldPlayWorldMusic()) info.cancel();
        }
    }

    @Inject(method = "startPlaying", at = @At("HEAD"), cancellable = true)
    private void stopMusicIfDisabledInConfigFancyMenu(MusicInfo $$0, CallbackInfo info) {
        if ((Minecraft.getInstance().level == null) && !FancyMenu.getOptions().playVanillaMenuMusic.getValue()) {
            this.stopPlaying();
            info.cancel();
        }
    }

}
