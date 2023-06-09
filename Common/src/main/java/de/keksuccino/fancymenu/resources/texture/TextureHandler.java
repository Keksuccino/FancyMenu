package de.keksuccino.fancymenu.resources.texture;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.konkrete.rendering.animation.ExternalGifAnimationRenderer;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TextureHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final TextureHandler INSTANCE = new TextureHandler();

    private final Map<String, ITexture> textures = new HashMap<>();
    private final Map<String, ExternalGifAnimationRenderer> gifs = new HashMap<>();

    @Nullable
    public LocalTexture getTexture(String path) {
        File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(path));
        return this.getTexture(f);
    }

    @Nullable
    public LocalTexture getTexture(File file) {
        if (!textures.containsKey(file.getAbsolutePath())) {
            if (file.exists() && file.isFile()) {
                LocalTexture t = LocalTexture.of(file.getAbsolutePath());
                textures.put(file.getAbsolutePath(), t);
                return t;
            } else {
                return null;
            }
        } else {
            return (LocalTexture) textures.get(file.getAbsolutePath());
        }
    }

    @Nullable
    public WebTexture getWebTexture(String url) {
        return getWebTexture(url, true);
    }

    @Nullable
    public WebTexture getWebTexture(String url, boolean autoLoadTextureAsynchronously) {
        if (!textures.containsKey(url)) {
            WebTexture t = WebTexture.of(url, autoLoadTextureAsynchronously);
            this.textures.put(url, t);
            return t;
        } else {
            return (WebTexture) textures.get(url);
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

    @Nullable
    public ExternalGifAnimationRenderer getGifTexture(String path) {
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

    public void removeResource(String pathOrUrl) {
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

}
