package de.keksuccino.fancymenu.util.resources.texture;

import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.file.type.types.ImageFileType;
import de.keksuccino.fancymenu.util.resources.ResourceSourceType;
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
     * Supports all {@link ImageFileType}s.
     *
     * @param resourceSource Can be a URL to a web resource, a path to a local resource or a ResourceLocation (namespace:path).
     */
    @NotNull
    public ITexture getTexture(@NotNull String resourceSource) {
        ResourceSourceType sourceType = ResourceSourceType.getSourceTypeOf(resourceSource);
        if (sourceType == ResourceSourceType.WEB) return this.getWebTexture(resourceSource);
        if (sourceType == ResourceSourceType.LOCATION) {
            ResourceLocation loc = ResourceLocation.tryParse(resourceSource);
            if (loc != null) return this.getLocationTexture(loc);
        }
        return this.getLocalTexture(GameDirectoryUtils.getAbsoluteGameDirectoryPath(resourceSource));
    }

    /**
     * Supports all {@link ImageFileType}s.
     */
    @NotNull
    public ITexture getLocationTexture(@NotNull ResourceLocation location) {
        Objects.requireNonNull(location);
        if (!textures.containsKey(location.toString())) {
            ITexture texture = null;
            try {
                for (ImageFileType type : FileTypes.getAllImageFileTypes()) {
                    if (type.isFileTypeLocation(location)) {
                        texture = type.getCodec().readLocation(location);
                        break;
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to load ResourceLocation texture: " + location, ex);
            }
            if (texture == null) {
                LOGGER.error("[FANCYMENU] Failed to load ResourceLocation texture! FileCodec returned NULL for: " + location);
                texture = SimpleTexture.FULLY_TRANSPARENT_SIMPLE_TEXTURE;
            }
            this.textures.put(location.toString(), texture);
            return texture;
        } else {
            return textures.get(location.toString());
        }
    }

    /**
     * Supports all {@link ImageFileType}s.
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
                texture = SimpleTexture.FULLY_TRANSPARENT_SIMPLE_TEXTURE;
            }
            this.textures.put(imageFile.getAbsolutePath(), texture);
            return texture;
        } else {
            return textures.get(imageFile.getAbsolutePath());
        }
    }

    /**
     * Supports all {@link ImageFileType}s.
     */
    @NotNull
    public ITexture getLocalTexture(@NotNull String imageFilePath) {
        Objects.requireNonNull(imageFilePath);
        return this.getLocalTexture(new File(GameDirectoryUtils.getAbsoluteGameDirectoryPath(imageFilePath)));
    }

    /**
     * Supports all {@link ImageFileType}s.
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
                texture = SimpleTexture.FULLY_TRANSPARENT_SIMPLE_TEXTURE;
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

    public void remove(@NotNull String resourceSource) {
        Objects.requireNonNull(resourceSource);
        ResourceSourceType sourceType = ResourceSourceType.getSourceTypeOf(resourceSource);
        if (sourceType == ResourceSourceType.LOCAL) {
            resourceSource = GameDirectoryUtils.getAbsoluteGameDirectoryPath(resourceSource);
        }
        ITexture texture = textures.get(resourceSource);
        if (texture != null) {
            CloseableUtils.closeQuietly(texture);
        }
        textures.remove(resourceSource);
    }

    public void clear() {
        for (ITexture t : this.textures.values()) {
            CloseableUtils.closeQuietly(t);
        }
        this.textures.clear();
    }

}
