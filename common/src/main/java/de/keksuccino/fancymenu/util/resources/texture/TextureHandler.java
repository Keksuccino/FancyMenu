package de.keksuccino.fancymenu.util.resources.texture;

import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.file.type.types.ImageFileType;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
public class TextureHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final TextureHandler INSTANCE = new TextureHandler();

    private final Map<String, ITexture> textures = new HashMap<>();

    /**
     * Supports all {@link ImageFileType}s.<br>
     */
    @NotNull
    public ITexture getLocalTexture(@NotNull File imageFile) {
        Objects.requireNonNull(imageFile);
        if (!textures.containsKey(imageFile.getAbsolutePath())) {
            ITexture texture = null;
            try {
                for (ImageFileType type : FileTypes.getAllImageFileTypes()) {
                    if (type.isFileTypeLocal(imageFile)) {
                        texture = type.getCodec().readLocal(imageFile);
                        break;
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to load local texture: " + imageFile.getPath(), ex);
            }
            if (texture == null) {
                LOGGER.error("[FANCYMENU] Failed to load web texture! FileCodec returned NULL for: " + imageFile.getPath());
                texture = WrappedTexture.FULLY_TRANSPARENT_WRAPPED_TEXTURE;
            }
            this.textures.put(imageFile.getAbsolutePath(), texture);
            return texture;
        } else {
            return textures.get(imageFile.getAbsolutePath());
        }
    }

    /**
     * Supports all {@link ImageFileType}s.<br>
     */
    @NotNull
    public ITexture getLocalTexture(@NotNull String imageFilePath) {
        Objects.requireNonNull(imageFilePath);
        return this.getLocalTexture(new File(GameDirectoryUtils.getAbsoluteGameDirectoryPath(imageFilePath)));
    }

    /**
     * Supports all {@link ImageFileType}s.<br>
     */
    @NotNull
    public ITexture getWebTexture(@NotNull String imageUrl) {
        Objects.requireNonNull(imageUrl);
        if (!textures.containsKey(imageUrl)) {
            ITexture texture = null;
            try {
                for (ImageFileType type : FileTypes.getAllImageFileTypes()) {
                    if (type.isFileTypeWeb(imageUrl)) {
                        texture = type.getCodec().readWeb(imageUrl);
                        break;
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to load web texture: " + imageUrl, ex);
            }
            if (texture == null) {
                LOGGER.error("[FANCYMENU] Failed to load web texture! FileCodec returned NULL for: " + imageUrl);
                texture = WrappedTexture.FULLY_TRANSPARENT_WRAPPED_TEXTURE;
            }
            this.textures.put(imageUrl, texture);
            return texture;
        } else {
            return textures.get(imageUrl);
        }
    }

    /**
     * Tries to find a registered {@link ITexture} by its current {@link ResourceLocation}.<br>
     * Due to the nature of some types of {@link ITexture}s, their {@link ResourceLocation} can change, so this it NOT A SAFE WAY to get a registered texture!<br><br>
     *
     * Will not register the texture! Only returns textures that are already registered and loaded!
     */
    @Nullable
    public ITexture getLoadedTextureByResourceLocation(@NotNull ResourceLocation location) {
        Objects.requireNonNull(location);
        for (ITexture t : this.textures.values()) {
            if (t.isReady() && (t.getResourceLocation() == location)) {
                return t;
            }
        }
        return null;
    }

    public void removeResource(@NotNull String pathOrUrl) {
        File f = new File(GameDirectoryUtils.getAbsoluteGameDirectoryPath(pathOrUrl));
        textures.remove(f.getAbsolutePath());
        textures.remove(pathOrUrl);
    }

    public void clearResources() {
        this.textures.clear();
    }

}
