package de.keksuccino.fancymenu.testing;

import de.keksuccino.fancymenu.util.file.type.FileCodec;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.types.AudioFileType;
import de.keksuccino.fancymenu.util.file.type.types.ImageFileType;
import de.keksuccino.fancymenu.util.file.type.types.TextFileType;
import de.keksuccino.fancymenu.util.file.type.types.VideoFileType;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resource.Resource;
import de.keksuccino.fancymenu.util.resource.ResourceHandler;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.video.IVideo;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A real {@link ResourceHandler} with one file type, whose codec creates {@link FakeResource}s instead of reading files.<br>
 * Its file type detection, cache and lifecycle are the ones of the production handler, so tests can check how code shares, owns and
 * releases resources without OpenAL, textures or video players.
 */
public final class FakeResourceHandler<R extends Resource, F extends FileType<R>> extends ResourceHandler<R, F> {

    @NotNull
    private final Class<R> resourceType;
    @NotNull
    private final F fileType;
    private final List<FakeResource> createdResources = new ArrayList<>();
    private int codecReads = 0;
    private boolean codecReturnsNull = false;

    @NotNull
    public static FakeResourceHandler<IAudio, AudioFileType> audio(@NotNull String extension) {
        return new FakeResourceHandler<>(IAudio.class, codec -> new AudioFileType(codec, null, extension));
    }

    @NotNull
    public static FakeResourceHandler<ITexture, ImageFileType> image(@NotNull String extension) {
        return new FakeResourceHandler<>(ITexture.class, codec -> new ImageFileType(codec, null, extension));
    }

    @NotNull
    public static FakeResourceHandler<IVideo, VideoFileType> video(@NotNull String extension) {
        return new FakeResourceHandler<>(IVideo.class, codec -> new VideoFileType(codec, null, extension));
    }

    @NotNull
    public static FakeResourceHandler<IText, TextFileType> text(@NotNull String extension) {
        return new FakeResourceHandler<>(IText.class, codec -> new TextFileType(codec, null, extension));
    }

    private FakeResourceHandler(@NotNull Class<R> resourceType, @NotNull Function<FileCodec<R>, F> fileTypeFactory) {
        this.resourceType = Objects.requireNonNull(resourceType);
        this.fileType = fileTypeFactory.apply(new FakeCodec());
    }

    @Override
    public @NotNull List<F> getAllowedFileTypes() {
        return List.of(this.fileType);
    }

    @Override
    public @Nullable F getFallbackFileType() {
        return null;
    }

    /**
     * Every resource the codec created, in the order it created them, shared and owned ones.
     */
    @NotNull
    public List<FakeResource> getCreatedResources() {
        return this.createdResources;
    }

    public int countOpenResources() {
        int open = 0;
        for (FakeResource resource : this.createdResources) {
            if (!resource.isClosed()) open++;
        }
        return open;
    }

    /**
     * How many times the handler asked its codec for a resource, including the times the codec returned NULL.
     */
    public int getCodecReads() {
        return this.codecReads;
    }

    /**
     * Makes the codec return NULL, like a codec that can't read its source.
     */
    public void setCodecReturnsNull(boolean codecReturnsNull) {
        this.codecReturnsNull = codecReturnsNull;
    }

    private final class FakeCodec extends FileCodec<R> {

        @Override
        public @Nullable R read(@NotNull InputStream in) {
            return this.create();
        }

        @Override
        public @Nullable R readLocation(@NotNull Identifier location) {
            return this.create();
        }

        @Override
        public @Nullable R readLocal(@NotNull File file) {
            return this.create();
        }

        @Override
        public @Nullable R readWeb(@NotNull String fileUrl) {
            return this.create();
        }

        @Nullable
        private R create() {
            FakeResourceHandler.this.codecReads++;
            if (FakeResourceHandler.this.codecReturnsNull) return null;
            FakeResource resource = new FakeResource();
            FakeResourceHandler.this.createdResources.add(resource);
            return FakeResourceHandler.this.resourceType.cast(resource);
        }

    }

    /**
     * Stands in for every resource type: it can play like audios and videos, has a size like textures and lines like texts.
     */
    public static final class FakeResource implements IAudio, IVideo, ITexture, IText {

        private boolean playing = false;
        private boolean closed = false;
        private int closeCalls = 0;
        private float volume = 1.0F;
        @NotNull
        private SoundSource soundChannel = SoundSource.MASTER;

        public int getCloseCalls() {
            return this.closeCalls;
        }

        @Override
        public void play() {
            if (!this.closed) this.playing = true;
        }

        @Override
        public boolean isPlaying() {
            return this.playing;
        }

        @Override
        public void pause() {
            this.playing = false;
        }

        @Override
        public boolean isPaused() {
            return !this.playing;
        }

        @Override
        public void stop() {
            this.playing = false;
        }

        @Override
        public void setVolume(float volume) {
            this.volume = volume;
        }

        @Override
        public float getVolume() {
            return this.volume;
        }

        @Override
        public void setSoundChannel(@NotNull SoundSource channel) {
            this.soundChannel = channel;
        }

        @Override
        public @NotNull SoundSource getSoundChannel() {
            return this.soundChannel;
        }

        @Override
        public float getDuration() {
            return 1.0F;
        }

        @Override
        public float getPlayTime() {
            return 0.0F;
        }

        @Override
        public void setPlayTime(float playTime) {
        }

        @Override
        public @Nullable Identifier getResourceLocation() {
            return null;
        }

        @Override
        public int getWidth() {
            return 1;
        }

        @Override
        public int getHeight() {
            return 1;
        }

        @Override
        public @NotNull AspectRatio getAspectRatio() {
            return new AspectRatio(1, 1);
        }

        @Override
        public void reset() {
        }

        @Override
        public @Nullable List<String> getTextLines() {
            return this.closed ? null : List.of("fake");
        }

        @Override
        public @Nullable InputStream open() {
            return null;
        }

        @Override
        public boolean isReady() {
            return !this.closed;
        }

        @Override
        public boolean isLoadingCompleted() {
            return !this.closed;
        }

        @Override
        public boolean isLoadingFailed() {
            return false;
        }

        @Override
        public boolean isClosed() {
            return this.closed;
        }

        @Override
        public void close() {
            this.closeCalls++;
            this.closed = true;
            this.playing = false;
        }

    }

}
