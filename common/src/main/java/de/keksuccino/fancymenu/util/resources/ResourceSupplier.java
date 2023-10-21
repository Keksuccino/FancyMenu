package de.keksuccino.fancymenu.util.resources;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resources.text.IText;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resources.video.IVideo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

@SuppressWarnings("unused")
public class ResourceSupplier<T extends Resource> {

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
    public static ResourceSupplier<IText> text(@NotNull String source) {
        return new ResourceSupplier<>(IText.class, FileMediaType.TEXT, source);
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
                ResourceHandler<?,?> handler = this.getResourceHandler();
                if (handler != null) {
                    this.current = (T) handler.get(getterSource);
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to get resource: " + getterSource + " (" + this.source + ")", ex);
            }
        }
        return this.current;
    }

    @Nullable
    public ResourceHandler<?,?> getResourceHandler() {
        if (this.mediaType == FileMediaType.IMAGE) return ResourceHandlers.getImageHandler();
        if (this.mediaType == FileMediaType.AUDIO) return ResourceHandlers.getAudioHandler();
        if (this.mediaType == FileMediaType.VIDEO) return ResourceHandlers.getVideoHandler();
        if (this.mediaType == FileMediaType.TEXT) return ResourceHandlers.getTextHandler();
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
    public ResourceSourceType getResourceSourceType() {
        return ResourceSourceType.getSourceTypeOf(PlaceholderParser.replacePlaceholders(this.source, false));
    }

    /**
     * The source without its {@link ResourceSourceType} prefix.<br>
     * Should <b>NOT</b> be used for saving/serializing the source! For saving, use {@link ResourceSupplier#getSourceWithPrefix()} instead!
     */
    @NotNull
    public String getSourceWithoutPrefix() {
        return ResourceSourceType.getWithoutSourcePrefix(this.source);
    }

    /**
     * The source with its {@link ResourceSourceType} prefix.<br>
     * This should be used for saving/serializing the source.
     */
    @NotNull
    public String getSourceWithPrefix() {
        if (ResourceSourceType.hasSourcePrefix(this.source)) return this.source;
        return this.getResourceSourceType().getSourcePrefix() + this.source;
    }

    public void setSource(@NotNull String source) {
        this.source = Objects.requireNonNull(source);
    }

}
