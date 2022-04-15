package de.keksuccino.fancymenu.mixin.client;

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

    @Inject(at = @At("HEAD"), method = "startPlaying", cancellable = true)
    private void onStartPlaying(Music music, CallbackInfo info) {

        if ((Minecraft.getInstance().level == null) && !FancyMenu.config.getOrDefault("playmenumusic", true)) {
            this.stopPlaying();
            info.cancel();
        }

    }

}
