package de.keksuccino.fancymenu.util.resource;

import de.keksuccino.fancymenu.util.file.LocalSourcePathResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import net.minecraft.resources.Identifier;

/**
 * A {@link ResourceSource}, as the name says, is the source of a {@link Resource},
 * which can be a URL to a web file, a file path or a {@link Identifier} (namespace:path).
 */
public class ResourceSource {

    protected ResourceSourceType sourceType;
    protected String resourceSourceWithoutPrefix;
    protected String serializationSourceWithoutPrefix;
    protected boolean isDotMinecraftSource = false;
    @Nullable
    protected LocalSourcePathResolver.ResolvedPath resolvedLocalPath;

    /**
     * Creates a {@link ResourceSource} out of the given source string.<br>
     * If the source is a local file path, ordinary paths are confined to the active game instance. The explicit
     * {@code .minecraft/} shorthand is confined to the platform's default Minecraft directory. Invalid local paths are
     * retained for serialization but have no usable local file, allowing imported customization data to fail closed.
     */
    @NotNull
    public static ResourceSource of(@NotNull String resourceSource, @Nullable ResourceSourceType sourceType) {
        ResourceSource source = createUnresolved(resourceSource, sourceType);
        if (source.sourceType == ResourceSourceType.LOCAL) {
            try {
                source.resolveLocalPath(LocalSourcePathResolver.createForGameAndMinecraftDirectories());
            } catch (IOException | RuntimeException ignored) {
                source.rejectLocalPath();
            }
        }
        return source;
    }

    @NotNull
    static ResourceSource of(@NotNull String resourceSource, @Nullable ResourceSourceType sourceType, @NotNull LocalSourcePathResolver resolver) {
        ResourceSource source = createUnresolved(resourceSource, sourceType);
        if (source.sourceType == ResourceSourceType.LOCAL) {
            try {
                source.resolveLocalPath(resolver);
            } catch (IOException | RuntimeException ignored) {
                source.rejectLocalPath();
            }
        }
        return source;
    }

    @NotNull
    private static ResourceSource createUnresolved(@NotNull String resourceSource, @Nullable ResourceSourceType sourceType) {
        Objects.requireNonNull(resourceSource);
        resourceSource = resourceSource.trim();
        ResourceSource source = new ResourceSource();
        source.resourceSourceWithoutPrefix = ResourceSourceType.getWithoutSourcePrefix(resourceSource);
        source.serializationSourceWithoutPrefix = source.resourceSourceWithoutPrefix;
        source.sourceType = (sourceType != null) ? sourceType : ResourceSourceType.getSourceTypeOf(resourceSource);
        return source;
    }

    private void resolveLocalPath(@NotNull LocalSourcePathResolver resolver) throws IOException {
        LocalSourcePathResolver.ResolvedPath resolvedPath = resolver.resolve(this.serializationSourceWithoutPrefix);
        this.resolvedLocalPath = resolvedPath;
        this.resourceSourceWithoutPrefix = toPortablePath(resolvedPath.path());
        this.isDotMinecraftSource = resolvedPath.allowedRoot() == LocalSourcePathResolver.AllowedRoot.DEFAULT_MINECRAFT_DIRECTORY;
        Path relativePath = resolvedPath.rootPath().relativize(resolvedPath.path());
        String portableRelativePath = toPortablePath(relativePath);
        if (this.isDotMinecraftSource) {
            this.serializationSourceWithoutPrefix = portableRelativePath.isEmpty() ? ".minecraft" : ".minecraft/" + portableRelativePath;
        } else {
            this.serializationSourceWithoutPrefix = portableRelativePath;
        }
    }

    private void rejectLocalPath() {
        this.resolvedLocalPath = null;
        this.resourceSourceWithoutPrefix = "";
        this.isDotMinecraftSource = false;
    }

    private static String toPortablePath(Path path) {
        return path.toString().replace('\\', '/');
    }

    /**
     * Creates a {@link ResourceSource} out of the given source string.<br>
     * Local path handling follows {@link #of(String, ResourceSourceType)}.
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
        String source = (this.sourceType == ResourceSourceType.LOCAL) ? this.serializationSourceWithoutPrefix : this.resourceSourceWithoutPrefix;
        return this.sourceType.getSourcePrefix() + source;
    }

    /**
     * DON'T USE THIS FOR SERIALIZATION! Returns absolute paths for local sources!
     */
    @NotNull
    public String getSourceWithPrefix() {
        String source = ((this.sourceType == ResourceSourceType.LOCAL) && (this.resolvedLocalPath == null)) ? this.serializationSourceWithoutPrefix : this.resourceSourceWithoutPrefix;
        return this.sourceType.getSourcePrefix() + source;
    }

    /**
     * DON'T USE THIS FOR SERIALIZATION! Returns absolute paths for local sources!
     */
    @NotNull
    public String getSourceWithoutPrefix() {
        return this.resourceSourceWithoutPrefix;
    }

    /**
     * Returns a revalidated local file immediately before local I/O, or {@code null} when this is not a valid local
     * source anymore. Display and serialization callers should continue using the string accessors instead.
     */
    @Nullable
    public File getValidatedLocalFile() {
        if ((this.sourceType != ResourceSourceType.LOCAL) || (this.resolvedLocalPath == null)) return null;
        try {
            return this.resolvedLocalPath.revalidate().toFile();
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
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
