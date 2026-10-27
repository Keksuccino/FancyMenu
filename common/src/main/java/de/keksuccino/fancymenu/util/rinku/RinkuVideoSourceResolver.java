package de.keksuccino.fancymenu.util.rinku;

import de.keksuccino.fancymenu.util.file.LocalSourcePathResolver;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.LongSupplier;

/**
 * Resolves placeholder-expanded Rinku sources immediately before handing them to Chromium.
 *
 * <p>Checking a local source touches the file system several times, so an instance remembers the last local check of one
 * video and only checks again when the source changes or {@link #LOCAL_RECHECK_INTERVAL_NANOS} passed. Files that appear,
 * vanish or get swapped later are still noticed that way.</p>
 *
 * <p>Callers only hand a URL to Chromium when the resolved value changes, and a cached value never differs from the one
 * returned before, so every handed-over URL still comes from a fresh check. Callers that forget the URL they handed over
 * must call {@link #reset()}, otherwise a cached URL would look new to them.</p>
 */
public final class RinkuVideoSourceResolver {

    static final long LOCAL_RECHECK_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(1);

    private final LongSupplier nanoClock;
    private final BiFunction<ResourceSource, String, String> uncachedResolver;
    @Nullable
    private LocalCheck lastLocalCheck;

    public RinkuVideoSourceResolver() {
        this(System::nanoTime, RinkuVideoSourceResolver::resolve);
    }

    RinkuVideoSourceResolver(@NotNull LongSupplier nanoClock, @NotNull BiFunction<ResourceSource, String, String> uncachedResolver) {
        this.nanoClock = Objects.requireNonNull(nanoClock);
        this.uncachedResolver = Objects.requireNonNull(uncachedResolver);
    }

    /**
     * Like {@link #resolve(ResourceSource, String)}, but reuses the last local check of this video while it is recent, so
     * it is cheap enough to call every frame.
     */
    @Nullable
    public String resolveCached(@NotNull ResourceSource rawSource, @NotNull String placeholderExpandedSource) {
        if (rawSource.getSourceType() != ResourceSourceType.LOCAL) {
            this.lastLocalCheck = null;
            return placeholderExpandedSource;
        }
        long now = this.nanoClock.getAsLong();
        LocalCheck last = this.lastLocalCheck;
        if ((last != null) && last.placeholderExpandedSource().equals(placeholderExpandedSource) && ((now - last.checkedAtNanos()) < LOCAL_RECHECK_INTERVAL_NANOS)) return last.resolvedSource();
        String resolvedSource = this.uncachedResolver.apply(rawSource, placeholderExpandedSource);
        this.lastLocalCheck = new LocalCheck(placeholderExpandedSource, resolvedSource, now);
        return resolvedSource;
    }

    /** Forgets the last local check, so the next {@link #resolveCached(ResourceSource, String)} call checks the file system again. */
    public void reset() {
        this.lastLocalCheck = null;
    }

    @Nullable
    public static String resolve(@NotNull ResourceSource rawSource, @NotNull String placeholderExpandedSource) {
        if (rawSource.getSourceType() != ResourceSourceType.LOCAL) return placeholderExpandedSource;
        try {
            return resolve(rawSource, placeholderExpandedSource, LocalSourcePathResolver.createForGameAndMinecraftDirectories());
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    static String resolve(@NotNull ResourceSource rawSource, @NotNull String placeholderExpandedSource, @NotNull LocalSourcePathResolver resolver) {
        return (rawSource.getSourceType() == ResourceSourceType.LOCAL) ? resolveLocal(placeholderExpandedSource, resolver) : placeholderExpandedSource;
    }

    @Nullable
    static String resolveLocal(@NotNull String placeholderExpandedSource, @NotNull LocalSourcePathResolver resolver) {
        try {
            LocalSourcePathResolver.ResolvedPath resolvedPath = resolver.resolve(placeholderExpandedSource);
            Path path = resolvedPath.revalidate();
            if (!Files.isRegularFile(path)) return null;
            // Chromium opens the URI after this handoff, so portable Java cannot make validation and Chromium's open
            // atomic. Revalidating at the handoff closes placeholder traversal and ancestor-swap paths as far as the
            // Java boundary can guarantee.
            return resolvedPath.revalidate().toUri().toString();
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private record LocalCheck(@NotNull String placeholderExpandedSource, @Nullable String resolvedSource, long checkedAtNanos) {
    }
}
