package de.keksuccino.fancymenu.customization.placeholder.placeholders.other;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Owns the legacy web-text cache lifecycle without exposing mutable state to render or loader threads.
 * Every load belongs to exactly one generation, so reload can cancel old work without letting a late
 * completion repopulate the new generation.
 */
final class WebTextPlaceholderCache {

    private static final Logger LOGGER = LogManager.getLogger();

    private final TaskLauncher taskLauncher;
    private final ContentLoader contentLoader;
    private final AtomicReference<Generation> currentGeneration = new AtomicReference<>(new Generation(0L));

    WebTextPlaceholderCache(@NotNull TaskLauncher taskLauncher, @NotNull ContentLoader contentLoader) {
        this.taskLauncher = Objects.requireNonNull(taskLauncher, "taskLauncher");
        this.contentLoader = Objects.requireNonNull(contentLoader, "contentLoader");
    }

    @NotNull
    Lookup getOrLoad(@NotNull String placeholder, @NotNull String link) {
        Objects.requireNonNull(placeholder, "placeholder");
        Objects.requireNonNull(link, "link");
        while (true) {
            Generation generation = this.currentGeneration.get();
            Lookup existing = generation.getExisting(placeholder, link);
            if (existing != null) return existing;
            LookupDecision decision = generation.lookupOrClaim(placeholder, link, this.currentGeneration, this);
            if (decision.retry()) continue;
            LoadClaim claim = decision.claim();
            if (claim != null) this.launch(generation, claim);
            return decision.lookup();
        }
    }

    boolean isInvalidLink(@NotNull String link) {
        return this.currentGeneration.get().isInvalidLink(Objects.requireNonNull(link, "link"));
    }

    @Nullable
    List<String> getCached(@NotNull String placeholder) {
        return this.currentGeneration.get().getCached(Objects.requireNonNull(placeholder, "placeholder"));
    }

    boolean isLoading(@NotNull String placeholder) {
        return this.currentGeneration.get().isLoading(Objects.requireNonNull(placeholder, "placeholder"));
    }

    void loadIfAbsent(@NotNull String placeholder, @NotNull String link) {
        this.getOrLoad(placeholder, link);
    }

    void reload() {
        Generation previous;
        Generation replacement;
        do {
            previous = this.currentGeneration.get();
            replacement = new Generation(Math.incrementExact(previous.number()));
        } while (!this.currentGeneration.compareAndSet(previous, replacement));
        previous.cancel();
    }

    long generationNumber() {
        return this.currentGeneration.get().number();
    }

    private void launch(@NotNull Generation generation, @NotNull LoadClaim claim) {
        try {
            this.taskLauncher.launch(claim);
        } catch (RuntimeException exception) {
            claim.cancel(false);
            generation.release(claim.placeholder(), claim);
            LOGGER.error("[FANCYMENU] Failed to start the Web Text placeholder loader for '{}'.", claim.link(), exception);
        } catch (Error error) {
            claim.cancel(false);
            generation.release(claim.placeholder(), claim);
            throw error;
        }
    }

    private void load(@NotNull LoadClaim claim) {
        try {
            LoadResult result = Objects.requireNonNull(this.contentLoader.load(claim.link()), "The Web Text content loader returned null");
            claim.generation().publish(claim.placeholder(), claim.link(), result, this.currentGeneration);
        } catch (Exception exception) {
            if (claim.generation().isCurrent(this.currentGeneration) && !claim.isCancelled()) {
                LOGGER.error("[FANCYMENU] Failed to load Web Text placeholder content from '{}'.", claim.link(), exception);
            }
        } finally {
            claim.generation().release(claim.placeholder(), claim);
        }
    }

    enum Status {
        LOADING,
        INVALID,
        LOADED
    }

    record Lookup(@NotNull Status status, @NotNull List<String> lines) {

        private static final Lookup LOADING = new Lookup(Status.LOADING, List.of());
        private static final Lookup INVALID = new Lookup(Status.INVALID, List.of());

        Lookup {
            Objects.requireNonNull(status, "status");
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
            if ((status != Status.LOADED) && !lines.isEmpty()) throw new IllegalArgumentException("Only a loaded lookup can contain lines");
        }

        private static Lookup loading() {
            return LOADING;
        }

        private static Lookup invalid() {
            return INVALID;
        }

        private static Lookup loaded(@NotNull List<String> lines) {
            return new Lookup(Status.LOADED, lines);
        }

    }

    record LoadResult(boolean valid, @NotNull List<String> lines) {

        LoadResult {
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
            if (!valid && !lines.isEmpty()) throw new IllegalArgumentException("An invalid link result cannot contain lines");
        }

