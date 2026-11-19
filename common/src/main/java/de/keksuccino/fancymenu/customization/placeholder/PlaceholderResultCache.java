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
 * <p>Results are keyed by the text as it got passed to the parser, before any processor ran. Whole texts and the values of placeholders
 * have separate caches: values don't get their formatting codes converted, so their results differ from the result of the same text
 * parsed on its own.</p>
 *
 * <p>Parses inside a {@link de.keksuccino.fancymenu.customization.FreshValuesScope} (action scripts) don't read either cache, because scripts
 * read values right after changing them, but they still store their results, so rendering gets the fresh results too.
 * See {@link PlaceholderEvaluator.ParsingContext#readCachedResults()}.</p>
 */
final class PlaceholderResultCache {

    private static final int TEXT_CACHE_MAX_ENTRIES = 512;
    // The weight limits are sized for PlaceholderParser#MAX_TEXT_LENGTH, see there. An entry weighs its text plus its result.
    private static final long TEXT_CACHE_MAX_WEIGHT = 4_194_304L;
    private static final int VALUE_CACHE_MAX_ENTRIES = 512;
    private static final long VALUE_CACHE_MAX_WEIGHT = 2_097_152L;
    private static final long ENTRY_OVERHEAD_WEIGHT = 64L;

    // String length is measured in retained UTF-16 code units, with a fixed allowance for each entry's objects.
    private static final BoundedConcurrentCache<String, CachedResult> TEXT_RESULTS = new BoundedConcurrentCache<>(TEXT_CACHE_MAX_ENTRIES, TEXT_CACHE_MAX_WEIGHT, (text, cached) -> ENTRY_OVERHEAD_WEIGHT + text.length() + cached.result().length());
    private static final BoundedConcurrentCache<String, CachedResult> VALUE_RESULTS = new BoundedConcurrentCache<>(VALUE_CACHE_MAX_ENTRIES, VALUE_CACHE_MAX_WEIGHT, (text, cached) -> ENTRY_OVERHEAD_WEIGHT + text.length() + cached.result().length());
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
     * @param value If the text is the value of a placeholder.
     * @return The cached result, or null if there is none that is still usable for the context.
     */
    @Nullable
    static String get(@NotNull String text, boolean value, @NotNull PlaceholderEvaluator.ParsingContext context) {
        CachedResult cached = (value ? VALUE_RESULTS : TEXT_RESULTS).get(text);
        return ((cached != null) && cached.isUsableFor(context, System.currentTimeMillis())) ? cached.result() : null;
    }

    static void put(@NotNull String text, @NotNull String result, boolean value, @NotNull PlaceholderEvaluator.ParsingContext context) {
        (value ? VALUE_RESULTS : TEXT_RESULTS).put(text, new CachedResult(result, System.currentTimeMillis(), context.processors().revision(), context.cachingControllerRevision()));
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
