package de.keksuccino.fancymenu.customization.element.elements.button.custombutton;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Short-lived template cache separated by the identity of the layer that owns the candidates.
 * Weak identity keys are important here: screen layers are runtime scopes, not values, and this cache must not extend
 * the lifetime of a layer if its registry ownership changes in the future.
 */
final class LayerScopedTemplateCache<S, T> {

    private final long cacheDurationMillis;
    @NotNull private final LongSupplier clock;
    @NotNull private final ReferenceQueue<S> collectedScopes = new ReferenceQueue<>();
    @NotNull private final Map<IdentityWeakReference<S>, ScopeEntries<T>> entriesByScope = new HashMap<>();

    LayerScopedTemplateCache(long cacheDurationMillis) {
        this(cacheDurationMillis, System::currentTimeMillis);
    }

    LayerScopedTemplateCache(long cacheDurationMillis, @NotNull LongSupplier clock) {
        if (cacheDurationMillis < 0L) throw new IllegalArgumentException("cacheDurationMillis must not be negative");
        this.cacheDurationMillis = cacheDurationMillis;
        this.clock = clock;
    }

    synchronized @Nullable T resolve(@NotNull S scope, boolean forSlider, @NotNull Supplier<@Nullable T> resolver) {
        this.removeCollectedScopes();
        long now = this.clock.getAsLong();
        IdentityWeakReference<S> lookupKey = new IdentityWeakReference<>(scope);
        ScopeEntries<T> scopeEntries = this.entriesByScope.get(lookupKey);
        if (scopeEntries == null) {
            scopeEntries = new ScopeEntries<>();
            this.entriesByScope.put(new IdentityWeakReference<>(scope, this.collectedScopes), scopeEntries);
        }
        CacheEntry<T> cached = forSlider ? scopeEntries.slider : scopeEntries.button;
        if ((cached != null) && this.isFresh(cached, now)) return cached.value;
        T value = resolver.get();
        CacheEntry<T> refreshed = new CacheEntry<>(now, value);
        if (forSlider) {
            scopeEntries.slider = refreshed;
        } else {
            scopeEntries.button = refreshed;
        }
        return value;
    }

    synchronized void clear() {
        this.entriesByScope.clear();
        while (this.collectedScopes.poll() != null) {
            // Drain stale references so a later cache population starts with an empty queue as well as an empty map.
        }
    }

    synchronized void invalidate(@NotNull S scope) {
        this.removeCollectedScopes();
        this.entriesByScope.remove(new IdentityWeakReference<>(scope));
    }

    private boolean isFresh(@NotNull CacheEntry<T> cached, long now) {
        if (now < cached.resolvedAtMillis) return false;
        // The signed subtraction can overflow across the full long domain. Comparing the elapsed bits as unsigned keeps
        // exact expiry semantics without mistaking an extremely old entry for a fresh one after that overflow.
        return Long.compareUnsigned(now - cached.resolvedAtMillis, this.cacheDurationMillis) < 0;
    }

    private void removeCollectedScopes() {
        IdentityWeakReference<?> collected;
        while ((collected = (IdentityWeakReference<?>)this.collectedScopes.poll()) != null) {
            this.entriesByScope.remove(collected);
        }
    }

    private static final class ScopeEntries<T> {

        @Nullable private CacheEntry<T> button;
        @Nullable private CacheEntry<T> slider;

    }

    private static final class CacheEntry<T> {

        private final long resolvedAtMillis;
        @Nullable private final T value;

        private CacheEntry(long resolvedAtMillis, @Nullable T value) {
            this.resolvedAtMillis = resolvedAtMillis;
            this.value = value;
        }

    }

    private static final class IdentityWeakReference<T> extends WeakReference<T> {

        private final int identityHashCode;

        private IdentityWeakReference(@NotNull T referent) {
            super(referent);
            this.identityHashCode = System.identityHashCode(referent);
        }

        private IdentityWeakReference(@NotNull T referent, @NotNull ReferenceQueue<T> queue) {
            super(referent, queue);
            this.identityHashCode = System.identityHashCode(referent);
        }

        @Override
        public int hashCode() {
            return this.identityHashCode;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof IdentityWeakReference<?> reference)) return false;
            Object referent = this.get();
            return (referent != null) && (referent == reference.get());
        }

    }

}
