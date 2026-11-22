package de.keksuccino.fancymenu.util.resource.resources.audio.ogg;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.resource.resources.audio.ALAudio;
import de.keksuccino.fancymenu.util.resource.resources.audio.ALAudioLoader;
import de.keksuccino.fancymenu.util.resource.resources.audio.AudioPlayTimeTracker;
import de.keksuccino.fancymenu.util.resource.resources.audio.AudioResourceReloadTracker;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.audio.OpenAlAudioClipFactory;
import de.keksuccino.fancymenu.util.threading.FancyMenuThreads;
import de.keksuccino.melody.resources.audio.openal.ALAudioBuffer;
import de.keksuccino.melody.resources.audio.openal.ALAudioClip;
import de.keksuccino.melody.resources.audio.openal.ALErrorHandler;
import de.keksuccino.melody.resources.audio.openal.ALUtils;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.audio.OggAudioStream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL10;
import javax.sound.sampled.AudioFormat;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class OggAudio implements IAudio, ALAudio {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int AL_SEC_OFFSET_FANCYMENU = 0x1024;

    @Nullable
    protected volatile ALAudioClip clip;
    @Nullable
    protected volatile ALAudioBuffer audioBuffer;
    protected ResourceLocation sourceLocation;
    protected File sourceFile;
    protected String sourceURL;
    protected volatile float duration = 0.0f;
    protected final AudioPlayTimeTracker playTimeTracker = new AudioPlayTimeTracker();
    protected volatile boolean decoded = false;
    protected volatile boolean loadingCompleted = false;
    protected volatile boolean loadingFailed = false;
    protected volatile boolean retryWhenOpenAlReady = false;
    protected volatile boolean closed = false;
    /**
     * Makes the clip and buffer hand-overs and the checks of background loads exclusive with {@link #close()}: a clip or buffer either
     * gets handed to the audio before it gets closed, and close() closes or deletes it then, or the hand-over sees the closed audio,
     * and the load closes or deletes it itself.
     */
    private final Object backgroundLoadLock = new Object();

    @NotNull
    public static OggAudio location(@NotNull ResourceLocation location) {
        return location(location, null);
    }

    @NotNull
    public static OggAudio location(@NotNull ResourceLocation location, @Nullable OggAudio writeTo) {

        Objects.requireNonNull(location);
        OggAudio audio = (writeTo != null) ? writeTo : new OggAudio();

        audio.sourceLocation = location;

        //Clips need to get created on the main thread, so make sure we're in the correct thread
        RenderSystem.assertOnRenderThread();

        if (isOpenAlNotReadyOrReloading()) {
            failBecauseOpenAlNotReady(audio, location.toString());
            return audio;
        }

        // Opens the resource before its clip gets created, so a missing resource never leaves an unused OpenAL source behind
        InputStream in = ALAudioLoader.openSource(() -> Minecraft.getInstance().getResourceManager().open(location), location.toString(), null, () -> audio.loadingFailed = true);
        if (in != null) of(in, location.toString(), audio, null);

        return audio;

    }

    @NotNull
    public static OggAudio local(@NotNull File oggAudioFile) {
        return local(oggAudioFile, null);
    }

    @NotNull
    public static OggAudio local(@NotNull File oggAudioFile, @Nullable OggAudio writeTo) {

        Objects.requireNonNull(oggAudioFile);
        OggAudio audio = (writeTo != null) ? writeTo : new OggAudio();

        audio.sourceFile = oggAudioFile;

        if (!oggAudioFile.isFile()) {
            audio.loadingFailed = true;
            LOGGER.error("[FANCYMENU] Failed to read OGG audio from file! File not found: " + oggAudioFile.getPath());
            return audio;
        }

        //Clips need to get created on the main thread, so make sure we're in the correct thread
        RenderSystem.assertOnRenderThread();

        if (isOpenAlNotReadyOrReloading()) {
            failBecauseOpenAlNotReady(audio, oggAudioFile.getPath());
            return audio;
        }

        // Opens the file before its clip gets created, so a file that can't get opened never leaves an unused OpenAL source behind
        InputStream in = ALAudioLoader.openSource(() -> new FileInputStream(oggAudioFile), oggAudioFile.getPath(), null, () -> audio.loadingFailed = true);
        if (in != null) of(in, oggAudioFile.getPath(), audio, null);

        return audio;

    }

    @NotNull
    public static OggAudio web(@NotNull String oggAudioURL) {
        return web(oggAudioURL, null);
    }

    @NotNull
    public static OggAudio web(@NotNull String oggAudioURL, @Nullable OggAudio writeTo) {

        Objects.requireNonNull(oggAudioURL);
        OggAudio audio = (writeTo != null) ? writeTo : new OggAudio();

        audio.sourceURL = oggAudioURL;

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(oggAudioURL)) {
            audio.loadingFailed = true;
            LOGGER.error("[FANCYMENU] Failed to read OGG audio from URL! Invalid URL: " + oggAudioURL);
            return audio;
        }

        //Clips need to get created on the main thread, so make sure we're in the correct thread
        RenderSystem.assertOnRenderThread();

        if (isOpenAlNotReadyOrReloading()) {
            failBecauseOpenAlNotReady(audio, oggAudioURL);
            return audio;
        }

        // Gets created here, before the web stream gets opened in the background, so the web load owns it until the audio takes it
        ALAudioClip clip = OpenAlAudioClipFactory.createSafe();
        if (clip == null) {
            failBecauseOpenAlReload(audio, oggAudioURL, "failed to allocate OpenAL source");
            return audio;
        }

        FancyMenuThreads.startDaemonThread(() -> {
            InputStream in = ALAudioLoader.openSource(() -> WebUtils.openResourceStreamOrThrow(oggAudioURL, WebUtils.WebResourceType.AUDIO), oggAudioURL, clip, () -> audio.loadingFailed = true);
            if (in != null) of(in, oggAudioURL, audio, clip);
        }, "OggAudio-WebLoader");

        return audio;

    }

    /**
     * Takes ownership of the passed {@link InputStream} and {@link ALAudioClip}, so it closes them when loading fails or doesn't start.
     */
    @NotNull
    public static OggAudio of(@NotNull InputStream in, @Nullable String oggAudioName, @Nullable OggAudio writeTo, @Nullable ALAudioClip clip) {
        String name = (oggAudioName != null) ? oggAudioName : "[Generic InputStream Source]";
        OggAudio audio = (writeTo != null) ? writeTo : new OggAudio();

        // The loader closes the stream once it read it and the audio closes the clip once it took it, so they only get closed here when that doesn't happen
        boolean loadingStarted = false;
        ALAudioClip unclaimedClip = clip;
        try {
            // Clips need to get created on the main thread, so make sure we're in the correct thread
            if (clip == null) RenderSystem.assertOnRenderThread();

            if (isOpenAlNotReadyOrReloading()) {
                failBecauseOpenAlNotReady(audio, name);
                return audio;
            }

            ALAudioClip audioClip = (clip != null) ? clip : OpenAlAudioClipFactory.createSafe();
            unclaimedClip = audioClip;
            if (audioClip == null) {
                failBecauseOpenAlReload(audio, name, "failed to allocate OpenAL source");
                return audio;
            }
            // Fails when the audio got closed while its source got opened
            if (!audio.takeClip(audioClip)) return audio;
            unclaimedClip = null;
            if (!audio.configureNonPositionalSource(name)) {
                return audio;
            }

            ALAudioLoader.loadInBackground(in, name, new BackgroundLoad(audio, audioClip, name));
            loadingStarted = true;
        } finally {
            if (!loadingStarted) CloseableUtils.closeQuietly(in);
            CloseableUtils.closeQuietly(unclaimedClip);
        }

        return audio;
    }

    /**
     * Takes ownership of the passed {@link InputStream}, so it closes it when loading fails or doesn't start.
     */
    @NotNull
    public static OggAudio of(@NotNull InputStream in) {
        return of(in, null, null, null);
    }

    protected OggAudio() {
        AudioResourceReloadTracker.registerAudioInstance_FancyMenu(this);
    }

    private static boolean isOpenAlNotReadyOrReloading() {
        return !ALUtils.isOpenAlReady();
    }

    private static void failBecauseOpenAlNotReady(@NotNull OggAudio audio, @NotNull String sourceName) {
        audio.loadingFailed = true;
        audio.loadingCompleted = false;
        audio.decoded = false;
        audio.retryWhenOpenAlReady = true;
        LOGGER.warn("[FANCYMENU] Delaying OGG audio load because OpenAL is not ready yet or still reloading. It will retry automatically once ready again: " + sourceName);
    }

    private static void failBecauseOpenAlReload(@NotNull OggAudio audio, @NotNull String sourceName, @NotNull String reason) {
        audio.loadingFailed = true;
        audio.loadingCompleted = false;
        audio.decoded = false;
        audio.retryWhenOpenAlReady = true;
        LOGGER.warn("[FANCYMENU] Delaying OGG audio load because OpenAL is reloading (" + reason + "). It will retry automatically once ready again: " + sourceName);
    }

    protected void forClip(@NotNull Consumer<ALAudioClip> clip) {
        ALAudioClip cached = this.clip;
        if (cached != null) clip.accept(cached);
    }

    @Nullable
    public ALAudioClip getClip() {
        return this.clip;
    }

    @Override
    public void play() {
        this.forClip(oggAudioClip -> {
            try {
                oggAudioClip.play();
                this.playTimeTracker.onPlay();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    @Override
    public boolean isPlaying() {
        try {
            ALAudioClip cached = this.clip;
            if (cached != null) return cached.isPlaying();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public void pause() {
        this.forClip(oggAudioClip -> {
            try {
                oggAudioClip.pause();
                this.playTimeTracker.onPause();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    @Override
    public boolean isPaused() {
        try {
            ALAudioClip cached = this.clip;
            if (cached != null) return cached.isPaused();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public void stop() {
        this.forClip(oggAudioClip -> {
            try {
                oggAudioClip.stop();
                this.playTimeTracker.onStop();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    @Override
    public void setVolume(float volume) {
        this.forClip(oggAudioClip -> {
            try {
                oggAudioClip.setVolume(volume);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    @Override
    public float getVolume() {
        ALAudioClip cached = this.clip;
        return (cached != null) ? cached.getVolume() : 0.0F;
    }

    public void setSoundChannel(@NotNull SoundSource channel) {
        this.forClip(oggAudioClip -> oggAudioClip.setSoundChannel(channel));
    }

    @NotNull
    public SoundSource getSoundChannel() {
        ALAudioClip cached = this.clip;
        return (cached != null) ? cached.getSoundChannel() : SoundSource.MASTER;
    }

    @Override
    public float getDuration() {
        return this.duration;
    }

    @Override
    public float getPlayTime() {
        return this.playTimeTracker.getCurrentPlayTime();
    }

    @Override
    public void setPlayTime(float playTime) {
        float clamped = playTime;
        if (!Float.isFinite(clamped) || clamped < 0.0F) clamped = 0.0F;
        float duration = this.getDuration();
        if (duration > 0.0F) {
            clamped = Math.min(clamped, duration);
        }
        ALAudioClip cachedClip = this.clip;
        if (cachedClip == null || cachedClip.isClosed() || !cachedClip.isValidOpenAlSource()) {
            return;
        }
        int source = this.getALSource();
        if (source == 0) {
            return;
        }
        boolean playing = this.isPlaying();
        boolean paused = this.isPaused();
        try {
            AL10.alSourcef(source, AL_SEC_OFFSET_FANCYMENU, clamped);
            ALErrorHandler.checkOpenAlError();
            this.playTimeTracker.setPlayTime(clamped, paused || !playing);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to seek OGG audio preview play time!", ex);
        }
    }

    @Override
    public @Nullable InputStream open() throws IOException {
        if (this.sourceURL != null) return WebUtils.openResourceStreamOrThrow(this.sourceURL, WebUtils.WebResourceType.AUDIO);
        if (this.sourceFile != null) return new FileInputStream(this.sourceFile);
        if (this.sourceLocation != null) return Minecraft.getInstance().getResourceManager().open(this.sourceLocation);
        return null;
    }

    @Override
    public boolean isReady() {
        if (this.closed || !this.decoded) return false;
        ALAudioClip cachedClip = this.clip;
        if (cachedClip != null) {
            if (cachedClip.isClosed()) return false;
            if (cachedClip.isValidOpenAlSource()) return true;
        }
        return false;
    }

    @Override
    public boolean isLoadingCompleted() {
        return !this.closed && !this.loadingFailed && this.loadingCompleted;
    }

    @Override
    public boolean isLoadingFailed() {
        return this.loadingFailed;
    }

    @Override
    public boolean isLoadingFailureRetryable() {
        return this.retryWhenOpenAlReady;
    }

    public boolean isValidOpenAlSource() {
        ALAudioClip cached = this.clip;
        return (cached != null) && cached.isValidOpenAlSource();
    }

    public int getALSource() {
        if (this.clip == null) return 0;
        try {
            Field f = ALAudioClip.class.getDeclaredField("source");
            f.setAccessible(true);
            return f.getInt(this.clip);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to get AL source in OggAudio!", ex);
        }
        return 0;
    }

    /**
     * Makes the clip the clip of this audio, which then closes it when it gets closed.
     *
     * @return False when the audio got closed already, so the caller still owns the clip and has to close it.
     */
    private boolean takeClip(@NotNull ALAudioClip newClip) {
        synchronized (this.backgroundLoadLock) {
            if (this.closed) return false;
            this.clip = newClip;
            return true;
        }
    }

    private boolean configureNonPositionalSource(@NotNull String sourceName) {
        ALAudioClip cachedClip = this.clip;
        if ((cachedClip == null) || this.closed || cachedClip.isClosed()) return false;
        if (isOpenAlNotReadyOrReloading()) {
            failBecauseOpenAlNotReady(this, sourceName);
            return false;
        }
        if (!cachedClip.isValidOpenAlSource()) {
            failBecauseOpenAlReload(this, sourceName, "OpenAL source became invalid");
            return false;
        }
        int source = this.getALSource();
        if (source == 0) {
            failBecauseOpenAlReload(this, sourceName, "OpenAL source handle was 0");
            return false;
        }
        try {
            AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
            AL10.alSource3f(source, AL10.AL_POSITION, 0.0F, 0.0F, 0.0F);
            AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 0.0F);
            ALErrorHandler.checkOpenAlError();
            return true;
        } catch (Exception ex) {
            failBecauseOpenAlReload(this, sourceName, "failed to configure non-positional source");
            LOGGER.debug("[FANCYMENU] OGG source configuration error details: " + sourceName, ex);
        }
        return false;
    }

    private boolean canContinueBackgroundLoading(@NotNull ALAudioClip targetClip, @NotNull String sourceName) {
        synchronized (this.backgroundLoadLock) {
            if (this.closed || (this.clip != targetClip) || targetClip.isClosed()) return false;
            if (isOpenAlNotReadyOrReloading()) {
                failBecauseOpenAlNotReady(this, sourceName);
                return false;
            }
            if (!targetClip.isValidOpenAlSource()) {
                failBecauseOpenAlReload(this, sourceName, "OpenAL source became invalid");
                return false;
            }
            return true;
        }
    }

    /**
     * @return True when the clip plays the buffer and the audio owns it, false when the caller has to delete the buffer.
     */
    private boolean tryAttachDecodedBuffer(@NotNull ALAudioClip targetClip, @NotNull ALAudioBuffer decodedBuffer, @NotNull String sourceName) {
        if (!this.canContinueBackgroundLoading(targetClip, sourceName)) return false;
        // Uploads the audio data to OpenAL without holding the lock, so close() never waits for that
        boolean prepared = (decodedBuffer.getSource() != null) && decodedBuffer.isValidOpenAlSource();
        synchronized (this.backgroundLoadLock) {
            if (!this.canContinueBackgroundLoading(targetClip, sourceName)) return false;
            if (!prepared) {
                failBecauseOpenAlReload(this, sourceName, "failed to prepare OpenAL buffer");
                return false;
            }
            try {
                targetClip.setStaticBuffer(decodedBuffer);
                if (!targetClip.isValidOpenAlSource()) {
                    failBecauseOpenAlReload(this, sourceName, "OpenAL source became invalid while attaching decoded audio");
                    return false;
                }
                this.audioBuffer = decodedBuffer;
                this.loadingFailed = false;
                this.retryWhenOpenAlReady = false;
                this.decoded = true;
                this.loadingCompleted = true;
                return true;
            } catch (Exception ex) {
                failBecauseOpenAlReload(this, sourceName, "failed to attach decoded audio buffer");
                LOGGER.debug("[FANCYMENU] OGG buffer attach error details: " + sourceName, ex);
            }
            return false;
        }
    }

    @Override
    public void close() {
        synchronized (this.backgroundLoadLock) {
            this.closed = true;
            this.retryWhenOpenAlReady = false;
            try {
                ALAudioClip cachedClip = this.clip;
                if (cachedClip != null) cachedClip.close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to close OGG audio clip!", ex);
            }
            this.clip = null;
            try {
                ALAudioBuffer cachedBuffer = this.audioBuffer;
                if (cachedBuffer != null) cachedBuffer.delete();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to delete OGG audio buffer!", ex);
            }
            this.audioBuffer = null;
            this.decoded = false;
        }
    }

    @Override
    public boolean isClosed() {
        if (!this.closed) {
            if (this.retryWhenOpenAlReady) {
                try {
                    if (!isOpenAlNotReadyOrReloading()) {
                        this.close();
                    }
                } catch (Exception ignored) {
                }
            }
            ALAudioClip cachedClip = this.clip;
            if ((cachedClip != null) && !cachedClip.isValidOpenAlSource()) {
                this.close();
            }
        }
        return this.closed;
    }

    /**
     * Connects a background load to the audio and the clip it got started for.
     */
    private static final class BackgroundLoad implements ALAudioLoader.Target {

        @NotNull
        private final OggAudio audio;
        @NotNull
        private final ALAudioClip clip;
        @NotNull
        private final String sourceName;

        private BackgroundLoad(@NotNull OggAudio audio, @NotNull ALAudioClip clip, @NotNull String sourceName) {
            this.audio = audio;
            this.clip = clip;
            this.sourceName = sourceName;
        }

        @Override
        public boolean canContinueLoading() {
            return this.audio.canContinueBackgroundLoading(this.clip, this.sourceName);
        }

        /**
         * Decodes the OGG file in a single pass, which also gives its exact duration.
         */
        @Override
        public @NotNull ALAudioBuffer decode(@NotNull byte[] audioData) throws IOException {
            try (OggAudioStream stream = new OggAudioStream(new ByteArrayInputStream(audioData))) {
                ByteBuffer pcmData = stream.readAll();
                AudioFormat format = stream.getFormat();
                // PCM bytes / (2 bytes per sample * channels) = samples per channel, divided by the sample rate gives seconds
                this.audio.duration = (float) (pcmData.remaining() / (2 * format.getChannels())) / format.getSampleRate();
                return new ALAudioBuffer(pcmData, format);
            }
        }

        @Override
        public boolean attachDecodedBuffer(@NotNull ALAudioBuffer buffer) {
            return this.audio.tryAttachDecodedBuffer(this.clip, buffer, this.sourceName);
        }

        @Override
        public void onLoadingFailed() {
            this.audio.loadingFailed = true;
        }

    }

}
