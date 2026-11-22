package de.keksuccino.fancymenu.util.resource;

import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.file.type.FileType;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Constructs usable instances of resources.
 *
 * @param <R> The {@link Resource} type returned by the handler.
 * @param <F> The {@link FileType} associated with the type of resource being handled.
 */
@SuppressWarnings("unused")
public abstract class ResourceHandler<R extends Resource, F extends FileType<R>> {

    private static final Logger LOGGER = LogManager.getLogger();

    protected final Map<String, R> resources = new ConcurrentHashMap<>();
    protected final Set<String> failedSources = ConcurrentHashMap.newKeySet();
    private final Object lifecycleLock = new Object();
    private volatile boolean shuttingDown = false;

    /**
     * Get a {@link Resource} from a {@link ResourceSource}.<br>
     * Registers the requested resource if not already registered and uses {@link ResourceSource#getSourceWithPrefix()} as key.<br><br>
     *
     * This method should only return NULL if the resource failed to get registered!<br>
     * {@link Resource}s should finish loading itself asynchronously after construction, so there's no other reason for returning NULL here.
     *
     * @param resourceSource Can be a URL to a web resource, a path to a local resource or a Identifier (namespace:path).
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
     * {@link Resource}s should finish loading itself asynchronously after construction, so there's no other reason for returning NULL here.
     *
     * @param resourceSource The source of the resource.
     * @return The requested {@link Resource} or NULL if the {@link Resource} failed to get registered.
     */
    @Nullable
    public R get(@NotNull ResourceSource resourceSource) {
        Objects.requireNonNull(resourceSource);
        if (this.shuttingDown) return null;
        try {
            //Check if resource is registered and return registered resource if true
            R registered = this.getFromMapAndClearClosed(resourceSource.getSourceWithPrefix());
            if (registered != null) return registered;
            //Check if handler failed to register resource in the past
            if (this.getFailedSourcesSet().contains(resourceSource.getSourceWithPrefix())) return null;
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
                return this.putAndReturn(fileType.getCodec().readWeb(resourceSource.getSourceWithoutPrefix()), resourceSource);
            } else if (resourceSource.getSourceType() == ResourceSourceType.LOCATION) {
                if (!fileType.isLocationAllowed()) {
                    LOGGER.error("[FANCYMENU] Failed to register location resource! File type does not support location sources: " + fileType + " (Source: " + resourceSource + ")" + " (RESOURCE HANDLER: " + this.getClass() + ")");
                    this.addToFailedSources(resourceSource);
                    return null;
                }
                Identifier loc = Identifier.tryParse(resourceSource.getSourceWithoutPrefix());
                if (loc == null) {
                    LOGGER.error("[FANCYMENU] Failed to register location resource! Unable to parse Identifier: " + resourceSource + " (RESOURCE HANDLER: " + this.getClass() + ")");
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
                File localFile = resourceSource.getValidatedLocalFile();
                if (localFile == null) {
                    this.addToFailedSources(resourceSource);
                    return null;
                }
                // This is the last shared handoff before a codec opens the file. Some native decoders retain the File
                // for asynchronous use; portable Java cannot make validation and that later third-party open atomic.
                return this.putAndReturn(fileType.getCodec().readLocal(localFile), resourceSource);
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
        if (this.shuttingDown) return null;
        return this.getResourceMap().get(Objects.requireNonNull(key));
    }

    /**
     * Allows for manual resource registration.<br>
     * Registers the resource if no resource with the given key is registered yet.
     */
    public void registerIfKeyAbsent(@NotNull String key, @NotNull R resource) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(resource);
        boolean rejectResource;
        synchronized (this.lifecycleLock) {
            rejectResource = this.shuttingDown;
            if (!rejectResource && (this.getResourceMap().putIfAbsent(key, resource) == null)) {
                LOGGER.debug("[FANCYMENU] Registering resource with key: " + key + " (RESOURCE HANDLER: " + this.getClass() + ")");
            }
        }
        if (rejectResource) CloseableUtils.closeQuietly(resource);
    }

    public boolean hasResource(@NotNull String key) {
        if (this.shuttingDown) return false;
        return this.getResourceMap().containsKey(Objects.requireNonNull(key));
    }

    @Nullable
    protected R getFromMapAndClearClosed(@Nullable String resourceSource) {
        if (resourceSource == null) return null;
        R resource = this.getResourceMap().get(resourceSource);
        if (resource == null) return null;
        if (resource.isClosed()) {
            //In case the resource isn't fully closed yet because of asynchronous shenanigans
            boolean detached;
            synchronized (this.lifecycleLock) {
                //Only remove the resource we inspected, so a concurrent replacement remains registered.
                detached = this.getResourceMap().remove(resourceSource, resource);
            }
            if (detached) CloseableUtils.closeQuietly(resource);
        } else if (!this.shuttingDown) {
            return resource;
        }
        return null;
    }

    @Nullable
    protected R putAndReturn(@Nullable R resource, @NotNull ResourceSource resourceSource) {
        Objects.requireNonNull(resourceSource);
        if (resource != null) {
            R registeredResource = resource;
            boolean rejectResource;
            synchronized (this.lifecycleLock) {
                rejectResource = this.shuttingDown;
                if (!rejectResource) {
                    R existingResource = this.getResourceMap().putIfAbsent(resourceSource.getSourceWithPrefix(), resource);
                    if (existingResource != null) registeredResource = existingResource;
                    else LOGGER.debug("[FANCYMENU] Registering resource with source: " + resourceSource + " (RESOURCE HANDLER: " + this.getClass() + ")");
                }
            }
            if (rejectResource) {
                CloseableUtils.closeQuietly(resource);
                return null;
            }
            if (registeredResource != resource) CloseableUtils.closeQuietly(resource);
            return registeredResource;
        } else {
            if (this.addToFailedSources(resourceSource)) {
                LOGGER.error("[FANCYMENU] Failed to register resource! Resource was NULL: " + resourceSource + " (RESOURCE HANDLER: " + this.getClass() + ")");
            }
        }
        return null;
    }

    protected boolean addToFailedSources(@NotNull ResourceSource resourceSource) {
        synchronized (this.lifecycleLock) {
            if (this.shuttingDown) return false;
            return this.getFailedSourcesSet().add(resourceSource.getSourceWithPrefix());
        }
    }

    @NotNull
    protected Map<String, R> getResourceMap() {
        return this.resources;
    }

    @NotNull
    protected Set<String> getFailedSourcesSet() {
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
     * @param key The key of the registered resource. In most cases, this is its resource source and can be a URL to a web resource, a path to a local resource or a Identifier (namespace:path).
     * @param isKeyResourceSource If the given key is a resource source.
     */
    public void release(@NotNull String key, boolean isKeyResourceSource) {
        Objects.requireNonNull(key);
        if (isKeyResourceSource) {
            String finalKey = key;
            this.getFailedSourcesSet().remove(finalKey);
            ResourceSourceType sourceType = ResourceSourceType.getSourceTypeOf(key);
            if (sourceType == ResourceSourceType.LOCAL) {
                key = GameDirectoryUtils.getAbsoluteGameDirectoryPath(ResourceSourceType.getWithoutSourcePrefix(key));
                key = sourceType.getSourcePrefix() + key;
            }
        }
        R resource;
        synchronized (this.lifecycleLock) {
            resource = this.getResourceMap().remove(key);
        }
        if (resource != null) CloseableUtils.closeQuietly(resource);
    }

    /**
     * Releases a resource.<br>
     * This will unregister the resource, remove it from any possible caches and close it.
     */
    public void release(@NotNull R resource) {
        Objects.requireNonNull(resource);
        boolean closeResource = false;
        synchronized (this.lifecycleLock) {
            for (Map.Entry<String, R> entry : this.getResourceMap().entrySet()) {
                if ((entry.getValue() == resource) && this.getResourceMap().remove(entry.getKey(), resource)) {
                    closeResource = true;
                    break;
                }
            }
            // Preserve the historic behavior for an unregistered resource during normal operation. Once shutdown owns the map snapshot, it owns closure too.
            if (!closeResource && !this.shuttingDown) closeResource = true;
        }
        if (closeResource) CloseableUtils.closeQuietly(resource);
    }

    /**
     * Releases all resources.<br>
     * This will unregister all resources, remove them from any possible caches and close them.
     */
    public void releaseAll() {
        this.releaseRegisteredResources(false);
    }

    /**
     * Permanently closes this handler for client shutdown. The lifecycle lock is intentionally held only while detaching
     * registrations; individual resources can perform slow native cleanup without blocking a late registration from being rejected.
     */
    public void shutdown() {
        this.releaseRegisteredResources(true);
    }

    private void releaseRegisteredResources(boolean shutdown) {
        List<R> resourcesToRelease;
        synchronized (this.lifecycleLock) {
            if (shutdown) {
                if (this.shuttingDown) return;
                this.shuttingDown = true;
            }
            resourcesToRelease = new ArrayList<>(this.getResourceMap().values());
            this.getResourceMap().clear();
            this.getFailedSourcesSet().clear();
        }
        resourcesToRelease.forEach(CloseableUtils::closeQuietly);
    }

}
