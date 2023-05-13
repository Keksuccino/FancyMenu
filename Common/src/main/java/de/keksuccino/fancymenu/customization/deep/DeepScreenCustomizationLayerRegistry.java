package de.keksuccino.fancymenu.customization.deep;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DeepScreenCustomizationLayerRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, DeepScreenCustomizationLayer<?>> LAYERS = new HashMap<>();

    public static void register(@NotNull DeepScreenCustomizationLayer<?> layer) {
        Objects.requireNonNull(layer.getTargetMenuIdentifier(), "[FANCYMENU] Failed to register DeepScreenCustomizationLayer! Identifier was NULL!");
        if (LAYERS.containsKey(layer.getTargetMenuIdentifier())) {
            LOGGER.warn("[FANCYMENU] DeepScreenCustomizationLayer with identifier '" + layer.getTargetMenuIdentifier() + "' already registered! Overriding layer!");
        }
        LAYERS.put(layer.getTargetMenuIdentifier(), layer);
    }

    public static void unregister(@NotNull String menuIdentifier) {
        LAYERS.remove(menuIdentifier);
    }

    @NotNull
    public static List<DeepScreenCustomizationLayer<?>> getLayers() {
        return new ArrayList<>(LAYERS.values());
    }

    @Nullable
    public static DeepScreenCustomizationLayer<?> getLayer(@NotNull String menuIdentifier) {
        return LAYERS.get(menuIdentifier);
    }

    public boolean hasLayer(@NotNull String menuIdentifier) {
        return LAYERS.containsKey(menuIdentifier);
    }

}
