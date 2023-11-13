package de.keksuccino.fancymenu.util.resources.audio;

import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;

public interface IAudio extends PlayableResourceWithAudio {

    void setSoundChannel(@NotNull SoundSource channel);

    @NotNull
    SoundSource getSoundChannel();

}
