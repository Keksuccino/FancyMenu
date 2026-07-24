package de.keksuccino.fancymenu.customization.remote;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Atomically caps active connection states while retaining a separately bounded access-ordered request-ID cache.
 * Cache eviction skips active URLs so a live state's identity can never change underneath it.
 */
final class BoundedConnectionRegistry<T> {

    private static final int MAX_REQUEST_ID_GENERATION_ATTEMPTS = 128;

    private final Map<String, T> statesByUrl = new HashMap<>();
    private final Map<String, T> statesByRequestId = new HashMap<>();
    private final LinkedHashMap<String, String> cachedRequestIdsByUrl = new LinkedHashMap<>(16, 0.75F, true);
    private final int maxActiveStates;
    private final int maxCachedRequestIds;

    BoundedConnectionRegistry(int maxActiveStates, int maxCachedRequestIds) {
        if (maxActiveStates <= 0 || maxCachedRequestIds < maxActiveStates) {
            throw new IllegalArgumentException("The request-ID cache must fit every active state");
        }
        this.maxActiveStates = maxActiveStates;
        this.maxCachedRequestIds = maxCachedRequestIds;
    }

    synchronized @NotNull Admission<T> getOrCreate(@NotNull String normalizedUrl, @NotNull Supplier<String> requestIdSupplier, @NotNull BiFunction<String, String, T> stateFactory) {
        T existing = this.statesByUrl.get(normalizedUrl);
        if (existing != null) {
            this.cachedRequestIdsByUrl.get(normalizedUrl);
            return new Admission<>(AdmissionType.EXISTING, existing);
        }
        if (this.statesByUrl.size() >= this.maxActiveStates) {
            return new Admission<>(AdmissionType.CAPACITY_EXCEEDED, null);
        }

        String requestId = this.cachedRequestIdsByUrl.get(normalizedUrl);
        if (requestId == null || this.statesByRequestId.containsKey(requestId)) {
            requestId = createUniqueRequestId(requestIdSupplier);
            if (requestId == null) {
                return new Admission<>(AdmissionType.REQUEST_ID_EXHAUSTED, null);
            }
        }

        T state = stateFactory.apply(normalizedUrl, requestId);
        this.statesByUrl.put(normalizedUrl, state);
        this.statesByRequestId.put(requestId, state);
        this.cachedRequestIdsByUrl.put(normalizedUrl, requestId);
        trimRequestIdCache();
        return new Admission<>(AdmissionType.CREATED, state);
    }

    synchronized @Nullable T getByRequestId(@NotNull String requestId) {
        return this.statesByRequestId.get(requestId);
    }

    synchronized @Nullable T getByUrl(@NotNull String normalizedUrl) {
        return this.statesByUrl.get(normalizedUrl);
    }

    synchronized boolean remove(@NotNull String normalizedUrl, @NotNull String requestId, @NotNull T expectedState) {
        if (this.statesByUrl.get(normalizedUrl) != expectedState || this.statesByRequestId.get(requestId) != expectedState) {
            return false;
        }
        this.statesByUrl.remove(normalizedUrl);
        this.statesByRequestId.remove(requestId);
        return true;
    }

    synchronized @NotNull List<T> snapshot() {
        return List.copyOf(this.statesByUrl.values());
    }

    synchronized int activeStateCount() {
        return this.statesByUrl.size();
    }

    synchronized int cachedRequestIdCount() {
        return this.cachedRequestIdsByUrl.size();
    }

    synchronized void clear() {
        this.statesByUrl.clear();
        this.statesByRequestId.clear();
        this.cachedRequestIdsByUrl.clear();
    }

    synchronized @Nullable String cachedRequestId(@NotNull String normalizedUrl) {
        return this.cachedRequestIdsByUrl.get(normalizedUrl);
    }

    private @Nullable String createUniqueRequestId(@NotNull Supplier<String> requestIdSupplier) {
        for (int attempt = 0; attempt < MAX_REQUEST_ID_GENERATION_ATTEMPTS; attempt++) {
            String candidate = requestIdSupplier.get();
            if (candidate != null && !candidate.isBlank() && !this.statesByRequestId.containsKey(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private void trimRequestIdCache() {
        while (this.cachedRequestIdsByUrl.size() > this.maxCachedRequestIds) {
            boolean removed = false;
            Iterator<Map.Entry<String, String>> iterator = this.cachedRequestIdsByUrl.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                if (!this.statesByUrl.containsKey(entry.getKey())) {
                    iterator.remove();
                    removed = true;
                    break;
                }
            }
            if (!removed) {
                throw new IllegalStateException("No inactive request-ID cache entry was available for eviction");
            }
        }
    }

    enum AdmissionType {
        CREATED,
        EXISTING,
        CAPACITY_EXCEEDED,
        REQUEST_ID_EXHAUSTED
    }

    record Admission<T>(@NotNull AdmissionType type, @Nullable T state) {
    }
}
