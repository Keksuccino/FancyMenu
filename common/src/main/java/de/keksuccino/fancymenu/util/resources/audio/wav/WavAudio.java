package de.keksuccino.fancymenu.util.resources.audio.wav;

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

@SuppressWarnings("unused")
public class WavAudio implements IAudio {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    protected volatile WavAudioClip clip;
    protected ResourceLocation sourceLocation;
    protected File sourceFile;
    protected String sourceURL;
    protected volatile boolean decoded = false;
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
    public static WavAudio local(@NotNull File wavAudioFile) {
        return local(wavAudioFile, null);
    }

    @NotNull
    public static WavAudio local(@NotNull File wavAudioFile, @Nullable WavAudio writeTo) {

        Objects.requireNonNull(wavAudioFile);
        WavAudio audio = (writeTo != null) ? writeTo : new WavAudio();

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
    public static WavAudio web(@NotNull String wavAudioURL) {
        return web(wavAudioURL, null);
    }

    @NotNull
    public static WavAudio web(@NotNull String wavAudioURL, @Nullable WavAudio writeTo) {

        Objects.requireNonNull(wavAudioURL);
        WavAudio audio = (writeTo != null) ? writeTo : new WavAudio();

        audio.sourceURL = wavAudioURL;

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(wavAudioURL)) {
            LOGGER.error("[FANCYMENU] Failed to read WAV audio from URL! Invalid URL: " + wavAudioURL);
            return audio;
        }

        //TODO byte array input stream entfernen !!! wird bereits in of() gemacht !!

        //TODO FIXEN: setVolume noch broken (zeigt immer clip not open ????)

        new Thread(() -> {
            InputStream webIn = null;
            try {
                webIn = WebUtils.openResourceStream(wavAudioURL);
                if (webIn == null) throw new NullPointerException("Web resource input stream was NULL!");
                ByteArrayInputStream byteIn = new ByteArrayInputStream(webIn.readAllBytes());
                of(byteIn, wavAudioURL, audio);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to read WAV audio from URL: " + wavAudioURL, ex);
            }
            CloseableUtils.closeQuietly(webIn);
        }).start();

        return audio;

    }

    @NotNull
    public static WavAudio of(@NotNull InputStream in, @Nullable String wavAudioName, @Nullable WavAudio writeTo) {

        Objects.requireNonNull(in);

        String name = (wavAudioName != null) ? wavAudioName : "[Generic InputStream Source]";
        WavAudio audio = (writeTo != null) ? writeTo : new WavAudio();

        try {
            audio.clip = WavAudioClip.create();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read WAV audio! Failed to create clip: " + name, ex);
            return audio;
        }

        WavAudioClip clip = audio.clip;
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
    public static WavAudio of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    protected WavAudio() {
    }

    protected void forClip(@NotNull Consumer<WavAudioClip> clip) {
        WavAudioClip cached = this.clip;
        if (cached != null) clip.accept(cached);
    }

    @Nullable
    public WavAudioClip getClip() {
        return this.clip;
    }

    @Override
    public void play() {
        this.forClip(WavAudioClip::play);
    }

    @Override
    public boolean isPlaying() {
        WavAudioClip cached = this.clip;
        return (cached != null) && cached.isPlaying();
    }

    @Override
    public void pause() {
        this.forClip(WavAudioClip::pause);
    }

    @Override
    public boolean isPaused() {
        WavAudioClip cached = this.clip;
        return (cached != null) && cached.isPaused();
    }

    @Override
    public void stop() {
        this.forClip(WavAudioClip::stop);
    }

    @Override
    public void setVolume(float volume) {
        this.forClip(WavAudioClip -> WavAudioClip.setVolume(volume));
    }

    @Override
    public float getVolume() {
        WavAudioClip cached = this.clip;
        return (cached != null) ? cached.getVolume() : 0.0F;
    }

    public void setSoundChannel(@NotNull SoundSource channel) {
        this.forClip(WavAudioClip -> WavAudioClip.setSoundChannel(channel));
    }

    @NotNull
    public SoundSource getSoundChannel() {
        WavAudioClip cached = this.clip;
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
        return !this.closed && this.decoded && (this.clip != null);
    }

    public boolean isClipOpen() {
        WavAudioClip cached = this.clip;
        return (cached != null) && cached.isJavaxClipOpen();
    }

    @Override
    public void close() {
        this.closed = true;
        try {
            WavAudioClip cachedClip = this.clip;
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
