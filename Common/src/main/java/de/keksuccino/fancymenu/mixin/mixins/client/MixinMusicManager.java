package de.keksuccino.fancymenu.mixin.mixins.client;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicManager.class)
public abstract class MixinMusicManager {

    @Shadow public abstract void stopPlaying();

    @Inject(method = "startPlaying", at = @At("HEAD"), cancellable = true)
    private void stopMusicIfDisabledInConfigFancyMenu(Music $$0, CallbackInfo info) {
        if ((Minecraft.getInstance().level == null) && !FancyMenu.getOptions().playVanillaMenuMusic.getValue()) {
            this.stopPlaying();
            info.cancel();
        }
    }

}
