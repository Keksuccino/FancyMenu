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
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Constructs usable instances of resources.
 *
 * @param <R> The {@link Resource} type returned by the handler.
 * @param <F> The {@link FileType} associated with the type of resource being handled.
 */
public abstract class ResourceHandler<R extends Resource, F extends FileType<R>> {

    private static final Logger LOGGER = LogManager.getLogger();

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
            if (sourceType == ResourceSourceType.WEB) {
                if (this.getResourceMap().containsKey(resourceSource)) return this.getResourceMap().get(resourceSource);
                F fileType = null;
                for (F type : this.getAllowedFileTypes()) {
                    if (type.isFileTypeWeb(resourceSource)) {
                        fileType = type;
                        break;
                    }
                }
                if (fileType != null) this.putAndReturn((R) fileType.getCodec().readWeb(resourceSource), resourceSource);
            } else if (sourceType == ResourceSourceType.LOCATION) {
                if (this.getResourceMap().containsKey(resourceSource)) return this.getResourceMap().get(resourceSource);
                ResourceLocation loc = ResourceLocation.tryParse(resourceSource);
                if (loc != null) {
                    F fileType = null;
                    for (F type : this.getAllowedFileTypes()) {
                        if (type.isFileTypeLocation(loc)) {
                            fileType = type;
                            break;
                        }
                    }
                    if (fileType != null) return this.putAndReturn((R) fileType.getCodec().readLocation(loc), resourceSource);
                }
            } else {
                resourceSource = GameDirectoryUtils.getAbsoluteGameDirectoryPath(resourceSource);
                if (this.getResourceMap().containsKey(resourceSource)) return this.getResourceMap().get(resourceSource);
                File file = new File(resourceSource);
                F fileType = null;
                for (F type : this.getAllowedFileTypes()) {
                    if (type.isFileTypeLocal(file)) {
                        fileType = type;
                        break;
                    }
                }
                if (fileType != null) return this.putAndReturn((R) fileType.getCodec().readLocal(file), resourceSource);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to get text resource: " + resourceSource, ex);
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
    protected abstract Map<String, R> getResourceMap();

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
            resourceSource = GameDirectoryUtils.getAbsoluteGameDirectoryPath(resourceSource);
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
