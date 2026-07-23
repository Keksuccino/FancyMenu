package de.keksuccino.fancymenu.customization.customlocals;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;

final class CustomLocalizationStorage {

    private volatile Map<String, String> localizations = Map.of();

    void reload(@NotNull Path rootDirectory, @Nullable String selectedLocale, @NotNull CustomLocalizationLoader.LoadErrorHandler errorHandler) {
        // Publish only the complete immutable result so placeholder evaluation cannot observe a half-loaded locale during a resource reload.
        this.localizations = CustomLocalizationLoader.load(rootDirectory, selectedLocale, errorHandler);
    }

    void clear() {
        this.localizations = Map.of();
    }

    @NotNull
    String localize(@NotNull String key) {
        return this.localizations.getOrDefault(key, key);
    }
}
