package de.keksuccino.fancymenu.customization.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.ToLongBiFunction;

/**
 * A small concurrent cache with lock-free reads, exact entry/weight bounds and first-in-first-out eviction.
 *
 * <p>Reads only touch the {@link ConcurrentHashMap}. Every write is serialized by one lock, so the map, the eviction
 * queue and the weight total always change together. Apart from the entries it evicts, every write takes constant time.
 * Each entry is its own node in a doubly linked eviction queue, ordered from the oldest to the newest write. Writing a
 * new value for a key that is already cached replaces the value of its existing node in place and moves that node to
 * the end of the queue. Two things depend on this:</p>
 * <ul>
 *     <li>A cached key never disappears from the map while its value gets replaced, so lock-free readers always see
 *     either the old or the new value, never a miss.</li>
 *     <li>The queue holds exactly one node per entry. There are no stale nodes of older values that could evict a newer
 *     value of the same key, and refreshing the same keys over and over can never grow the queue. Never give a key a
 *     second node while it still has one.</li>
 * </ul>
 * <p>Cache misses may race and calculate the same value more than once, but publication and eviction remain safe and
 * bounded.</p>
 */
public final class BoundedConcurrentCache<K, V> {

    private final ConcurrentMap<K, Node<K, V>> entries = new ConcurrentHashMap<>();
    private final Object writeLock = new Object();
    private final int maximumEntries;
    private final long maximumWeight;
    private final ToLongBiFunction<? super K, ? super V> weigher;
    // Everything below is guarded by the write lock
    @Nullable
    private Node<K, V> oldest;
    @Nullable
    private Node<K, V> newest;
    private int entryCount;
    private long weightedSize;

    /**
     * @param maximumEntries The maximum number of cached entries.
     * @param maximumWeight The maximum total weight of all cached entries. Values heavier than this never get cached.
     * @param weigher Calculates the weight of an entry. Weights below 1 count as 1.
     */
    public BoundedConcurrentCache(int maximumEntries, long maximumWeight, @NotNull ToLongBiFunction<? super K, ? super V> weigher) {
        if (maximumEntries <= 0) throw new IllegalArgumentException("maximumEntries must be positive");
        if (maximumWeight <= 0L) throw new IllegalArgumentException("maximumWeight must be positive");
        this.maximumEntries = maximumEntries;
        this.maximumWeight = maximumWeight;
        this.weigher = Objects.requireNonNull(weigher);
    }

    @Nullable
    public V get(@NotNull K key) {
        Node<K, V> node = this.entries.get(Objects.requireNonNull(key));
        return (node != null) ? node.value : null;
    }

    /**
     * Caches the value for the key, replacing its old value if it has one. A value heavier than the maximum weight
     * removes the old value of the key without getting cached itself.
     */
    public void put(@NotNull K key, @NotNull V value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        long weight = this.weigh(key, value);
        synchronized (this.writeLock) {
            this.putLocked(key, value, weight);
        }
    }

    /**
     * Caches the value for the key only if the key has no cached value yet.
     *
     * @return The value that was already cached for the key, or {@code null} if the key had none.
     */
    @Nullable
    public V putIfAbsent(@NotNull K key, @NotNull V value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        long weight = this.weigh(key, value);
        synchronized (this.writeLock) {
            Node<K, V> existing = this.entries.get(key);
            if (existing != null) return existing.value;
            this.putLocked(key, value, weight);
            return null;
        }
    }

    /**
     * Atomically replaces the cached value of the key with the result of the function, like {@link #put(Object, Object)},
     * or removes the key if the function returns {@code null}. The function runs while holding the write lock, so keep it short.
     *
     * @return The result of the function.
     */
    @Nullable
    public V compute(@NotNull K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(remappingFunction);
        synchronized (this.writeLock) {
            Node<K, V> previous = this.entries.get(key);
            V replacement = remappingFunction.apply(key, (previous != null) ? previous.value : null);
            if (replacement == null) {
                if (previous != null) this.removeLocked(previous);
            } else {
                this.putLocked(key, replacement, this.weigh(key, replacement));
            }
            return replacement;
        }
    }

