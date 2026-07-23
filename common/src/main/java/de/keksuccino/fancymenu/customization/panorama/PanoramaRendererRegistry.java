package de.keksuccino.fancymenu.customization.panorama;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Owns the renderers currently published by {@link PanoramaHandler} and replaces them as one lifecycle operation.
 * Detached values are closed while the old snapshot is still published, so readers can never observe a registry
 * containing a renderer whose cleanup has not at least been attempted.
 */
final class PanoramaRendererRegistry<R extends AutoCloseable> implements AutoCloseable {

    private final Map<String, R> renderers = new LinkedHashMap<>();
    private final BiConsumer<? super R, ? super Throwable> closeFailureHandler;
    private boolean closed;

    PanoramaRendererRegistry(@NotNull BiConsumer<? super R, ? super Throwable> closeFailureHandler) {
        this.closeFailureHandler = Objects.requireNonNull(closeFailureHandler);
    }

    @Nullable
    synchronized R get(@NotNull String name) {
        return this.renderers.get(Objects.requireNonNull(name));
    }

    synchronized @NotNull List<R> values() {
        return new ArrayList<>(this.renderers.values());
    }

    synchronized @NotNull List<String> names() {
        return new ArrayList<>(this.renderers.keySet());
    }

    synchronized boolean contains(@NotNull String name) {
        return this.renderers.containsKey(Objects.requireNonNull(name));
    }

    /**
     * Takes ownership of every supplied registration. Duplicate names keep the last renderer, while every displaced
     * staged renderer and every detached current renderer is closed exactly once by identity. An identical renderer
     * retained by the replacement snapshot remains open so aliases and intentional cache reuse stay valid.
     */
    synchronized void replaceAll(@NotNull Collection<Registration<R>> replacements) {
        Objects.requireNonNull(replacements);
        Map<String, R> nextRenderers = new LinkedHashMap<>();
        List<R> suppliedRenderers = new ArrayList<>(replacements.size());
        for (Registration<R> registration : replacements) {
            Registration<R> checkedRegistration = Objects.requireNonNull(registration);
            R renderer = Objects.requireNonNull(checkedRegistration.renderer());
            suppliedRenderers.add(renderer);
            nextRenderers.put(Objects.requireNonNull(checkedRegistration.name()), renderer);
        }

        if (this.closed) {
            this.closeDetached(suppliedRenderers, Set.of(), newIdentitySet());
            return;
        }

        Set<R> retainedRenderers = newIdentitySet();
        retainedRenderers.addAll(nextRenderers.values());
        Set<R> closedRenderers = newIdentitySet();
        this.closeDetached(this.renderers.values(), retainedRenderers, closedRenderers);
        this.closeDetached(suppliedRenderers, retainedRenderers, closedRenderers);

        this.renderers.clear();
        this.renderers.putAll(nextRenderers);
    }

    /** Releases staged registrations when their producer fails before {@link #replaceAll(Collection)} takes ownership. */
    synchronized void discardUnpublished(@NotNull Collection<Registration<R>> registrations) {
        Objects.requireNonNull(registrations);
        List<R> unpublishedRenderers = new ArrayList<>(registrations.size());
        for (Registration<R> registration : registrations) {
            unpublishedRenderers.add(Objects.requireNonNull(registration).renderer());
        }
        Set<R> publishedRenderers = newIdentitySet();
        publishedRenderers.addAll(this.renderers.values());
        this.closeDetached(unpublishedRenderers, publishedRenderers, newIdentitySet());
    }

    @Override
    public synchronized void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        Set<R> closedRenderers = newIdentitySet();
        this.closeDetached(this.renderers.values(), Set.of(), closedRenderers);
        this.renderers.clear();
    }

    private void closeDetached(Collection<R> candidates, Set<R> retainedRenderers, Set<R> closedRenderers) {
        for (R renderer : candidates) {
            if (retainedRenderers.contains(renderer) || !closedRenderers.add(renderer)) {
                continue;
            }
            try {
                renderer.close();
            } catch (Throwable throwable) {
                try {
                    this.closeFailureHandler.accept(renderer, throwable);
                } catch (Throwable ignored) {
                    // Cleanup must continue even when a caller-provided failure reporter is itself broken.
                }
            }
        }
    }

    private static <R> Set<R> newIdentitySet() {
        return java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    }

    record Registration<R>(@NotNull String name, @NotNull R renderer) {

        Registration {
            Objects.requireNonNull(name);
            Objects.requireNonNull(renderer);
        }

    }

}
