package de.keksuccino.fancymenu.util;

import de.keksuccino.fancymenu.util.file.ResourceFile;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.video.IVideo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SerializationHelper {

    public static final SerializationHelper INSTANCE = new SerializationHelper() {};

    @Nullable
    default ResourceSupplier<ITexture> deserializeImageResourceSupplier(@Nullable String resourceSource) {
        if (resourceSource != null) return ResourceSupplier.image(resourceSource);
        return null;
    }

    @Nullable
    default ResourceSupplier<IAudio> deserializeAudioResourceSupplier(@Nullable String resourceSource) {
        if (resourceSource != null) return ResourceSupplier.audio(resourceSource);
        return null;
    }

    @Nullable
    default ResourceSupplier<IVideo> deserializeVideoResourceSupplier(@Nullable String resourceSource) {
        if (resourceSource != null) return ResourceSupplier.video(resourceSource);
        return null;
    }

    @Nullable
    default ResourceSupplier<IText> deserializeTextResourceSupplier(@Nullable String resourceSource) {
        if (resourceSource != null) return ResourceSupplier.text(resourceSource);
        return null;
    }

    @Nullable
    default ResourceFile deserializeAssetResourceFile(@Nullable String gameDirectoryFilePath) {
        if (gameDirectoryFilePath == null) return null;
        else return ResourceFile.asset(gameDirectoryFilePath);
    }

    @Nullable
    default ResourceFile deserializeResourceFile(@Nullable String gameDirectoryFilePath) {
        if (gameDirectoryFilePath == null) return null;
        else return ResourceFile.of(gameDirectoryFilePath);
    }

    @NotNull
    default <T extends Number> T deserializeNumber(@NotNull Class<T> type, @NotNull T fallbackValue, @Nullable String serialized) {
        try {
            if (serialized != null) {
                serialized = serialized.replace(" ", "");
                if (type == Float.class) {
                    return (T) Float.valueOf(serialized);
                }
                if (type == Double.class) {
                    return (T) Double.valueOf(serialized);
                }
                if (type == Integer.class) {
                    return (T) Integer.valueOf(serialized);
                }
                if (type == Long.class) {
                    return (T) Long.valueOf(serialized);
                }
            }
        } catch (Exception ignore) {}
        return fallbackValue;
    }

    default boolean deserializeBoolean(boolean fallbackValue, @Nullable String serialized) {
        if (serialized != null) {
            if (serialized.replace(" ", "").equalsIgnoreCase("true")) {
                return true;
            }
            if (serialized.replace(" ", "").equalsIgnoreCase("false")) {
                return false;
            }
        }
        return fallbackValue;
    }

}
