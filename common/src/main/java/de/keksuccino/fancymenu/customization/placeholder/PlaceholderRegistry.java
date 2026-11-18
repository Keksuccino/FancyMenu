//Copyright (c) 2022-2025 Keksuccino.
//This code is licensed under DSMSLv3.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.customization.placeholder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/**
 * Placeholders get registered during startup, but get looked up by parsers on the render thread and on Async Ticker threads.
 * Registration copies the current state, changes the copy while holding a lock and publishes it as one immutable snapshot
 * through a volatile field, so lookups never take a lock and always see a complete registry.
 */
public class PlaceholderRegistry {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Object REGISTRATION_LOCK = new Object();

    private static volatile Snapshot snapshot = new Snapshot(0L, new HashMap<>(), List.of());

    public static void register(@NotNull Placeholder placeholder) {
        String identifier = Objects.requireNonNull(placeholder.getIdentifier());
        synchronized (REGISTRATION_LOCK) {
            Snapshot current = snapshot;
            LookupEntry existing = current.lookup().get(identifier);
            if ((existing != null) && !existing.alternative()) {
                throw new IllegalStateException("[FANCYMENU] A placeholder with the identifier '" + identifier + "' is already registered!");
            }
            RegisteredPlaceholder registered = new RegisteredPlaceholder(placeholder, readValueNames(placeholder));
            HashMap<String, LookupEntry> lookup = new HashMap<>(current.lookup());
            // Main identifiers always win over alternative identifiers, so they replace an alternative identifier of an earlier placeholder.
            lookup.put(identifier, new LookupEntry(registered, false));
            List<String> alternatives = placeholder.getAlternativeIdentifiers();
            if (alternatives != null) {
                LookupEntry alternative = new LookupEntry(registered, true);
                // The first registered placeholder keeps an alternative identifier, and main identifiers are never replaced by one.
                for (String alternativeIdentifier : alternatives) {
                    if (alternativeIdentifier != null) lookup.putIfAbsent(alternativeIdentifier, alternative);
                }
            }
            List<Placeholder> placeholders = new ArrayList<>(current.placeholders().size() + 1);
            placeholders.addAll(current.placeholders());
            placeholders.add(placeholder);
            snapshot = new Snapshot(current.revision() + 1L, lookup, List.copyOf(placeholders));
        }
        placeholder.onRegistered();
    }

    @NotNull
    public static List<Placeholder> getPlaceholders() {
        return new ArrayList<>(snapshot.placeholders());
    }

    /**
     * Returns the placeholder with the given main identifier or, if there is none, the first registered placeholder that has it as alternative identifier.
     */
    @Nullable
    public static Placeholder getPlaceholder(@Nullable String identifier) {
        if (identifier == null) return null;
        LookupEntry entry = snapshot.lookup().get(identifier);
        return (entry != null) ? entry.registered().placeholder() : null;
    }

    /**
     * Returns the value names of the placeholder. They get read once when the placeholder gets registered,
     * so parsing does not call {@link Placeholder#getValueNames()} again. Placeholders that are not registered get asked directly.
     *
     * @return An immutable list, empty if the placeholder has no values.
     */
    @NotNull
    static List<String> getValueNames(@NotNull Placeholder placeholder) {
        String identifier = placeholder.getIdentifier();
        if (identifier != null) {
            LookupEntry entry = snapshot.lookup().get(identifier);
            if ((entry != null) && (entry.registered().placeholder() == placeholder) && (entry.registered().valueNames() != null)) {
                return entry.registered().valueNames();
            }
        }
        return copyValueNames(placeholder.getValueNames());
    }

    /**
     * Increases with every registration, so caches that resolved placeholder identifiers can tell when they need to resolve them again.
     */
    static long getRevision() {
        return snapshot.revision();
    }

    /**
     * @return The value names, or null if the placeholder failed to return them, so they get read again when they are needed.
     */
    @Nullable
    private static List<String> readValueNames(@NotNull Placeholder placeholder) {
        try {
            return copyValueNames(placeholder.getValueNames());
        } catch (RuntimeException ex) {
            LOGGER.warn("[FANCYMENU] Failed to read the value names of placeholder '" + placeholder.getIdentifier() + "' while registering it! They get read again when the placeholder gets parsed.", ex);
            return null;
        }
    }

    @NotNull
    private static List<String> copyValueNames(@Nullable List<String> valueNames) {
        if ((valueNames == null) || valueNames.isEmpty()) return List.of();
        List<String> copy = new ArrayList<>(valueNames.size());
        for (String name : valueNames) {
            if (name != null) copy.add(name);
        }
        return List.copyOf(copy);
    }

    /**
     * @param valueNames Null if the placeholder failed to return its value names while it got registered.
     */
    private record RegisteredPlaceholder(@NotNull Placeholder placeholder, @Nullable List<String> valueNames) {
    }

    private record LookupEntry(@NotNull RegisteredPlaceholder registered, boolean alternative) {
    }

    /**
     * Never changed after it got published. The lookup map holds main and alternative identifiers.
     */
    private record Snapshot(long revision, @NotNull Map<String, LookupEntry> lookup, @NotNull List<Placeholder> placeholders) {
    }

}
