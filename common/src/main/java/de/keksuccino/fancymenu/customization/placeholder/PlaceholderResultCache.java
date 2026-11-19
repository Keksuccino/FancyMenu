//Copyright (c) 2022-2025 Keksuccino.
//This code is licensed under DSMSLv3.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.customization.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

/**
 * Keeps parse results for the placeholder caching duration (see {@link PlaceholderParser.PlaceholderCachingController}),
 * so texts that get parsed every frame only get parsed again when their result can have changed.
 *
 * <p>Results are keyed by the text as it got passed to the parser, before any processor ran.</p>
 */
final class PlaceholderResultCache {

    private static final int CACHE_MAX_ENTRIES = 512;
    private static final long CACHE_MAX_WEIGHT = 4_194_304L;
    private static final long ENTRY_OVERHEAD_WEIGHT = 64L;

    // String length is measured in retained UTF-16 code units, with a fixed allowance for each entry's objects.
    private static final BoundedConcurrentCache<String, CachedResult> RESULTS = new BoundedConcurrentCache<>(CACHE_MAX_ENTRIES, CACHE_MAX_WEIGHT, (text, cached) -> ENTRY_OVERHEAD_WEIGHT + text.length() + cached.result().length());
    private static final Object CACHING_CONTROLLER_LOCK = new Object();

    private static long cachingControllerRevision = 0L;
    private static volatile CachingControllerSnapshot cachingControllerSnapshot = new CachingControllerSnapshot(0L, new PlaceholderParser.PlaceholderCachingController(() -> true, () -> 30L));

    private PlaceholderResultCache() {
    }

    @NotNull
    static CachingControllerSnapshot cachingControllerSnapshot() {
        return cachingControllerSnapshot;
    }

    static void setCachingController(@NotNull PlaceholderParser.PlaceholderCachingController cachingController) {
        Objects.requireNonNull(cachingController);
        synchronized (CACHING_CONTROLLER_LOCK) {
            long revision = Math.incrementExact(cachingControllerRevision);
            cachingControllerRevision = revision;
            cachingControllerSnapshot = new CachingControllerSnapshot(revision, cachingController);
        }
    }

    /**
     * @return The cached result, or null if there is none that is still usable for the context.
     */
    @Nullable
    static String get(@NotNull String text, @NotNull PlaceholderEvaluator.ParsingContext context) {
        CachedResult cached = RESULTS.get(text);
        return ((cached != null) && cached.isUsableFor(context, System.currentTimeMillis())) ? cached.result() : null;
    }

    static void put(@NotNull String text, @NotNull String result, @NotNull PlaceholderEvaluator.ParsingContext context) {
        RESULTS.put(text, new CachedResult(result, System.currentTimeMillis(), context.processors().revision(), context.cachingControllerRevision()));
    }

    record CachingControllerSnapshot(long revision, @NotNull PlaceholderParser.PlaceholderCachingController controller) {
    }

    /**
     * Revisions are validity tokens, not just diagnostics. An old parse may finish after a processor/controller
     * change and overwrite the same cache slot; a later parse must reject that value instead of observing old logic.
     */
    private record CachedResult(@NotNull String result, long cachedAtMillis, long processorRevision, long cachingControllerRevision) {

        private boolean isUsableFor(@NotNull PlaceholderEvaluator.ParsingContext context, long nowMillis) {
            if ((this.processorRevision != context.processors().revision()) || (this.cachingControllerRevision != context.cachingControllerRevision())) return false;
            if (nowMillis < this.cachedAtMillis) return false;
            long elapsed = nowMillis - this.cachedAtMillis;
            return (elapsed >= 0L) && (elapsed < context.cachingDurationMillis());
        }

    }

}
