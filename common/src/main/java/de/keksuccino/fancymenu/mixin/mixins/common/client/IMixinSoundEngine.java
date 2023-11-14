package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundEngine.class)
public interface IMixinSoundEngine {

    @Accessor("loaded") boolean getLoadedFancyMenu();

}
