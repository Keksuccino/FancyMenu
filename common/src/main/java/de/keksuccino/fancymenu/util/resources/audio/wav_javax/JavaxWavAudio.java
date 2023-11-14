package de.keksuccino.fancymenu.util.resources.audio.wav_javax;

import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.resources.audio.IAudio;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Unused WAV audio class that uses Java's default audio handling libraries instead of OpenAL.
 */
@SuppressWarnings("unused")
public class JavaxWavAudio implements IAudio {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    protected volatile JavaxWavAudioClip clip;
    protected ResourceLocation sourceLocation;
    protected File sourceFile;
    protected String sourceURL;
    protected volatile boolean decoded = false;
    protected volatile boolean closed = false;

    @NotNull
    public static JavaxWavAudio location(@NotNull ResourceLocation location) {
        return location(location, null);
    }

    @NotNull
    public static JavaxWavAudio location(@NotNull ResourceLocation location, @Nullable JavaxWavAudio writeTo) {

        Objects.requireNonNull(location);
        JavaxWavAudio audio = (writeTo != null) ? writeTo : new JavaxWavAudio();

        audio.sourceLocation = location;

        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isPresent()) {
                InputStream in = resource.get().open();
                of(in, location.toString(), audio);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read WAV audio from ResourceLocation: " + location, ex);
        }

        return audio;

    }

    @NotNull
    public static JavaxWavAudio local(@NotNull File wavAudioFile) {
        return local(wavAudioFile, null);
    }

    @NotNull
    public static JavaxWavAudio local(@NotNull File wavAudioFile, @Nullable JavaxWavAudio writeTo) {

        Objects.requireNonNull(wavAudioFile);
        JavaxWavAudio audio = (writeTo != null) ? writeTo : new JavaxWavAudio();

        audio.sourceFile = wavAudioFile;

        if (!wavAudioFile.isFile()) {
            LOGGER.error("[FANCYMENU] Failed to read WAV audio from file! File not found: " + wavAudioFile.getPath());
            return audio;
        }

        try {
            InputStream in = new FileInputStream(wavAudioFile);
            of(in, wavAudioFile.getPath(), audio);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read WAV audio from file: " + wavAudioFile.getPath(), ex);
        }

        return audio;

    }

    @NotNull
    public static JavaxWavAudio web(@NotNull String wavAudioURL) {
        return web(wavAudioURL, null);
    }

    @NotNull
    public static JavaxWavAudio web(@NotNull String wavAudioURL, @Nullable JavaxWavAudio writeTo) {

        Objects.requireNonNull(wavAudioURL);
        JavaxWavAudio audio = (writeTo != null) ? writeTo : new JavaxWavAudio();

        audio.sourceURL = wavAudioURL;

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(wavAudioURL)) {
            LOGGER.error("[FANCYMENU] Failed to read WAV audio from URL! Invalid URL: " + wavAudioURL);
            return audio;
        }

        new Thread(() -> {
            try {
                InputStream webIn = WebUtils.openResourceStream(wavAudioURL);
                if (webIn == null) throw new NullPointerException("Web resource input stream was NULL!");
                of(webIn, wavAudioURL, audio);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to read WAV audio from URL: " + wavAudioURL, ex);
            }
        }).start();

        return audio;

    }

    @NotNull
    public static JavaxWavAudio of(@NotNull InputStream in, @Nullable String wavAudioName, @Nullable JavaxWavAudio writeTo) {

        Objects.requireNonNull(in);

        String name = (wavAudioName != null) ? wavAudioName : "[Generic InputStream Source]";
        JavaxWavAudio audio = (writeTo != null) ? writeTo : new JavaxWavAudio();

        try {
            audio.clip = JavaxWavAudioClip.create();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read WAV audio! Failed to create clip: " + name, ex);
            return audio;
        }

        JavaxWavAudioClip clip = audio.clip;
        if (clip == null) {
            LOGGER.error("[FANCYMENU] Failed to read WAV audio! Clip was NULL: " + name);
            return audio;
        }

        new Thread(() -> {
            AudioInputStream stream = null;
            ByteArrayInputStream byteIn = null;
            try {
                byteIn = new ByteArrayInputStream(in.readAllBytes());
                stream = AudioSystem.getAudioInputStream(byteIn);
                clip.setStream(stream);
                audio.decoded = true;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to read WAV audio: " + name, ex);
            }
            CloseableUtils.closeQuietly(stream);
            CloseableUtils.closeQuietly(in);
            CloseableUtils.closeQuietly(byteIn);
        }).start();

        return audio;

    }

    @NotNull
    public static JavaxWavAudio of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    protected JavaxWavAudio() {
    }

    protected void forClip(@NotNull Consumer<JavaxWavAudioClip> clip) {
        JavaxWavAudioClip cached = this.clip;
        if (cached != null) clip.accept(cached);
    }

    @Nullable
    public JavaxWavAudioClip getClip() {
        return this.clip;
    }

    @Override
    public void play() {
        this.forClip(JavaxWavAudioClip::play);
    }

    @Override
    public boolean isPlaying() {
        JavaxWavAudioClip cached = this.clip;
        return (cached != null) && cached.isPlaying();
    }

    @Override
    public void pause() {
        this.forClip(JavaxWavAudioClip::pause);
    }

    @Override
    public boolean isPaused() {
        JavaxWavAudioClip cached = this.clip;
        return (cached != null) && cached.isPaused();
    }

    @Override
    public void stop() {
        this.forClip(JavaxWavAudioClip::stop);
    }

    @Override
    public void setVolume(float volume) {
        this.forClip(WavAudioClip -> WavAudioClip.setVolume(volume));
    }

    @Override
    public float getVolume() {
        JavaxWavAudioClip cached = this.clip;
        return (cached != null) ? cached.getVolume() : 0.0F;
    }

    public void setSoundChannel(@NotNull SoundSource channel) {
        this.forClip(WavAudioClip -> WavAudioClip.setSoundChannel(channel));
    }

    @NotNull
    public SoundSource getSoundChannel() {
        JavaxWavAudioClip cached = this.clip;
        return (cached != null) ? cached.getSoundChannel() : SoundSource.MASTER;
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
        if (!this.closed && this.decoded) {
            JavaxWavAudioClip cached = this.clip;
            return ((cached != null) && cached.isJavaxClipOpen());
        }
        return false;
    }

    public boolean isClipOpen() {
        JavaxWavAudioClip cached = this.clip;
        return (cached != null) && cached.isJavaxClipOpen();
    }

    @Override
    public void close() {
        this.closed = true;
        try {
            JavaxWavAudioClip cachedClip = this.clip;
            if (cachedClip != null) cachedClip.close();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to close WAV audio clip!", ex);
        }
        this.clip = null;
        this.decoded = false;
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

}
