package de.keksuccino.fancymenu.customization.action;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public interface ValuePlaceholderHolder {

    public static final String VALUE_PLACEHOLDER_PREFIX = "$$";

    void addValuePlaceholder(@NotNull String placeholder, @NotNull Supplier<String> replaceWithSupplier);

    @NotNull
    Map<String, Supplier<String>> getValuePlaceholders();

    /**
     * Replaces value placeholders in a deterministic order so placeholders that share prefixes
     * (for example {@code $$video_source} and {@code $$video_source_type}) resolve correctly.
     */
    @NotNull
    static String applyValuePlaceholders(@NotNull String value, @NotNull Map<String, Supplier<String>> placeholders) {
        if (placeholders.isEmpty()) return value;
        String resolved = value;
        List<Map.Entry<String, Supplier<String>>> entries = new ArrayList<>(placeholders.entrySet());
        entries.sort(Comparator.comparingInt((Map.Entry<String, Supplier<String>> e) -> e.getKey().length()).reversed());
        for (Map.Entry<String, Supplier<String>> entry : entries) {
            String replaceWith = entry.getValue().get();
            if (replaceWith == null) replaceWith = "";
            resolved = resolved.replace(VALUE_PLACEHOLDER_PREFIX + entry.getKey(), replaceWith);
        }
        return resolved;
    }

}
