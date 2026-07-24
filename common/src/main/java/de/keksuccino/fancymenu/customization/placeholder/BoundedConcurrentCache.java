package de.keksuccino.fancymenu.customization.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.ToLongBiFunction;

/**
 * A small concurrent cache with lock-free reads and exact entry/weight bounds.
 *
 * <p>Every write is serialized so the map and eviction order stay in one coherent state. Updated keys are removed
 * from the eviction queue before being re-added, which is important: leaving stale queue nodes behind could let an
 * older node evict a newer value for the same key. Cache misses may race and calculate the same value more than once,
 * but publication and eviction remain safe and bounded.</p>
 */
final class BoundedConcurrentCache<K, V> {

    private final ConcurrentMap<K, WeightedValue<V>> entries = new ConcurrentHashMap<>();
    private final ArrayDeque<K> evictionOrder = new ArrayDeque<>();
    private final Object writeLock = new Object();
    private final int maximumEntries;
    private final long maximumWeight;
    private final ToLongBiFunction<? super K, ? super V> weigher;
    private long weightedSize;

    BoundedConcurrentCache(int maximumEntries, long maximumWeight, @NotNull ToLongBiFunction<? super K, ? super V> weigher) {
        if (maximumEntries <= 0) throw new IllegalArgumentException("maximumEntries must be positive");
        if (maximumWeight <= 0L) throw new IllegalArgumentException("maximumWeight must be positive");
        this.maximumEntries = maximumEntries;
        this.maximumWeight = maximumWeight;
        this.weigher = Objects.requireNonNull(weigher);
    }

    @Nullable
    V get(@NotNull K key) {
        WeightedValue<V> value = this.entries.get(Objects.requireNonNull(key));
        return (value != null) ? value.value() : null;
    }

    void put(@NotNull K key, @NotNull V value) {
        synchronized (this.writeLock) {
            this.putLocked(Objects.requireNonNull(key), Objects.requireNonNull(value));
        }
    }

    @Nullable
    V compute(@NotNull K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(remappingFunction);
        synchronized (this.writeLock) {
            WeightedValue<V> previous = this.entries.get(key);
            V replacement = remappingFunction.apply(key, (previous != null) ? previous.value() : null);
            if (replacement == null) {
                this.removeLocked(key);
            } else {
                this.putLocked(key, replacement);
            }
            return replacement;
        }
    }

    int size() {
        return this.entries.size();
    }

    long weightedSize() {
        synchronized (this.writeLock) {
            return this.weightedSize;
        }
    }

    private void putLocked(@NotNull K key, @NotNull V value) {
        long weight = Math.max(1L, this.weigher.applyAsLong(key, value));
        this.removeLocked(key);
        if (weight > this.maximumWeight) return;
        while ((this.entries.size() >= this.maximumEntries) || wouldExceed(this.weightedSize, weight, this.maximumWeight)) {
            K evictedKey = this.evictionOrder.pollFirst();
            if (evictedKey == null) break;
            WeightedValue<V> evicted = this.entries.remove(evictedKey);
            if (evicted != null) this.weightedSize -= evicted.weight();
        }
        this.entries.put(key, new WeightedValue<>(value, weight));
        this.evictionOrder.addLast(key);
        this.weightedSize += weight;
    }

    private void removeLocked(@NotNull K key) {
        WeightedValue<V> removed = this.entries.remove(key);
        if (removed == null) return;
        this.weightedSize -= removed.weight();
        this.evictionOrder.removeFirstOccurrence(key);
    }

    private static boolean wouldExceed(long currentWeight, long addedWeight, long maximumWeight) {
        return (addedWeight > (maximumWeight - currentWeight));
    }

    private record WeightedValue<V>(@NotNull V value, long weight) {
    }

}
