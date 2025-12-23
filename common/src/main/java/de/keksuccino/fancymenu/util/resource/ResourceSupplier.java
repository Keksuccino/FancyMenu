package de.keksuccino.fancymenu.util.resource;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.video.IVideo;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class ResourceSupplier<R extends Resource> {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    protected String source;
    @NotNull
    protected Class<R> resourceType;
    @NotNull
    FileMediaType mediaType;
    @Nullable
    protected R current;
    @Nullable
    protected String lastGetterSource;
    @Nullable
    protected Consumer<R> onUpdateCurrent = null;
    protected boolean empty = false;

    /**
     * Returns a dummy-like {@link ResourceSupplier} that will never return a {@link Resource}, it will only return NULL.<br>
     * Empty {@link ResourceSupplier}s can be identified by calling {@link ResourceSupplier#isEmpty()}.
     */
    @NotNull
    public static <R extends Resource> ResourceSupplier<R> empty(@NotNull Class<R> resourceType, @NotNull FileMediaType mediaType) {
        ResourceSupplier<R> supplier = new ResourceSupplier<>(resourceType, mediaType, "");
        supplier.empty = true;
        return supplier;
    }

    /**
     * Returns a new {@link ResourceSupplier} for an image source.
     *
     * @param source Can be a URL to a web resource, a path to a local resource or a Identifier (namespace:path).
     *               Sources support placeholders and the {@link ResourceSupplier} will update itself when the placeholders change.
     */
    @NotNull
    public static ResourceSupplier<ITexture> image(@NotNull String source) {
        return new ResourceSupplier<>(ITexture.class, FileMediaType.IMAGE, source);
    }

    /**
     * Returns a new {@link ResourceSupplier} for an audio source.
     *
     * @param source Can be a URL to a web resource, a path to a local resource or a Identifier (namespace:path).
     *               Sources support placeholders and the {@link ResourceSupplier} will update itself when the placeholders change.
     */
    @NotNull
    public static ResourceSupplier<IAudio> audio(@NotNull String source) {
        return new ResourceSupplier<>(IAudio.class, FileMediaType.AUDIO, source)
                .setOnUpdateResourceTask(PlayableResource::stop);
    }

    /**
     * Returns a new {@link ResourceSupplier} for a video source.
     *
     * @param source Can be a URL to a web resource, a path to a local resource or a Identifier (namespace:path).
     *               Sources support placeholders and the {@link ResourceSupplier} will update itself when the placeholders change.
     */
    @NotNull
    public static ResourceSupplier<IVideo> video(@NotNull String source) {
        return new ResourceSupplier<>(IVideo.class, FileMediaType.VIDEO, source);
    }

    /**
     * Returns a new {@link ResourceSupplier} for a text source.
     *
     * @param source Can be a URL to a web resource, a path to a local resource or a Identifier (namespace:path).
     *               Sources support placeholders and the {@link ResourceSupplier} will update itself when the placeholders change.
     */
    @NotNull
    public static ResourceSupplier<IText> text(@NotNull String source) {
        return new ResourceSupplier<>(IText.class, FileMediaType.TEXT, source);
    }

    public ResourceSupplier(@NotNull Class<R> resourceType, @NotNull FileMediaType mediaType, @NotNull String source) {
        this.source = Objects.requireNonNull(source);
        this.resourceType = Objects.requireNonNull(resourceType);
        this.mediaType = Objects.requireNonNull(mediaType);
    }

    @SuppressWarnings("all")
    @Nullable
    public R get() {
        if (this.empty) return null;
        if ((this.current != null) && this.current.isClosed()) {
            this.current = null;
        }
        String getterSource = PlaceholderParser.replacePlaceholders(this.source, false);
        if (!getterSource.equals(this.lastGetterSource)) {
            if ((this.onUpdateCurrent != null) && (this.current != null)) {
                this.onUpdateCurrent.accept(this.current);
            }
            this.current = null;
        }
        this.lastGetterSource = getterSource;
        if (this.current == null) {
            ResourceSource resourceSource = ResourceSource.of(getterSource);
            try {
                ResourceHandler<?,?> handler = this.getResourceHandler();
                if (handler != null) {
                    this.current = (R) handler.get(resourceSource);
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] ResourceSupplier failed to get resource: " + resourceSource + " (" + this.source + ")", ex);
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

    /**
     * Only works if this {@link ResourceSupplier}'s resource type is a {@link RenderableResource}.<br>
     * The {@link BiConsumer}'s {@link Resource} and {@link ResourceLocation} is never NULL!<br><br>
     *
     * The {@link BiConsumer}'s {@link ResourceLocation} is the {@link RenderableResource}'s
     * current {@link ResourceLocation} ({@link RenderableResource#getResourceLocation()}).
     * You should always use the provided location instead of calling {@link RenderableResource#getResourceLocation()},
     * because some types of resources asynchronously change that method's return value.
     */
    public void forRenderable(@NotNull BiConsumer<R, ResourceLocation> task) {
        R resource = this.get();
        if (resource instanceof RenderableResource r) {
            Identifier loc = r.getResourceLocation();
            if (loc != null) task.accept(resource, loc);
        }
    }

    @NotNull
    public Class<R> getResourceType() {
        return this.resourceType;
    }

    @NotNull
    public FileMediaType getMediaType() {
        return this.mediaType;
    }

    @NotNull
    public ResourceSourceType getSourceType() {
        if (this.empty) return ResourceSourceType.LOCAL;
        return ResourceSourceType.getSourceTypeOf(PlaceholderParser.replacePlaceholders(this.source, false));
    }

    /**
     * The source without its {@link ResourceSourceType} prefix.<br>
     * Should <b>NOT</b> be used for saving/serializing the source! For saving, use {@link ResourceSupplier#getSourceWithPrefix()} instead!
     */
    @NotNull
    public String getSourceWithoutPrefix() {
        if (this.empty) return "";
        return ResourceSourceType.getWithoutSourcePrefix(this.source);
    }

    /**
     * The source with its {@link ResourceSourceType} prefix.<br>
     * This should be used for saving/serializing the source.
     */
    @NotNull
    public String getSourceWithPrefix() {
        if (this.empty) return "";
        if (ResourceSourceType.hasSourcePrefix(this.source)) return this.source;
        return this.getSourceType().getSourcePrefix() + this.source;
    }

    public void setSource(@NotNull String source) {
        if (this.empty) return;
        this.source = Objects.requireNonNull(source);
    }

    /**
     * Set a task that should get executed when updating the {@link ResourceSupplier#current} resource to a new one.<br>
     * The consumer contains the old resource that is about to get replaced.
     */
    public ResourceSupplier<R> setOnUpdateResourceTask(@Nullable Consumer<R> oldResourceConsumer) {
        this.onUpdateCurrent = oldResourceConsumer;
        return this;
    }

    public boolean isEmpty() {
        return this.empty;
    }

}
