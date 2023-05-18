package de.keksuccino.fancymenu.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

import de.keksuccino.fancymenu.rendering.AspectRatio;
import de.keksuccino.fancymenu.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.resources.SelfcleaningDynamicTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WebTexture implements ITexture {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    private final String url;
    @Nullable
    private ResourceLocation textureLocation;
    private int width = 10;
    private int height = 10;
    private AspectRatio aspectRatio = new AspectRatio(10, 10);
    private volatile NativeImage nativeImage;
    private volatile boolean isLoading = false;

    /** Returns a new {@link WebTexture} instance and automatically asynchronously loads the texture. **/
    @NotNull
    public static WebTexture create(@NotNull String sourceURL) {
        Objects.requireNonNull(sourceURL);
        WebTexture t = new WebTexture(sourceURL);
        t.downloadAndLoadAsynchronously();
        return t;
    }

    /** Returns a new {@link WebTexture} instance. **/
    @NotNull
    public static WebTexture create(@NotNull String sourceURL, boolean autoLoadTextureAsynchronously) {
        Objects.requireNonNull(sourceURL);
        WebTexture t = new WebTexture(sourceURL);
        if (autoLoadTextureAsynchronously) {
            t.downloadAndLoadAsynchronously();
        }
        return t;
    }

    protected WebTexture(@NotNull String url) {
        this.url = url;
    }

    public void downloadTexture() {
        if (!this.isLoading) {
            this.downloadTextureInternal();
        } else {
            LOGGER.error("[FANCYMENU] Can't download WebTexture while it is already loading: " + this.url);
        }
    }

    protected void downloadTextureInternal() {
        InputStream input = null;
        try {
            URL actualURL = new URL(this.url);
            HttpURLConnection connection = (HttpURLConnection)actualURL.openConnection();
            connection.addRequestProperty("User-Agent", "Mozilla/4.0");
            input = connection.getInputStream();
            if (input != null) {
                this.nativeImage = NativeImage.read(input);
            }
        } catch (Exception ex) {
            this.nativeImage = null;
            LOGGER.error("[FANCYMENU] Failed to download WebTexture: " + this.url);
            ex.printStackTrace();
        }
        IOUtils.closeQuietly(input);
    }

    public void loadTexture() {
        if (!this.isLoading) {
            this.loadTextureInternal();
        } else {
            LOGGER.error("[FANCYMENU] Can't load WebTexture while it is already loading: " + this.url);
        }
    }

    protected void loadTextureInternal() {
        if ((this.nativeImage != null) && (this.textureLocation == null)) {
            try {
                this.textureLocation = Minecraft.getInstance().getTextureManager().register("fancymenu_web_texture", new SelfcleaningDynamicTexture(this.nativeImage));
                this.width = this.nativeImage.getWidth();
                this.height = this.nativeImage.getHeight();
                this.aspectRatio = new AspectRatio(this.width, this.height);
            } catch (Exception ex) {
                this.textureLocation = null;
                LOGGER.error("[FANCYMENU] Failed to load WebTexture: " + this.url);
                ex.printStackTrace();
            }
            this.nativeImage = null;
        }
    }

    public void downloadAndLoadAsynchronously() {
        if (!this.isLoading) {
            this.isLoading = true;
            new Thread(() -> {
                this.downloadTextureInternal();
                if (this.nativeImage != null) {
                    MainThreadTaskExecutor.executeInMainThread(() -> {
                        this.loadTextureInternal();
                        this.isLoading = false;
                    }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                } else {
                    this.isLoading = false;
                }
            }).start();
        }
    }

    @Nullable
    public ResourceLocation getResourceLocation() {
        return this.textureLocation;
    }

    @NotNull
    public String getURL() {
        return this.url;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    @NotNull
    public AspectRatio getAspectRatio() {
        return this.aspectRatio;
    }

    public boolean isReady() {
        return this.textureLocation != null;
    }

}

