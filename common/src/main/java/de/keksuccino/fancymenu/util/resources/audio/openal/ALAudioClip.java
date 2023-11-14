package de.keksuccino.fancymenu.util.resources.audio.openal;

import de.keksuccino.fancymenu.util.resources.audio.AudioClip;
import de.keksuccino.fancymenu.util.resources.audio.MinecraftSoundSettingsObserver;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL10;
import java.util.Objects;

@SuppressWarnings("unused")
public class ALAudioClip implements AudioClip {

    private static final Logger LOGGER = LogManager.getLogger();

    protected final int source;
    @NotNull
    protected SoundSource soundChannel = SoundSource.MASTER;
    protected float volume = 1.0F;
    protected long volumeListenerId;
    protected volatile boolean closed = false;

    /**
     * Creates a new {@link ALAudioClip} with the given static {@link ALAudioBuffer}.<br>
     * {@link ALAudioClip}s stop working after Minecraft performs a resource reload or audio settings get changed, so make sure to
     * always check if the {@link ALAudioClip} is still working by calling {@link ALAudioClip#isLoadedInOpenAL()}.
     */
    @Nullable
    public static ALAudioClip of(@NotNull ALAudioBuffer completeStaticDataBuffer) {
        Objects.requireNonNull(completeStaticDataBuffer);
        ALAudioClip clip = ALAudioClip.create();
        if (clip != null) {
            clip.setStaticBuffer(completeStaticDataBuffer);
        }
        return clip;
    }

    /**
     * Creates a new {@link ALAudioClip}.<br>
     * {@link ALAudioClip}s stop working after Minecraft performs a resource reload or audio settings get changed, so make sure to
     * always check if the {@link ALAudioClip} is still working by calling {@link ALAudioClip#isLoadedInOpenAL()}.
     */
    @Nullable
    public static ALAudioClip create() {
        int[] audioSource = new int[1];
        AL10.alGenSources(audioSource);
        return ALUtils.checkAndPrintOpenAlError("Generate OpenAL audio source") ? null : new ALAudioClip(audioSource[0]);
    }

    protected ALAudioClip(int source) {
        this.source = source;
        this.volumeListenerId = MinecraftSoundSettingsObserver.registerVolumeListener((soundSource, aFloat) -> {
            if (soundSource == this.soundChannel) this.updateVolume();
        });
        this.updateVolume();
    }

    public int getState() {
        if (this.closed) return AL10.AL_STOPPED;
        int state = AL10.alGetSourcei(this.source, AL10.AL_SOURCE_STATE);
        ALUtils.checkAndPrintOpenAlError("Get OpenAL audio state");
        return state;
    }

    /**
     * If the audio is not playing, this will START the audio.<br>
     * If the audio is paused, this will RESUME the audio.<br>
     * If the audio is playing, this will RESTART the audio.
     */
    @Override
    public void play() {
        if (this.closed) return;
        if (!this.isPlaying()) AL10.alSourcePlay(this.source);
    }

    @Override
    public boolean isPlaying() {
        return this.getState() == AL10.AL_PLAYING;
    }

    /**
     * Will stop the audio.<br>
     * The audio will start at the beginning the next time it starts playing via {@link ALAudioClip#play()}.
     */
    @Override
    public void stop() {
        if (this.closed) return;
        if (!this.isStopped()) {
            AL10.alSourceStop(this.source);
            ALUtils.checkAndPrintOpenAlError("Stop OpenAL audio");
        }
    }

    public boolean isStopped() {
        return this.getState() == AL10.AL_STOPPED;
    }

    /**
     * Will pause the audio if it is currently playing and preserves its current state.<br>
     * To unpause the audio, use {@link ALAudioClip#resume()}.
     */
    @Override
    public void pause() {
        if (this.closed) return;
        if (this.isPlaying()) {
            AL10.alSourcePause(this.source);
            ALUtils.checkAndPrintOpenAlError("Pause OpenAL audio");
        }
    }

    /**
     * Will resume the audio if it is currently paused.
     */
    @Override
    public void resume() {
        if (this.closed) return;
        if (this.isPaused()) {
            AL10.alSourcePlay(this.source);
            ALUtils.checkAndPrintOpenAlError("Resume OpenAL audio");
        }
    }

    public boolean isPaused() {
        return this.getState() == AL10.AL_PAUSED;
    }

    public void setLooping(boolean looping) {
        if (this.closed) return;
        AL10.alSourcei(this.source, AL10.AL_LOOPING, looping ? 1 : 0);
        ALUtils.checkAndPrintOpenAlError("Set OpenAL audio looping");
    }

    public boolean isLooping() {
        if (this.closed) return false;
        boolean loop = AL10.alGetSourcei(this.source, AL10.AL_LOOPING) == 1;
        ALUtils.checkAndPrintOpenAlError("Get OpenAL audio looping");
        return loop;
    }

    @Override
    public void setVolume(float volume) {
        if (this.closed) return;
        if (volume > 1.0F) volume = 1.0F;
        if (volume < 0.0F) volume = 0.0F;
        this.volume = volume;
        float actualVolume = this.volume;
        if (this.soundChannel != SoundSource.MASTER) {
            float soundSourceVolume = Minecraft.getInstance().options.getSoundSourceVolume(this.soundChannel);
            actualVolume = actualVolume * soundSourceVolume; //Calculate percentage of volume by this audio's sound channel
        }
        AL10.alSourcef(this.source, AL10.AL_GAIN, Math.min(1.0F, Math.max(0.0F, actualVolume)));
        ALUtils.checkAndPrintOpenAlError("Set OpenAL audio volume");
    }

    @Override
    public float getVolume() {
        if (this.closed) return 0.0F;
        return this.volume;
    }

    public void updateVolume() {
        this.setVolume(this.volume);
    }

    @Override
    public void setSoundChannel(@NotNull SoundSource channel) {
        this.soundChannel = Objects.requireNonNull(channel);
        this.updateVolume();
    }

    @Override
    @NotNull
    public SoundSource getSoundChannel() {
        return this.soundChannel;
    }

    /**
     * Sets the static data buffer of this audio.<br>
     * The buffer should contain the complete audio data.
     */
    public void setStaticBuffer(@NotNull ALAudioBuffer completeDataBuffer) {
        if (this.closed) return;
        Integer buffer = completeDataBuffer.getSource();
        if (buffer != null) {
            AL10.alSourcei(this.source, AL10.AL_BUFFER, buffer);
            ALUtils.checkAndPrintOpenAlError("Set OpenAL audio buffer");
        }
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            MinecraftSoundSettingsObserver.unregisterVolumeListener(this.volumeListenerId);
            if (this.isLoadedInOpenAL()) {
                //Can't call stop(), because already closed
                AL10.alSourceStop(this.source);
                ALUtils.checkAndPrintOpenAlError("Stop OpenAL audio");
                AL10.alDeleteSources(new int[]{this.source});
                ALUtils.checkAndPrintOpenAlError("Delete OpenAL audio source");
            }
        }
    }

    @Override
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