        static LoadResult valid(@NotNull List<String> lines) {
            return new LoadResult(true, lines);
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
    interface ContentLoader {

        @NotNull LoadResult load(@NotNull String link) throws Exception;

    }

    private record LookupDecision(@NotNull Lookup lookup, @Nullable LoadClaim claim, boolean retry) {

        private static LookupDecision loading(@Nullable LoadClaim claim) {
            return new LookupDecision(Lookup.loading(), claim, false);
        }

        private static LookupDecision resolved(@NotNull Lookup lookup) {
            return new LookupDecision(lookup, null, false);
        }

        private static LookupDecision retryCurrentGeneration() {
            return new LookupDecision(Lookup.loading(), null, true);
        }

    }

    private static final class Generation {

        private final long number;
        private final Object lifecycleLock = new Object();
        private final ConcurrentMap<String, Lookup> cached = new ConcurrentHashMap<>();
        private final Set<String> invalidLinks = ConcurrentHashMap.newKeySet();
        private final ConcurrentMap<String, LoadClaim> inFlight = new ConcurrentHashMap<>();
        private boolean cancelled;

        private Generation(long number) {
            this.number = number;
        }

        private long number() {
            return this.number;
        }

        @Nullable
        private Lookup getExisting(@NotNull String placeholder, @NotNull String link) {
            if (this.invalidLinks.contains(link)) return Lookup.invalid();
            return this.cached.get(placeholder);
        }

        private boolean isInvalidLink(@NotNull String link) {
            return this.invalidLinks.contains(link);
        }

        @Nullable
        private List<String> getCached(@NotNull String placeholder) {
            Lookup lookup = this.cached.get(placeholder);
            return lookup == null ? null : lookup.lines();
        }

        private boolean isLoading(@NotNull String placeholder) {
            return this.inFlight.containsKey(placeholder);
        }

        @NotNull
        private LookupDecision lookupOrClaim(@NotNull String placeholder, @NotNull String link, @NotNull AtomicReference<Generation> currentGeneration, @NotNull WebTextPlaceholderCache owner) {
            synchronized (this.lifecycleLock) {
                if (this.cancelled || (currentGeneration.get() != this)) return LookupDecision.retryCurrentGeneration();
                Lookup existing = this.getExisting(placeholder, link);
                if (existing != null) return LookupDecision.resolved(existing);
                LoadClaim claim = new LoadClaim(owner, this, placeholder, link);
                LoadClaim previous = this.inFlight.putIfAbsent(placeholder, claim);
                return LookupDecision.loading(previous == null ? claim : null);
            }
        }

        private void publish(@NotNull String placeholder, @NotNull String link, @NotNull LoadResult result, @NotNull AtomicReference<Generation> currentGeneration) {
            synchronized (this.lifecycleLock) {
                // The identity check is essential: network APIs may ignore interruption and return after reload.
                if (this.cancelled || (currentGeneration.get() != this)) return;
                if (result.valid()) {
                    this.cached.put(placeholder, Lookup.loaded(result.lines()));
                } else {
                    this.invalidLinks.add(link);
                }
            }
        }

        private void release(@NotNull String placeholder, @NotNull LoadClaim claim) {
            // Identity removal prevents an old completion from releasing a newer retry for the same placeholder.
            this.inFlight.remove(placeholder, claim);
        }

        private boolean isCurrent(@NotNull AtomicReference<Generation> currentGeneration) {
            synchronized (this.lifecycleLock) {
                return !this.cancelled && (currentGeneration.get() == this);
            }
        }

        private void cancel() {
            List<LoadClaim> claims;
            synchronized (this.lifecycleLock) {
                if (this.cancelled) return;
                this.cancelled = true;
                claims = new ArrayList<>(this.inFlight.values());
                this.inFlight.clear();
                this.cached.clear();
                this.invalidLinks.clear();
            }
            for (LoadClaim claim : claims) claim.cancel(true);
        }

    }

    private static final class LoadClaim implements Runnable {

        private final Generation generation;
        private final String placeholder;
        private final String link;
        private final AtomicBoolean started = new AtomicBoolean();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicReference<Thread> runner = new AtomicReference<>();
        private final WebTextPlaceholderCache owner;

        private LoadClaim(@NotNull WebTextPlaceholderCache owner, @NotNull Generation generation, @NotNull String placeholder, @NotNull String link) {
            this.owner = owner;
            this.generation = generation;
            this.placeholder = placeholder;
            this.link = link;
        }

        @Override
        public void run() {
            if (!this.started.compareAndSet(false, true)) return;
            if (this.cancelled.get()) return;
            Thread currentThread = Thread.currentThread();
            if (!this.runner.compareAndSet(null, currentThread)) return;
            try {
                if (!this.cancelled.get()) this.owner.load(this);
            } finally {
                this.runner.compareAndSet(currentThread, null);
            }
        }

        private Generation generation() {
            return this.generation;
        }

        private String placeholder() {
            return this.placeholder;
        }

        private String link() {
            return this.link;
        }

        private boolean isCancelled() {
            return this.cancelled.get();
        }

        private void cancel(boolean mayInterruptIfRunning) {
            this.cancelled.set(true);
            Thread runningThread = this.runner.get();
            if (mayInterruptIfRunning && (runningThread != null)) runningThread.interrupt();
        }

    }

}
