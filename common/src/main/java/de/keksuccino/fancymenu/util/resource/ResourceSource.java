package de.keksuccino.fancymenu.util.resource;

import de.keksuccino.fancymenu.util.file.DotMinecraftUtils;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;
import net.minecraft.resources.Identifier;

/**
 * A {@link ResourceSource}, as the name says, is the source of a {@link Resource},
 * which can be a URL to a web file, a file path or a {@link Identifier} (namespace:path).
 */
public class ResourceSource {

    protected ResourceSourceType sourceType;
    protected String resourceSourceWithoutPrefix;
    protected boolean isDotMinecraftSource = false;

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
        source.resourceSourceWithoutPrefix = ResourceSourceType.getWithoutSourcePrefix(resourceSource);
        source.sourceType = (sourceType != null) ? sourceType : ResourceSourceType.getSourceTypeOf(resourceSource);
        // Only treat the source as a .minecraft-local file when the detected source type is LOCAL.
        if (source.sourceType == ResourceSourceType.LOCAL) {
            String dotMcSourcePath = DotMinecraftUtils.convertToShortenedDotMinecraftPath(source.resourceSourceWithoutPrefix);
            source.isDotMinecraftSource = dotMcSourcePath != null;
            if (source.isDotMinecraftSource) {
                source.resourceSourceWithoutPrefix = new File(DotMinecraftUtils.resolveMinecraftPath(dotMcSourcePath)).getAbsolutePath().replace("\\", "/");
            } else {
                source.resourceSourceWithoutPrefix = GameDirectoryUtils.getAbsoluteGameDirectoryPath(source.resourceSourceWithoutPrefix);
            }
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

    /**
     * Returns whether the serialized source can be dispatched to a resource handler. Prefix-only and whitespace-only
     * sources are not dispatchable. Neither are location sources with an empty path, because Minecraft accepts them as
     * namespace-root identifiers such as {@code minecraft:}. Invalid but non-empty identifiers remain dispatchable so
     * the resource handler retains its existing validation and error behavior.
     *
     * <p>Unlike constructing a {@link ResourceSource}, this check does not expand local paths.</p>
     */
    public static boolean isDispatchable(@NotNull String resourceSource) {
        Objects.requireNonNull(resourceSource);
        String trimmedSource = resourceSource.trim();
        String sourcePayload = ResourceSourceType.getWithoutSourcePrefix(trimmedSource);
        if (sourcePayload.isBlank()) return false;
        if (ResourceSourceType.getSourceTypeOf(trimmedSource) == ResourceSourceType.LOCATION) {
            Identifier location = Identifier.tryParse(sourcePayload);
            if ((location != null) && location.getPath().isEmpty()) return false;
        }
        return true;
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
        if (this.isDotMinecraftSource) {
            source = DotMinecraftUtils.convertToShortenedDotMinecraftPath(source);
        } else {
            if (this.sourceType == ResourceSourceType.LOCAL) source = GameDirectoryUtils.getPathWithoutGameDirectory(source);
        }
        return this.sourceType.getSourcePrefix() + source;
    }

    /**
     * DON'T USE THIS FOR SERIALIZATION! Returns absolute paths for local sources!
     */
    @NotNull
    public String getSourceWithPrefix() {
        return this.sourceType.getSourcePrefix() + this.resourceSourceWithoutPrefix;
    }

    /**
     * DON'T USE THIS FOR SERIALIZATION! Returns absolute paths for local sources!
     */
    @NotNull
    public String getSourceWithoutPrefix() {
        return this.resourceSourceWithoutPrefix;
    }

    public boolean isDotMinecraftSource() {
        return this.isDotMinecraftSource;
    }

    @Override
    public String toString() {
        return "ResourceSource{" +
                "sourceType=" + sourceType +
                ", source='" + resourceSourceWithoutPrefix + '\'' +
                '}';
    }

}
