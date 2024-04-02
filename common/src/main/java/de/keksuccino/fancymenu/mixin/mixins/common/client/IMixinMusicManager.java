package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//TODO übernehmen
@Mixin(MusicManager.class)
public interface IMixinMusicManager {

    @Accessor("currentMusic") SoundInstance getCurrentMusic_FancyMenu();

}
