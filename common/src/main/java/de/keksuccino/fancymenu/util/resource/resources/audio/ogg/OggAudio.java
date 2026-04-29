package de.keksuccino.fancymenu.util.resource.resources.audio.ogg;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.resource.resources.audio.ALAudio;
import de.keksuccino.fancymenu.util.resource.resources.audio.AudioPlayTimeTracker;
import de.keksuccino.fancymenu.util.resource.resources.audio.AudioResourceReloadTracker;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.audio.OpenAlAudioClipFactory;
import de.keksuccino.melody.resources.audio.openal.ALAudioBuffer;
import de.keksuccino.melody.resources.audio.openal.ALAudioClip;
import de.keksuccino.melody.resources.audio.openal.ALErrorHandler;
import de.keksuccino.melody.resources.audio.openal.ALUtils;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.audio.OggAudioStream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL10;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
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
            LOGGER.error("[FANCYMENU] Failed to read OGG audio from ResourceLocation: " + location, ex);
        }

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

        ALAudioClip clip = OpenAlAudioClipFactory.createSafe();
        if (clip == null) {
            failBecauseOpenAlReload(audio, oggAudioFile.getPath(), "failed to allocate OpenAL source");
            return audio;
        }

        try {
            InputStream in = new FileInputStream(oggAudioFile);
            of(in, oggAudioFile.getPath(), audio, clip);
        } catch (Exception ex) {
            audio.loadingFailed = true;
            LOGGER.error("[FANCYMENU] Failed to read OGG audio from file: " + oggAudioFile.getPath(), ex);
        }

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

        ALAudioClip clip = OpenAlAudioClipFactory.createSafe();
        if (clip == null) {
            failBecauseOpenAlReload(audio, oggAudioURL, "failed to allocate OpenAL source");
            return audio;
        }

        new Thread(() -> {
            InputStream webIn = null;
            try {
                webIn = WebUtils.openResourceStream(oggAudioURL);
                if (webIn == null) throw new NullPointerException("Web resource input stream was NULL!");
                ByteArrayInputStream byteIn = new ByteArrayInputStream(webIn.readAllBytes());
                of(byteIn, oggAudioURL, audio, clip);
            } catch (Exception ex) {
                audio.loadingFailed = true;
                LOGGER.error("[FANCYMENU] Failed to read OGG audio from URL: " + oggAudioURL, ex);
            }
            CloseableUtils.closeQuietly(webIn);
        }).start();

        return audio;

    }

    @NotNull
    public static OggAudio of(@NotNull InputStream in, @Nullable String oggAudioName, @Nullable OggAudio writeTo, @Nullable ALAudioClip clip) {
        String name = (oggAudioName != null) ? oggAudioName : "[Generic InputStream Source]";
        OggAudio audio = (writeTo != null) ? writeTo : new OggAudio();

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
            OggAudioStream stream = null;
            try {
                byte[] fullData = in.readAllBytes();
                ByteArrayInputStream byteIn = new ByteArrayInputStream(fullData);

                try {
                    stream = new OggAudioStream(byteIn);

                    // Get total PCM data length by reading the full stream
                    ByteBuffer pcmData = stream.readAll();

                    // Calculate duration from PCM data:
                    // PCM bytes / (2 bytes per sample * channels) = samples per channel
                    // Then divide by sample rate to get seconds
                    audio.duration = (float) (pcmData.remaining() / (2 * stream.getFormat().getChannels())) / stream.getFormat().getSampleRate();

                } catch (Exception ex) {
                    LOGGER.warn("[FANCYMENU] Failed to read OGG duration metadata, duration will be approximate: " + name, ex);
                }

                // Reset stream for actual audio loading
                byteIn.reset();
                CloseableUtils.closeQuietly(stream);
                stream = new OggAudioStream(byteIn);

                // Continue with normal audio loading
                if (!audio.canContinueBackgroundLoading(cachedClip, name)) {
                    return;
                }
                ByteBuffer byteBuffer = stream.readAll();
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
                LOGGER.error("[FANCYMENU] Failed to read OGG audio: " + name, ex);
            }
            CloseableUtils.closeQuietly(stream);
            CloseableUtils.closeQuietly(in);
        }).start();

        return audio;
    }

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
            LOGGER.error("[FANCYMENU] Failed to get AL source in OggAudio!", ex);
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
            LOGGER.debug("[FANCYMENU] OGG source configuration error details: " + sourceName, ex);
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
            LOGGER.debug("[FANCYMENU] OGG buffer attach error details: " + sourceName, ex);
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
