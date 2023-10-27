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

    /**
     * Get a {@link Resource} from a resource source.<br>
     * Registers the requested resource if not already registered (uses the resource source as key).<br><br>
     *
     * This method needs to be able to tell web, local and location sources apart.<br><br>
     *
     * This method should only return NULL if the resource failed to get registered!<br>
     * By default, {@link Resource}s should finish loading itself asynchronously after construction.
     *
     * @param resourceSource Can be a URL to a web resource, a path to a local resource or a ResourceLocation (namespace:path).
     * @return The requested {@link Resource} or NULL if the {@link Resource} failed to get registered.
     */
    @Nullable
    public R get(@NotNull String resourceSource) {
        Objects.requireNonNull(resourceSource);
        try {
            resourceSource = resourceSource.trim();
            ResourceSourceType sourceType = ResourceSourceType.getSourceTypeOf(resourceSource);
            String withoutPrefix = ResourceSourceType.getWithoutSourcePrefix(resourceSource);
            //Make sure the resource source has a prefix, to make source type check of FileType#isFileType(source) more efficient
            resourceSource = sourceType.getSourcePrefix() + withoutPrefix;
            //Convert local paths to valid game directory paths
            if (sourceType == ResourceSourceType.LOCAL) {
                withoutPrefix = GameDirectoryUtils.getAbsoluteGameDirectoryPath(withoutPrefix);
                resourceSource = sourceType.getSourcePrefix() + withoutPrefix;
            }
            //Check if map contains resource and return cached resource if true
            R cached = this.getFromMapAndClearClosed(resourceSource);
            if (cached != null) return cached;
            //Search file type of resource
            F fileType = null;
            for (F type : this.getAllowedFileTypes()) {
                if (type.isFileType(resourceSource)) {
                    fileType = type;
                    break;
                }
            }
            if (fileType == null) {
                LOGGER.error("[FANCYMENU] Failed to get resource! Unsupported file type: " + resourceSource);
                return null;
            }
            if (sourceType == ResourceSourceType.WEB) {
                if (!fileType.isWebAllowed()) {
                    LOGGER.error("[FANCYMENU] Failed to get web resource! Web sources are not supported by this file type: " + fileType + " (Source: " + resourceSource + ")");
                    return null;
                }
                this.putAndReturn(fileType.getCodec().readWeb(withoutPrefix), resourceSource);
            } else if (sourceType == ResourceSourceType.LOCATION) {
                if (!fileType.isLocationAllowed()) {
                    LOGGER.error("[FANCYMENU] Failed to get location resource! Location sources are not supported by this file type: " + fileType + " (Source: " + resourceSource + ")");
                    return null;
                }
                ResourceLocation loc = ResourceLocation.tryParse(withoutPrefix);
                if (loc == null) {
                    LOGGER.error("[FANCYMENU] Failed to get location resource! Unable to parse ResourceLocation: " + resourceSource);
                    return null;
                }
                return this.putAndReturn(fileType.getCodec().readLocation(loc), resourceSource);
            } else {
                if (!fileType.isLocalAllowed()) {
                    LOGGER.error("[FANCYMENU] Failed to get local resource! Local sources are not supported by this file type: " + fileType + " (Source: " + resourceSource + ")");
                    return null;
                }
                return this.putAndReturn(fileType.getCodec().readLocal(new File(withoutPrefix)), resourceSource);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to get resource: " + resourceSource, ex);
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

//    /**
//     * Used to get {@link Resource}s.<br>
//     * Registers the requested resource if not already registered.<br><br>
//     *
//     * This method needs to be able to tell web, local and location sources apart.
//     *
//     * @param resourceSource Can be a URL to a web resource, a path to a local resource or a ResourceLocation (namespace:path).
//     */
//    @SuppressWarnings("all")
//    @Nullable
//    public R get(@NotNull String resourceSource) {
//        Objects.requireNonNull(resourceSource);
//        try {
//            resourceSource = resourceSource.trim();
//            ResourceSourceType sourceType = ResourceSourceType.getSourceTypeOf(resourceSource);
//            String withoutPrefix = ResourceSourceType.getWithoutSourcePrefix(resourceSource);
//            resourceSource = sourceType.getSourcePrefix() + withoutPrefix;
//            if (sourceType == ResourceSourceType.WEB) {
//                R cached = this.getFromMapAndClearClosed(resourceSource);
//                if (cached != null) return cached;
//                F fileType = null;
//                for (F type : this.getAllowedFileTypes()) {
//                    if (type.isFileTypeWeb(withoutPrefix)) {
//                        fileType = type;
//                        break;
//                    }
//                }
//                if (fileType != null) {
//                    if (!fileType.isWebAllowed()) {
//                        LOGGER.error("[FANCYMENU] Failed to get web resource! Web sources are not supported by this file type: " + fileType + " (Source: " + resourceSource + ")");
//                        return null;
//                    }
//                    this.putAndReturn((R) fileType.getCodec().readWeb(withoutPrefix), resourceSource);
//                }
//            } else if (sourceType == ResourceSourceType.LOCATION) {
//                R cached = this.getFromMapAndClearClosed(resourceSource);
//                if (cached != null) return cached;
//                ResourceLocation loc = ResourceLocation.tryParse(withoutPrefix);
//                if (loc != null) {
//                    F fileType = null;
//                    for (F type : this.getAllowedFileTypes()) {
//                        if (type.isFileTypeLocation(loc)) {
//                            fileType = type;
//                            break;
//                        }
//                    }
//                    if (fileType != null) {
//                        if (!fileType.isLocationAllowed()) {
//                            LOGGER.error("[FANCYMENU] Failed to get location resource! Location sources are not supported by this file type: " + fileType + " (Source: " + resourceSource + ")");
//                            return null;
//                        }
//                        return this.putAndReturn((R) fileType.getCodec().readLocation(loc), resourceSource);
//                    }
//                }
//            } else {
//                withoutPrefix = GameDirectoryUtils.getAbsoluteGameDirectoryPath(withoutPrefix);
//                resourceSource = sourceType.getSourcePrefix() + withoutPrefix;
//                R cached = this.getFromMapAndClearClosed(resourceSource);
//                if (cached != null) return cached;
//                File file = new File(withoutPrefix);
//                F fileType = null;
//                for (F type : this.getAllowedFileTypes()) {
//                    if (type.isFileTypeLocal(file)) {
//                        fileType = type;
//                        break;
//                    }
//                }
//                if (fileType != null) {
//                    if (!fileType.isLocalAllowed()) {
//                        LOGGER.error("[FANCYMENU] Failed to get local resource! Local sources are not supported by this file type: " + fileType + " (Source: " + resourceSource + ")");
//                        return null;
//                    }
//                    return this.putAndReturn((R) fileType.getCodec().readLocal(file), resourceSource);
//                }
//            }
//        } catch (Exception ex) {
//            LOGGER.error("[FANCYMENU] Failed to get resource: " + resourceSource, ex);
//        }
//        return null;
//    }

    /**
     * Allows for manual resource registration.<br>
     * Registers the resource if no resource with the given key is registered yet.
     */
    public void registerIfKeyAbsent(@NotNull String key, @NotNull R resource) {
        if (!this.hasResource(key)) {
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
    protected R putAndReturn(@Nullable R resource, @NotNull String resourceSource) {
        if (resource != null) {
            LOGGER.debug("[FANCYMENU] Registering resource: " + resourceSource);
            this.getResourceMap().put(resourceSource, resource);
        } else {
            LOGGER.error("[FANCYMENU] Failed to register resource: " + resourceSource);
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
     * Releases a resource.<br>
     * This will unregister the resource and close it.
     *
     * @param key The key of the registered resource. In most cases, this is its resource source and can be a URL to a web resource, a path to a local resource or a ResourceLocation (namespace:path).
     * @param isKeyResourceSource If the given key is a resource source.
     */
    public void release(@NotNull String key, boolean isKeyResourceSource) {
        Objects.requireNonNull(key);
        if (isKeyResourceSource) {
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
    }

}