    public void clear() {
        synchronized (this.writeLock) {
            this.entries.clear();
            this.oldest = null;
            this.newest = null;
            this.entryCount = 0;
            this.weightedSize = 0L;
        }
    }

    public int size() {
        return this.entries.size();
    }

    public long weightedSize() {
        synchronized (this.writeLock) {
            return this.weightedSize;
        }
    }

    /**
     * Walks the whole eviction queue to count its nodes, for consistency checks of the queue.
     */
    int evictionQueueLength() {
        synchronized (this.writeLock) {
            int length = 0;
            for (Node<K, V> node = this.oldest; node != null; node = node.newer) length++;
            return length;
        }
    }

    private long weigh(@NotNull K key, @NotNull V value) {
        return Math.max(1L, this.weigher.applyAsLong(key, value));
    }

    private void putLocked(@NotNull K key, @NotNull V value, long weight) {
        Node<K, V> node = this.entries.get(key);
        if (weight > this.maximumWeight) {
            if (node != null) this.removeLocked(node);
            return;
        }
        if (node != null) {
            // The node stays in the map while it is out of the queue, so readers keep getting the old value until the new one gets set
            this.unlink(node);
            this.weightedSize -= node.weight;
            this.evictOldestWhileFull(false, weight);
            node.value = value;
            node.weight = weight;
        } else {
            this.evictOldestWhileFull(true, weight);
            node = new Node<>(key, value, weight);
            this.entries.put(key, node);
            this.entryCount++;
        }
        this.linkAsNewest(node);
        this.weightedSize += weight;
    }

    /**
     * Evicts the oldest entries until an entry of the given weight fits, counting it as an additional entry if it is new.
     */
    private void evictOldestWhileFull(boolean newEntry, long weight) {
        while ((this.oldest != null) && ((newEntry && (this.entryCount >= this.maximumEntries)) || wouldExceed(this.weightedSize, weight, this.maximumWeight))) {
            this.removeLocked(this.oldest);
        }
    }

    private void removeLocked(@NotNull Node<K, V> node) {
        this.unlink(node);
        // Removing by node keeps the mapping of the key if it ever belongs to another node
        if (this.entries.remove(node.key, node)) {
            this.entryCount--;
            this.weightedSize -= node.weight;
        }
    }

    private void linkAsNewest(@NotNull Node<K, V> node) {
        node.older = this.newest;
        node.newer = null;
        if (this.newest != null) {
            this.newest.newer = node;
        } else {
            this.oldest = node;
        }
        this.newest = node;
    }

    private void unlink(@NotNull Node<K, V> node) {
        Node<K, V> older = node.older;
        Node<K, V> newer = node.newer;
        if (older != null) {
            older.newer = newer;
        } else if (this.oldest == node) {
            this.oldest = newer;
        }
        if (newer != null) {
            newer.older = older;
        } else if (this.newest == node) {
            this.newest = older;
        }
        node.older = null;
        node.newer = null;
    }

    private static boolean wouldExceed(long currentWeight, long addedWeight, long maximumWeight) {
        return (addedWeight > (maximumWeight - currentWeight));
    }

    /**
     * A cache entry and its place in the eviction queue. Its identity matters: never override {@code equals}, because
     * {@link #removeLocked(Node)} relies on {@link ConcurrentMap#remove(Object, Object)} comparing nodes by identity.
     */
    private static final class Node<K, V> {

        private final K key;
        // Written under the write lock and read lock-free by get(), so it has to be volatile for safe publication
        private volatile V value;
        // The fields below are guarded by the write lock
        private long weight;
        @Nullable
        private Node<K, V> older;
        @Nullable
        private Node<K, V> newer;

        private Node(@NotNull K key, @NotNull V value, long weight) {
            this.key = key;
            this.value = value;
            this.weight = weight;
        }

    }

}
