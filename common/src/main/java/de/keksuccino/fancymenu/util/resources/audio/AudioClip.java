package de.keksuccino.fancymenu.util.resources.audio;

import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import java.io.Closeable;

public interface AudioClip extends Closeable {

    void play();

    boolean isPlaying();

    void pause();

    boolean isPaused();

    void resume();

    void stop();

    /**
     * Set the audio volume.<br>
     * The ACTUAL volume of the audio is a sub-volume of Minecraft's {@link SoundSource#MASTER} volume and the {@link AudioClip#getSoundChannel()} of the audio.
     *
     * @param volume Float between 0.0F and 1.0F.
     */
    void setVolume(float volume);

    /**
     * Get the audio volume.<br>
     * The ACTUAL volume of the audio is a sub-volume of Minecraft's {@link SoundSource#MASTER} volume and the {@link AudioClip#getSoundChannel()} of the audio.
     *
     * @return Float between 0.0F and 1.0F.
     */
    float getVolume();

    void setSoundChannel(@NotNull SoundSource channel);

    @NotNull
    SoundSource getSoundChannel();

    boolean isClosed();

}
