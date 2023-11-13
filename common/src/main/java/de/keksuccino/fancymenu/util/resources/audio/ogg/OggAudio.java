package de.keksuccino.fancymenu.util.resources.audio.ogg;

import com.mojang.blaze3d.audio.OggAudioStream;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resources.audio.ogg.base.OggAudioBuffer;
import de.keksuccino.fancymenu.util.resources.audio.ogg.base.OggAudioClip;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class OggAudio implements IAudio {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    protected volatile OggAudioClip clip;
    @Nullable
    protected volatile OggAudioBuffer audioBuffer;
    protected ResourceLocation sourceLocation;
    protected File sourceFile;
    protected String sourceURL;
    protected volatile boolean decoded = false;
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

        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isPresent()) {
                InputStream in = resource.get().open();
                of(in, location.toString(), audio);
            }
        } catch (Exception ex) {
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
            LOGGER.error("[FANCYMENU] Failed to read OGG audio from file! File not found: " + oggAudioFile.getPath());
            return audio;
        }

        try {
            InputStream in = new FileInputStream(oggAudioFile);
            of(in, oggAudioFile.getPath(), audio);
        } catch (Exception ex) {
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
            LOGGER.error("[FANCYMENU] Failed to read OGG audio from URL! Invalid URL: " + oggAudioURL);
            return audio;
        }

        new Thread(() -> {
            try {
                InputStream in = WebUtils.openResourceStream(oggAudioURL);
                if (in == null) throw new NullPointerException("Web resource input stream was NULL!");
                of(in, oggAudioURL, audio);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to read OGG audio from URL: " + oggAudioURL, ex);
            }
        }).start();

        return audio;

    }

    @NotNull
    public static OggAudio of(@NotNull InputStream in, @Nullable String oggAudioName, @Nullable OggAudio writeTo) {

        String name = (oggAudioName != null) ? oggAudioName : "[Generic InputStream Source]";
        OggAudio audio = (writeTo != null) ? writeTo : new OggAudio();

        try {
            audio.clip = OggAudioClip.create();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read OGG audio! Failed to create clip: " + name, ex);
            return audio;
        }

        OggAudioClip clip = audio.clip;
        if (clip == null) {
            LOGGER.error("[FANCYMENU] Failed to read OGG audio! Clip was NULL: " + name);
            return audio;
        }

        new Thread(() -> {
            OggAudioStream stream = null;
            try {
                stream = new OggAudioStream(in);
                ByteBuffer byteBuffer = stream.readAll();
                OggAudioBuffer audioBuffer = new OggAudioBuffer(byteBuffer, stream.getFormat());
                audio.audioBuffer = audioBuffer;
                clip.setBuffer(audioBuffer);
                audio.decoded = true;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to read OGG audio: " + name, ex);
            }
            CloseableUtils.closeQuietly(stream);
            CloseableUtils.closeQuietly(in);
        }).start();

        return audio;

    }

    @NotNull
    public static OggAudio of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    protected OggAudio() {
    }

    protected void forClip(@NotNull Consumer<OggAudioClip> clip) {
        OggAudioClip cached = this.clip;
        if (cached != null) clip.accept(cached);
    }

    @Nullable
    public OggAudioClip getClip() {
        return this.clip;
    }

    @Override
    public void play() {
        this.forClip(OggAudioClip::play);
    }

    @Override
    public boolean isPlaying() {
        OggAudioClip cached = this.clip;
        return (cached != null) && cached.isPlaying();
    }

    @Override
    public void pause() {
        this.forClip(OggAudioClip::pause);
    }

    @Override
    public boolean isPaused() {
        OggAudioClip cached = this.clip;
        return (cached != null) && cached.isPaused();
    }

    @Override
    public void stop() {
        this.forClip(OggAudioClip::stop);
    }

    @Override
    public void setVolume(float volume) {
        this.forClip(oggAudioClip -> oggAudioClip.setVolume(volume));
    }

    @Override
    public float getVolume() {
        OggAudioClip cached = this.clip;
        return (cached != null) ? cached.getVolume() : 0.0F;
    }

    public void setSoundChannel(@NotNull SoundSource channel) {
        this.forClip(oggAudioClip -> oggAudioClip.setSoundChannel(channel));
    }

    @NotNull
    public SoundSource getSoundChannel() {
        OggAudioClip cached = this.clip;
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

    public boolean isClipLoaded() {
        OggAudioClip cached = this.clip;
        return (cached != null) && cached.isLoadedInOpenAL();
    }

    @Override
    public void close() {
        this.closed = true;
        try {
            OggAudioClip cachedClip = this.clip;
            if (cachedClip != null) cachedClip.close();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to close OGG audio clip!", ex);
        }
        this.clip = null;
        try {
            OggAudioBuffer cachedBuffer = this.audioBuffer;
            if (cachedBuffer != null) cachedBuffer.delete();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to delete OGG audio buffer!", ex);
        }
        this.audioBuffer = null;
        this.decoded = false;
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

}
