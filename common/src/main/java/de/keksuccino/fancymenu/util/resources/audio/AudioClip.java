package de.keksuccino.fancymenu.util.resources.audio;

import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import java.io.Closeable;

public interface AudioClip extends Closeable {

    /**
     * If the audio is not playing, this will START the audio.<br>
     * If the audio is paused, this will RESUME the audio.<br>
     * If the audio is playing, this will RESTART the audio.
     */
    void play();

    boolean isPlaying();

    /**
     * Will pause the audio if it is currently playing and preserves its current state.<br>
     * To unpause the audio, use {@link AudioClip#resume()}.
     */
    void pause();

    boolean isPaused();

    /**
     * Will resume the audio if it is currently paused.
     */
    void resume();

    /**
     * Will stop the audio.<br>
     * The audio will start at the beginning the next time it starts playing via {@link AudioClip#play()}.
     */
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
