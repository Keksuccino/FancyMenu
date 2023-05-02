package de.keksuccino.fancymenu.rendering.texture;

import de.keksuccino.konkrete.rendering.animation.ExternalGifAnimationRenderer;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.konkrete.resources.ITextureResourceLocation;
import de.keksuccino.konkrete.resources.WebTextureResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ExternalTextureHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final ExternalTextureHandler INSTANCE = new ExternalTextureHandler();

    private final Map<String, ITextureResourceLocation> textures = new HashMap<>();
    private final Map<String, ExternalGifAnimationRenderer> gifs = new HashMap<>();

    @Nullable
    public ExternalTextureResourceLocation getTexture(String path) {
        File f = new File(path);
        return this.getTexture(f);
    }

    @Nullable
    public ExternalTextureResourceLocation getTexture(File f) {
        if (!textures.containsKey(f.getAbsolutePath())) {
            if (f.exists() && f.isFile()) {
                ExternalTextureResourceLocation r = new ExternalTextureResourceLocation(f.getAbsolutePath());
                r.loadTexture();
                textures.put(f.getAbsolutePath(), r);
                return r;
            } else {
                return null;
            }
        } else {
            return (ExternalTextureResourceLocation) textures.get(f.getAbsolutePath());
        }
    }

    /** Will not register the texture! Only returns textures that are already registered and loaded! **/
    @Nullable
    public ExternalTextureResourceLocation getLoadedTextureByResourceLocation(ResourceLocation location) {
        for (ITextureResourceLocation r : this.textures.values()) {
            if ((r instanceof ExternalTextureResourceLocation) && r.isReady()) {
                if (r.getResourceLocation() == location) {
                    return (ExternalTextureResourceLocation) r;
                }
            }
        }
        return null;
    }

    @Nullable
    public WebTextureResourceLocation getWebTexture(String url) {
        return getWebTexture(url, true);
    }

    @Nullable
    public WebTextureResourceLocation getWebTexture(String url, boolean loadTexture) {
        if (!textures.containsKey(url)) {
            try {
                WebTextureResourceLocation r = new WebTextureResourceLocation(url);
                if (loadTexture) {
                    r.loadTexture();
                    if (!r.isReady()) {
                        return null;
                    }
                }
                textures.put(url, r);
                return r;
            } catch (Exception var3) {
                var3.printStackTrace();
                return null;
            }
        } else {
            return (WebTextureResourceLocation) textures.get(url);
        }
    }

    @Nullable
    public ExternalGifAnimationRenderer getGif(String path) {
        File f = new File(path);
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

    public void removeResource(String path) {
        File f = new File(path);
        textures.remove(f.getAbsolutePath());
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
