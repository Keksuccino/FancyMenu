package de.keksuccino.fancymenu.util.resources;

import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * A {@link ResourceSource}, as the name says, is the source of a {@link Resource},
 * which can be a URL to a web file, a file path or a {@link ResourceLocation} (namespace:path).
 */
public class ResourceSource {

    protected ResourceSourceType sourceType;
    protected String resourceSourceWithoutPrefix;

    /**
     * Creates a {@link ResourceSource} out of the given source string.<br>
     * If the source is a local file path, it will get converted to a valid and absolute
     * game directory file path via {@link GameDirectoryUtils#getAbsoluteGameDirectoryPath(String)}.
     */
    @NotNull
    public static ResourceSource of(@NotNull String resourceSource, @Nullable ResourceSourceType sourceType) {
        Objects.requireNonNull(resourceSource);
        resourceSource = resourceSource.trim();
        ResourceSource source = new ResourceSource();
        source.sourceType = (sourceType != null) ? sourceType : ResourceSourceType.getSourceTypeOf(resourceSource);
        source.resourceSourceWithoutPrefix = ResourceSourceType.getWithoutSourcePrefix(resourceSource);
        if (source.sourceType == ResourceSourceType.LOCAL) {
            source.resourceSourceWithoutPrefix = GameDirectoryUtils.getAbsoluteGameDirectoryPath(source.resourceSourceWithoutPrefix);
        }
        return source;
    }

    /**
     * Creates a {@link ResourceSource} out of the given source string.<br>
     * If the source is a local file path, it will get converted to a valid and absolute
     * game directory file path via {@link GameDirectoryUtils#getAbsoluteGameDirectoryPath(String)}.
     */
    @NotNull
    public static ResourceSource of(@NotNull String resourceSource) {
        return of(resourceSource, null);
    }

    protected ResourceSource() {
    }

    @NotNull
    public ResourceSourceType getSourceType() {
        return this.sourceType;
    }

    /**
     * The source with prefix, but local sources get converted to short paths.<br>
     * Used when serializing sources.
     */
    @NotNull
    public String getSerializationSource() {
        String source = this.resourceSourceWithoutPrefix;
        if (this.sourceType == ResourceSourceType.LOCAL) source = GameDirectoryUtils.getPathWithoutGameDirectory(source);
        return this.sourceType.getSourcePrefix() + source;
    }

    @NotNull
    public String getSourceWithPrefix() {
        return this.sourceType.getSourcePrefix() + this.resourceSourceWithoutPrefix;
    }

    @NotNull
    public String getSourceWithoutPrefix() {
        return this.resourceSourceWithoutPrefix;
    }

    @Override
    public String toString() {
        return "ResourceSource{" +
                "sourceType=" + sourceType +
                ", source='" + resourceSourceWithoutPrefix + '\'' +
                '}';
    }

}
