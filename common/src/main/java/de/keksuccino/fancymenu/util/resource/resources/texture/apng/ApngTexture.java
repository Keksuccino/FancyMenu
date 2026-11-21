package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImage;
import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImageSource;
import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImageTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * An APNG (or plain PNG) image that plays its animation on a single texture. Playback, streaming and memory handling are shared with all animated images, see {@link AnimatedImageTexture}.
 */
public class ApngTexture extends AnimatedImageTexture {

    @NotNull
    public static ApngTexture location(@NotNull Identifier location) {
        return location(location, null);
    }

    @NotNull
    public static ApngTexture location(@NotNull Identifier location, @Nullable ApngTexture writeTo) {
        ApngTexture texture = (writeTo != null) ? writeTo : new ApngTexture();
        texture.loadFromLocation(location);
        return texture;
    }

    @NotNull
    public static ApngTexture local(@NotNull File apngFile) {
        return local(apngFile, null);
    }

    @NotNull
    public static ApngTexture local(@NotNull File apngFile, @Nullable ApngTexture writeTo) {
        ApngTexture texture = (writeTo != null) ? writeTo : new ApngTexture();
        texture.loadFromFile(apngFile);
        return texture;
    }

    @NotNull
    public static ApngTexture web(@NotNull String apngUrl) {
        return web(apngUrl, null);
    }

    @NotNull
    public static ApngTexture web(@NotNull String apngUrl, @Nullable ApngTexture writeTo) {
        ApngTexture texture = (writeTo != null) ? writeTo : new ApngTexture();
        texture.loadFromWeb(apngUrl);
        return texture;
    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static ApngTexture of(@NotNull InputStream in, @Nullable String apngTextureName, @Nullable ApngTexture writeTo) {
        ApngTexture texture = (writeTo != null) ? writeTo : new ApngTexture();
        texture.loadFromStream(in, apngTextureName);
        return texture;
    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static ApngTexture of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    protected ApngTexture() {
        super(ApngImage.FORMAT_NAME);
    }

    @Override
    protected @NotNull AnimatedImage readImage(@NotNull AnimatedImageSource source, @NotNull String sourceName) throws IOException {
        return ApngReader.read(source, sourceName);
    }

}
