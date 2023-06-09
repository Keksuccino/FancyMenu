package de.keksuccino.fancymenu.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.rendering.AspectRatio;
import de.keksuccino.konkrete.resources.SelfcleaningDynamicTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Objects;

public class LocalTexture implements ITexture {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    private ResourceLocation textureLocation;
    @Nullable
    private final String path;
    @Nullable
    private InputStream inputStream;
    private int width = 10;
    private int height = 10;
    private AspectRatio aspectRatio = new AspectRatio(10, 10);

    /** Returns a new {@link LocalTexture} instance. **/
    @NotNull
    public static LocalTexture of(@NotNull String path) {
        Objects.requireNonNull(path);
        LocalTexture t = new LocalTexture(path, null);
        t.loadTexture();
        return t;
    }

    /** Returns a new {@link LocalTexture} instance. **/
    @NotNull
    public static LocalTexture of(@NotNull InputStream inputStream) {
        Objects.requireNonNull(inputStream);
        LocalTexture t = new LocalTexture(null, inputStream);
        t.loadTexture();
        return t;
    }

    protected LocalTexture(@Nullable String path, @Nullable InputStream inputStream) {
        this.path = path;
        this.inputStream = inputStream;
    }

    protected void loadTexture() {
        NativeImage i = null;
        try {
            if ((this.path != null) || (this.inputStream != null)) {
                if (this.inputStream == null) {
                    File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(this.path));
                    this.inputStream = new FileInputStream(f);
                }
                i = NativeImage.read(this.inputStream);
                this.width = i.getWidth();
                this.height = i.getHeight();
                this.textureLocation = Minecraft.getInstance().getTextureManager().register("fancymenu_external_texture", new SelfcleaningDynamicTexture(i));
            }
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to load LocalTexture: " + this.path + " | " + this.textureLocation);
            e.printStackTrace();
            this.textureLocation = null;
        }
        try {
            if (i != null) {
                i.close();
            }
        } catch (Exception ignored) {}
        IOUtils.closeQuietly(this.inputStream);
        this.aspectRatio = new AspectRatio(this.width, this.height);
    }

    @Nullable
    public ResourceLocation getResourceLocation() {
        return textureLocation;
    }

    @Nullable
    public String getPath() {
        return this.path;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @NotNull
    public AspectRatio getAspectRatio() {
        return this.aspectRatio;
    }

    public boolean isReady() {
        return this.textureLocation != null;
    }

}
