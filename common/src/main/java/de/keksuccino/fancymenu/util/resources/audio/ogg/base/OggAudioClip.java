package de.keksuccino.fancymenu.util.resources.audio.ogg.base;

import de.keksuccino.fancymenu.util.resources.audio.AudioClip;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL10;
import java.util.Objects;

@SuppressWarnings("unused")
public class OggAudioClip implements AudioClip {

    //TODO Implement SoundSource handling
    //TODO Implement SoundSource handling
    //TODO Implement SoundSource handling
    //TODO Implement SoundSource handling
    //TODO Implement SoundSource handling

    protected final int source;
    @NotNull
    protected SoundSource soundCategory = SoundSource.MASTER;
    protected volatile boolean closed = false;

    /**
     * Creates a new {@link OggAudioClip} with the given {@link OggAudioBuffer}.<br>
     * {@link OggAudioClip}s stop working after Minecraft performs a resource reload or audio settings get changed, so make sure to
     * always check if the {@link OggAudioClip} is still working by calling {@link OggAudioClip#isLoadedInOpenAL()}.
     */
    @Nullable
    public static OggAudioClip of(@NotNull OggAudioBuffer completeDataBuffer) {
        Objects.requireNonNull(completeDataBuffer);
        OggAudioClip clip = OggAudioClip.create();
        if (clip != null) {
            clip.setBuffer(completeDataBuffer);
        }
        return clip;
    }

    /**
     * Creates a new {@link OggAudioClip}.<br>
     * {@link OggAudioClip}s stop working after Minecraft performs a resource reload or audio settings get changed, so make sure to
     * always check if the {@link OggAudioClip} is still working by calling {@link OggAudioClip#isLoadedInOpenAL()}.
     */
    @Nullable
    public static OggAudioClip create() {
        int[] audioSource = new int[1];
        AL10.alGenSources(audioSource);
        return OpenALUtils.checkAndPrintOpenAlError("Generate OpenAL audio source") ? null : new OggAudioClip(audioSource[0]);
    }

    protected OggAudioClip(int source) {
        this.source = source;
    }

    public int getState() {
        if (this.closed) return AL10.AL_STOPPED;
        int state = AL10.alGetSourcei(this.source, AL10.AL_SOURCE_STATE);
        OpenALUtils.checkAndPrintOpenAlError("Get OpenAL audio state");
        return state;
    }

    /**
     * When the audio is not playing, this will start the audio.<br>
     * If the audio is playing, this will RESTART the audio.
     */
    public void play() {
        if (this.closed) return;
        if (!this.isPlaying()) AL10.alSourcePlay(this.source);
    }

    public boolean isPlaying() {
        return this.getState() == AL10.AL_PLAYING;
    }

    /**
     * Will stop the audio.<br>
     * The audio will start at the beginning the next time it starts playing via {@link OggAudioClip#play()}.
     */
    public void stop() {
        if (this.closed) return;
        if (!this.isStopped()) {
            AL10.alSourceStop(this.source);
            OpenALUtils.checkAndPrintOpenAlError("Stop OpenAL audio");
        }
    }

    public boolean isStopped() {
        return this.getState() == AL10.AL_STOPPED;
    }

    /**
     * Will pause the audio if it is currently playing and preserves its current state.<br>
     * To unpause the audio, use {@link OggAudioClip#resume()}.
     */
    public void pause() {
        if (this.closed) return;
        if (this.isPlaying()) {
            AL10.alSourcePause(this.source);
            OpenALUtils.checkAndPrintOpenAlError("Pause OpenAL audio");
        }
    }

    /**
     * Will resume the audio if it is currently paused.
     */
    public void resume() {
        if (this.closed) return;
        if (this.isPaused()) {
            AL10.alSourcePlay(this.source);
            OpenALUtils.checkAndPrintOpenAlError("Resume OpenAL audio");
        }
    }

    public boolean isPaused() {
        return this.getState() == AL10.AL_PAUSED;
    }

    public void setLooping(boolean looping) {
        if (this.closed) return;
        AL10.alSourcei(this.source, AL10.AL_LOOPING, looping ? 1 : 0);
        OpenALUtils.checkAndPrintOpenAlError("Set OpenAL audio looping");
    }

    public boolean isLooping() {
        if (this.closed) return false;
        boolean loop = AL10.alGetSourcei(this.source, AL10.AL_LOOPING) == 1;
        OpenALUtils.checkAndPrintOpenAlError("Get OpenAL audio looping");
        return loop;
    }

    /**
     * Set the audio volume.<br><br>
     *
     * The audio's ACTUAL volume is a sub-volume of Minecraft's {@link SoundSource#MASTER} volume, so if this audio's volume is 1.0F and
     * Minecraft's {@link SoundSource#MASTER} volume is at 0.5F, this audio's actual volume is also 0.5F,
     * because {@link SoundSource#MASTER} is at 50%, so this audio is at 50% of 1.0F.
     *
     * @param volume Float between 0.0F and 1.0F.
     */
    public void setVolume(float volume) {
        if (this.closed) return;
        AL10.alSourcef(this.source, AL10.AL_GAIN, volume);
        OpenALUtils.checkAndPrintOpenAlError("Set OpenAL audio volume");
    }

    /**
     * Get the audio volume.<br><br>
     *
     * The audio's ACTUAL volume is a sub-volume of Minecraft's {@link SoundSource#MASTER} volume, so if this audio's volume is 1.0F and
     * Minecraft's {@link SoundSource#MASTER} volume is at 0.5F, this audio's actual volume is also 0.5F,
     * because {@link SoundSource#MASTER} is at 50%, so this audio is at 50% of 1.0F.
     *
     * @return Float between 0.0F and 1.0F.
     */
    public float getVolume() {
        if (this.closed) return 0.0F;
        float vol = AL10.alGetSourcef(this.source, AL10.AL_GAIN);
        OpenALUtils.checkAndPrintOpenAlError("Get OpenAL audio volume");
        return vol;
    }

    /**
     * Sets the data buffer of this audio.<br>
     * The buffer should contain the complete audio data.
     */
    public void setBuffer(@NotNull OggAudioBuffer completeDataBuffer) {
        if (this.closed) return;
        Integer buffer = completeDataBuffer.getSource();
        if (buffer != null) {
            AL10.alSourcei(this.source, AL10.AL_BUFFER, buffer);
            OpenALUtils.checkAndPrintOpenAlError("Set OpenAL audio buffer");
        }
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            if (this.isLoadedInOpenAL()) {
                //Can't call stop(), because already closed
                AL10.alSourceStop(this.source);
                OpenALUtils.checkAndPrintOpenAlError("Stop OpenAL audio");
                AL10.alDeleteSources(new int[]{this.source});
                OpenALUtils.checkAndPrintOpenAlError("Delete OpenAL audio source");
            }
        }
    }

    public boolean isClosed() {
        return this.closed;
    }

    /**
     * Checks if this audio's source is still a valid OpenAL source.<br>
     * Should return FALSE if the audio got closed or is not registered in OpenAL anymore due to reloading it, etc.
     */
    public boolean isLoadedInOpenAL() {
        return AL10.alIsSource(this.source);
    }

}
