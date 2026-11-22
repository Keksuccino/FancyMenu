package de.keksuccino.fancymenu.util.resource;

import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.video.IVideo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.Closeable;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Supplies a {@link Resource} that only its owner uses.<br>
 * Unlike a {@link ResourceSupplier}, it never gets its resource from the shared cache of a {@link ResourceHandler}, but has the
 * handler create one of its own ({@link ResourceHandler#createOwned(ResourceSource)}). So the owner can play, pause, seek and close it
 * without affecting anything else that uses the same source, like the elements of the current screen.<br><br>
 *
 * Made for short-lived resources, like the previews of file browsers. The owner has to call {@link #close()} once it no longer needs the resource.
 */
public class OwnedResourceSupplier<R extends Resource> implements Closeable {

    @NotNull
    private final Supplier<? extends ResourceHandler<R, ?>> handlerSupplier;
    @NotNull
    private final String source;
    @Nullable
    private R current;
    private boolean creationAttempted = false;
    private boolean closed = false;

    /**
     * Returns a new {@link OwnedResourceSupplier} for an image source.
     *
     * @param source Can be a URL to a web resource, a path to a local resource or a ResourceLocation (namespace:path). Placeholders don't get replaced.
     */
    @NotNull
    public static OwnedResourceSupplier<ITexture> image(@NotNull String source) {
        return new OwnedResourceSupplier<>(ResourceHandlers::getImageHandler, source);
    }

    /**
     * Returns a new {@link OwnedResourceSupplier} for an audio source.
     *
     * @param source Can be a URL to a web resource, a path to a local resource or a ResourceLocation (namespace:path). Placeholders don't get replaced.
     */
    @NotNull
    public static OwnedResourceSupplier<IAudio> audio(@NotNull String source) {
        return new OwnedResourceSupplier<>(ResourceHandlers::getAudioHandler, source);
    }

    /**
     * Returns a new {@link OwnedResourceSupplier} for a video source.
     *
     * @param source Can be a URL to a web resource, a path to a local resource or a ResourceLocation (namespace:path). Placeholders don't get replaced.
     */
    @NotNull
    public static OwnedResourceSupplier<IVideo> video(@NotNull String source) {
        return new OwnedResourceSupplier<>(ResourceHandlers::getVideoHandler, source);
    }

    /**
     * Returns a new {@link OwnedResourceSupplier} for a text source.
     *
     * @param source Can be a URL to a web resource, a path to a local resource or a ResourceLocation (namespace:path). Placeholders don't get replaced.
     */
    @NotNull
    public static OwnedResourceSupplier<IText> text(@NotNull String source) {
        return new OwnedResourceSupplier<>(ResourceHandlers::getTextHandler, source);
    }

    /**
     * @param handlerSupplier Supplies the {@link ResourceHandler} that creates the resource, when it gets created.
     * @param source The source of the resource.
     */
    public OwnedResourceSupplier(@NotNull Supplier<? extends ResourceHandler<R, ?>> handlerSupplier, @NotNull String source) {
        this.handlerSupplier = Objects.requireNonNull(handlerSupplier);
        this.source = Objects.requireNonNull(source);
    }

    /**
     * Creates the resource on the first call and returns it on the following ones.<br>
     * When something else closes the resource, like the sound engine reload that closes all audios, or the resource itself to retry
     * a load OpenAL wasn't ready for, the next call creates a new one. A resource that can't get created only gets tried once.
     *
     * @return The resource, or NULL if it can't get created for the source or this supplier got closed.
     */
    @Nullable
    public R get() {
        if (this.closed) return null;
        if ((this.current != null) && this.current.isClosed()) {
            this.current = null;
            this.creationAttempted = false;
        }
        if (!this.creationAttempted) {
            this.creationAttempted = true;
            // Same check as ResourceSupplier#get(), which explains why sources without a payload never get dispatched
            if (ResourceSource.isDispatchable(this.source)) {
                this.current = this.handlerSupplier.get().createOwned(ResourceSource.of(this.source));
            }
        }
        return this.current;
    }

    /**
     * Closes the resource, after which this supplier never creates one again.
     */
    @Override
    public void close() {
        this.closed = true;
        R resource = this.current;
        this.current = null;
        CloseableUtils.closeQuietly(resource);
    }

}
