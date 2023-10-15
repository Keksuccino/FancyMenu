package de.keksuccino.fancymenu.util.resources;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.file.type.types.TextFileType;
import de.keksuccino.fancymenu.util.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resources.video.IVideo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

@SuppressWarnings("unused")
public class ResourceSupplier<T> {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    protected String source;
    @NotNull
    protected Class<T> resourceType;
    @NotNull
    FileMediaType mediaType;
    @Nullable
    protected T current;
    @Nullable
    protected String lastGetterSource;

    /**
     * Returns a new {@link ResourceSupplier} for an image source.
     *
     * @param source Can be a URL to a web resource, a path to a local resource or a ResourceLocation (namespace:path).
     *               Sources support placeholders and the {@link ResourceSupplier} will update itself when the placeholders change.
     */
    @NotNull
    public static ResourceSupplier<ITexture> image(@NotNull String source) {
        return new ResourceSupplier<>(ITexture.class, FileMediaType.IMAGE, source);
    }

    /**
     * Returns a new {@link ResourceSupplier} for an audio source.
     *
     * @param source Can be a URL to a web resource, a path to a local resource or a ResourceLocation (namespace:path).
     *               Sources support placeholders and the {@link ResourceSupplier} will update itself when the placeholders change.
     */
    @NotNull
    public static ResourceSupplier<IAudio> audio(@NotNull String source) {
        return new ResourceSupplier<>(IAudio.class, FileMediaType.AUDIO, source);
    }

    /**
     * Returns a new {@link ResourceSupplier} for a video source.
     *
     * @param source Can be a URL to a web resource, a path to a local resource or a ResourceLocation (namespace:path).
     *               Sources support placeholders and the {@link ResourceSupplier} will update itself when the placeholders change.
     */
    @NotNull
    public static ResourceSupplier<IVideo> video(@NotNull String source) {
        return new ResourceSupplier<>(IVideo.class, FileMediaType.VIDEO, source);
    }

    /**
     * Returns a new {@link ResourceSupplier} for a text source.
     *
     * @param source Can be a URL to a web resource, a path to a local resource or a ResourceLocation (namespace:path).
     *               Sources support placeholders and the {@link ResourceSupplier} will update itself when the placeholders change.
     */
    @NotNull
    public static ResourceSupplier<TextFileType.PlainText> text(@NotNull String source) {
        return new ResourceSupplier<>(TextFileType.PlainText.class, FileMediaType.TEXT, source);
    }

    protected ResourceSupplier(@NotNull Class<T> resourceType, @NotNull FileMediaType mediaType, @NotNull String source) {
        this.source = Objects.requireNonNull(source);
        this.resourceType = Objects.requireNonNull(resourceType);
        this.mediaType = Objects.requireNonNull(mediaType);
    }

    @SuppressWarnings("all")
    @Nullable
    public T get() {
        String getterSource = PlaceholderParser.replacePlaceholders(this.source, false);
        if (!getterSource.equals(this.lastGetterSource)) {
            this.current = null;
        }
        this.lastGetterSource = getterSource;
        if (this.current == null) {
            try {
                ConsumingSupplier<String, ?> factory = this.getResourceFactory();
                if (factory != null) {
                    this.current = (T) factory.get(getterSource);
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to get resource: " + this.source, ex);
            }
        }
        return this.current;
    }

    @Nullable
    public ConsumingSupplier<String, ?> getResourceFactory() {
        if (this.mediaType == FileMediaType.IMAGE) return ResourceFactories.getImageFactory();
        if (this.mediaType == FileMediaType.AUDIO) return ResourceFactories.getAudioFactory();
        if (this.mediaType == FileMediaType.VIDEO) return ResourceFactories.getVideoFactory();
        if (this.mediaType == FileMediaType.TEXT) return ResourceFactories.getTextFactory();
        return null;
    }

    @NotNull
    public Class<T> getResourceType() {
        return this.resourceType;
    }

    @NotNull
    public FileMediaType getMediaType() {
        return this.mediaType;
    }

    @NotNull
    public String getSource() {
        return this.source;
    }

    public void setSource(@NotNull String source) {
        this.source = Objects.requireNonNull(source);
    }

}
