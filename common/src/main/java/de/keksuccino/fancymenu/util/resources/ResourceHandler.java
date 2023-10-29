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
@SuppressWarnings("unused")
public abstract class ResourceHandler<R extends Resource, F extends FileType<R>> {

    private static final Logger LOGGER = LogManager.getLogger();

    protected Map<String, R> resources = new HashMap<>();
    protected List<String> failedSources = new ArrayList<>();

    /**
     * Get a {@link Resource} from a {@link ResourceSource}.<br>
     * Registers the requested resource if not already registered and uses {@link ResourceSource#getSourceWithPrefix()} as key.<br><br>
     *
     * This method should only return NULL if the resource failed to get registered!<br>
     * By default, {@link Resource}s should finish loading itself asynchronously after construction, so there's no other reason for returning NULL here.
     *
     * @param resourceSource Can be a URL to a web resource, a path to a local resource or a ResourceLocation (namespace:path).
     * @return The requested {@link Resource} or NULL if the {@link Resource} failed to get registered.
     */
    @Nullable
    public R get(@NotNull String resourceSource) {
        Objects.requireNonNull(resourceSource);
        return get(ResourceSource.of(resourceSource));
    }

    /**
     * Get a {@link Resource} from a {@link ResourceSource}.<br>
     * Registers the requested resource if not already registered and uses {@link ResourceSource#getSourceWithPrefix()} as key.<br><br>
     *
     * This method should only return NULL if the resource failed to get registered!<br>
     * By default, {@link Resource}s should finish loading itself asynchronously after construction, so there's no other reason for returning NULL here.
     *
     * @param resourceSource The source of the resource.
     * @return The requested {@link Resource} or NULL if the {@link Resource} failed to get registered.
     */
    @Nullable
    public R get(@NotNull ResourceSource resourceSource) {
        Objects.requireNonNull(resourceSource);
        try {
            //Check if resource is registered and return registered resource if true
            R registered = this.getFromMapAndClearClosed(resourceSource.getSourceWithPrefix());
            if (registered != null) return registered;
            //Check if handler failed to register resource in the past
            if (this.getFailedSourcesList().contains(resourceSource.getSourceWithPrefix())) return null;
            //Search file type of resource
            F fileType = null;
            for (F type : this.getAllowedFileTypes()) {
                if (type.isFileType(resourceSource, false)) {
                    fileType = type;
                    break;
                }
            }
            //Do advanced web checks if basic checks were not enough
            if ((fileType == null) && (resourceSource.getSourceType() == ResourceSourceType.WEB)) {
                for (F type : this.getAllowedFileTypes()) {
                    if (type.isFileTypeWebAdvanced(resourceSource.getSourceWithoutPrefix())) {
                        fileType = type;
                        break;
                    }
                }
            }
            //In case file type is still NULL, use fallback type (if a fallback type is defined)
            if (fileType == null) fileType = this.getFallbackFileType();
            //If file type is still NULL at this point, see resource loading as failed and add source to failed sources list
            if (fileType == null) {
                LOGGER.error("[FANCYMENU] Failed to register resource! Unsupported file type or failed to identify file type: " + resourceSource + " (RESOURCE HANDLER: " + this.getClass() + ")");
                this.addToFailedSources(resourceSource);
                return null;
            }
            if (resourceSource.getSourceType() == ResourceSourceType.WEB) {
                if (!fileType.isWebAllowed()) {
                    LOGGER.error("[FANCYMENU] Failed to register web resource! File type does not support web sources: " + fileType + " (Source: " + resourceSource + ")" + " (RESOURCE HANDLER: " + this.getClass() + ")");
                    this.addToFailedSources(resourceSource);
                    return null;
                }
                this.putAndReturn(fileType.getCodec().readWeb(resourceSource.getSourceWithoutPrefix()), resourceSource);
            } else if (resourceSource.getSourceType() == ResourceSourceType.LOCATION) {
                if (!fileType.isLocationAllowed()) {
                    LOGGER.error("[FANCYMENU] Failed to register location resource! File type does not support location sources: " + fileType + " (Source: " + resourceSource + ")" + " (RESOURCE HANDLER: " + this.getClass() + ")");
                    this.addToFailedSources(resourceSource);
                    return null;
                }
                ResourceLocation loc = ResourceLocation.tryParse(resourceSource.getSourceWithoutPrefix());
                if (loc == null) {
                    LOGGER.error("[FANCYMENU] Failed to register location resource! Unable to parse ResourceLocation: " + resourceSource + " (RESOURCE HANDLER: " + this.getClass() + ")");
                    this.addToFailedSources(resourceSource);
                    return null;
                }
                return this.putAndReturn(fileType.getCodec().readLocation(loc), resourceSource);
            } else {
                if (!fileType.isLocalAllowed()) {
                    LOGGER.error("[FANCYMENU] Failed to register local resource! File type does not support local sources: " + fileType + " (Source: " + resourceSource + ")" + " (RESOURCE HANDLER: " + this.getClass() + ")");
                    this.addToFailedSources(resourceSource);
                    return null;
                }
                return this.putAndReturn(fileType.getCodec().readLocal(new File(resourceSource.getSourceWithoutPrefix())), resourceSource);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to register resource: " + resourceSource + " (RESOURCE HANDLER: " + this.getClass() + ")", ex);
            this.addToFailedSources(resourceSource);
        }
        return null;
    }

    /**
     * Gets a registered {@link Resource} by its key.<br>
     * Will NOT register any {@link Resource}s!
     *
     * @return The registered {@link Resource} or NULL if no {@link Resource} was found for the given key.
     */
    @Nullable
    public R getIfRegistered(@NotNull String key) {
        return this.getResourceMap().get(Objects.requireNonNull(key));
    }

    /**
     * Allows for manual resource registration.<br>
     * Registers the resource if no resource with the given key is registered yet.
     */
    public void registerIfKeyAbsent(@NotNull String key, @NotNull R resource) {
        if (!this.hasResource(key)) {
            LOGGER.debug("[FANCYMENU] Registering resource with key: " + key + " (RESOURCE HANDLER: " + this.getClass() + ")");
            this.getResourceMap().put(key, Objects.requireNonNull(resource));
        }
    }

    public boolean hasResource(@NotNull String key) {
        return this.getResourceMap().containsKey(Objects.requireNonNull(key));
    }

    @Nullable
    protected R getFromMapAndClearClosed(@Nullable String resourceSource) {
        if (resourceSource == null) return null;
        if (this.getResourceMap().containsKey(resourceSource)) {
            R resource = this.getResourceMap().get(resourceSource);
            if (resource.isClosed()) {
                //In case the resource isn't fully closed yet because of asynchronous shenanigans
                CloseableUtils.closeQuietly(resource);
                //Remove closed resource from map
                this.getResourceMap().remove(resourceSource);
            } else {
                return resource;
            }
        }
        return null;
    }

    @Nullable
    protected R putAndReturn(@Nullable R resource, @NotNull ResourceSource resourceSource) {
        Objects.requireNonNull(resourceSource);
        if (resource != null) {
            LOGGER.debug("[FANCYMENU] Registering resource with source: " + resourceSource + " (RESOURCE HANDLER: " + this.getClass() + ")");
            this.getResourceMap().put(resourceSource.getSourceWithPrefix(), resource);
        } else {
            if (!this.getFailedSourcesList().contains(resourceSource.getSourceWithPrefix())) {
                this.getFailedSourcesList().add(resourceSource.getSourceWithPrefix());
                LOGGER.error("[FANCYMENU] Failed to register resource! Resource was NULL: " + resourceSource + " (RESOURCE HANDLER: " + this.getClass() + ")");
            }
        }
        return resource;
    }

    protected void addToFailedSources(@NotNull ResourceSource resourceSource) {
        if (!this.getFailedSourcesList().contains(resourceSource.getSourceWithPrefix())) {
            this.getFailedSourcesList().add(resourceSource.getSourceWithPrefix());
        }
    }

    @NotNull
    protected Map<String, R> getResourceMap() {
        return this.resources;
    }

    @NotNull
    protected List<String> getFailedSourcesList() {
        return this.failedSources;
    }

    @NotNull
    public abstract List<F> getAllowedFileTypes();

    /**
     * In case the {@link ResourceHandler} was unable to identify the {@link FileType} of the resource source, it will try to use the fallback {@link FileType}.<br>
     * Some {@link ResourceHandler}s have no fallback {@link FileType}. In that case, this method will return NULL.
     */
    @Nullable
    public abstract F getFallbackFileType();

    /**
     * Releases a resource.<br>
     * This will unregister the resource and close it.
     *
     * @param key The key of the registered resource. In most cases, this is its resource source and can be a URL to a web resource, a path to a local resource or a ResourceLocation (namespace:path).
     * @param isKeyResourceSource If the given key is a resource source.
     */
    public void release(@NotNull String key, boolean isKeyResourceSource) {
        Objects.requireNonNull(key);
        if (isKeyResourceSource) {
            String finalKey = key;
            this.getFailedSourcesList().removeIf(s -> s.equals(finalKey));
            ResourceSourceType sourceType = ResourceSourceType.getSourceTypeOf(key);
            if (sourceType == ResourceSourceType.LOCAL) {
                key = GameDirectoryUtils.getAbsoluteGameDirectoryPath(ResourceSourceType.getWithoutSourcePrefix(key));
                key = sourceType.getSourcePrefix() + key;
            }
        }
        R resource = this.getResourceMap().get(key);
        if (resource != null) {
            CloseableUtils.closeQuietly(resource);
        }
        this.getResourceMap().remove(key);
    }

    /**
     * Releases a resource.<br>
     * This will unregister the resource, remove it from any possible caches and close it.
     */
    public void release(@NotNull R resource) {
        Objects.requireNonNull(resource);
        String key = null;
        for (Map.Entry<String, R> m : this.getResourceMap().entrySet()) {
            if (m.getValue() == resource) {
                key = m.getKey();
                break;
            }
        }
        CloseableUtils.closeQuietly(resource);
        if (key != null) {
            this.getResourceMap().remove(key);
        }
    }

    /**
     * Releases all resources.<br>
     * This will unregister all resources, remove them from any possible caches and close them.
     */
    public void releaseAll() {
        this.getResourceMap().values().forEach(CloseableUtils::closeQuietly);
        this.getResourceMap().clear();
        this.getFailedSourcesList().clear();
    }

}
