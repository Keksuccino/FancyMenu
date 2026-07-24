package de.keksuccino.fancymenu.util.threading;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;

/**
 * A small stale-while-refresh cache for placeholder sources. Cache reads stay lock-free, while
 * admission rechecks freshness under {@link AsyncLoadCoordinator}'s publication boundary so a
 * caller cannot schedule redundant work from an obsolete cache snapshot.
 */
public final class AsyncRefreshingValueCache<K, V> {

    public static final long NO_REFRESH = -1L;

    private final ConcurrentMap<K, TimedValue<V>> cache = new ConcurrentHashMap<>();
    private final AsyncLoadCoordinator<K> loadCoordinator = new AsyncLoadCoordinator<>();
    private final TaskLauncher taskLauncher;
    private final ValueLoader<K, V> valueLoader;
    private final LoadFailureHandler<K, V> loadFailureHandler;
    private final BiConsumer<K, RuntimeException> launchFailureHandler;
    private final LongSupplier timeSource;

    public AsyncRefreshingValueCache(@NotNull TaskLauncher taskLauncher, @NotNull ValueLoader<K, V> valueLoader, @NotNull LoadFailureHandler<K, V> loadFailureHandler, @NotNull BiConsumer<K, RuntimeException> launchFailureHandler, @NotNull LongSupplier timeSource) {
        this.taskLauncher = Objects.requireNonNull(taskLauncher, "taskLauncher");
        this.valueLoader = Objects.requireNonNull(valueLoader, "valueLoader");
        this.loadFailureHandler = Objects.requireNonNull(loadFailureHandler, "loadFailureHandler");
        this.launchFailureHandler = Objects.requireNonNull(launchFailureHandler, "launchFailureHandler");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
    }

    @Nullable
    public V getOrLoad(@NotNull K key, long refreshIntervalMillis) {
        Objects.requireNonNull(key, "key");
        if (refreshIntervalMillis < NO_REFRESH) throw new IllegalArgumentException("refreshIntervalMillis must be non-negative or NO_REFRESH");
        long currentTime = this.timeSource.getAsLong();
        TimedValue<V> cached = this.cache.get(key);
        if (this.needsRefresh(cached, currentTime, refreshIntervalMillis)) this.tryLaunch(key, currentTime, refreshIntervalMillis);
        return cached == null ? null : cached.value();
    }

    @Nullable
    public V getCached(@NotNull K key) {
        TimedValue<V> cached = this.cache.get(Objects.requireNonNull(key, "key"));
        return cached == null ? null : cached.value();
    }

    public boolean isLoading(@NotNull K key) {
        return this.loadCoordinator.isLoading(Objects.requireNonNull(key, "key"));
    }

    public int size() {
        return this.cache.size();
    }

    public void clear() {
        this.loadCoordinator.reset(this.cache::clear);
    }

    private void tryLaunch(@NotNull K key, long currentTime, long refreshIntervalMillis) {
        AsyncLoadCoordinator.Claim<K> claim = this.loadCoordinator.tryClaim(key, currentTime, () -> this.needsRefresh(this.cache.get(key), currentTime, refreshIntervalMillis));
        if (claim == null) return;
        try {
            this.taskLauncher.launch(() -> this.loadCoordinator.runClaim(claim, () -> this.loadAndPublish(claim)));
        } catch (RuntimeException exception) {
            this.loadCoordinator.abandon(claim);
            this.launchFailureHandler.accept(key, exception);
        } catch (Error error) {
            this.loadCoordinator.abandon(claim);
            throw error;
        }
    }

    private void loadAndPublish(@NotNull AsyncLoadCoordinator.Claim<K> claim) {
        V value;
        try {
            value = Objects.requireNonNull(this.valueLoader.load(claim.key()), "The asynchronous value loader returned null");
        } catch (Exception exception) {
            value = Objects.requireNonNull(this.loadFailureHandler.recover(claim.key(), exception), "The asynchronous load failure handler returned null");
        }
        V publishedValue = value;
        long loadedAt = this.timeSource.getAsLong();
        this.loadCoordinator.publishIfCurrent(claim, () -> this.cache.put(claim.key(), new TimedValue<>(loadedAt, publishedValue)));
    }

    private boolean needsRefresh(@Nullable TimedValue<V> cached, long currentTime, long refreshIntervalMillis) {
        if (cached == null) return true;
        if (refreshIntervalMillis == NO_REFRESH) return false;
        return (currentTime - cached.loadedAt()) >= refreshIntervalMillis;
    }

    private record TimedValue<V>(long loadedAt, @NotNull V value) {

        private TimedValue {
            Objects.requireNonNull(value, "value");
        }

    }

    @FunctionalInterface
    public interface TaskLauncher {

        void launch(@NotNull Runnable task);

    }

    @FunctionalInterface
    public interface ValueLoader<K, V> {

        @NotNull V load(@NotNull K key) throws Exception;

    }

    @FunctionalInterface
    public interface LoadFailureHandler<K, V> {

        @NotNull V recover(@NotNull K key, @NotNull Exception exception);

    }

}
