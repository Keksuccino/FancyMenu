package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.util.threading.AsyncLoadCoordinator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongSupplier;

/**
 * Owns remote JSON placeholder cache state and its asynchronous load lifecycle. Timeout and reload
 * share the coordinator's publication boundary, so even an interrupted loader that keeps running
 * cannot publish or release a newer retry.
 */
final class JsonPlaceholderWebCache {

    private static final Logger LOGGER = LogManager.getLogger();

    private final ConcurrentMap<String, List<String>> cachedPlaceholders = new ConcurrentHashMap<>();
    private final Set<String> invalidSources = ConcurrentHashMap.newKeySet();
    private final AsyncLoadCoordinator<String> loadCoordinator = new AsyncLoadCoordinator<>();
    private final TaskLauncher taskLauncher;
    private final JsonLoader jsonLoader;
    private final LongSupplier timeSource;
    private final long timeoutMillis;

    JsonPlaceholderWebCache(@NotNull TaskLauncher taskLauncher, @NotNull JsonLoader jsonLoader, @NotNull LongSupplier timeSource, long timeoutMillis) {
        if (timeoutMillis < 0L) throw new IllegalArgumentException("timeoutMillis must not be negative");
        this.taskLauncher = Objects.requireNonNull(taskLauncher, "taskLauncher");
        this.jsonLoader = Objects.requireNonNull(jsonLoader, "jsonLoader");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
        this.timeoutMillis = timeoutMillis;
    }

    @NotNull
    Lookup getOrLoad(@NotNull String placeholder, @NotNull String source, @NotNull String jsonPath) {
        Objects.requireNonNull(placeholder, "placeholder");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(jsonPath, "jsonPath");
        Lookup existing = this.getExisting(placeholder, source);
        if (existing != null) return existing;
        long currentTime = this.timeSource.getAsLong();
        AsyncLoadCoordinator.Claim<String> claim = this.loadCoordinator.tryClaim(placeholder, currentTime, () -> this.getExisting(placeholder, source) == null);
        if (claim != null) {
            this.launch(claim, source, jsonPath);
            return Lookup.loading();
        }
        Lookup resolvedDuringAdmission = this.getExisting(placeholder, source);
        return resolvedDuringAdmission == null ? Lookup.loading() : resolvedDuringAdmission;
    }

    void loadIfAbsent(@NotNull String placeholder, @NotNull String source, @NotNull String jsonPath) {
        this.getOrLoad(placeholder, source, jsonPath);
    }

    boolean isInvalidSource(@NotNull String source) {
        return this.invalidSources.contains(Objects.requireNonNull(source, "source"));
    }

    @Nullable
    List<String> getCached(@NotNull String placeholder) {
        return this.cachedPlaceholders.get(Objects.requireNonNull(placeholder, "placeholder"));
    }

    boolean isLoading(@NotNull String placeholder) {
        return this.loadCoordinator.isLoading(Objects.requireNonNull(placeholder, "placeholder"));
    }

    @NotNull
    List<String> cleanupTimedOut() {
        List<String> timedOut = this.loadCoordinator.cancelTimedOut(this.timeSource.getAsLong(), this.timeoutMillis);
        for (String placeholder : timedOut) LOGGER.warn("[FANCYMENU] Placeholder update timed out for: {}", placeholder);
        return timedOut;
    }

    void reload() {
        this.loadCoordinator.reset(() -> {
            this.cachedPlaceholders.clear();
            this.invalidSources.clear();
        });
    }

    long generationNumber() {
        return this.loadCoordinator.generationNumber();
    }

    @Nullable
    private Lookup getExisting(@NotNull String placeholder, @NotNull String source) {
        if (this.invalidSources.contains(source)) return Lookup.invalid();
        List<String> cached = this.cachedPlaceholders.get(placeholder);
        return cached == null ? null : Lookup.loaded(cached);
    }

    private void launch(@NotNull AsyncLoadCoordinator.Claim<String> claim, @NotNull String source, @NotNull String jsonPath) {
        try {
            this.taskLauncher.launch(() -> this.loadCoordinator.runClaim(claim, () -> this.loadAndPublish(claim, source, jsonPath)));
        } catch (RuntimeException exception) {
            this.loadCoordinator.abandon(claim);
            LOGGER.error("[FANCYMENU] Failed to start the web JSON loader for '{}'.", source, exception);
        } catch (Error error) {
            this.loadCoordinator.abandon(claim);
            throw error;
        }
    }

    private void loadAndPublish(@NotNull AsyncLoadCoordinator.Claim<String> claim, @NotNull String source, @NotNull String jsonPath) {
        LoadResult result;
        try {
            result = Objects.requireNonNull(this.jsonLoader.load(source, jsonPath), "The web JSON loader returned null");
        } catch (Exception exception) {
            if (!claim.isCancelled()) LOGGER.error("[FANCYMENU] Error while caching a web JSON in the JsonPlaceholder!", exception);
            return;
        }
        this.loadCoordinator.publishIfCurrent(claim, () -> {
            if (result.valid()) {
                this.cachedPlaceholders.put(claim.key(), result.values());
            } else {
                this.invalidSources.add(source);
            }
        });
    }

    enum Status {
        LOADING,
        INVALID,
        LOADED
    }

    record Lookup(@NotNull Status status, @NotNull List<String> values) {

        private static final Lookup LOADING = new Lookup(Status.LOADING, List.of());
        private static final Lookup INVALID = new Lookup(Status.INVALID, List.of());

        Lookup {
            Objects.requireNonNull(status, "status");
            values = List.copyOf(Objects.requireNonNull(values, "values"));
            if ((status != Status.LOADED) && !values.isEmpty()) throw new IllegalArgumentException("Only a loaded lookup can contain values");
        }

        private static Lookup loading() {
            return LOADING;
        }

        private static Lookup invalid() {
            return INVALID;
        }

        private static Lookup loaded(@NotNull List<String> values) {
            return new Lookup(Status.LOADED, values);
        }

    }

    record LoadResult(boolean valid, @NotNull List<String> values) {

        LoadResult {
            values = List.copyOf(Objects.requireNonNull(values, "values"));
            if (!valid && !values.isEmpty()) throw new IllegalArgumentException("An invalid JSON source cannot contain values");
        }

        static LoadResult loaded(@NotNull List<String> values) {
            return new LoadResult(true, values);
        }

        static LoadResult invalid() {
            return new LoadResult(false, List.of());
        }

    }

    @FunctionalInterface
    interface TaskLauncher {

        void launch(@NotNull Runnable task);

    }

    @FunctionalInterface
    interface JsonLoader {

        @NotNull LoadResult load(@NotNull String source, @NotNull String jsonPath) throws Exception;

    }

}
