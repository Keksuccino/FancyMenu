package de.keksuccino.fancymenu.util.resource.resources.texture.gif;

import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImage;
import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImageSource;
import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImageTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A GIF image (animated or not) that plays its animation on a single texture. Playback, streaming and memory handling are shared with all animated images, see {@link AnimatedImageTexture}.
 */
public class GifTexture extends AnimatedImageTexture {

    @NotNull
    public static GifTexture location(@NotNull ResourceLocation location) {
        return location(location, null);
    }

    @NotNull
    public static GifTexture location(@NotNull ResourceLocation location, @Nullable GifTexture writeTo) {
        GifTexture texture = (writeTo != null) ? writeTo : new GifTexture();
        texture.loadFromLocation(location);
        return texture;
    }

    @NotNull
    public static GifTexture local(@NotNull File gifFile) {
        return local(gifFile, null);
    }

    @NotNull
    public static GifTexture local(@NotNull File gifFile, @Nullable GifTexture writeTo) {
        GifTexture texture = (writeTo != null) ? writeTo : new GifTexture();
        texture.loadFromFile(gifFile);
        return texture;
    }

    @NotNull
    public static GifTexture web(@NotNull String gifUrl) {
        return web(gifUrl, null);
    }

    @NotNull
    public static GifTexture web(@NotNull String gifUrl, @Nullable GifTexture writeTo) {
        GifTexture texture = (writeTo != null) ? writeTo : new GifTexture();
        texture.loadFromWeb(gifUrl);
        return texture;
    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static GifTexture of(@NotNull InputStream in, @Nullable String gifTextureName, @Nullable GifTexture writeTo) {
        GifTexture texture = (writeTo != null) ? writeTo : new GifTexture();
        texture.loadFromStream(in, gifTextureName);
        return texture;
    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static GifTexture of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    protected GifTexture() {
        super(GifImage.FORMAT_NAME);
    }

    @Override
    protected @NotNull AnimatedImage readImage(@NotNull AnimatedImageSource source, @NotNull String sourceName) throws IOException {
        return GifReader.read(source, sourceName);
    }

}
