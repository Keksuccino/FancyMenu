
package de.keksuccino.fancymenu.customization.element.elements.playerentity;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.resources.WebTextureResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class CapeWebTextureResourceLocation extends WebTextureResourceLocation {

    private static final Logger LOGGER = LogManager.getLogger();

    private volatile String url;
    private volatile ResourceLocation location;
    private volatile boolean loaded = false;
    private volatile int width = 0;
    private volatile int height = 0;
    private volatile NativeImage downloadedTexture = null;

    public CapeWebTextureResourceLocation(String url) {
        super(url);
        this.url = url;
    }

    @Nullable
    public NativeImage getDownloadedTexture() {
        return this.downloadedTexture;
    }

    public void downloadTexture() {
        try {
            URL u = new URL(this.url);
            HttpURLConnection httpcon = (HttpURLConnection) u.openConnection();
            httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");
            InputStream s = httpcon.getInputStream();
            if (s == null) {
                return;
            }
            NativeImage i = NativeImage.read(s);
            this.width = i.getWidth();
            this.height = i.getHeight();
            this.downloadedTexture = i;
            IOUtils.closeQuietly(s);
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Can't download texture '" + this.url + "'!");
            e.printStackTrace();
        }
    }

    public void loadTexture() {
        if (this.loaded) {
            return;
        }
        try {
            if (Minecraft.getInstance().getTextureManager() == null) {
                LOGGER.error("[FANCYMENU] Can't load texture '" + this.url + "'! Minecraft TextureManager instance not ready yet!");
                return;
            }
            if (this.downloadedTexture == null) {
                this.downloadTexture();
            }
            if (this.downloadedTexture != null) {
                location = Minecraft.getInstance().getTextureManager().register(filterUrl(this.url), new DynamicTexture(this.downloadedTexture));
                loaded = true;
            } else {
                LOGGER.error("[FANCYMENU] Can't load texture! Downloaded texture is NULL!");
            }
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Can't load texture '" + this.url + "'! Failed to register texture!");
            loaded = false;
            e.printStackTrace();
        }
    }

    public ResourceLocation getResourceLocation() {
        return this.location;
    }

    public String getURL() {
        return this.url;
    }

    public boolean isReady() {
        return this.loaded;
    }

    public int getHeight() {
        return this.height;
    }

    public int getWidth() {
        return this.width;
    }

    private String filterUrl(String url) {
        CharacterFilter c = new CharacterFilter();
        c.addAllowedCharacters("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n",
                "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".");
        return c.filterForAllowedChars(url.toLowerCase());
    }

}
