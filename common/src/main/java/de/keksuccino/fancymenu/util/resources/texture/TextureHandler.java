package de.keksuccino.fancymenu.util.resources.texture;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.konkrete.rendering.animation.ExternalGifAnimationRenderer;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class TextureHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final TextureHandler INSTANCE = new TextureHandler();

    private final Map<String, ITexture> textures = new HashMap<>();
    private final Map<String, ExternalGifAnimationRenderer> gifs = new HashMap<>();

    @Nullable
    public ITexture getTexture(@NotNull String path) {
        File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(path));
        return this.getTexture(f);
    }

    @Nullable
    public ITexture getTexture(@NotNull File file) {
        if (!textures.containsKey(file.getAbsolutePath())) {
            if (file.exists() && file.isFile()) {
                ITexture t;
                if (file.getPath().toLowerCase().endsWith(".gif")) {
                    t = GifTexture.local(file);
                } else {
                    t = LocalTexture.of(file.getAbsolutePath());
                }
                textures.put(file.getAbsolutePath(), t);
                return t;
            } else {
                return null;
            }
        } else {
            return textures.get(file.getAbsolutePath());
        }
    }

    /**
     * Supports PNG, JPEG and GIF.<br>
     * In case of PNG or JPEG, a new {@link WebTexture} will get wrapped into a new {@link WrappedWebTexture}.<br>
     * In case of GIF, a new {@link GifTexture} will get wrapped into a new {@link WrappedWebTexture}.<br><br>
     *
     * It is possible that this method returns different texture types than {@link WrappedWebTexture}.
     *
     * @param url The image URL.
     */
    @NotNull
    public ITexture getWebTexture(@NotNull String url) {
        return getWebTexture(url, true);
    }

    /**
     * Supports PNG, JPEG and GIF.<br>
     * In case of PNG or JPEG, a new {@link WebTexture} will get wrapped into a new {@link WrappedWebTexture}.<br>
     * In case of GIF, a new {@link GifTexture} will get wrapped into a new {@link WrappedWebTexture}.<br><br>
     *
     * It is possible that this method returns different texture types than {@link WrappedWebTexture}.
     *
     * @param url The image URL.
     * @param autoLoadPngAndJpeg If PNG and JPEG images should automatically (asynchronously) load. GIF images will always load automatically.
     */
    @NotNull
    public ITexture getWebTexture(@NotNull String url, boolean autoLoadPngAndJpeg) {
        if (!textures.containsKey(url)) {
            WrappedWebTexture t = WrappedWebTexture.of(url, autoLoadPngAndJpeg);
            this.textures.put(url, t);
            return t;
        } else {
            return textures.get(url);
        }
    }

    /**
     * Supports PNG and JPEG.<br>
     * For GIFs, use {@link TextureHandler#getWebTexture(String, boolean)} instead!
     *
     * @param autoLoad If the texture should automatically (asynchronously) load.
     */
    @NotNull
    public ITexture getSimpleWebTexture(@NotNull String url, boolean autoLoad) {
        if (!textures.containsKey(url)) {
            WebTexture texture = WebTexture.of(url, autoLoad);
            this.textures.put(url, texture);
            return texture;
        } else {
            return textures.get(url);
        }
    }

    /** Will not register the texture! Only returns textures that are already registered and loaded! **/
    @Nullable
    public ITexture getLoadedTextureByResourceLocation(ResourceLocation location) {
        for (ITexture t : this.textures.values()) {
            if (t.isReady() && (t.getResourceLocation() == location)) {
                return t;
            }
        }
        return null;
    }

    @Deprecated
    @Nullable
    public ExternalGifAnimationRenderer getGifTexture(@NotNull String path) {
        File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(path));
        if (!gifs.containsKey(f.getAbsolutePath())) {
            if (f.exists() && f.isFile() && f.getPath().toLowerCase().replace(" ", "").endsWith(".gif")) {
                ExternalGifAnimationRenderer r = new ExternalGifAnimationRenderer(f.getPath(), true, 0, 0, 0, 0);
                r.prepareAnimation();
                gifs.put(f.getAbsolutePath(), r);
                return r;
            } else {
                return null;
            }
        } else {
            return gifs.get(f.getAbsolutePath());
        }
    }

    public void removeResource(@NotNull String pathOrUrl) {
        File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(pathOrUrl));
        textures.remove(f.getAbsolutePath());
        textures.remove(pathOrUrl);
    }

    public void clearResources() {
        this.textures.clear();
        for (ExternalGifAnimationRenderer g : this.gifs.values()) {
            g.setLooped(false);
            g.resetAnimation();
        }
        this.gifs.clear();
    }

    public static boolean isGifUrl(@NotNull String gifUrl) {
        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(gifUrl)) return false;
        if (gifUrl.toLowerCase().endsWith(".gif")) return true;
        if (gifUrl.toLowerCase().endsWith(".gif/")) return true;
        try {
            URL url = new URL(gifUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String mimeType = connection.getContentType();
            connection.disconnect();
            if ((mimeType != null) && mimeType.equalsIgnoreCase("image/gif")) return true;
        } catch (Exception ignore) {}
        return false;
    }

}
