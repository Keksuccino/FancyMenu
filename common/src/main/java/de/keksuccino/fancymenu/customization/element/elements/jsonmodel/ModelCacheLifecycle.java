package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import de.keksuccino.fancymenu.util.CloseableUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Publishes a fully assembled model cache as one owned unit. Builds run outside this class's monitor; the generation in
 * their token prevents a candidate from being published after invalidation or destruction. Detached resources are also
 * closed outside the monitor because SpriteContents ultimately releases native memory.
 */
final class ModelCacheLifecycle<C extends AutoCloseable> {

    @Nullable
    private volatile C current;
    private long generation;
    private volatile boolean destroyed;

    @Nullable
    synchronized BuildToken beginBuild() {
        return this.destroyed ? null : new BuildToken(this, this.generation);
    }

    /** Takes ownership of {@code candidate} whether publication succeeds or the build token has become stale. */
    void publish(@NotNull BuildToken token, @NotNull C candidate) {
        Objects.requireNonNull(token);
        Objects.requireNonNull(candidate);
        C resourceToClose;
        boolean closeResource;
        synchronized (this) {
            boolean ownedToken = token.owner == this;
            boolean accepted = ownedToken && !token.consumed && token.generation == this.generation && !this.destroyed;
            if (ownedToken) token.consumed = true;
            if (accepted) {
                resourceToClose = this.current;
                this.current = candidate;
                closeResource = resourceToClose != candidate;
            } else {
                resourceToClose = candidate;
                closeResource = this.current != candidate;
            }
        }
        if (closeResource) CloseableUtils.closeQuietly(resourceToClose);
    }

    void invalidate() {
        C detached;
        synchronized (this) {
            if (this.destroyed) return;
            this.generation++;
            detached = this.current;
            this.current = null;
        }
        CloseableUtils.closeQuietly(detached);
    }

    void destroy() {
        C detached;
        synchronized (this) {
            if (this.destroyed) return;
            this.destroyed = true;
            this.generation++;
            detached = this.current;
            this.current = null;
        }
        CloseableUtils.closeQuietly(detached);
    }

    @Nullable
    C current() {
        return this.current;
    }

    boolean isDestroyed() {
        return this.destroyed;
    }

    static final class BuildToken {

        private final ModelCacheLifecycle<?> owner;
        private final long generation;
        private boolean consumed;

        private BuildToken(@NotNull ModelCacheLifecycle<?> owner, long generation) {
            this.owner = owner;
            this.generation = generation;
        }

    }

}
