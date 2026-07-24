package de.keksuccino.fancymenu.util.resource.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.util.CloseableUtils;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks one owned image as it moves from decoder output into a registered texture-manager entry. The lifecycle lock is
 * important: resource reload can close an image while a render call is trying to register it, and either ordering must
 * leave exactly one owner responsible for cleanup. Once registered, the texture manager is the sole owner and release is
 * dispatched to the client thread; closing both the DynamicTexture and its NativeImage separately would double-dispose it.
 */
final class TextureManagerEntry<I extends AutoCloseable, T extends AutoCloseable> implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();

    private final BiFunction<ResourceLocation, I, T> textureFactory;
    private final BiConsumer<ResourceLocation, T> textureRegistrar;
    private final Consumer<ResourceLocation> textureReleaser;
    private final Function<T, I> imageGetter;
    private final Consumer<Runnable> releaseDispatcher;
    private final Object lifecycleLock = new Object();

    @Nullable
    private I decodedImage;
    @Nullable
    private T registeredTexture;
    @Nullable
    private ResourceLocation registeredIdentifier;
    private boolean registrationAttempted;
    private boolean registered;
    private boolean closed;

    TextureManagerEntry(@NotNull BiFunction<ResourceLocation, I, T> textureFactory, @NotNull BiConsumer<ResourceLocation, T> textureRegistrar, @NotNull Consumer<ResourceLocation> textureReleaser, @NotNull Function<T, I> imageGetter, @NotNull Consumer<Runnable> releaseDispatcher) {
        this.textureFactory = Objects.requireNonNull(textureFactory);
        this.textureRegistrar = Objects.requireNonNull(textureRegistrar);
        this.textureReleaser = Objects.requireNonNull(textureReleaser);
        this.imageGetter = Objects.requireNonNull(imageGetter);
        this.releaseDispatcher = Objects.requireNonNull(releaseDispatcher);
    }

    @NotNull
    static TextureManagerEntry<NativeImage, DynamicTexture> dynamicTexture() {
        return new TextureManagerEntry<>((identifier, image) -> new DynamicTexture(image), (identifier, texture) -> Minecraft.getInstance().getTextureManager().register(identifier, texture), identifier -> Minecraft.getInstance().getTextureManager().release(identifier), DynamicTexture::getPixels, TextureManagerReleaseDispatcher::dispatch);
    }

    boolean adopt(@NotNull I image) {
        Objects.requireNonNull(image);
        boolean duplicate = false;
        synchronized (this.lifecycleLock) {
            if (!this.closed) {
                if ((this.decodedImage == null) && (this.registeredTexture == null)) {
                    this.decodedImage = image;
                    return true;
                }
                duplicate = true;
            }
        }
        CloseableUtils.closeQuietly(image);
        if (duplicate) throw new IllegalStateException("Texture manager entry already owns an image");
        return false;
    }

    @Nullable
    ResourceLocation register(@NotNull ResourceLocation identifier) {
        Objects.requireNonNull(identifier);
        synchronized (this.lifecycleLock) {
            if (this.closed || this.registrationAttempted || (this.decodedImage == null)) return this.registeredIdentifier;
            this.registrationAttempted = true;
            I image = this.decodedImage;
            T texture = null;
            try {
                texture = this.textureFactory.apply(identifier, image);
                this.decodedImage = null;
                this.textureRegistrar.accept(identifier, texture);
                this.registeredTexture = texture;
                this.registeredIdentifier = identifier;
                this.registered = true;
                return identifier;
            } catch (RuntimeException | Error throwable) {
                this.decodedImage = null;
                CloseableUtils.closeQuietly((texture != null) ? texture : image);
                throw throwable;
            }
        }
    }

    @Nullable
    I getImage() {
        synchronized (this.lifecycleLock) {
            if (this.decodedImage != null) return this.decodedImage;
            return (this.registeredTexture != null) ? this.imageGetter.apply(this.registeredTexture) : null;
        }
    }

    @Nullable
    ResourceLocation getIdentifier() {
        synchronized (this.lifecycleLock) {
            return this.registeredIdentifier;
        }
    }

    boolean canRegister() {
        synchronized (this.lifecycleLock) {
            return !this.closed && !this.registrationAttempted && (this.decodedImage != null);
        }
    }

    boolean isClosed() {
        synchronized (this.lifecycleLock) {
            return this.closed;
        }
    }

    @Override
    public void close() {
        I image;
        T texture;
        ResourceLocation identifier;
        boolean releaseRegisteredTexture;
        synchronized (this.lifecycleLock) {
            if (this.closed) return;
            this.closed = true;
            image = this.decodedImage;
            texture = this.registeredTexture;
            identifier = this.registeredIdentifier;
            releaseRegisteredTexture = this.registered;
            this.decodedImage = null;
            this.registeredTexture = null;
            this.registeredIdentifier = null;
            this.registered = false;
        }
        if (image != null) CloseableUtils.closeQuietly(image);
        if ((texture == null) && (identifier == null)) return;
        this.releaseDispatcher.accept(() -> this.releaseRegisteredTexture(identifier, texture, releaseRegisteredTexture));
    }

    private void releaseRegisteredTexture(@Nullable ResourceLocation identifier, @Nullable T texture, boolean releaseRegisteredTexture) {
        if (releaseRegisteredTexture && (identifier != null)) {
            try {
                this.textureReleaser.accept(identifier);
                return;
            } catch (Throwable throwable) {
                LOGGER.error("[FANCYMENU] Failed to release dynamic texture-manager entry: {}", identifier, throwable);
            }
        }
        CloseableUtils.closeQuietly(texture);
    }

}
