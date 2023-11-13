package de.keksuccino.fancymenu.util.resources.audio.wav;

import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.resources.audio.AudioClip;
import de.keksuccino.fancymenu.util.resources.audio.MinecraftSoundSettingsObserver;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.util.Objects;

@SuppressWarnings("unused")
public class WavAudioClip implements AudioClip {

    private static final Logger LOGGER = LogManager.getLogger();

    protected final Clip clip;
    @NotNull
    protected SoundSource soundChannel = SoundSource.MASTER;
    protected float volume = 1.0F;
    protected long volumeListenerId;
    protected boolean canHandleVolume = true;
    protected boolean paused = false;
    protected long pauseMicrosecondPosition = 0L;
    protected boolean looping = false;
    protected boolean open = false;
    protected volatile boolean closed = false;

    /**
     * Creates a new {@link WavAudioClip} with the given {@link AudioInputStream}.<br>
     * The given {@link AudioInputStream} gets closed at the end.
     *
     * @param dataStream The audio data stream. The whole stream gets loaded into the clip.
     */
    @Nullable
    public static WavAudioClip of(@NotNull AudioInputStream dataStream) {
        Objects.requireNonNull(dataStream);
        WavAudioClip clip = WavAudioClip.create();
        if (clip != null) {
            try {
                clip.setStream(dataStream);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to create WavAudioClip! Exception while trying to set data stream!", ex);
                CloseableUtils.closeQuietly(clip);
                CloseableUtils.closeQuietly(dataStream);
                return null;
            }
        }
        CloseableUtils.closeQuietly(dataStream);
        return clip;
    }

    /**
     * Creates a new {@link WavAudioClip}.<br>
     */
    @Nullable
    public static WavAudioClip create() {
        Clip clip = null;
        try {
            clip = AudioSystem.getClip();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to create WavAudioClip!", ex);
        }
        return (clip == null) ? null : new WavAudioClip(clip);
    }

    protected WavAudioClip(@NotNull Clip clip) {
        this.clip = clip;
        this.volumeListenerId = MinecraftSoundSettingsObserver.registerVolumeListener((soundSource, aFloat) -> {
            if (soundSource == this.soundChannel) this.updateVolume();
        });
        this.updateVolume();
    }

    /**
     * If the audio is not playing, this will START the audio.<br>
     * If the audio is paused, this will RESUME the audio.<br>
     * If the audio is playing, this will RESTART the audio.
     */
    @Override
    public void play() {
        if (this.closed) return;
        if (this.paused) {
            this.resume();
        } else {
            if (this.isPlaying()) {
                this.stop();
            }
            this.clip.start();
        }
    }

    @Override
    public boolean isPlaying() {
        return !this.closed && !this.paused && this.clip.isRunning();
    }

    /**
     * Will stop the audio.<br>
     * The audio will start at the beginning the next time it starts playing via {@link WavAudioClip#play()}.
     */
    @Override
    public void stop() {
        if (this.closed) return;
        this.clip.stop();
        this.clip.flush();
        this.clip.setMicrosecondPosition(0L);
    }

    /**
     * Will pause the audio if it is currently playing and preserves its current state.<br>
     * To unpause the audio, use {@link WavAudioClip#resume()}.
     */
    @Override
    public void pause() {
        if (this.closed) return;
        if (this.isPlaying()) {
            this.paused = true;
            this.pauseMicrosecondPosition = this.clip.getMicrosecondPosition();
            this.stop();
        }
    }

    /**
     * Will resume the audio if it is currently paused.
     */
    @Override
    public void resume() {
        if (this.closed) return;
        if (this.paused) {
            this.paused = false;
            this.clip.setMicrosecondPosition(this.pauseMicrosecondPosition);
            this.clip.start();
        }
    }

    public boolean isPaused() {
        return this.paused;
    }

    public void setLooping(boolean looping) {
        if (this.closed) return;
        this.looping = looping;
        if (this.looping) {
            this.clip.setLoopPoints(0, -1);
            this.clip.loop(-1);
        } else {
            this.clip.loop(0);
        }
    }

    public boolean isLooping() {
        return this.looping;
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
        float masterVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
        actualVolume = actualVolume * masterVolume; //Calculate percentage of volume based on Minecraft's MASTER channel
        this._setVolume(Math.min(1.0F, Math.max(0.0F, actualVolume)));
    }

    protected void _setVolume(float volume) {
        if (!this.canHandleVolume) return;
        try {
            if (this.isJavaxClipOpen()) {
                if (this.clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl control = (FloatControl)this.clip.getControl(FloatControl.Type.MASTER_GAIN);
//                    int gain = (int)((float)((int)control.getMinimum()) + (control.getMaximum() - control.getMinimum()) / 100.0F * (float)percentage);
//                    control.setValue((float)gain);
                    if (volume > control.getMaximum()) volume = control.getMaximum();
                    if (volume < control.getMinimum()) volume = control.getMinimum();
                    //TODO remove debug
                    LOGGER.info("########### SETTING WAV VOLUME: " + volume);
                    control.setValue(volume);
                } else {
                    this.canHandleVolume = false;
                    LOGGER.error("[FANCYMENU] Critically failed to set volume of WAV audio! MASTER_GAIN control not supported! Disabling volume handling for audio!");
                }
            } else {
                LOGGER.error("[FANCYMENU] Failed to set volume of WAV audio! Javax clip not open!");
            }
        } catch (Exception ex) {
            this.canHandleVolume = false;
            LOGGER.error("[FANCYMENU] Critically failed to set volume of WAV audio! Disabling volume handling for audio!", ex);
        }
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
     * Sets the audio data stream of this audio.<br>
     * The whole {@link AudioInputStream} gets loaded into the clip, so it should be safe to close it at the end.<br>
     *
     * Does NOT close the given {@link AudioInputStream} at the end!
     */
    public void setStream(@NotNull AudioInputStream dataStream) throws Exception {
        if (this.closed) return;
        if (!this.open) {
            this.clip.open(Objects.requireNonNull(dataStream));
            this.open = true;
        } else {
            throw new IllegalStateException("WavAudioClip audio data stream already set!");
        }
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            MinecraftSoundSettingsObserver.unregisterVolumeListener(this.volumeListenerId);
            this.clip.close();
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    /**
     * Checks if this audio's JavaX {@link Clip} is open.<br>
     */
    public boolean isJavaxClipOpen() {
        return this.clip.isOpen();
    }

}
