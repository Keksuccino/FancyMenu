package de.keksuccino.fancymenu.util.rendering.text.smooth;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Lazily loads an optional font and keeps its availability stable until a resource reload.
 * The owner of the font remains responsible for disposing it before clearing this provider.
 */
final class FallbackFontProvider<T> implements Supplier<T> {

    private final Supplier<T> loader;
    // null means unattempted; Optional.empty() remembers a failed load without retrying every UI measurement.
    private volatile Optional<T> cached;

    FallbackFontProvider(@NotNull Supplier<T> loader) {
        this.loader = Objects.requireNonNull(loader);
    }

    @Override
    @Nullable
    public T get() {
        Optional<T> result = this.cached;
        if (result == null) {
            synchronized (this) {
                result = this.cached;
                if (result == null) {
                    result = Optional.ofNullable(this.loader.get());
                    this.cached = result;
                }
            }
        }
        return result.orElse(null);
    }

    boolean shouldUseFallback(boolean preferFallback) {
        return preferFallback || this.get() == null;
    }

    synchronized void clear() {
        this.cached = null;
    }

}
