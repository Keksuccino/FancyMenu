package de.keksuccino.fancymenu.util.resource.resources.audio;

import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;

public interface IAudio extends PlayableResourceWithAudio {

    /**
     * If the audio is not playing, this will START the audio.<br>
     * If the audio is paused, this will RESUME the audio.<br>
     * If the audio is playing, this will RESTART the audio.
     */
    @Override
    void play();

    void setSoundChannel(@NotNull SoundSource channel);

    @NotNull
    SoundSource getSoundChannel();

    /**
     * Returns the duration of the audio in seconds.
     *
     * @return Duration in seconds
     */
    float getDuration();

    /**
     * Returns the current play time position in seconds.<br>
     * This takes into account pauses and resumes.
     *
     * @return Current play time in seconds
     */
    float getPlayTime();

}
