package de.keksuccino.fancymenu.util.resources;

import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.file.type.FileType;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.*;

/**
 * Constructs usable instances of resources.
 *
 * @param <R> The {@link Resource} type returned by the handler.
 * @param <F> The {@link FileType} associated with the type of resource being handled.
 */
public abstract class ResourceHandler<R extends Resource, F extends FileType<R>> {

    private static final Logger LOGGER = LogManager.getLogger();

    protected Map<String, R> resources = new HashMap<>();

    /**
     * Used to get {@link Resource}s.<br>
     * Registers the requested resource if not already registered.<br><br>
     *
     * This method needs to be able to tell web, local and location sources apart.
     *
     * @param resourceSource Can be a URL to a web resource, a path to a local resource or a ResourceLocation (namespace:path).
     */
    @SuppressWarnings("all")
    @Nullable
    public R get(@NotNull String resourceSource) {
        Objects.requireNonNull(resourceSource);
        try {
            ResourceSourceType sourceType = ResourceSourceType.getSourceTypeOf(resourceSource);
            String resourceSourceWithoutPrefix = ResourceSourceType.getWithoutSourcePrefix(resourceSource);
            if (sourceType == ResourceSourceType.WEB) {
                if (this.getResourceMap().containsKey(resourceSource)) return this.getResourceMap().get(resourceSource);
                F fileType = null;
                for (F type : this.getAllowedFileTypes()) {
                    if (type.isFileTypeWeb(resourceSourceWithoutPrefix)) {
                        fileType = type;
                        break;
                    }
                }
                if (fileType != null) {
                    if (!fileType.isWebAllowed()) {
                        LOGGER.error("[FANCYMENU] Failed to get web resource! Web sources are not supported by this file type: " + fileType + " (Source: " + resourceSource + ")");
                        return null;
                    }
                    this.putAndReturn((R) fileType.getCodec().readWeb(resourceSourceWithoutPrefix), resourceSource);
                }
            } else if (sourceType == ResourceSourceType.LOCATION) {
                if (this.getResourceMap().containsKey(resourceSource)) return this.getResourceMap().get(resourceSource);
                ResourceLocation loc = ResourceLocation.tryParse(resourceSourceWithoutPrefix);
                if (loc != null) {
                    F fileType = null;
                    for (F type : this.getAllowedFileTypes()) {
                        if (type.isFileTypeLocation(loc)) {
                            fileType = type;
                            break;
                        }
                    }
                    if (fileType != null) {
                        if (!fileType.isLocationAllowed()) {
                            LOGGER.error("[FANCYMENU] Failed to get location resource! Location sources are not supported by this file type: " + fileType + " (Source: " + resourceSource + ")");
                            return null;
                        }
                        return this.putAndReturn((R) fileType.getCodec().readLocation(loc), resourceSource);
                    }
                }
            } else {
                resourceSourceWithoutPrefix = GameDirectoryUtils.getAbsoluteGameDirectoryPath(resourceSourceWithoutPrefix);
                resourceSource = sourceType.getSourcePrefix() + resourceSourceWithoutPrefix;
                if (this.getResourceMap().containsKey(resourceSource)) return this.getResourceMap().get(resourceSource);
                File file = new File(resourceSourceWithoutPrefix);
                F fileType = null;
                for (F type : this.getAllowedFileTypes()) {
                    if (type.isFileTypeLocal(file)) {
                        fileType = type;
                        break;
                    }
                }
                if (fileType != null) {
                    if (!fileType.isLocalAllowed()) {
                        LOGGER.error("[FANCYMENU] Failed to get local resource! Local sources are not supported by this file type: " + fileType + " (Source: " + resourceSource + ")");
                        return null;
                    }
                    return this.putAndReturn((R) fileType.getCodec().readLocal(file), resourceSource);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to get resource: " + resourceSource, ex);
        }
        return null;
    }

    @Nullable
    protected R putAndReturn(@Nullable R resource, @NotNull String resourceSource) {
        if (resource != null) {
            this.getResourceMap().put(resourceSource, resource);
        }
        return resource;
    }

    @NotNull
    protected Map<String, R> getResourceMap() {
        return this.resources;
    }

    @NotNull
    public abstract List<F> getAllowedFileTypes();

    /**
     * Reloads all resources of the handler.<br>
     * This gets called when MC reloads its resources.
     */
    public void reload() {
        this.getResourceMap().values().forEach(Resource::reload);
    }

    /**
     * Releases a resource.<br>
     * This will unregister the resource, remove it from any possible caches and close it.
     *
     * @param resourceSource Can be a URL to a web resource, a path to a local resource or a ResourceLocation (namespace:path).
     */
    public void release(@NotNull String resourceSource) {
        Objects.requireNonNull(resourceSource);
        ResourceSourceType sourceType = ResourceSourceType.getSourceTypeOf(resourceSource);
        if (sourceType == ResourceSourceType.LOCAL) {
            resourceSource = GameDirectoryUtils.getAbsoluteGameDirectoryPath(ResourceSourceType.getWithoutSourcePrefix(resourceSource));
            resourceSource = sourceType.getSourcePrefix() + resourceSource;
        }
        R resource = this.getResourceMap().get(resourceSource);
        if (resource != null) {
            CloseableUtils.closeQuietly(resource);
        }
        this.getResourceMap().remove(resourceSource);
    }

    /**
     * Releases all resources.<br>
     * This will unregister all resources, remove them from any possible caches and close them.
     */
    public void releaseAll() {
        this.getResourceMap().values().forEach(CloseableUtils::closeQuietly);
        this.getResourceMap().clear();
    }

}
