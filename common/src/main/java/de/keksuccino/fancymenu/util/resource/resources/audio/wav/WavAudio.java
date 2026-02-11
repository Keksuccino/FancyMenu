package de.keksuccino.fancymenu.util.resource.resources.audio.wav;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.resource.resources.audio.ALAudio;
import de.keksuccino.fancymenu.util.resource.resources.audio.AudioPlayTimeTracker;
import de.keksuccino.fancymenu.util.resource.resources.audio.AudioEngineReloadHandler;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.audio.OpenAlAudioClipFactory;
import de.keksuccino.melody.resources.audio.openal.ALAudioBuffer;
import de.keksuccino.melody.resources.audio.openal.ALAudioClip;
import de.keksuccino.melody.resources.audio.openal.ALErrorHandler;
import de.keksuccino.melody.resources.audio.openal.ALUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL10;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class WavAudio implements IAudio, ALAudio {

    private static final Logger LOGGER = LogManager.getLogger();

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

    @NotNull
    public static WavAudio location(@NotNull ResourceLocation location) {
        return location(location, null);
    }

    @NotNull
    public static WavAudio location(@NotNull ResourceLocation location, @Nullable WavAudio writeTo) {

        Objects.requireNonNull(location);
        WavAudio audio = (writeTo != null) ? writeTo : new WavAudio();

        audio.sourceLocation = location;

        //Clips need to get created on the main thread, so make sure we're in the correct thread
        RenderSystem.assertOnRenderThread();

        if (isOpenAlNotReadyOrReloading()) {
            failBecauseOpenAlNotReady(audio, location.toString());
            return audio;
        }

        ALAudioClip clip = OpenAlAudioClipFactory.createSafe();
        if (clip == null) {
            failBecauseOpenAlReload(audio, location.toString(), "failed to allocate OpenAL source");
            return audio;
        }

        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isPresent()) {
                InputStream in = resource.get().open();
                of(in, location.toString(), audio, clip);
            }
        } catch (Exception ex) {
            audio.loadingFailed = true;
            LOGGER.error("[FANCYMENU] Failed to read WAV audio from ResourceLocation: " + location, ex);
        }

        return audio;

    }

    @NotNull
    public static WavAudio local(@NotNull File wavAudioFile) {
        return local(wavAudioFile, null);
    }

    @NotNull
    public static WavAudio local(@NotNull File wavAudioFile, @Nullable WavAudio writeTo) {

        Objects.requireNonNull(wavAudioFile);
        WavAudio audio = (writeTo != null) ? writeTo : new WavAudio();

        audio.sourceFile = wavAudioFile;

        if (!wavAudioFile.isFile()) {
            audio.loadingFailed = true;
            LOGGER.error("[FANCYMENU] Failed to read WAV audio from file! File not found: " + wavAudioFile.getPath());
            return audio;
        }

        //Clips need to get created on the main thread, so make sure we're in the correct thread
        RenderSystem.assertOnRenderThread();

        if (isOpenAlNotReadyOrReloading()) {
            failBecauseOpenAlNotReady(audio, wavAudioFile.getPath());
            return audio;
        }

        ALAudioClip clip = OpenAlAudioClipFactory.createSafe();
        if (clip == null) {
            failBecauseOpenAlReload(audio, wavAudioFile.getPath(), "failed to allocate OpenAL source");
            return audio;
        }

        try {
            InputStream in = new FileInputStream(wavAudioFile);
            of(in, wavAudioFile.getPath(), audio, clip);
        } catch (Exception ex) {
            audio.loadingFailed = true;
            LOGGER.error("[FANCYMENU] Failed to read WAV audio from file: " + wavAudioFile.getPath(), ex);
        }

        return audio;

    }

    @NotNull
    public static WavAudio web(@NotNull String wavAudioURL) {
        return web(wavAudioURL, null);
    }

    @NotNull
    public static WavAudio web(@NotNull String wavAudioURL, @Nullable WavAudio writeTo) {

        Objects.requireNonNull(wavAudioURL);
        WavAudio audio = (writeTo != null) ? writeTo : new WavAudio();

        audio.sourceURL = wavAudioURL;

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(wavAudioURL)) {
            audio.loadingFailed = true;
            LOGGER.error("[FANCYMENU] Failed to read WAV audio from URL! Invalid URL: " + wavAudioURL);
            return audio;
        }

        //Clips need to get created on the main thread, so make sure we're in the correct thread
        RenderSystem.assertOnRenderThread();

        if (isOpenAlNotReadyOrReloading()) {
            failBecauseOpenAlNotReady(audio, wavAudioURL);
            return audio;
        }

        ALAudioClip clip = OpenAlAudioClipFactory.createSafe();
        if (clip == null) {
            failBecauseOpenAlReload(audio, wavAudioURL, "failed to allocate OpenAL source");
            return audio;
        }

        new Thread(() -> {
            try {
                InputStream in = WebUtils.openResourceStream(wavAudioURL);
                if (in == null) throw new NullPointerException("Web resource input stream was NULL!");
                of(in, wavAudioURL, audio, clip);
            } catch (Exception ex) {
                audio.loadingFailed = true;
                LOGGER.error("[FANCYMENU] Failed to read WAV audio from URL: " + wavAudioURL, ex);
            }
        }).start();

        return audio;

    }

    @NotNull
    public static WavAudio of(@NotNull InputStream in, @Nullable String wavAudioName, @Nullable WavAudio writeTo, @Nullable ALAudioClip clip) {
        String name = (wavAudioName != null) ? wavAudioName : "[Generic InputStream Source]";
        WavAudio audio = (writeTo != null) ? writeTo : new WavAudio();

        // Clips need to get created on the main thread, so make sure we're in the correct thread
        if (clip == null) RenderSystem.assertOnRenderThread();

        if (isOpenAlNotReadyOrReloading()) {
            CloseableUtils.closeQuietly(clip);
            failBecauseOpenAlNotReady(audio, name);
            return audio;
        }

        audio.clip = (clip != null) ? clip : OpenAlAudioClipFactory.createSafe();

        ALAudioClip cachedClip = audio.clip;
        if (cachedClip == null) {
            failBecauseOpenAlReload(audio, name, "failed to allocate OpenAL source");
            return audio;
        }
        if (!audio.configureNonPositionalSource(name)) {
            return audio;
        }

        new Thread(() -> {
            AudioInputStream stream = null;
            ByteArrayInputStream byteIn = null;
            try {
                // Read the full stream into a byte array
                byte[] fullData = in.readAllBytes();

                // Read header first - WAV header is minimum 44 bytes
                if (fullData.length >= 44) {
                    try {
                        // Create a new input stream that wraps your fullData
                        WavHeader header = null;
                        InputStream headerStream = null;
                        try {
                            headerStream = new ByteArrayInputStream(fullData);
                            header = WavHeader.read(headerStream);
                        } catch (IOException ex) {
                            LOGGER.error("[FANCYMENU] Failed to read WAV header of WavAudio: " + name, ex);
                        }
                        CloseableUtils.closeQuietly(headerStream);
                        float calculatedDuration = 0;
                        if (header != null) calculatedDuration = header.getDurationInSeconds();
                        if (calculatedDuration > 0) {
                            audio.duration = calculatedDuration;
                        } else {
                            LOGGER.warn("[FANCYMENU] Invalid WAV header duration calculated for: " + name);
                        }
                    } catch (Exception ex) {
                        LOGGER.warn("[FANCYMENU] Failed to read WAV header of WavAudio: " + name, ex);
                    }
                } else {
                    LOGGER.warn("[FANCYMENU] WAV file too small, missing header data: " + name);
                }

                // Continue with normal audio loading
                if (!audio.canContinueBackgroundLoading(cachedClip, name)) {
                    return;
                }
                byteIn = new ByteArrayInputStream(fullData);
                stream = AudioSystem.getAudioInputStream(byteIn);
                ByteBuffer byteBuffer = ALUtils.readStreamIntoBuffer(stream);
                ALAudioBuffer audioBuffer = new ALAudioBuffer(byteBuffer, stream.getFormat());
                audio.audioBuffer = audioBuffer;
                if (!audio.tryAttachDecodedBuffer(cachedClip, audioBuffer, name)) {
                    return;
                }
                audio.loadingFailed = false;
                audio.retryWhenOpenAlReady = false;
                audio.decoded = true;
                audio.loadingCompleted = true;
            } catch (Exception ex) {
                audio.loadingFailed = true;
                LOGGER.error("[FANCYMENU] Failed to read WAV audio: " + name, ex);
            }
            CloseableUtils.closeQuietly(stream);
            CloseableUtils.closeQuietly(in);
            CloseableUtils.closeQuietly(byteIn);
        }).start();

        return audio;
    }

    @NotNull
    public static WavAudio of(@NotNull InputStream in) {
        return of(in, null, null, null);
    }

    protected WavAudio() {
    }

    private static boolean isOpenAlNotReadyOrReloading() {
        return !ALUtils.isOpenAlReady()
                || AudioEngineReloadHandler.isInPostReloadCooldown()
                || OpenAlAudioClipFactory.isCreationTemporarilyBlocked();
    }

    @Nullable
    public ALAudioClip getClip() {
        return this.clip;
    }

    @Override
    public void play() {
        this.forClip(alAudioClip -> {
            try {
                alAudioClip.play();
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
        this.forClip(alAudioClip -> {
            try {
                alAudioClip.pause();
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
        this.forClip(alAudioClip -> {
            try {
                alAudioClip.stop();
                this.playTimeTracker.onStop();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    @Override
    public void setVolume(float volume) {
        this.forClip(alAudioClip -> {
            try {
                alAudioClip.setVolume(volume);
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

    protected void forClip(@NotNull Consumer<ALAudioClip> clip) {
        ALAudioClip cached = this.clip;
        if (cached != null) clip.accept(cached);
    }

    private static void failBecauseOpenAlNotReady(@NotNull WavAudio audio, @NotNull String sourceName) {
        audio.loadingFailed = true;
        audio.loadingCompleted = false;
        audio.decoded = false;
        audio.retryWhenOpenAlReady = true;
        LOGGER.warn("[FANCYMENU] Delaying WAV audio load because OpenAL is not ready yet or still reloading. It will retry automatically once ready again: " + sourceName);
    }

    private static void failBecauseOpenAlReload(@NotNull WavAudio audio, @NotNull String sourceName, @NotNull String reason) {
        audio.loadingFailed = true;
        audio.loadingCompleted = false;
        audio.decoded = false;
        audio.retryWhenOpenAlReady = true;
        LOGGER.warn("[FANCYMENU] Delaying WAV audio load because OpenAL is reloading (" + reason + "). It will retry automatically once ready again: " + sourceName);
    }

    @Override
    public @Nullable InputStream open() throws IOException {
        if (this.sourceURL != null) return WebUtils.openResourceStream(this.sourceURL);
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
            LOGGER.error("[FANCYMENU] Failed to get AL source in WavAudio!", ex);
        }
        return 0;
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
            LOGGER.debug("[FANCYMENU] WAV source configuration error details: " + sourceName, ex);
        }
        return false;
    }

    private boolean canContinueBackgroundLoading(@NotNull ALAudioClip targetClip, @NotNull String sourceName) {
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

    private boolean tryAttachDecodedBuffer(@NotNull ALAudioClip targetClip, @NotNull ALAudioBuffer decodedBuffer, @NotNull String sourceName) {
        if (!this.canContinueBackgroundLoading(targetClip, sourceName)) return false;
        Integer preparedBuffer = decodedBuffer.getSource();
        if ((preparedBuffer == null) || !decodedBuffer.isValidOpenAlSource()) {
            failBecauseOpenAlReload(this, sourceName, "failed to prepare OpenAL buffer");
            return false;
        }
        try {
            targetClip.setStaticBuffer(decodedBuffer);
            if (!targetClip.isValidOpenAlSource()) {
                failBecauseOpenAlReload(this, sourceName, "OpenAL source became invalid while attaching decoded audio");
                return false;
            }
            return true;
        } catch (Exception ex) {
            failBecauseOpenAlReload(this, sourceName, "failed to attach decoded audio buffer");
            LOGGER.debug("[FANCYMENU] WAV buffer attach error details: " + sourceName, ex);
        }
        return false;
    }

    @Override
    public void close() {
        this.closed = true;
        this.retryWhenOpenAlReady = false;
        try {
            ALAudioClip cachedClip = this.clip;
            if (cachedClip != null) cachedClip.close();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to close WAV audio clip!", ex);
        }
        this.clip = null;
        try {
            ALAudioBuffer cachedBuffer = this.audioBuffer;
            if (cachedBuffer != null) cachedBuffer.delete();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to delete WAV audio buffer!", ex);
        }
        this.audioBuffer = null;
        this.decoded = false;
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

}
