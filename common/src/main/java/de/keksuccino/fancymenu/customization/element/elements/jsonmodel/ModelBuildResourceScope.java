package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import de.keksuccino.fancymenu.util.CloseableUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Owns closeable resources while a model cache is being assembled. Ownership is replaced when a wrapper takes over
 * responsibility for an inner resource (NativeImage -> SpriteContents -> TextureAtlasSprite), so a failed constructor
 * always leaves exactly one closeable responsible for the native allocation.
 */
final class ModelBuildResourceScope implements AutoCloseable {

    private final List<AutoCloseable> ownedResources = new ArrayList<>();
    private boolean closed;

    /** Adds a resource while the scope is open. On contract failure the caller retains ownership of {@code resource}. */
    @NotNull
    synchronized <T extends AutoCloseable> T own(@NotNull T resource) {
        Objects.requireNonNull(resource);
        if (this.closed) throw new IllegalStateException("Cannot add a resource to a closed model-build scope");
        this.ownedResources.add(resource);
        return resource;
    }

    /**
     * Replaces an owned inner resource with the wrapper that now owns it. The scope must be open and
     * {@code previousOwner} must be owned by this scope. If either precondition fails, ownership is unchanged and the
     * caller remains responsible for {@code newOwner}; closing it here could close the inner resource twice.
     */
    @NotNull
    synchronized <T extends AutoCloseable> T replaceOwnership(@NotNull AutoCloseable previousOwner, @NotNull T newOwner) {
        Objects.requireNonNull(previousOwner);
        Objects.requireNonNull(newOwner);
        // On contract failure the caller still owns newOwner. Closing it here could also close previousOwner a second
        // time when newOwner is a wrapper that already accepted the previous resource in its constructor.
        if (this.closed) throw new IllegalStateException("Cannot replace a resource in a closed model-build scope");
        int index = this.findOwnedResource(previousOwner);
        if (index < 0) throw new IllegalStateException("The previous resource is not owned by this model-build scope");
        this.ownedResources.set(index, newOwner);
        return newOwner;
    }

    @NotNull
    synchronized <T extends AutoCloseable> T transfer(@NotNull T resource) {
        Objects.requireNonNull(resource);
        if (this.closed) throw new IllegalStateException("Cannot transfer a resource from a closed model-build scope");
        int index = this.findOwnedResource(resource);
        if (index < 0) throw new IllegalStateException("The resource is not owned by this model-build scope");
        this.ownedResources.remove(index);
        return resource;
    }

    @Override
    public void close() {
        List<AutoCloseable> resourcesToClose;
        synchronized (this) {
            if (this.closed) return;
            this.closed = true;
            resourcesToClose = new ArrayList<>(this.ownedResources);
            this.ownedResources.clear();
        }
        for (int index = resourcesToClose.size() - 1; index >= 0; index--) CloseableUtils.closeQuietly(resourcesToClose.get(index));
    }

    private int findOwnedResource(@NotNull AutoCloseable resource) {
        for (int index = 0; index < this.ownedResources.size(); index++) {
            if (this.ownedResources.get(index) == resource) return index;
        }
        return -1;
    }

}
